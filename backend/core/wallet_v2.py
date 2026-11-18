"""USDT withdrawals: administrative approval, provider verification, audited settlement.

Never allow a client-side "paid" checkbox to debit a wallet. The balance is
settled only once, after a signed NOWPayments payout notification.
"""
import hashlib
import hmac
import json
import os
import re
import uuid
from decimal import Decimal, InvalidOperation
from urllib.request import Request, urlopen

from django.db import transaction
from django.http import HttpResponse, JsonResponse
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt

from .models import UserNotification, Wallet, Withdrawal, Profile
from .views import endpoint, ApiError, text, amount, User
from .mobile_admin import require_admin
from .admin_security import event, scopes

NETWORK_PATTERNS = {
    "usdttrc20": r"T[1-9A-HJ-NP-Za-km-z]{33}",
    "usdtbsc": r"0x[a-fA-F0-9]{40}",
}


def valid_address(network, address):
    return network in NETWORK_PATTERNS and bool(re.fullmatch(NETWORK_PATTERNS[network], address))


def notice(user, title, body, kind="wallet", reference=""):
    return UserNotification.objects.create(
        user=user, title=title, body=body, kind=kind, reference=str(reference)[:100]
    )


def finance_notice(title, body, reference):
    # Each eligible administrator sees a private notice, not a public feed entry.
    for admin in User.objects.filter(is_staff=True, is_active=True).iterator():
        if "finance.view" in scopes(admin):
            notice(admin, title, body, "admin_withdrawal", reference)


def serialize(row):
    return {
        "id": str(row.pk), "status": row.status,
        "amount_cents": row.amount_cents,
        "network": row.network,
        "currency": "USDT",
        "destination": row.destination,
        "payout_reference": row.payout_reference,
        "provider_status": row.provider_status,
        "created_at": row.created_at,
        "processed_at": row.processed_at,
    }


@endpoint(["GET", "POST"])
def withdrawals(request):
    if request.method == "GET":
        return JsonResponse({"items": [serialize(w) for w in
            Withdrawal.objects.filter(user=request.actor).order_by("-created_at")[:100]]})
    if not Profile.objects.filter(user=request.actor, publishing_allowed=True).exists():
        raise ApiError("creator_account_required", 403)
    payload = request.payload
    key = uuid.UUID(text(payload, "request_key", 36, 36))
    cents = amount(payload, "amount_cents")
    address = text(payload, "destination", 5, 300)

    # Older installed clients did not send a network. Keep their outstanding
    # requests readable but DO NOT let an unvalidated legacy address be paid.
    network = payload.get("network", "legacy")
    if network != "legacy" and not valid_address(network, address):
        raise ApiError("invalid_withdrawal_network_or_address")
    if "network" in payload and network == "legacy":
        raise ApiError("invalid_withdrawal_network_or_address")

    with transaction.atomic():
        Wallet.objects.get_or_create(user=request.actor)
        wallet = Wallet.objects.select_for_update().get(user=request.actor)
        existing = Withdrawal.objects.filter(user=request.actor, request_key=key).first()
        if existing:
            if (existing.amount_cents, existing.destination, existing.network) != (cents, address, network):
                raise ApiError("idempotency_conflict", 409)
            return JsonResponse(serialize(existing))
        if cents > wallet.balance_cents - wallet.reserved_cents:
            raise ApiError("insufficient_funds", 409)
        wallet.reserved_cents += cents
        wallet.save(update_fields=["reserved_cents"])
        row = Withdrawal.objects.create(
            user=request.actor, request_key=key, amount_cents=cents,
            network=network, destination=address,
        )
        notice(request.actor, "طلب السحب قيد المراجعة",
               "حُجز المبلغ حتى انتهاء مراجعة الإدارة.", "withdrawal", row.pk)
        finance_notice("طلب سحب جديد", "طلب سحب USDT يحتاج إلى مراجعة.", row.pk)
    return JsonResponse(serialize(row), status=201)


def _provider(path, payload, token=None):
    headers = {"x-api-key": os.environ["NOWPAYMENTS_API_KEY"], "Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = Request("https://api.nowpayments.io/v1/" + path,
                  data=json.dumps(payload, separators=(",", ":")).encode(),
                  headers=headers, method="POST")
    with urlopen(req, timeout=15) as response:
        result = json.loads(response.read(65536))
    if not isinstance(result, dict):
        raise ValueError("invalid_provider_response")
    return result


def _auth_token():
    if not all(os.environ.get(k) for k in
               ("NOWPAYMENTS_API_KEY", "NOWPAYMENTS_PAYOUT_EMAIL", "NOWPAYMENTS_PAYOUT_PASSWORD")):
        raise ApiError("payout_gateway_not_configured", 503)
    token = _provider("auth", {
        "email": os.environ["NOWPAYMENTS_PAYOUT_EMAIL"],
        "password": os.environ["NOWPAYMENTS_PAYOUT_PASSWORD"],
    }).get("token")
    if not isinstance(token, str) or not token:
        raise ValueError("provider_auth_failed")
    return token


