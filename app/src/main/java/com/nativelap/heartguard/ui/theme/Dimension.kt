package com.nativelap.heartguard.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object HeartGuardSpacing {
    val ScreenHorizontal = 16.dp
    val HeaderHorizontal = 24.dp
    val RecordContentHorizontal = 28.dp
    val Section = 20.dp
    val Item = 12.dp
    val Compact = 8.dp
    val Tight = 4.dp
    val Hairline = 1.dp
}

object HeartGuardRadius {
    val Card = 16.dp
    val Button = 8.dp
    val PrimaryAction = 12.dp
    val Checkbox = 8.dp
    val Small = 4.dp
    val Pill = 100.dp
}

object HeartGuardIconSize {
    val Navigation = 24.dp
    val HeaderAction = 28.dp
    val RecordOption = 48.dp
    val Checkbox = 24.dp
    val CameraAction = 42.dp
    val BrandMark = 80.dp
    val Weather = 72.dp
    val StatusIndicator = 12.dp
    val Small = 16.dp
}

object HeartGuardComponentSize {
    val PrimaryButtonHeight = 48.dp
    val TextFieldHeight = 48.dp
    val TouchTarget = 48.dp
    // Figma 카드의 콘텐츠가 잘리지 않도록 최소 높이만 제공하고, 긴 텍스트에는 높이를 열어 둔다.
    val RecordOptionMinHeight = 110.dp
    val CameraAction = 64.dp
    val WeatherCardMinHeight = 160.dp
}

object HeartGuardBorderWidth {
    val Checkbox = 2.dp
}

object HeartGuardFontSize {
    val PageTitle = 20.sp
    val RecordOptionTitle = 16.sp
    val RecordOptionDescription = 14.sp
}
