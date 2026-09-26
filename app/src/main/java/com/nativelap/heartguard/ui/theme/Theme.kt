package com.nativelap.heartguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class HeartGuardExtraColors(
    val success: Color,
    val warning: Color,
    val mutedText: Color,
    val cardBorder: Color,
    val infoContainer: Color,
    val cameraContainer: Color,
    val disabledContent: Color,
    val disabledText: Color,
    val authInputBackground: Color,
    val sheetBackground: Color,
    val photoContainer: Color,
    val alertContainer: Color,
    val successContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val authBackground: Color,
    val authPrimary: Color,
    val authOnSurface: Color,
    val authOnSurfaceVariant: Color,
    val homeMetricContainer: Color,
    val homeMutedText: Color,
    val homeTimelineTrack: Color,
    val homeTimelineActive: Color,
    val homeTimelineInactive: Color,
    val homeTimelineDotBorder: Color,
    val homeTimelineCurrentRing: Color,
    val homeContactAlertContainer: Color,
    val pageBackground: Color,
    val temperatureRiseContainer: Color,
    val onTemperatureRiseContainer: Color,
    val strongText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val subtleDivider: Color,
    val overlayScrim: Color,
)

val LocalHeartGuardExtraColors = staticCompositionLocalOf<HeartGuardExtraColors> {
    error("HeartGuardExtraColors is not provided.")
}

val MaterialTheme.extraColors: HeartGuardExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalHeartGuardExtraColors.current

private val primaryBlueColor = Color(0xFF2879EA)
private val onPrimaryColor = Color(0xFFFFFFFF)
private val primaryContainerColor = Color(0xFFEAF3FF)
private val onPrimaryContainerColor = Color(0xFF0D3B78)
private val secondaryColor = Color(0xFF4A4A4A)
private val onSecondaryColor = Color(0xFFFFFFFF)
private val secondaryContainerColor = Color(0xFFF1F1F1)
private val onSecondaryContainerColor = Color(0xFF202020)
private val tertiaryColor = Color(0xFFFFA726)
private val onTertiaryColor = Color(0xFF2A1700)
private val tertiaryContainerColor = Color(0xFFFFE9C7)
private val onTertiaryContainerColor = Color(0xFF4A2800)
private val backgroundColor = Color.White
private val onBackgroundColor = Color(0xFF000000)
private val surfaceColor = Color(0xFFFFFFFF)
private val onSurfaceColor = Color(0xFF000000)
private val surfaceVariantColor = Color(0xFFF8F8F8)
private val onSurfaceVariantColor = Color(0xFF666666)
private val outlineColor = Color(0xFFE1E1E2)
private val outlineVariantColor = Color(0xFFF0F0F0)
private val errorColor = Color(0xFFE53935)
private val onErrorColor = Color(0xFFFFFFFF)
private val errorContainerColor = Color(0xFFFFDAD6)
private val onErrorContainerColor = Color(0xFF410002)
private val successColor = Color(0xFF4CAF50)
private val warningColor = Color(0xFFFFA726)
private val mutedTextColor = Color(0xFF77767A)
private val cardBorderColor = Color(0xFFE5E5E5)
private val infoContainerColor = Color(0xFFEAF3FF)
private val cameraContainerColor = Color(0xFFEAF3FF)
private val disabledContentColor = Color(0xFFBDBDBD)
private val disabledTextColor = Color(0xFFAEB3C4)
private val authInputBackgroundColor = Color(0xFFF8FAFF)
private val sheetBackgroundColor = Color.White
private val photoContainerColor = Color(0xFFE7F2FF)
private val alertContainerColor = Color(0xFFFFEEEE)
private val successContainerColor = Color(0xFFE2F5EA)
private val warningContainerColor = Color(0xFFFFE2D6)
private val onWarningContainerColor = Color(0xFFFF6B00)
private val authBackgroundColor = Color(0xFFFFFFFF)
private val authPrimaryColor = Color(0xFF467EE5)
private val authOnSurfaceColor = Color(0xFF202632)
private val authOnSurfaceVariantColor = Color(0xFF7A8292)
private val homeMetricContainerColor = Color(0xFFEBF0FB)
private val homeMutedTextColor = Color(0xFF54596B)
private val homeTimelineTrackColor = Color(0xFFE8EBF2)
private val homeTimelineActiveColor = Color(0xFF4470ED)
private val homeTimelineInactiveColor = Color(0xFFA6A8B2)
private val homeTimelineDotBorderColor = Color(0xFFBFC2CC)
// 현재 체크 시점 링은 활성 파랑(#4470ED)의 16% 투명도다.
private val homeTimelineCurrentRingColor = Color(0x294470ED)
private val homeContactAlertContainerColor = Color(0xFFFEE2E2)
// 흰 카드가 배경과 구분되도록 Figma 홈 리디자인·회원탈퇴 화면에서 쓰는 옅은 회청색 페이지 배경이다.
private val pageBackgroundColor = Color(0xFFF6F9FC)
private val temperatureRiseContainerColor = Color(0xFFFFDDE2)
private val onTemperatureRiseContainerColor = Color(0xFFFF6172)
// 메뉴 드로어·회원탈퇴 화면에서 공통으로 쓰는 본문 강조/보조/3차 텍스트 색이다.
private val strongTextColor = Color(0xFF1F2633)
private val secondaryTextColor = Color(0xFF788294)
private val tertiaryTextColor = Color(0xFFAEB3C4)
private val subtleDividerColor = Color(0xFFE5EBF2)
// 드로어 뒤 화면을 가리는 딤으로, Figma의 검정 40% 투명도다.
private val overlayScrimColor = Color(0x66000000)

