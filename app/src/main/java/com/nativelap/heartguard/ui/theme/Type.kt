package com.nativelap.heartguard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R

// 영문은 Asta Sans를 우선 사용하고, Asta Sans에 없는 한글 글리프는 Noto Sans KR로 대체한다.
private val HeartGuardFontFamily = FontFamily(
    Font(
        resId = R.font.asta_sans_light,
        weight = FontWeight.Light,
    ),
    Font(
        resId = R.font.asta_sans_regular,
        weight = FontWeight.Normal,
    ),
    Font(
        resId = R.font.asta_sans_medium,
        weight = FontWeight.Medium,
    ),
    Font(
        resId = R.font.asta_sans_semi_bold,
        weight = FontWeight.SemiBold,
    ),
    Font(
        resId = R.font.asta_sans_bold,
        weight = FontWeight.Bold,
    ),
    Font(
        resId = R.font.asta_sans_extra_bold,
        weight = FontWeight.ExtraBold,
    ),
    Font(
        resId = R.font.noto_sans_kr_thin,
        weight = FontWeight.Thin,
    ),
    Font(
        resId = R.font.noto_sans_kr_extra_light,
        weight = FontWeight.ExtraLight,
    ),
    Font(
        resId = R.font.noto_sans_kr_light,
        weight = FontWeight.Light,
    ),
    Font(
        resId = R.font.noto_sans_kr_regular,
        weight = FontWeight.Normal,
    ),
    Font(
        resId = R.font.noto_sans_kr_medium,
        weight = FontWeight.Medium,
    ),
    Font(
        resId = R.font.noto_sans_kr_semi_bold,
        weight = FontWeight.SemiBold,
    ),
    Font(
        resId = R.font.noto_sans_kr_bold,
        weight = FontWeight.Bold,
    ),
    Font(
        resId = R.font.noto_sans_kr_extra_bold,
        weight = FontWeight.ExtraBold,
    ),
    Font(
        resId = R.font.noto_sans_kr_black,
        weight = FontWeight.Black,
    ),
)

// Material 3의 각 텍스트 스타일에 Asta Sans와 Noto Sans KR fallback 패밀리를 적용한다.
private fun TextStyle.withHeartGuardFontFamily(): TextStyle {
    return copy(fontFamily = HeartGuardFontFamily)
}

private val DefaultTypography = Typography()

val Typography = DefaultTypography.copy(
    displayLarge = DefaultTypography.displayLarge.withHeartGuardFontFamily(),
    displayMedium = DefaultTypography.displayMedium.withHeartGuardFontFamily(),
    displaySmall = DefaultTypography.displaySmall.withHeartGuardFontFamily(),
    headlineLarge = DefaultTypography.headlineLarge.withHeartGuardFontFamily(),
    headlineMedium = DefaultTypography.headlineMedium.withHeartGuardFontFamily(),
    headlineSmall = DefaultTypography.headlineSmall.withHeartGuardFontFamily(),
    titleLarge = DefaultTypography.titleLarge.withHeartGuardFontFamily(),
    titleMedium = DefaultTypography.titleMedium.withHeartGuardFontFamily(),
    titleSmall = DefaultTypography.titleSmall.withHeartGuardFontFamily(),
    bodyLarge = DefaultTypography.bodyLarge
        .withHeartGuardFontFamily()
        .copy(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
    bodyMedium = DefaultTypography.bodyMedium.withHeartGuardFontFamily(),
    bodySmall = DefaultTypography.bodySmall.withHeartGuardFontFamily(),
    labelLarge = DefaultTypography.labelLarge.withHeartGuardFontFamily(),
    labelMedium = DefaultTypography.labelMedium.withHeartGuardFontFamily(),
    labelSmall = DefaultTypography.labelSmall.withHeartGuardFontFamily(),
)
