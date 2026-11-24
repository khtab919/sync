from django.db import migrations, models

def classify(apps, schema_editor):
    db = schema_editor.connection.alias
    def rows(name):
        return apps.get_model("core", name).objects.using(db)
    for wallet in rows("Wallet").all().iterator():
        uid = wallet.user_id
        # Manual receipts have no approval timestamp in older releases. Count
        # these deposits as restricted; never infer earnings from their balance.
        events = []
        def add(qs, when, value, bucket):
            for at, coins in qs.values_list(when, value):
                if at is not None:
                    events.append((at, bucket, coins))
        operations = rows("FinancialOperation").filter(status="settled")
        sales = operations.filter(creator_id=uid)
        eligible = ("booking", "live_gift")
        add(sales.filter(kind__in=eligible),"settled_at","creator_cents","earned")
        add(sales.exclude(kind__in=eligible),"settled_at","creator_cents","restricted")
        add(operations.filter(buyer_id=uid),"settled_at","amount_cents","spend")
        add(rows("ChatCharge").filter(creator_id=uid),"created_at","creator_cents","restricted")
        add(rows("ChatCharge").filter(buyer_id=uid),"created_at","amount_cents","spend")
        add(rows("Withdrawal").filter(user_id=uid,status="paid"),"processed_at","amount_cents","payout")
        add(rows("BalanceAdjustment").filter(user_id=uid),"created_at","amount_cents","restricted")
        for gateway in ("GatewayInvoice","StripeCheckout"):
            add(rows(gateway).filter(quote__user_id=uid),"credited_at","quote__amount_cents","restricted")
        for at,cost,prize in rows("PaidSpin").filter(user_id=uid).values_list("created_at","cost_cents","prize_cents"):
            events.append((at,"spin_cost",cost))
            events.append((at,"spin_prize",prize))
        earned = restricted = 0
        for _,kind,coins in sorted(events):
            if kind == "earned": earned += coins
            elif kind in ("restricted","spin_prize"): restricted += coins
            elif kind == "payout":
                remainder = max(0, coins-earned)
                earned = max(0, earned-coins)
                restricted = max(0, restricted-remainder)
            else:
                earned = max(0, earned-max(0,coins-restricted))
                restricted = max(0, restricted-coins)
        wallet.earned_coins = min(wallet.balance_cents, earned)
        wallet.save(update_fields=["earned_coins"])

class Migration(migrations.Migration):
    dependencies = [("core","0045_royal_voice_rooms")]
    operations = [
        migrations.AddField(model_name="wallet",name="earned_coins",
                            field=models.PositiveBigIntegerField(default=0,editable=False)),
        migrations.RunPython(classify,migrations.RunPython.noop),
    ]
