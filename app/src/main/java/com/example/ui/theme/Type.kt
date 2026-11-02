package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CleanSansFontFamily = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 25.sp
    ),
    titleLarge = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 25.sp
    ),
    titleSmall = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    )
)