private val HeartGuardColorScheme = lightColorScheme(
    primary = primaryBlueColor,
    onPrimary = onPrimaryColor,
    primaryContainer = primaryContainerColor,
    onPrimaryContainer = onPrimaryContainerColor,
    secondary = secondaryColor,
    onSecondary = onSecondaryColor,
    secondaryContainer = secondaryContainerColor,
    onSecondaryContainer = onSecondaryContainerColor,
    tertiary = tertiaryColor,
    onTertiary = onTertiaryColor,
    tertiaryContainer = tertiaryContainerColor,
    onTertiaryContainer = onTertiaryContainerColor,
    background = backgroundColor,
    onBackground = onBackgroundColor,
    surface = surfaceColor,
    onSurface = onSurfaceColor,
    surfaceVariant = surfaceVariantColor,
    onSurfaceVariant = onSurfaceVariantColor,
    outline = outlineColor,
    outlineVariant = outlineVariantColor,
    error = errorColor,
    onError = onErrorColor,
    errorContainer = errorContainerColor,
    onErrorContainer = onErrorContainerColor,
)

private val heartGuardExtraColors = HeartGuardExtraColors(
    success = successColor,
    warning = warningColor,
    mutedText = mutedTextColor,
    cardBorder = cardBorderColor,
    infoContainer = infoContainerColor,
    cameraContainer = cameraContainerColor,
    disabledContent = disabledContentColor,
    disabledText = disabledTextColor,
    authInputBackground = authInputBackgroundColor,
    sheetBackground = sheetBackgroundColor,
    photoContainer = photoContainerColor,
    alertContainer = alertContainerColor,
    successContainer = successContainerColor,
    warningContainer = warningContainerColor,
    onWarningContainer = onWarningContainerColor,
    authBackground = authBackgroundColor,
    authPrimary = authPrimaryColor,
    authOnSurface = authOnSurfaceColor,
    authOnSurfaceVariant = authOnSurfaceVariantColor,
    homeMetricContainer = homeMetricContainerColor,
    homeMutedText = homeMutedTextColor,
    homeTimelineTrack = homeTimelineTrackColor,
    homeTimelineActive = homeTimelineActiveColor,
    homeTimelineInactive = homeTimelineInactiveColor,
    homeTimelineDotBorder = homeTimelineDotBorderColor,
    homeTimelineCurrentRing = homeTimelineCurrentRingColor,
    homeContactAlertContainer = homeContactAlertContainerColor,
    pageBackground = pageBackgroundColor,
    temperatureRiseContainer = temperatureRiseContainerColor,
    onTemperatureRiseContainer = onTemperatureRiseContainerColor,
    strongText = strongTextColor,
    secondaryText = secondaryTextColor,
    tertiaryText = tertiaryTextColor,
    subtleDivider = subtleDividerColor,
    overlayScrim = overlayScrimColor,
)

/**
 * Figma 현장앱 Light 화면을 기반으로 정리한 semantic color와 Pretendard typography를 앱 전체에 제공합니다.
 * 이번 디자인 베이스 단계에서는 Android Dynamic Color와 Dark UI를 사용하지 않습니다.
 */
@Composable
fun HeartGuardTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHeartGuardExtraColors provides heartGuardExtraColors,
    ) {
        MaterialTheme(
            colorScheme = HeartGuardColorScheme,
            typography = HeartGuardTypography,
            shapes = HeartGuardShapes,
            content = content,
        )
    }
}
