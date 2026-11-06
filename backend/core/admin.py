from django.contrib import admin
from django import forms
from django.db import transaction
from django.contrib.auth import get_user_model
from django.contrib.auth.admin import UserAdmin
from django.urls import path, reverse
from django.shortcuts import get_object_or_404
from django.template.response import TemplateResponse
from django.utils.html import format_html
from .models import BalanceAdjustment, SessionToken
from .models import Profile, Service, Booking, Wallet, Withdrawal, Reel, Report, SupportTicket

@admin.register(Profile)
class ProfileAdmin(admin.ModelAdmin):
    list_display = ('user', 'creator')
    search_fields = ('user__username', 'user__email')
    list_filter = ('creator',)
    exclude = ('identity_verified',)
    readonly_fields = ('date_of_birth', 'policy_version', 'policy_accepted_at')

@admin.register(Service)
class ServiceAdmin(admin.ModelAdmin):
    list_display = ('title', 'owner', 'approved', 'active')
    readonly_fields = ('owner', 'price_cents', 'created_at')

@admin.register(Reel)
class ReelAdmin(admin.ModelAdmin):
    list_display = ('id', 'owner', 'approved')

@admin.register(Report)
class ReportAdmin(admin.ModelAdmin):
    list_display = ('id', 'target_type', 'target_id', 'resolved')
    readonly_fields = ('reporter', 'target_type', 'target_id', 'reason', 'created_at')

class FinancialReadOnlyAdmin(admin.ModelAdmin):
    def has_add_permission(self, request): return False
    def has_change_permission(self, request, obj=None): return False
    def has_delete_permission(self, request, obj=None): return False

admin.site.register(Booking, FinancialReadOnlyAdmin)
class CreditForm(forms.Form):
    amount_cents = forms.IntegerField(min_value=1, max_value=100000000, label='المبلغ بالسنت — 100 سنت = 1 دولار')
    reason = forms.CharField(max_length=300, label='سبب إضافة الرصيد')

@admin.register(Wallet)
class WalletAdmin(FinancialReadOnlyAdmin):
    list_display = ('user', 'balance_cents', 'reserved_cents', 'credit_link')
    search_fields = ('user__username', 'user__email')

    def credit_link(self, obj):
        return format_html('<a href="{}">إضافة رصيد</a>', reverse('admin:core_wallet_credit', args=[obj.pk]))
    credit_link.short_description = 'الرصيد'

    def get_urls(self):
        return [path('<int:wallet_id>/credit/', self.admin_site.admin_view(self.credit), name='core_wallet_credit')] + super().get_urls()

    def credit(self, request, wallet_id):
        from django.core.exceptions import PermissionDenied
        if not request.user.is_superuser:
            raise PermissionDenied
        wallet = get_object_or_404(Wallet, pk=wallet_id)
        form = CreditForm(request.POST or None)
        if request.method == 'POST' and form.is_valid():
            with transaction.atomic():
                wallet = Wallet.objects.select_for_update().get(pk=wallet_id)
                wallet.balance_cents += form.cleaned_data['amount_cents']
                wallet.save(update_fields=['balance_cents'])
                entry = BalanceAdjustment.objects.create(user=wallet.user, administrator=request.user, **form.cleaned_data)
                self.log_change(request, wallet, 'Credit adjustment #' + str(entry.pk))
            from django.shortcuts import redirect
            self.message_user(request, 'تمت إضافة الرصيد وتسجيل العملية.')
            return redirect('admin:core_wallet_changelist')
        return TemplateResponse(request, 'admin/core/credit.html', dict(self.admin_site.each_context(request), form=form, wallet=wallet, title='إضافة رصيد', opts=self.model._meta))

@admin.register(BalanceAdjustment)
class BalanceAdjustmentAdmin(FinancialReadOnlyAdmin):
    list_display = ('user', 'amount_cents', 'administrator', 'reason', 'created_at')
    search_fields = ('user__username', 'user__email', 'reason')

