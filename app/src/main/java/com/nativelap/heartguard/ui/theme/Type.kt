package com.nativelap.heartguard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R

private val PretendardFontFamily = FontFamily(
    Font(
        resId = R.font.pretendard_thin,
        weight = FontWeight.Thin,
    ),
    Font(
        resId = R.font.pretendard_extra_light,
        weight = FontWeight.ExtraLight,
    ),
    Font(
        resId = R.font.pretendard_light,
        weight = FontWeight.Light,
    ),
    Font(
        resId = R.font.pretendard_regular,
        weight = FontWeight.Normal,
    ),
    Font(
        resId = R.font.pretendard_medium,
        weight = FontWeight.Medium,
    ),
    Font(
        resId = R.font.pretendard_semi_bold,
        weight = FontWeight.SemiBold,
    ),
    Font(
        resId = R.font.pretendard_bold,
        weight = FontWeight.Bold,
    ),
    Font(
        resId = R.font.pretendard_extra_bold,
        weight = FontWeight.ExtraBold,
    ),
    Font(
        resId = R.font.pretendard_black,
        weight = FontWeight.Black,
    ),
)

// Material 3의 모든 기본 텍스트 역할에 Figma에서 확인한 Pretendard 패밀리를 적용한다.
private fun TextStyle.withPretendard(): TextStyle {
    return copy(fontFamily = PretendardFontFamily)
}

private val DefaultTypography = Typography()

val HeartGuardTypography = DefaultTypography.copy(
    displayLarge = DefaultTypography.displayLarge.withPretendard(),
    displayMedium = DefaultTypography.displayMedium.withPretendard(),
    displaySmall = DefaultTypography.displaySmall.withPretendard(),
    headlineLarge = DefaultTypography.headlineLarge.withPretendard(),
    headlineMedium = DefaultTypography.headlineMedium.withPretendard(),
    headlineSmall = DefaultTypography.headlineSmall.withPretendard(),
    titleLarge = DefaultTypography.titleLarge.withPretendard(),
    titleMedium = DefaultTypography.titleMedium.withPretendard(),
    titleSmall = DefaultTypography.titleSmall.withPretendard(),
    bodyLarge = DefaultTypography.bodyLarge
        .withPretendard()
        .copy(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
    bodyMedium = DefaultTypography.bodyMedium.withPretendard(),
    bodySmall = DefaultTypography.bodySmall.withPretendard(),
    labelLarge = DefaultTypography.labelLarge.withPretendard(),
    labelMedium = DefaultTypography.labelMedium.withPretendard(),
    labelSmall = DefaultTypography.labelSmall.withPretendard(),
)
