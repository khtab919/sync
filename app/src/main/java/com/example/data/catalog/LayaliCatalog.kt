package com.example.data.catalog

import com.example.data.model.CityInfo
import com.example.data.model.CuratedReview
import com.example.data.model.ServicePackage
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
        CityInfo("aleppo", "حلب", "سوريا", "طرب القدود الحلبية وحمامات الغار الملكية", 14),
        CityInfo("damascus", "دمشق", "سوريا", "عراقة الياسمين والسهرات الشامية الأصيلة", 18),
        CityInfo("latakia", "اللاذقية", "سوريا", "إطلالات المتوسط ومنتجعات الاستجمام البحرية", 12),
        CityInfo("tartus", "طرطوس", "سوريا", "نسيم البحر والسهرات الساحلية الهادئة", 9),
        CityInfo("homs", "حمص", "سوريا", "ليالي العاصي ومراكز العناية المتكاملة", 8),
        CityInfo("hama", "حماة", "سوريا", "سحر النواعير والمنتجعات التراثية", 6),
        // المدن التركية
        CityInfo("istanbul", "إسطنبول", "تركيا", "سهرات البوسفور الساحرة وأفخم مراكز السبا", 29),
        CityInfo("antalya", "أنطاليا", "تركيا", "منتجعات الريفييرا التركية ولاونجات المارينا", 19),
        CityInfo("ankara", "أنقرة", "تركيا", "الفخامة الكلاسيكية ومراكز المساج العلاجية", 11),
        CityInfo("izmir", "إزمير", "تركيا", "ليالي بحر إيجة وإطلالات الكوردون الراقية", 13),
        CityInfo("gaziantep", "غازي عنتاب", "تركيا", "الأصالة الشرقية والحمامات التركية العريقة", 10),
        CityInfo("mersin", "مرسين", "تركيا", "لاونجات المارينا البحرية ومنتجعات الاسترخاء", 11),
        CityInfo("bursa", "بورصة", "تركيا", "ينابيع المياه الكبريتية الحارة والسبا الجبلي", 9)
    )

    val defaultTimeSlots = listOf(
        "05:00 مساءً",
        "07:00 مساءً",
        "08:30 مساءً",
        "10:00 مساءً",
        "11:30 مساءً"
    )

    val venues: List<Venue> = listOf(
        // 0-A. حلب - قصر السهرات والقدود الحلبية (بطل الشاشة الرئيسية وشاشة التفاصيل)
        Venue(
            id = "aleppo_palace_qudud_main",
            name = "قصر السهرات والقدود الحلبية",
            cityName = "حلب",
            country = "سوريا",
            neighborhood = "العزيزية - ساحة سعد الله الجابري",
            category = "سهرات فنية",
            shortDescription = "ليالي الطرب الأصيل والقدود الحلبية العريقة في قصر ملكي فاخر مع ضيافة متكاملة.",
            fullDescription = "• تهيئة الخدمة الراقية والمريحة بأعلى معايير الخصوصية والرفاهية.\n• تفاصيل القصر الملكي الشرقي الأصيل مع القاعات المذهبة والثريات الكريستالية.\n• برنامج فني يومي يجمع نخبة مطربي القدود الحلبية والموشحات الأصيلة مع مطبخ حلبي عريق.",
            ratingAverage = 4.9,
            reviewCount = 265,
            startingPriceUsd = 50,
            openingHours = "07:00 مساءً – 03:00 فجراً",
            badges = listOf("قدود حلبية", "مطبخ عريق", "مريحة وخاصة", "افتتاحات VIP"),
            accentHex = 0xFFE5B84B,
            secondaryHex = 0xFF2B1438,
            providerDisplayName = "الأميرة راما • حلب",
            isOnlineNow = true,
            visualPresetKey = "palace_hall",
            showcaseVideoTitle = "جولة فيديو 4K داخل قصر السهرات والقدود الحلبية",
            showcaseDuration = "02:30 4K HD",
            galleryHighlights = listOf(
                "القاعة الملكية الرئيسية والثريات الكريستالية",
                "جناح السهرات الخاصة والقدود الحلبية",
                "ركن الضيافة والمطبخ الحلبي العريق",
                "الأجنحة المريحة والخاصة لكبار الشخصيات"
            ),
            packages = listOf(
                ServicePackage(
                    id = "pkg_qudud_1",
                    title = "حجز طاولة قصر السهرات والقدود الحلبية",
                    durationLabel = "ليلة كاملة",
                    priceUsd = 50,
                    description = "طاولة VIP أمام المسرح مع قائمة عشاء حلبي فاخر وضيافة ملكية متكاملة.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_qudud_2",
                    title = "جناح ملكي خاص للسهرة + خدمة ضيافة شخصية",
                    durationLabel = "ليلة كاملة",
                    priceUsd = 95,
                    description = "جناح خاص بإطلالة بانورامية على القاعة مع خصوصية تامة ومضيف خاص."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_qudud_1",
                    authorName = "سامر الحلبي",
                    cityBadge = "حلب",
                    serviceTitle = "حجز طاولة قصر السهرات",
                    rating = 5,
                    comment = "قمة الفخامة والترتيب، القاعة الملكية ساحرة والطرب الحلبي الأصيل لا يعلى عليه.",
                    dateLabel = "أمس"
                ),
                CuratedReview(
                    id = "cr_qudud_2",
                    authorName = "نور الشهباء",
                    cityBadge = "حلب",
                    serviceTitle = "جناح ملكي خاص للسهرة",
                    rating = 5,
                    comment = "حجزنا ودفعنا عبر المحفظة بكل سهولة، استقبال ملكي وخدمة سريعة جداً.",
                    dateLabel = "قبل 3 أيام"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        // 0-B. حلب / دمشق - قصر العزت الفام الحلبية
        Venue(
            id = "aleppo_izzat_fam_lounge",
            name = "قصر العزت الفام الحلبية",
            cityName = "حلب",
            country = "سوريا",
            neighborhood = "الجميلية - الموكامبو",
            category = "سهرات فنية",
            shortDescription = "جلسات لاونج مخملية وسهرات فنية خاصة بأجواء ملكية دافئة وإضاءة وردية ساحرة.",
            fullDescription = "• صالات لاونج مخملية مصممة لراحة الضيوف بأعلى درجات الخصوصية.\n• سهرات فنية وموسيقى حية مع ضيافة شرقية وعالمية فاخرة.\n• إمكانية الحجز المباشر للطاولات أو الأجنحة الخاصة.",
            ratingAverage = 4.9,
            reviewCount = 214,
            startingPriceUsd = 65,
            openingHours = "06:30 مساءً – 02:30 فجراً",
            badges = listOf("جلسات مخملية", "خصوصية تامة", "سهرات VIP"),
            accentHex = 0xFFE87A5D,
            secondaryHex = 0xFF341642,
            providerDisplayName = "ميرا • لاونج خاص",
            isOnlineNow = true,
            visualPresetKey = "card_lounge",
            packages = listOf(
                ServicePackage(
                    id = "pkg_izzat_1",
                    title = "حجز جناح لاونج مخملي خاص للسهرة",
                    durationLabel = "ليلة كاملة",
                    priceUsd = 65,
                    description = "جلسة مخملية فاخرة مع ضيافة متكاملة وموسيقى مسائية راقية.",
                    isPopular = true
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_izzat_1",
                    authorName = "رامي كنجو",
                    cityBadge = "حلب",
                    serviceTitle = "حجز جناح لاونج مخملي",
                    rating = 5,
                    comment = "الأجواء والديكور البنفسجي والذهبي في قمة الرقي والهدوء.",
                    dateLabel = "قبل يومين"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        // 0-C. زيارات خارجية وبطاقة فنية VIP
        Venue(
            id = "vip_external_visits_service",
            name = "بطاقة فنية وزيارات خارجية VIP",
            cityName = "دمشق",
            country = "سوريا",
            neighborhood = "المالكي - المزة فيلات - جميع المدن",
            category = "زيارات خارجية",
            shortDescription = "خدمة الزيارات الخارجية الملكية للمنازل والفنادق (مساج استرخائي أو تنسيق سهرة خاصة).",
            fullDescription = "• فريق متخصص يصل إليك في الفندق أو الجناح الخاص في جميع المدن السورية والتركية.\n• تجهيز كامل بالزيوت العطرية الدافئة والشموع والموسيقى الهادئة.\n• سرية وخصوصية تامة مع التزام كامل بالمواعيد.",
            ratingAverage = 4.9,
            reviewCount = 198,
            startingPriceUsd = 75,
            openingHours = "متاح على مدار 24 ساعة",
            badges = listOf("زيارات فندقية ومنزلية", "خصوصية 100%", "جميع المدن"),
            accentHex = 0xFFD6789B,
            secondaryHex = 0xFF2B143D,
            providerDisplayName = "لينا • خدمة VIP",
            isOnlineNow = true,
            visualPresetKey = "card_vip_night",
            packages = listOf(
                ServicePackage(
                    id = "pkg_ext_1",
                    title = "جلسة مساج ملكي خارجي (فندق / منزل خاص)",
                    durationLabel = "90 دقيقة",
                    priceUsd = 75,
                    description = "يشمل كافة التجهيزات والزيوت الطبيعية العطرية والأحجار الدافئة.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_ext_2",
                    title = "تنسيق سهرة خاصة VIP شاملة الضيافة",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 130,
                    description = "تجهيز متكامل للسهرة الخاصة في الجناح أو الفيلا مع خدمة فندقية."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_ext_1",
                    authorName = "كريم الترك",
                    cityBadge = "دمشق",
                    serviceTitle = "جلسة مساج ملكي خارجي",
                    rating = 5,
                    comment = "خدمة احترافية وراقية جداً في الفندق، التزام تام بالوقت ونظافة عالية.",
                    dateLabel = "أمس"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        // 1. دمشق - سبا ومساج
        Venue(
            id = "damascus_royal_spa",
            name = "سبا وحمام قصر الياسمين الدمشقي",
            cityName = "دمشق",
            country = "سوريا",
            neighborhood = "باب توما - القيمرية",
            category = "سبا ومساج",
            shortDescription = "جلسات مساج ملكي بالأحجار الساخنة والزيوت الدمشقية العطرية داخل قصر أثري مرمم.",
            fullDescription = "يجمع قصر الياسمين بين سحر البيوت الدمشقية العريقة وأحدث تقنيات العلاج الطبيعي والمساج الاسترخائي. يضم أجنحة خاصة معزولة صوتياً، جلسات تدليك بالأحجار البركانية الساخنة، وعطور الورد الشامي واللافندر مع ضيافة ملكية.",
            ratingAverage = 4.9,
            reviewCount = 342,
            startingPriceUsd = 45,
            openingHours = "02:00 ظهراً – 01:00 بعد منتصف الليل",
            badges = listOf("حجز فوري مؤكد", "أجنحة VIP خاصة", "خبراء معتمدون"),
            accentHex = 0xFFE5B84B,
            secondaryHex = 0xFF3E2753,
            visualPresetKey = "card_spa",
            packages = listOf(
                ServicePackage(
                    id = "pkg_dm_1",
                    title = "مساج ملكي بالأحجار الساخنة والزيوت العطرية",
                    durationLabel = "75 دقيقة",
                    priceUsd = 55,
                    description = "تدليك كامل للجسم مع أحجار البازلت الدافئة وزيت الورد الدمشقي لإزالة التوتر.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_dm_2",
                    title = "جلسة مساج سويدي علاجي للأعصاب والعضلات",
                    durationLabel = "60 دقيقة",
                    priceUsd = 45,
                    description = "جلسة مركزة لتخفيف آلام الظهر والرقبة وتجديد النشاط."
                ),
                ServicePackage(
                    id = "pkg_dm_3",
                    title = "باقة قصر الياسمين الملكية (مساج + حمام شامي + عناية)",
                    durationLabel = "120 دقيقة",
                    priceUsd = 90,
                    description = "تجربة متكاملة تشمل البخار، التقشير بصابون الغار، مساج كامل، وضيافة شاي أبيض."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_dm_1",
                    authorName = "مازن العظم",
                    cityBadge = "دمشق",
                    serviceTitle = "مساج ملكي بالأحجار الساخنة",
                    rating = 5,
                    comment = "نظافة فائقة وهدوء لا يوصف، الزيوت العطرية أصلية والمعالجون على درجة عالية من الاحترافية.",
                    dateLabel = "قبل يومين"
                ),
                CuratedReview(
                    id = "cr_dm_2",
                    authorName = "رنا الشامي",
                    cityBadge = "دمشق",
                    serviceTitle = "باقة قصر الياسمين الملكية",
                    rating = 5,
                    comment = "حجزت مباشرة من التطبيق ووجدت الجناح جاهزاً بالشموع والورود في الموعد تماماً.",
                    dateLabel = "قبل 5 أيام"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        // 2. دمشق - سهرات فنية
        Venue(
            id = "damascus_malki_lounge",
            name = "لاونج وسهرات ليالي المالكي",
            cityName = "دمشق",
            country = "سوريا",
            neighborhood = "حي المالكي - شارع عبد المنعم رياض",
            category = "سهرات فنية",
            shortDescription = "سهرات طربية راقية وعزف عود وكمان حي مع قائمة عشاء فاخرة وأجواء مسائية ساحرة.",
            fullDescription = "وجهة النخبة في قلب دمشق لقضاء أرقى السهرات المسائية. برامج فنية يومية تشمل الطرب الأصيل والموسيقى الشرقية والغربية الهادئة، مع خدمة طاولات VIP ومطبخ شرقي ومتوسطي بإشراف أمهر الطهاة.",
            ratingAverage = 4.8,
            reviewCount = 289,
            startingPriceUsd = 60,
            openingHours = "07:00 مساءً – 03:00 فجراً",
            badges = listOf("موسيقى حية يومياً", "طاولات VIP", "تقييم موثوق 4.8★"),
            accentHex = 0xFFE87A5D,
            secondaryHex = 0xFF2B193D,
            packages = listOf(
                ServicePackage(
                    id = "pkg_ml_1",
                    title = "حجز طاولة سهرة طربية + عشاء فاخر",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 60,
                    description = "يشمل مقبلات شامية، طبق رئيسي مشوي، فواكه وضيافة مع إطلالة مباشرة على المسرح.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_ml_2",
                    title = "جناح لاونج VIP خاص للمجموعات",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 140,
                    description = "طاولة ركنية فاخرة مع مضيف خاص وقائمة ضيافة ملكية متكاملة."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_ml_1",
                    authorName = "فراس كنجو",
                    cityBadge = "دمشق",
                    serviceTitle = "حجز طاولة سهرة طربية",
                    rating = 5,
                    comment = "أجمل سهرة في الشام! الصوت والفرقة الموسيقية رائعة والخدمة سريعة وراقية.",
                    dateLabel = "قبل 3 أيام"
                )
            ),
            availableTimeSlots = listOf("08:00 مساءً", "09:30 مساءً", "11:00 مساءً")
        ),

        // 3. إسطنبول - سهرات فنية ولاونج
        Venue(
            id = "istanbul_bosphorus_lounge",
            name = "لاونج وسهرات ليالي البوسفور الملكية",
            cityName = "إسطنبول",
            country = "تركيا",
            neighborhood = "أورتاكوي - بشكتاش",
            category = "سهرات فنية",
            shortDescription = "سهرات عربية وتركية حية بإطلالة بانورامية مباشرة على جسر البوسفور المضيء.",
            fullDescription = "استمتع بسحر ليل إسطنبول على ضفاف البوسفور مع برنامج فني يجمع نجوم الطرب العربي والموسيقى التركية المعاصرة. تتوفر طاولات تراس مطلة على المضيق وأجنحة خاصة للمناسبات والسهرات الراقية.",
            ratingAverage = 4.9,
            reviewCount = 518,
            startingPriceUsd = 85,
            openingHours = "06:30 مساءً – 03:30 فجراً",
            badges = listOf("إطلالة البوسفور", "سهرات عربية وتركية", "الأكثر طلباً في إسطنبول"),
            accentHex = 0xFFD4AF37,
            secondaryHex = 0xFF1A2A4A,
            packages = listOf(
                ServicePackage(
                    id = "pkg_ist_1",
                    title = "سهرة طربية على تراس البوسفور + قائمة عشاء ملكي",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 85,
                    description = "طاولة مطلة على جسر البوسفور مع مازة تركية وسورية فاخرة وطبق رئيسي وحلويات.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_ist_2",
                    title = "باقة يخت خاص مسائي + سهرة موسيقية",
                    durationLabel = "3 ساعات",
                    priceUsd = 220,
                    description = "جولة مسائية خاصة في مضيق البوسفور مع عشاء فاخر وفرقة موسيقية."
                ),
                ServicePackage(
                    id = "pkg_ist_3",
                    title = "طاولة لاونج كوكتيل وموسيقى DJ مسائية",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 70,
                    description = "أجواء شبابية راقية مع مقبلات ومشروبات ترحيبية."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_ist_1",
                    authorName = "طارق الحلبي",
                    cityBadge = "إسطنبول",
                    serviceTitle = "سهرة طربية على تراس البوسفور",
                    rating = 5,
                    comment = "الإطلالة على البوسفور ليلاً خيالية، الحجز من التطبيق وفر علينا الانتظار وأعطونا أفضل طاولة.",
                    dateLabel = "أمس"
                ),
                CuratedReview(
                    id = "cr_ist_2",
                    authorName = "نادين الترك",
                    cityBadge = "إسطنبول",
                    serviceTitle = "باقة يخت خاص مسائي",
                    rating = 5,
                    comment = "تنظيم احترافي من اللحظة الأولى ومصداقية عالية جداً في التقييمات والأسعار.",
                    dateLabel = "قبل 4 أيام"
                )
            ),
            availableTimeSlots = listOf("07:30 مساءً", "09:00 مساءً", "10:30 مساءً", "12:00 منتصف الليل")
        ),

        // 4. إسطنبول - سبا ومساج
        Venue(
            id = "istanbul_sultan_spa",
            name = "سبا وحمام السلطان الفاخر - شيشلي وتقسيم",
            cityName = "إسطنبول",
            country = "تركيا",
            neighborhood = "نيشانتاشي - شيشلي",
            category = "سبا ومساج",
            shortDescription = "مساج تايلاندي وبالي احترافي وحمام تركي ملكي بالرغوة والزيوت العضوية.",
            fullDescription = "مركز سبا متكامل من فئة 5 نجوم في أرقى أحياء إسطنبول. يضم غرف مساج فردية ومزدوجة، ساونا فنلندية، غرفة ملح الهيمالايا، وحمام رخامي عثماني مع أمهر المعالجين المتخصصين.",
            ratingAverage = 4.9,
            reviewCount = 412,
            startingPriceUsd = 65,
            openingHours = "11:00 صباحاً – 02:00 فجراً",
            badges = listOf("معالجون دوليون", "حمام عثماني خاص", "تعقيم فندقي 5 نجوم"),
            accentHex = 0xFF2DD4BF,
            secondaryHex = 0xFF16323B,
            packages = listOf(
                ServicePackage(
                    id = "pkg_sultan_1",
                    title = "مساج بالي وتايلاندي ملكي عميق",
                    durationLabel = "90 دقيقة",
                    priceUsd = 75,
                    description = "مزيج مثالي من التمدد التايلاندي والضغط المتوازن بالزيوت الدافئة.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_sultan_2",
                    title = "حمام السلطان التركي بالرغوة + مساج أروماثيرابي",
                    durationLabel = "110 دقائق",
                    priceUsd = 95,
                    description = "تقشير بالكيسة الحريرية، مساج رغوة الصابون الطبيعي، يليه مساج كامل بالزيوت."
                ),
                ServicePackage(
                    id = "pkg_sultan_3",
                    title = "جلسة مساج مسائية سريعة لإزالة إجهاد السفر",
                    durationLabel = "50 دقيقة",
                    priceUsd = 65,
                    description = "تركيز خاص على القدمين والظهر والكتفين."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_sultan_1",
                    authorName = "وسيم خوري",
                    cityBadge = "إسطنبول",
                    serviceTitle = "حمام السلطان التركي + مساج",
                    rating = 5,
                    comment = "أفضل مركز مساج جربته في إسطنبول، استقبال راقٍ وخصوصية تامة.",
                    dateLabel = "قبل 3 أيام"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        // 5. حلب - سهرات فنية
        Venue(
            id = "aleppo_shahba_evening",
            name = "قصر السهرات والقدود الحلبية - الشهباء",
            cityName = "حلب",
            country = "سوريا",
            neighborhood = "العزيزية - تقاطع بارون",
            category = "سهرات فنية",
            shortDescription = "ليالي الطرب الأصيل والقدود الحلبية العريقة مع أفخر الموائد الحلبية المسائية.",
            fullDescription = "عيشوا أصالة السهر الحلبي مع نخبة المطربين والعازفين في أجواء تراثية فخمة تجمع بين الحجر الحلبي الأصفر والإضاءة الدافئة، مع قائمة غنية بالكباب الحلبي والمقبلات الشهيرة.",
            ratingAverage = 4.9,
            reviewCount = 265,
            startingPriceUsd = 50,
            openingHours = "07:30 مساءً – 02:30 فجراً",
            badges = listOf("قدود حلبية أصيلة", "مطبخ حلبي عريق", "حجز مباشر"),
            accentHex = 0xFFE5B84B,
            secondaryHex = 0xFF3A241B,
            packages = listOf(
                ServicePackage(
                    id = "pkg_al_1",
                    title = "سهرة طرب وقدود حلبية + عشاء مشاوي فاخر",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 50,
                    description = "طاولة مميزة أمام التخت الشرقي مع عشاء متكامل وضيافة فستق حلبي.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_al_2",
                    title = "مساج وحمام النحاسين الملكي بصابون الغار",
                    durationLabel = "90 دقيقة",
                    priceUsd = 45,
                    description = "جلسة استرخاء وتدليك علاجي قبل السهرة في الجناح الصحي الملحق."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_al_1",
                    authorName = "عبد القادر المدرس",
                    cityBadge = "حلب",
                    serviceTitle = "سهرة طرب وقدود حلبية",
                    rating = 5,
                    comment = "أجواء ترد الروح وطرب حلبي أصيل لا يعلى عليه، أنصح بالحجز المسبق لعطلة نهاية الأسبوع.",
                    dateLabel = "قبل 6 أيام"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        // 6. اللاذقية - لاونج وسبا بحري
        Venue(
            id = "latakia_cote_lounge",
            name = "منتجع ولاونج نسيم المتوسط - اللاذقية",
            cityName = "اللاذقية",
            country = "سوريا",
            neighborhood = "الكورنيش الغربي - الشاطئ الأزرق",
            category = "لاونج وعشاء",
            shortDescription = "سهرات ساحلية على أمواج المتوسط مع جناح سبا ومساج استجمامي بإطلالة بحرية.",
            fullDescription = "يجمع المنتجع بين لاونج مسائي مفتوح على البحر لتناول العشاء والاستماع للموسيقى الحية، وبين مركز سبا حديث يقدم جلسات المساج السويدي والتايلاندي على صوت أمواج البحر.",
            ratingAverage = 4.8,
            reviewCount = 198,
            startingPriceUsd = 40,
            openingHours = "04:00 عصراً – 02:30 فجراً",
            badges = listOf("إطلالة بحرية مباشرة", "مساج + سهرة", "أجواء رومانسية"),
            accentHex = 0xFF38BDF8,
            secondaryHex = 0xFF172554,
            packages = listOf(
                ServicePackage(
                    id = "pkg_lat_1",
                    title = "جلسة مساج استرخائي بإطلالة البحر",
                    durationLabel = "60 دقيقة",
                    priceUsd = 40,
                    description = "تدليك كامل بالزيوت البحرية المنعشة في كبينة خاصة.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_lat_2",
                    title = "طاولة سهرة مسائية على التراس البحري",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 55,
                    description = "عشاء مأكولات بحرية وموسيقى لاونج حية على الشاطئ الأزرق."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_lat_1",
                    authorName = "حيدر صقر",
                    cityBadge = "اللاذقية",
                    serviceTitle = "طاولة سهرة مسائية على التراس البحري",
                    rating = 5,
                    comment = "المكان ساحر على البحر والخدمة ممتازة جداً.",
                    dateLabel = "قبل أسبوع"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        // 7. أنطاليا - سهرات وسبا الريفييرا
        Venue(
            id = "antalya_marina_club",
            name = "نادي وسبا مارينا أنطاليا الملكي",
            cityName = "أنطاليا",
            country = "تركيا",
            neighborhood = "لارا - كاليتشي مارينا",
            category = "لاونج وعشاء",
            shortDescription = "سهرات لاونج عالمية على يخت مارينا أنطاليا مع مركز مساج علاجي فاخر.",
            fullDescription = "أيقونة السهر والاستجمام في أنطاليا؛ استمتع بجلسات المساج العلاجي بعد يوم طويل على الشاطئ، ثم انتقل إلى التراس البانورامي لحضور عروض الموسيقى الحية والعشاء الفاخر.",
            ratingAverage = 4.9,
            reviewCount = 310,
            startingPriceUsd = 70,
            openingHours = "03:00 عصراً – 03:00 فجراً",
            badges = listOf("مارينا أنطاليا", "عروض حية", "مساج VIP"),
            accentHex = 0xFFF43F5E,
            secondaryHex = 0xFF3B122B,
            packages = listOf(
                ServicePackage(
                    id = "pkg_ant_1",
                    title = "طاولة سهرة VIP في لاونج المارينا",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 80,
                    description = "تشمل قائمة عشاء متوسطي وعروض موسيقية حية.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_ant_2",
                    title = "مساج أروماثيرابي وتجديد البشرة",
                    durationLabel = "75 دقيقة",
                    priceUsd = 70,
                    description = "تدليك كامل للجسم بزيوت الصبار واللافندر الطبيعي."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_ant_1",
                    authorName = "لينا قاسم",
                    cityBadge = "أنطاليا",
                    serviceTitle = "طاولة سهرة VIP في لاونج المارينا",
                    rating = 5,
                    comment = "سهرة لا تنسى في أنطاليا! الحجز كان مؤكداً عند وصولنا دون أي تأخير.",
                    dateLabel = "قبل 3 أيام"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        // 8. طرطوس وحمص وحماة
        Venue(
            id = "tartus_arwad_breeze",
            name = "منتجع ولاونج لؤلؤة الساحل - طرطوس",
            cityName = "طرطوس",
            country = "سوريا",
            neighborhood = "الكورنيش البحري - مقابل جزيرة أرواد",
            category = "سبا ومساج",
            shortDescription = "مساج استرخائي وعلاجي وسهرات طربية هادئة على إطلالة جزيرة أرواد.",
            fullDescription = "يوفر منتجع لؤلؤة الساحل في طرطوس أجنحة مساج وعناية متكاملة مع تراس مسائي يقدم سهرات فنية راقية وعشاء بحري طازج.",
            ratingAverage = 4.7,
            reviewCount = 142,
            startingPriceUsd = 38,
            openingHours = "03:00 عصراً – 01:30 فجراً",
            badges = listOf("إطلالة أرواد", "مساج علاجي", "أسعار مميزة"),
            accentHex = 0xFF34D399,
            secondaryHex = 0xFF132E2B,
            packages = listOf(
                ServicePackage(
                    id = "pkg_tar_1",
                    title = "مساج استرخائي بالزيوت الدافئة",
                    durationLabel = "60 دقيقة",
                    priceUsd = 38,
                    description = "جلسة تدليك متكاملة لتهدئة الأعصاب وتنشيط الدورة الدموية.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_tar_2",
                    title = "سهرة عشاء بحري وموسيقى مسائية",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 48,
                    description = "طاولة مطلة على البحر مع أطباق سمك طازجة وموسيقى هادئة."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_tar_1",
                    authorName = "سليمان عيسى",
                    cityBadge = "طرطوس",
                    serviceTitle = "مساج استرخائي بالزيوت الدافئة",
                    rating = 5,
                    comment = "مكان راقٍ ونظيف جداً في طرطوس، تجربة ممتازة.",
                    dateLabel = "قبل 5 أيام"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        Venue(
            id = "homs_orontes_nights",
            name = "دار السهرات والسبا - ليالي العاصي",
            cityName = "حمص",
            country = "سوريا",
            neighborhood = "حي الإنشاءات - شارع البرازيل",
            category = "حمام ملكي",
            shortDescription = "حمام ملكي ومساج علاجي مع صالة سهرات مسائية راقية في قلب حمص.",
            fullDescription = "وجهتك الأولى في حمص للاستجمام والسهر الراقي؛ يضم قسم الحمام الملكي والمساج الطبي، بالإضافة إلى صالة سهرات مسائية تقدم أمسيات فنية وعشاء فاخر.",
            ratingAverage = 4.8,
            reviewCount = 164,
            startingPriceUsd = 35,
            openingHours = "02:00 ظهراً – 01:30 فجراً",
            badges = listOf("حمام ملكي", "سهرات عائلية وشبابية", "حجز فوري"),
            accentHex = 0xFFC084FC,
            secondaryHex = 0xFF2B183E,
            packages = listOf(
                ServicePackage(
                    id = "pkg_hm_1",
                    title = "باقة الحمام الملكي والمساج الاسترخائي",
                    durationLabel = "90 دقيقة",
                    priceUsd = 42,
                    description = "بخار وتقشير طبيعي يتبعه مساج كامل بالزيوت العطرية.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_hm_2",
                    title = "حجز طاولة سهرة مسائية وعشاء",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 45,
                    description = "أمسية موسيقية حية مع قائمة عشاء شرقي."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_hm_1",
                    authorName = "كريم الأتاسي",
                    cityBadge = "حمص",
                    serviceTitle = "باقة الحمام الملكي والمساج",
                    rating = 5,
                    comment = "خدمة خمس نجوم في حمص والتزام تام بالمواعيد.",
                    dateLabel = "قبل 4 أيام"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        Venue(
            id = "hama_noria_resort",
            name = "منتجع ولاونج النواعير المسائي - حماة",
            cityName = "حماة",
            country = "سوريا",
            neighborhood = "ضفاف العاصي - قرب النواعير",
            category = "سهرات فنية",
            shortDescription = "سهرات مسائية ساحرة على ضفاف العاصي مع قسم خاص للمساج والاسترخاء.",
            fullDescription = "استمتع بصوت النواعير العريق في أمسيات طربية مميزة مع جلسات مساج علاجي واسترخائي في أجنحة المنتجع الخاصة.",
            ratingAverage = 4.7,
            reviewCount = 118,
            startingPriceUsd = 35,
            openingHours = "04:00 عصراً – 01:00 بعد منتصف الليل",
            badges = listOf("إطلالة النواعير", "أجواء تراثية", "مساج وسهرة"),
            accentHex = 0xFFFBBF24,
            secondaryHex = 0xFF332612,
            packages = listOf(
                ServicePackage(
                    id = "pkg_hama_1",
                    title = "سهرة ضفاف العاصي + عشاء تقليدي فاخر",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 40,
                    description = "طاولة مطلة على النواعير مع عشاء حماوي أصيل وموسيقى شرقية.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_hama_2",
                    title = "جلسة مساج ريلاكس كاملة",
                    durationLabel = "60 دقيقة",
                    priceUsd = 35,
                    description = "مساج هادئ بالزيوت الطبيعية في جناح خاص."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_hama_1",
                    authorName = "يامن البرازي",
                    cityBadge = "حماة",
                    serviceTitle = "سهرة ضفاف العاصي",
                    rating = 5,
                    comment = "إطلالة ساحرة وخدمة محترمة جداً.",
                    dateLabel = "قبل أسبوع"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        // 9. أنقرة وإزمير وغازي عنتاب ومرسين وبورصة
        Venue(
            id = "ankara_cankaya_spa",
            name = "مركز ولاونج تشانكايا الملكي - أنقرة",
            cityName = "أنقرة",
            country = "تركيا",
            neighborhood = "تشانكايا - تونالي حلمي",
            category = "سبا ومساج",
            shortDescription = "أفخم مراكز المساج العلاجي واللاونجات المسائية الدبلوماسية في العاصمة أنقرة.",
            fullDescription = "يقع في حي تشانكايا الراقي ويقدم جلسات مساج طبي ورياضي واسترخائي بأيدي خبراء معتمدين، إضافة إلى لاونج مسائي هادئ للموسيقى الحية.",
            ratingAverage = 4.8,
            reviewCount = 205,
            startingPriceUsd = 60,
            openingHours = "12:00 ظهراً – 02:00 فجراً",
            badges = listOf("حي تشانكايا الراقي", "مساج طبي وملكي", "تقييم موثق"),
            accentHex = 0xFF60A5FA,
            secondaryHex = 0xFF172542,
            packages = listOf(
                ServicePackage(
                    id = "pkg_ank_1",
                    title = "مساج الأنسجة العميقة والأحجار الدافئة",
                    durationLabel = "75 دقيقة",
                    priceUsd = 65,
                    description = "علاج متكامل لتشنجات العضلات والإرهاق مع ساونا وبخار.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_ank_2",
                    title = "حجز سهرة لاونج موسيقى الجاز والعود",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 70,
                    description = "أمسية راقية مع مقبلات وعشاء فاخر."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_ank_1",
                    authorName = "عمر يلماز",
                    cityBadge = "أنقرة",
                    serviceTitle = "مساج الأنسجة العميقة",
                    rating = 5,
                    comment = "مستوى فندقي فاخر جداً في أنقرة.",
                    dateLabel = "قبل 4 أيام"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        Venue(
            id = "izmir_kordon_nights",
            name = "لاونج وسبا كوردون إزمير",
            cityName = "إزمير",
            country = "تركيا",
            neighborhood = "ألشانجاك - كوردون إزمير",
            category = "لاونج وعشاء",
            shortDescription = "سهرات بحر إيجة المفعمة بالحياة مع مركز سبا ومساج أروماثيرابي حديث.",
            fullDescription = "على كورنيش ألشانجاك الشهير في إزمير، استمتع بسهرات موسيقية حيوية وإطلالة الغروب على بحر إيجة، أو احجز جلسة مساج أروماثيرابي منعشة.",
            ratingAverage = 4.8,
            reviewCount = 230,
            startingPriceUsd = 55,
            openingHours = "03:00 عصراً – 03:00 فجراً",
            badges = listOf("كوردون إزمير", "سهرات بحر إيجة", "حجز فوري"),
            accentHex = 0xFF38BDF8,
            secondaryHex = 0xFF142E46,
            packages = listOf(
                ServicePackage(
                    id = "pkg_izm_1",
                    title = "طاولة سهرة كوردون إزمير الموسيقية",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 65,
                    description = "عشاء بحري وموسيقى حية بإطلالة بحر إيجة.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_izm_2",
                    title = "جلسة مساج أروماثيرابي إيجة",
                    durationLabel = "60 دقيقة",
                    priceUsd = 55,
                    description = "مساج بزيوت الزيتون البكر والأعشاب المتوسطية."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_izm_1",
                    authorName = "رامي قباقيبي",
                    cityBadge = "إزمير",
                    serviceTitle = "طاولة سهرة كوردون إزمير",
                    rating = 5,
                    comment = "أجواء إزمير الليلية رائعة وهذا المكان هو الأفضل على الكوردون.",
                    dateLabel = "قبل 3 أيام"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        Venue(
            id = "gaziantep_zevgma_lounge",
            name = "دار سهرات وحمام زيوغما الملكي - غازي عنتاب",
            cityName = "غازي عنتاب",
            country = "تركيا",
            neighborhood = "شهيد كامل - المركز التاريخي",
            category = "حمام ملكي",
            shortDescription = "حمام تركي ومساج ملكي وسهرات طربية تجمع التراث السوري والتركي.",
            fullDescription = "في قلب غازي عنتاب، يقدم مركز زيوغما تجربة الحمام التركي العثماني والمساج الاسترخائي، يليها سهرات طربية شرقية مع أشهى المأكولات العنتابية والبقلاوة الفاخرة.",
            ratingAverage = 4.9,
            reviewCount = 276,
            startingPriceUsd = 45,
            openingHours = "01:00 ظهراً – 02:00 فجراً",
            badges = listOf("تراث عنتابي وحلبي", "حمام ومساج ملكي", "سهرات طربية"),
            accentHex = 0xFFE5B84B,
            secondaryHex = 0xFF382214,
            packages = listOf(
                ServicePackage(
                    id = "pkg_gaz_1",
                    title = "حمام تركي ملكي + مساج كامل بالزيوت",
                    durationLabel = "90 دقيقة",
                    priceUsd = 50,
                    description = "حمام بخار، تقشير، رغوة صابون غار، ومساج استرخائي كامل.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_gaz_2",
                    title = "سهرة طرب شرقي وعشاء مشاوي عنتابية",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 55,
                    description = "سهرة موسيقية حية مع كباب عنتابي وبقلاوة بالفستق."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_gaz_1",
                    authorName = "محمود الصباغ",
                    cityBadge = "غازي عنتاب",
                    serviceTitle = "حمام تركي ملكي + مساج",
                    rating = 5,
                    comment = "نظافة ممتازة وخدمة احترافية، والسهرة الشرقية في المساء قمة في الروعة.",
                    dateLabel = "قبل يومين"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        Venue(
            id = "mersin_marina_spa",
            name = "منتجع ولاونج مارينا مرسين المسائي",
            cityName = "مرسين",
            country = "تركيا",
            neighborhood = "يني شهير - مرسين مارينا",
            category = "سهرات فنية",
            shortDescription = "سهرات عربية وتركية في مارينا مرسين مع جناح مساج وسبا متكامل.",
            fullDescription = "الوجهة المفضلة في مرسين لقضاء السهرات الفنية المطلة على اليخوت، مع مركز سبا متخصص يقدم جلسات المساج التايلاندي والسويدي وحمام البخار.",
            ratingAverage = 4.8,
            reviewCount = 244,
            startingPriceUsd = 50,
            openingHours = "03:00 عصراً – 02:30 فجراً",
            badges = listOf("مرسين مارينا", "حفلات عربية وتركية", "سبا 5 نجوم"),
            accentHex = 0xFFA78BFA,
            secondaryHex = 0xFF251A42,
            packages = listOf(
                ServicePackage(
                    id = "pkg_mer_1",
                    title = "حجز طاولة سهرة عربية في مارينا مرسين",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 60,
                    description = "مطربون عرب وعزف حي مع عشاء فاخر على المارينا.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_mer_2",
                    title = "جلسة مساج ملكي تايلاندي وسويدي",
                    durationLabel = "70 دقيقة",
                    priceUsd = 50,
                    description = "استرخاء تام في جناح هادئ مع جاكوزي دافئ."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_mer_1",
                    authorName = "دانيا الساحلي",
                    cityBadge = "مرسين",
                    serviceTitle = "حجز طاولة سهرة عربية",
                    rating = 5,
                    comment = "أفضل مكان للسهر في مرسين، أجواء راقية ومحترمة جداً.",
                    dateLabel = "قبل 3 أيام"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        ),

        Venue(
            id = "bursa_thermal_spa",
            name = "منتجع الينابيع الحارة والسهرات - بورصة",
            cityName = "بورصة",
            country = "تركيا",
            neighborhood = "تشيكيرجه - سفح أولوداغ",
            category = "سبا ومساج",
            shortDescription = "مساج بالمياه الكبريتية الطبيعية الدافئة وسهرات مسائية بإطلالة جبل أولوداغ.",
            fullDescription = "يشتهر منتجع بورصة بمياهه المعدنية الحارة الطبيعية وأجنحة المساج بالأحجار البركانية، مع تراس مسائي دافئ يقدم سهرات موسيقية هادئة ومشويات تركية.",
            ratingAverage = 4.9,
            reviewCount = 290,
            startingPriceUsd = 55,
            openingHours = "12:00 ظهراً – 01:30 فجراً",
            badges = listOf("ينابيع كبريتية طبيعية", "أجنحة مساج خاصة", "إطلالة أولوداغ"),
            accentHex = 0xFF10B981,
            secondaryHex = 0xFF123026,
            packages = listOf(
                ServicePackage(
                    id = "pkg_bur_1",
                    title = "مسبح كبريتي خاص + مساج بالأحجار البركانية",
                    durationLabel = "90 دقيقة",
                    priceUsd = 65,
                    description = "استجمام في مياه الينابيع الحارة الطبيعية يتبعه مساج علاجي كامل.",
                    isPopular = true
                ),
                ServicePackage(
                    id = "pkg_bur_2",
                    title = "سهرة تراس أولوداغ + عشاء إسكندر كباب ملكي",
                    durationLabel = "سهرة كاملة",
                    priceUsd = 55,
                    description = "سهرة دافئة بإطلالة بانورامية على مدينة بورصة."
                )
            ),
            curatedReviews = listOf(
                CuratedReview(
                    id = "cr_bur_1",
                    authorName = "سامر قباني",
                    cityBadge = "بورصة",
                    serviceTitle = "مسبح كبريتي خاص + مساج",
                    rating = 5,
                    comment = "تجربة علاجية واستجمامية خيالية في بورصة، الحجز عبر التطبيق دقيق وسهل جداً.",
                    dateLabel = "قبل يومين"
                )
            ),
            availableTimeSlots = defaultTimeSlots
        )
    )
}