User = get_user_model()
admin.site.unregister(User)
@admin.register(User)
class AccountAdmin(UserAdmin):
    list_display = ('username', 'email', 'is_active', 'is_staff', 'preview_link')
    actions = ('ban_accounts', 'unban_accounts')

    @admin.action(description='حظر الحسابات المحددة')
    def ban_accounts(self, request, queryset):
        queryset = queryset.exclude(pk=request.user.pk).filter(is_superuser=False)
        with transaction.atomic():
            for user in queryset:
                self.log_change(request, user, 'Account banned')
            SessionToken.objects.filter(user__in=queryset).delete()
            queryset.update(is_active=False)

    @admin.action(description='إلغاء حظر الحسابات المحددة')
    def unban_accounts(self, request, queryset):
        for user in queryset:
            self.log_change(request, user, 'Account unbanned')
        queryset.update(is_active=True)

    def preview_link(self, obj):
        return format_html('<a href="{}">معاينة الحساب</a>', reverse('admin:core_account_preview', args=[obj.pk]))
    preview_link.short_description = 'معاينة'

    def get_urls(self):
        return [path('<int:user_id>/preview/', self.admin_site.admin_view(self.preview), name='core_account_preview')] + super().get_urls()

    def preview(self, request, user_id):
        from django.core.exceptions import PermissionDenied
        if not self.has_view_permission(request):
            raise PermissionDenied
        account = get_object_or_404(User, pk=user_id)
        return TemplateResponse(request, 'admin/core/account_preview.html', dict(self.admin_site.each_context(request), title='معاينة الحساب', account=account, profile=Profile.objects.filter(user=account).first(), wallet=Wallet.objects.filter(user=account).first(), offers=Service.objects.filter(owner=account), reels=Reel.objects.filter(owner=account)))
admin.site.register(Withdrawal, FinancialReadOnlyAdmin)


@admin.register(SupportTicket)
class SupportTicketAdmin(admin.ModelAdmin):
    list_display = ('id', 'user', 'created_at')
    readonly_fields = ('user', 'body', 'created_at')
    def has_add_permission(self, request): return False
    def has_delete_permission(self, request, obj=None): return False

admin.site.site_header = "Relaxia · الإدارة"
admin.site.site_title = "Relaxia"
admin.site.index_title = "إدارة الحسابات والمحتوى"


from .models import TopUp, SellerApplication, PaidWheelConfiguration, PaidSpin, UploadedMedia
admin.site.register(TopUp, FinancialReadOnlyAdmin)
admin.site.register(SellerApplication, FinancialReadOnlyAdmin)
admin.site.register(PaidSpin, FinancialReadOnlyAdmin)

@admin.register(PaidWheelConfiguration)
class PaidWheelConfigurationAdmin(admin.ModelAdmin):
    list_display = ('enabled', 'cost_cents', 'bankroll_cents')
    def has_module_permission(self, request): return request.user.is_superuser
    def has_view_permission(self, request, obj=None): return request.user.is_superuser
    def has_add_permission(self, request): return request.user.is_superuser and not PaidWheelConfiguration.objects.exists()
    def has_change_permission(self, request, obj=None): return request.user.is_superuser
    def has_delete_permission(self, request, obj=None): return False
    def get_form(self, request, obj=None, **kwargs):
        base = super().get_form(request, obj, **kwargs)
        from .commerce import wheel_settings
        class CheckedForm(base):
            def clean(self):
                data = super().clean()
                candidate = PaidWheelConfiguration(cost_cents=data.get('cost_cents') or 0, prizes=data.get('prizes') or [])
                if data.get('enabled') and not wheel_settings(candidate)[0]:
                    raise forms.ValidationError('حدد سعرًا موجبًا وجوائز بصيغة amount_cents وweight قبل التفعيل.')
                return data
        return CheckedForm