def _payout_create(row, callback):
    result = _provider("payout", {
        "withdrawals": [{
            "address": row.destination,
            "currency": row.network,
            "amount": float(Decimal(row.amount_cents) / 100),
            "extra_id": None,
            "ipn_callback_url": callback,
        }]
    }, _auth_token())
    batch_id = str(result.get("id") or result.get("batch_withdrawal_id") or "")
    entries = result.get("withdrawals") or []
    payout_id = str(entries[0].get("id") or "") if isinstance(entries, list) and entries and isinstance(entries[0], dict) else ""
    if not batch_id.isdigit() or not payout_id.isdigit():
        raise ValueError("provider_payout_ids_unavailable")
    return batch_id, payout_id


def _payout_verify(row, code):
    return _provider("payout/" + row.provider_batch_id + "/verify",
                     {"verification_code": code}, _auth_token())


@endpoint(["GET", "POST"])
def payouts(request):
    require_admin(request)
    if request.method == "GET":
        qs = Withdrawal.objects.select_related("user__profile").order_by("-created_at")[:100]
        return JsonResponse({"items": [
            {**serialize(row), "user_id": row.user_id,
             "name": row.user.profile.display_name or row.user.username}
            for row in qs
        ]})
    data = request.payload
    key = uuid.UUID(text(data, "id", 36, 36))
    action = text(data, "action", 4, 24)
    with transaction.atomic():
        row = Withdrawal.objects.select_for_update().filter(pk=key).first()
        if row is None:
            raise ApiError("withdrawal_unavailable", 404)
        Wallet.objects.get_or_create(user_id=row.user_id)
        wallet = Wallet.objects.select_for_update().get(user_id=row.user_id)
        if action == "approve":
            if row.status != "pending":
                raise ApiError("invalid_payout_transition", 409)
            if not valid_address(row.network, row.destination):
                raise ApiError("invalid_withdrawal_network_or_address", 409)
            if wallet.reserved_cents < row.amount_cents:
                raise ApiError("financial_review_required", 409)
            row.status = "approved"
            row.reviewed_by = request.actor
            row.reviewed_at = timezone.now()
            row.save(update_fields=["status", "reviewed_by", "reviewed_at"])
            notice(row.user, "تمت الموافقة على السحب",
                   "تمت الموافقة الإدارية وينتظر طلبك التنفيذ عبر بوابة الدفع.",
                   "withdrawal", row.pk)
        elif action == "reject":
            if row.status not in ("pending", "approved"):
                raise ApiError("invalid_payout_transition", 409)
            if wallet.reserved_cents < row.amount_cents:
                raise ApiError("financial_review_required", 409)
            wallet.reserved_cents -= row.amount_cents
            wallet.save(update_fields=["reserved_cents"])
            row.status = "rejected"
            row.reviewed_by = request.actor
            row.reviewed_at = timezone.now()
            row.processed_at = timezone.now()
            row.save(update_fields=["status", "reviewed_by", "reviewed_at", "processed_at"])
            notice(row.user, "رُفض طلب السحب", "أُعيد المبلغ المحجوز إلى الرصيد المتاح.",
                   "withdrawal", row.pk)
        elif action == "dispatch":
            if row.status != "approved" or not valid_address(row.network, row.destination):
                raise ApiError("invalid_payout_transition", 409)
            if not all(os.environ.get(k) for k in
                       ("NOWPAYMENTS_API_KEY", "NOWPAYMENTS_PAYOUT_EMAIL", "NOWPAYMENTS_PAYOUT_PASSWORD", "NOWPAYMENTS_IPN_SECRET")):
                raise ApiError("payout_gateway_not_configured", 503)
            if wallet.reserved_cents < row.amount_cents:
                raise ApiError("financial_review_required", 409)
            # Lock this operation BEFORE the external call. A timeout must never
            # cause a second payout to be issued automatically.
            row.status = "dispatching"
            row.save(update_fields=["status"])
        elif action == "verify":
            if row.status != "verification_required" or not row.provider_batch_id:
                raise ApiError("invalid_payout_transition", 409)
        else:
            raise ApiError("invalid_action")
        event(request.actor, "withdrawal_" + action, row.pk, data.get("reason", ""),
              details={"network": row.network, "amount_cents": row.amount_cents})
    if action == "dispatch":
        try:
            callback = request.build_absolute_uri("/api/nowpayments/payout-ipn")
            if not callback.startswith("https://"):
                raise ApiError("secure_callback_required", 503)
            batch, individual = _payout_create(row, callback)
        except Exception:
            Withdrawal.objects.filter(pk=row.pk, status="dispatching").update(
                status="dispatch_review", provider_status="creation_unknown")
            raise ApiError("payout_dispatch_review_required", 409)
        with transaction.atomic():
            locked = Withdrawal.objects.select_for_update().get(pk=row.pk)
            locked.provider_batch_id = batch
            locked.provider_withdrawal_id = individual
            locked.provider_status = "CREATING"
            locked.status = "verification_required"
            locked.save(update_fields=["provider_batch_id", "provider_withdrawal_id",
                                       "provider_status", "status"])
            notice(locked.user, "طلب السحب بانتظار تحقق البوابة",
                   "أُرسل الطلب إلى NOWPayments ويحتاج تأكيد البوابة.", "withdrawal", locked.pk)
            row = locked
    if action == "verify":
        code = text(data, "verification_code", 4, 20)
        if not re.fullmatch(r"[0-9]{4,20}", code):
            raise ApiError("invalid_verification_code")
        try:
            _payout_verify(row, code)
        except Exception:
            # Never mark paid or retry a provider payout because a verification call failed.
            raise ApiError("provider_verification_pending", 409)
        with transaction.atomic():
            locked = Withdrawal.objects.select_for_update().get(pk=row.pk)
            if locked.status == "verification_required":
                locked.status = "processing"
                locked.provider_status = "PROCESSING"
                locked.save(update_fields=["status", "provider_status"])
            row = locked
    return JsonResponse(serialize(row))


