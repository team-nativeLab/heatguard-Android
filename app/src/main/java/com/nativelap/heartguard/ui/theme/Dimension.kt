package com.nativelap.heartguard.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object HeartGuardSpacing {
    val ScreenHorizontal = 16.dp
    val HeaderHorizontal = 24.dp
    val RecordContentHorizontal = 28.dp
    val AuthHorizontal = 40.dp
    val AuthTop = 142.dp
    val AuthLogoTitle = 20.dp
    val AuthTitleForm = 31.dp
    val AuthFormButton = 19.dp
    val AuthButtonPrompt = 0.dp
    val AuthFieldLabelInput = 6.dp
    val AuthFieldGroup = 26.dp
    val AuthTitleDescription = 4.dp
    val PageContentTop = 12.dp
    val HomeCardHorizontal = 25.dp
    val SectionTitleHorizontal = 43.dp
    val Section = 20.dp
    val CompactSection = 8.dp
    val LargeSection = 24.dp
    val Card = 16.dp
    val Item = 12.dp
    val Compact = 8.dp
    val Tight = 4.dp
    val Hairline = 1.dp
}

object HeartGuardRadius {
    val Card = 16.dp
    val LargeCard = 25.dp
    val WeatherCard = 32.dp
    val Sheet = 40.dp
    val Button = 8.dp
    val PrimaryAction = 12.dp
    val Checkbox = 8.dp
    val Small = 4.dp
    val Pill = 100.dp
}

object HeartGuardIconSize {
    val Navigation = 24.dp
    val HeaderAction = 28.dp
    val Information = 28.dp
    val RecordOption = 48.dp
    val Checkbox = 24.dp
    val CameraAction = 42.dp
    val BrandMark = 80.dp
    val Weather = 72.dp
    val WeatherIllustrationMax = 150.dp
    val TimelineMarker = 12.dp
    val EmergencyIndicatorMax = 112.dp
    val Result = 84.dp
    val StatusIndicator = 12.dp
    val Small = 16.dp
    val WeatherStatus = 48.dp
}

object HeartGuardComponentSize {
    val PrimaryButtonHeight = 48.dp
    val AuthButtonHeight = 40.dp
    val AuthContentMaxWidth = 280.dp
    val AuthActionMaxWidth = 318.dp
    val AuthTextFieldHeight = 45.dp
    val TextFieldHeight = 48.dp
    val TouchTarget = 48.dp
    // Figma 카드의 콘텐츠가 잘리지 않도록 최소 높이만 제공하고, 긴 텍스트에는 높이를 열어 둔다.
    val RecordOptionMinHeight = 110.dp
    val CameraAction = 64.dp
    val WeatherCardMinHeight = 244.dp
    val CompactScreenHeightBreakpoint = 840.dp
    val WeatherWideLayoutBreakpoint = 300.dp
    val MetricThreeColumnBreakpoint = 280.dp
    val CompactLayoutBreakpoint = 360.dp
    val RecordTypeOptionHeight = 143.dp
    val PhotoSelectionHeight = 313.dp
    val TemperatureErrorMessageMaxWidth = 180.dp
    val PhotoSelectionTitleMaxWidth = 200.dp
    val PhotoCaptureTextMaxWidth = 220.dp
    // 온도 기록 화면의 현장 사진 선택 영역은 작업·휴식 사진 카드보다 낮은 Figma 컴포넌트다.
    val FieldPhotoSelectionHeight = 104.dp
    val PhotoMemoMinHeight = 96.dp
    val TemperatureSummaryHeight = 138.dp
    val FieldPhotoCaptureRowHeight = 79.dp
    val HomeActionCardHeight = 148.dp
    val TemperatureDeltaMinWidth = 72.dp
}

object HeartGuardOverlayBlur {
    val FigmaBackdrop = 4.dp
}

object HeartGuardBorderWidth {
    val Checkbox = 2.dp
    val RecordTypeSelection = 2.dp
    val EmergencyIndicator = 8.dp
    val TimelineRail = 1.dp
}

object HeartGuardFontSize {
    val PageTitle = 20.sp
    val AuthTitle = 26.5.sp
    val AuthTitleLineHeight = 34.sp
    val AuthDescription = 14.5.sp
    val AuthInput = 14.sp
    val AuthLabel = 13.25.sp
    val AuthPrompt = 14.5.sp
    val AuthTitleLetterSpacing = (-0.25).sp
    val HeroTemperature = 40.sp
    val TemperatureSummary = 32.sp
    val RecordOptionTitle = 16.sp
    val RecordOptionDescription = 14.sp
    val Caption = 11.sp
    val SmallLabel = 13.sp
}
