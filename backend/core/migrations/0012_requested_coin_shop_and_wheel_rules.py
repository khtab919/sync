from django.db import migrations


def configure(apps,schema_editor):
    PaymentSettings=apps.get_model('core','PaymentSettings')
    PaymentSettings.objects.get_or_create(pk=1,defaults={'shamcash_enabled':True,'shamcash_account':'42c9f2bd84250c5ab6c444616b8ef0c2','shamcash_currency':'USD','shamcash_rate':1,'shamcash_instructions':'حوّل بالدولار إلى الحساب المعروض، ثم أرفق إشعار الدفع للتحقق اليدوي.'})
    Wheel=apps.get_model('core','PaidWheelConfiguration')
    values={'cost_cents':350,'price_step_cents':50,'maximum_cost_cents':1000,'first_reward_cents':500,'grand_unlock_spend_cents':20000,'prizes':[{'amount_cents':0,'weight':500},{'amount_cents':100,'weight':120},{'amount_cents':300,'weight':100},{'amount_cents':500,'weight':160},{'amount_cents':1000,'weight':100},{'amount_cents':5000,'weight':18},{'amount_cents':10000,'weight':2}]}
    row,_=Wheel.objects.get_or_create(pk=1)
    for key,value in values.items():setattr(row,key,value)
    # Preserve operator-controlled activation and reserve. No money is seeded.
    row.save(update_fields=list(values))


class Migration(migrations.Migration):
    dependencies=[('core','0011_paymentsettings_topupquote_message_client_key_and_more')]
    operations=[migrations.RunPython(configure,migrations.RunPython.noop)]
