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
private val backgroundColor = Color(0xFFF6F9FC)
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
private val sheetBackgroundColor = Color(0xFFF9FBFC)
private val photoContainerColor = Color(0xFFE7F2FF)
private val alertContainerColor = Color(0xFFFFEEEE)
private val successContainerColor = Color(0xFFE2F5EA)
private val warningContainerColor = Color(0xFFFFE2D6)
private val onWarningContainerColor = Color(0xFFFF6B00)
private val authBackgroundColor = Color(0xFFFFFFFF)
private val authPrimaryColor = Color(0xFF467EE5)
private val authOnSurfaceColor = Color(0xFF202632)
private val authOnSurfaceVariantColor = Color(0xFF7A8292)

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
