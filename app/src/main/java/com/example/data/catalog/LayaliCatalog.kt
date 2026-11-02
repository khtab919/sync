package com.example.data.catalog

import com.example.data.model.CityInfo
import com.example.data.model.Venue

object LayaliCatalog {

    val countries = listOf("الكل", "سوريا", "تركيا")

    val categories = listOf(
        "الكل",
        "سهرات فنية",
        "سبا ومساج",
        "زيارات خارجية",
        "حمام ملكي"
    )

    val cities: List<CityInfo> = listOf(
        // المدن السورية
        CityInfo("aleppo", "حلب", "سوريا", "طرب القدود الحلبية وحمامات الغار الملكية", 0),
        CityInfo("damascus", "دمشق", "سوريا", "عراقة الياسمين والسهرات الشامية الأصيلة", 0),
        CityInfo("latakia", "اللاذقية", "سوريا", "إطلالات المتوسط ومنتجعات الاستجمام البحرية", 0),
        CityInfo("tartus", "طرطوس", "سوريا", "نسيم البحر والسهرات الساحلية الهادئة", 0),
        CityInfo("homs", "حمص", "سوريا", "ليالي العاصي ومراكز العناية المتكاملة", 0),
        CityInfo("hama", "حماة", "سوريا", "سحر النواعير والمنتجعات التراثية", 0),
        // المدن التركية
        CityInfo("istanbul", "إسطنبول", "تركيا", "سهرات البوسفور الساحرة وأفخم مراكز السبا", 0),
        CityInfo("antalya", "أنطاليا", "تركيا", "منتجعات الريفييرا التركية ولاونجات المارينا", 0),
        CityInfo("ankara", "أنقرة", "تركيا", "الفخامة الكلاسيكية ومراكز المساج العلاجية", 0),
        CityInfo("izmir", "إزمير", "تركيا", "ليالي بحر إيجة وإطلالات الكوردون الراقية", 0),
        CityInfo("gaziantep", "غازي عنتاب", "تركيا", "الأصالة الشرقية والحمامات التركية العريقة", 0),
        CityInfo("mersin", "مرسين", "تركيا", "لاونجات المارينا البحرية ومنتجعات الاسترخاء", 0),
        CityInfo("bursa", "بورصة", "تركيا", "ينابيع المياه الكبريتية الحارة والسبا الجبلي", 0)
    )

    val defaultTimeSlots = listOf(
        "05:00 مساءً",
        "07:00 مساءً",
        "08:30 مساءً",
        "10:00 مساءً",
        "11:30 مساءً"
    )

    // Listings are loaded only from real published Firestore records.
    val venues: List<Venue> = emptyList()
}
