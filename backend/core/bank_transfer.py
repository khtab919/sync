from datetime import datetime
from decimal import Decimal, InvalidOperation
from urllib.error import URLError
from urllib.request import urlopen
from xml.etree import ElementTree
from zoneinfo import ZoneInfo

from django.core.cache import cache
from .views import ApiError

IBAN = "TR850082900009491652391003"
BENEFICIARY = "Luay Elomar"

def transfer_rate():
    today = datetime.now(ZoneInfo("Europe/Istanbul")).date()
    key = "banktr:tcmb:selling:" + today.isoformat()
    saved = cache.get(key)
    if saved:
        return Decimal(saved)
    try:
        with urlopen("https://www.tcmb.gov.tr/kurlar/today.xml", timeout=5) as response:
            bulletin = ElementTree.fromstring(response.read(262144))
        date = datetime.strptime(bulletin.attrib["Tarih"], "%d.%m.%Y").date()
        if not 0 <= (today-date).days <= 7:
            raise ValueError("outdated bulletin")
        rate = Decimal(bulletin.findtext("./Currency[@CurrencyCode='USD']/ForexSelling") or "")
        if not rate.is_finite() or rate <= 0:
            raise ValueError("invalid quote")
    except (URLError, OSError, ValueError, InvalidOperation, KeyError, ElementTree.ParseError) as exc:
        raise ApiError("bank_exchange_rate_unavailable", 503) from exc
    cache.set(key, str(rate), 900)
    return rate
