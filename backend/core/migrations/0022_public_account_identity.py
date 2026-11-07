from django.db import migrations,models
import core.identity

def backfill(apps,schema_editor):
    import secrets
    Profile=apps.get_model('core','Profile');Registry=apps.get_model('core','IdentityRegistry')
    Registry.objects.get_or_create(pk=1)
    used=set(Profile.objects.exclude(public_id__isnull=True).values_list('public_id',flat=True))
    targets=list(Profile.objects.filter(public_id__isnull=True).values_list('pk',flat=True))
    available=[str(n) for n in range(10000,100000) if str(n) not in used]
    if len(targets)>len(available):raise RuntimeError('Five-digit account capacity exceeded; no account data was changed.')
    for profile in Profile.objects.filter(display_name='').select_related('user').iterator():
        Profile.objects.filter(pk=profile.pk).update(display_name=profile.user.username)
    assigned=secrets.SystemRandom().sample(available,len(targets))
    for pk,value in zip(targets,assigned):Profile.objects.filter(pk=pk).update(public_id=value)

class Migration(migrations.Migration):
    dependencies=[('core','0021_adminevent_details')]
    operations=[migrations.CreateModel(name='IdentityRegistry',fields=[('id',models.PositiveSmallIntegerField(default=1,primary_key=True,serialize=False))]),migrations.AddField(model_name='profile',name='public_id',field=models.CharField(max_length=5,null=True,unique=True)),migrations.RunPython(backfill,migrations.RunPython.noop),migrations.AlterField(model_name='profile',name='public_id',field=models.CharField(default=core.identity.allocate_public_id,max_length=5,unique=True))]
