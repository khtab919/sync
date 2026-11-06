from django.contrib import admin
from .models import Profile, Service, Booking, Wallet, Withdrawal, Reel, Report

@admin.register(Profile)
class ProfileAdmin(admin.ModelAdmin):
    list_display = ('user', 'creator', 'identity_verified')
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
admin.site.register(Wallet, FinancialReadOnlyAdmin)
admin.site.register(Withdrawal, FinancialReadOnlyAdmin)