@csrf_exempt
def payout_ipn(request):
    if request.method != "POST":
        return HttpResponse(status=405)
    secret = os.environ.get("NOWPAYMENTS_IPN_SECRET", "")
    if not secret:
        return JsonResponse({"error": "gateway_not_configured"}, status=503)
    if len(request.body) > 65536:
        return HttpResponse(status=413)
    sig = request.headers.get("x-nowpayments-sig", "")
    if not re.fullmatch(r"[0-9a-fA-F]{128}", sig):
        return HttpResponse(status=403)
    try:
        data = json.loads(request.body)
        if not isinstance(data, dict):
            raise ValueError()
        canonical = json.dumps(data, sort_keys=True, separators=(",", ":"),
                               ensure_ascii=False, allow_nan=False).encode()
        expected = hmac.new(secret.encode(), canonical, hashlib.sha512).hexdigest()
        if not hmac.compare_digest(expected, sig.lower()):
            return HttpResponse(status=403)
        with transaction.atomic():
            provider_id = str(data.get("id", ""))
            row = Withdrawal.objects.select_for_update().filter(
                provider_withdrawal_id=provider_id
            ).first()
            if row is None:
                raise ValueError("unknown_provider_withdrawal")
            if (row.network != str(data.get("currency", "")).lower()
                    or row.destination != str(data.get("address", ""))):
                raise ValueError("payout_binding_mismatch")
            quantity = Decimal(str(data.get("amount")))
            if not quantity.is_finite() or quantity != Decimal(row.amount_cents) / 100:
                raise ValueError("payout_amount_mismatch")
            status = str(data.get("status", "")).upper()
            if row.status == "paid":
                return JsonResponse({"ok": True})
            row.provider_status = status[:32]
            if status == "FINISHED":
                txid = str(data.get("hash") or "")
                if not re.fullmatch(r"(?:0x)?[0-9a-fA-F]{64}", txid):
                    raise ValueError("missing_blockchain_hash")
                if row.status not in ("processing", "verification_required"):
                    raise ValueError("unexpected_payout_state")
                wallet = Wallet.objects.select_for_update().get(user_id=row.user_id)
                if wallet.reserved_cents < row.amount_cents or wallet.balance_cents < row.amount_cents:
                    raise ValueError("wallet_reservation_inconsistent")
                wallet.balance_cents -= row.amount_cents
                wallet.reserved_cents -= row.amount_cents
                wallet.save(update_fields=["balance_cents", "reserved_cents"])
                row.payout_reference = txid
                row.status = "paid"
                row.processed_at = timezone.now()
                notice(row.user, "اكتمل سحب USDT",
                       "اكتمل التحويل وتوفر مرجع المعاملة في سجل العمليات.",
                       "withdrawal", row.pk)
                row.save(update_fields=["provider_status", "payout_reference", "status", "processed_at"])
            else:
                if status in ("REJECTED", "REJECTED_NOT_CHECKED"):
                    row.status = "provider_review"
                    notice(row.user, "السحب يحتاج مراجعة", "يجري التحقق من حالة التحويل مع البوابة.",
                           "withdrawal", row.pk)
                    row.save(update_fields=["status", "provider_status"])
                else:
                    row.save(update_fields=["provider_status"])
    except (ValueError, TypeError, InvalidOperation):
        return JsonResponse({"error": "payout_validation_failed"}, status=400)
    return JsonResponse({"ok": True})


@endpoint(["GET", "POST"])
def notifications(request):
    if request.method == "GET":
        qs = UserNotification.objects.filter(user=request.actor)
        return JsonResponse({"unread": qs.filter(read_at__isnull=True).count(),
                             "items": list(qs.order_by("-created_at").values(
                                 "id", "kind", "title", "body", "reference",
                                 "created_at", "read_at")[:100])})
    ids = request.payload.get("ids")
    if not isinstance(ids, list) or len(ids) > 100 or any(type(i) is not int for i in ids):
        raise ApiError("invalid_notification_ids")
    updated = UserNotification.objects.filter(
        user=request.actor, id__in=ids, read_at__isnull=True
    ).update(read_at=timezone.now())
    return JsonResponse({"read": updated})
