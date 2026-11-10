"""Persist once-only awards. Never award for merely opening a screen."""
from django.db.models.signals import post_save
from django.dispatch import receiver
from django.db import transaction
from django.db.models import Sum
from django.utils import timezone
from .models import Reel, Service, Booking, LiveBroadcast, ExperienceAward


def award(user_id, source, points):
    from django.contrib.auth import get_user_model
    with transaction.atomic():
        get_user_model().objects.select_for_update().get(pk=user_id)
        if ExperienceAward.objects.filter(user_id=user_id,source=source).exists():return
        used=ExperienceAward.objects.filter(user_id=user_id,created_at__date=timezone.localdate()).aggregate(n=Sum('points'))['n'] or 0
        points=min(points,max(0,200-used))
        # Record exhausted events too: replaying them tomorrow must not earn XP.
        ExperienceAward.objects.create(user_id=user_id,source=source,points=points)

@receiver(post_save,sender=Reel)
@receiver(post_save,sender=Service)
@receiver(post_save,sender=Booking)
@receiver(post_save,sender=LiveBroadcast)
def activity(sender,instance,raw=False,**kwargs):
    if raw:return
    # Publish alone earns nothing; validated engagement is needed.
    if sender==Booking and instance.status=='completed' and instance.buyer_id!=instance.service.owner_id:
        award(instance.service.owner_id,'booking:'+str(instance.pk),80)
    if sender==LiveBroadcast and instance.status=='ended' and instance.started_at and instance.ended_at and (instance.ended_at-instance.started_at).total_seconds()>=300:
        if instance.liveadmission_set.exclude(user_id=instance.owner_id).exists() if hasattr(instance,'liveadmission_set') else False:
            award(instance.owner_id,'live:'+str(instance.pk),40)

from .models import Message, InternalCall, Withdrawal, TopUp
from .push import enqueue

@receiver(post_save,sender=Message)
def message_event(sender,instance,created=False,raw=False,**kwargs):
    if created and not raw and not instance.call_id:
        enqueue(instance.recipient_id,'message:'+str(instance.pk),'messages',instance.sender_id,'رسالة جديدة على Relaxia')

@receiver(post_save,sender=InternalCall)
def call_event(sender,instance,created=False,raw=False,**kwargs):
    if created and not raw and instance.status=='ringing':
        enqueue(instance.callee_id,'call:'+str(instance.pk),'calls',instance.pk,'مكالمة واردة على Relaxia',45)

@receiver(post_save,sender=Booking)
def booking_event(sender,instance,raw=False,**kwargs):
    if not raw:
        for uid in {instance.buyer_id,instance.service.owner_id}:
            enqueue(uid,'booking:'+str(instance.pk)+':'+instance.status,'bookings',instance.pk,'تحديث على حجزك')

@receiver(post_save,sender=Withdrawal)
@receiver(post_save,sender=TopUp)
def money_event(sender,instance,raw=False,**kwargs):
    if not raw:enqueue(instance.user_id,sender.__name__+':'+str(instance.pk)+':'+instance.status,'wallet',instance.pk,'تحديث على محفظتك')

from django.db.models.signals import pre_save
@receiver(pre_save,sender=LiveBroadcast)
def live_transition(sender,instance,raw=False,**kwargs):
    instance._notify_live=not raw and instance.status=='active' and not LiveBroadcast.objects.filter(pk=instance.pk,status='active').exists()

@receiver(post_save,sender=LiveBroadcast)
def live_event(sender,instance,raw=False,**kwargs):
    if getattr(instance,'_notify_live',False) and not instance.private and not instance.invite_only:
        from .models import Follow
        from .paging import visible_owners
        for uid in Follow.objects.filter(creator_id=instance.owner_id).values_list('user_id',flat=True).iterator():
            from django.contrib.auth import get_user_model
            viewer=get_user_model().objects.get(pk=uid)
            if instance.owner_id not in visible_owners(viewer):enqueue(uid,'live:'+str(instance.pk),'live',instance.pk,'بدأ بث حساب تتابعه',300)
