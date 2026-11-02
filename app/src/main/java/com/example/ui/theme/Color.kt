package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// 1. Core Backgrounds & Surfaces
val RoyalNavyBackground = Color(0xFF101923) // خلفية ليلية داكنة فاخرة
val RoyalNavyCard = Color(0xFF182432)       // بطاقات كحلي/أوبسيديان أنيقة
val ElevatedNavySurface = Color(0xFF223244) // سطح فرعي مرفوع

val CharcoalNight = RoyalNavyBackground
val DarkCardSurface = RoyalNavyCard
val ElevatedDarkSurface = ElevatedNavySurface

// Light Mode Soft Surfaces (when toggled)
val PureWhite = Color(0xFFF8FAFC)           // النص الأساسي - أبيض ناعم
val SnowGraySurface = Color(0xFF182432)
val LightCardSurface = Color(0xFF182432)

// Restrained navy accents
val DeepTealPrimary = Color(0xFF263F5B)     // اللون الأساسي للأزرار والتفاعل
val DeepTealBright = Color(0xFF58728F)      // تدرج سيان مضيء
val DeepTealDark = Color(0xFF1A3049)        // أزرق ياقوتي عميق

// Muted champagne accents
val RoyalAmethystVip = Color(0xFFAD9167)    // بنفسجي ملكي فاخر للـ VIP والـ LEVEL
val LuminousLavender = Color(0xFFC7B18D)    // لافندر بلاتيني مضيء
val ImperialRosePink = Color(0xFF9B7881)    // وردي ياقوتي فاخر

// Semantic aliases (replacing yellow/gold tokens with Royal Amethyst & Lavender)
val MutedLuxuryGold = RoyalAmethystVip
val SoftChampagneGold = LuminousLavender

val CoralPinkPrimary = DeepTealPrimary
val CoralSoftAccent = DeepTealBright
val VioletBlueVip = RoyalAmethystVip
val CharcoalButton = RoyalNavyCard

val SovereignGold = RoyalAmethystVip
val DeepAmberGold = Color(0xFF8D7554)
val ShamCashTeal = Color(0xFF263F5B)
val ShamCashIndigo = Color(0xFF536781)
val UsdtTetherGreen = Color(0xFF50816D)
val MidnightSapphire = RoyalNavyBackground

// 4. Typography & Hierarchy Colors
val SoftWhiteText = Color(0xFFF8FAFC)
val SlateSecondaryText = Color(0xFFA8B4C2)

val TitleDarkText = SoftWhiteText
val SubtitleGrayText = SlateSecondaryText
val MutedLightText = SlateSecondaryText

// 5. Subtle Borders & Dividers
val DarkBorderGray = Color(0xFF334355)
val LightBorderGray = Color(0xFF334355)

// 6. Status & Semantic Accents
val EmeraldVerified = Color(0xFF50816D)
val TelegramBlue = Color(0xFF263F5B)
val ErrorCrimson = Color(0xFFB65C65)

// ============================================================================
// Compatibility Aliases
// ============================================================================
val ObsidianNight = RoyalNavyBackground
val VelvetPlum = RoyalNavyCard
val ElevatedPlumCard = ElevatedNavySurface
val DeepRoyalViolet = ElevatedNavySurface

val DustyRose = DeepTealPrimary
val SoftRosePink = DeepTealBright
val WarmCoral = Color(0xFFB65C65)

val RoyalGold = RoyalAmethystVip
val SoftGold = LuminousLavender

val IvoryCream = SoftWhiteText
val SoftBlushBackground = RoyalNavyBackground
val SoftRoseSurface = RoyalNavyCard
val SoftRoseCard = RoyalNavyCard
val DeepPlumText = SoftWhiteText
val MutedPlumText = SlateSecondaryText

val MutedLavender = SlateSecondaryText
val DeepWineOutline = DarkBorderGray
val SoftRoseOutline = DarkBorderGray

val BrandChampagneGold = RoyalAmethystVip
val BrandDeepViolet = RoyalNavyCard
val BrandDustyRose = DeepTealPrimary
val BrandRoyalPlum = RoyalNavyCard
val BrandSoftPearl = SoftWhiteText
