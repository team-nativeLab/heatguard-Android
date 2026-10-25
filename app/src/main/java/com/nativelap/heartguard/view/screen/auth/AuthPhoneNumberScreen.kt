package com.nativelap.heartguard.view.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.auth.AuthTitleBlock
import com.nativelap.heartguard.view.component.heartGuardResponsivePage

@Composable
internal fun AuthPhoneNumberScreen(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    onLinkClick: () -> Unit,
    isSubmitting: Boolean = false,
    statusMessage: String? = null,
    onCancelClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier =
            modifier
                .heartGuardResponsivePage(
                    MaterialTheme.extraColors.authBackground,
                ).widthIn(max = PHONE_SCREEN_MAX_WIDTH),
        containerColor = MaterialTheme.extraColors.authBackground,
        bottomBar = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(horizontal = PHONE_SCREEN_HORIZONTAL_PADDING, vertical = PHONE_SCREEN_BOTTOM_PADDING),
            ) {
                Column {
                    PhoneLinkButton(
                        onClick = onLinkClick,
                        enabled = !isSubmitting && phoneNumber.any(Char::isDigit),
                    )
                    if (onCancelClick != null) {
                        TextButton(
                            onClick = onCancelClick,
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.auth_phone_cancel_link))
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .padding(horizontal = PHONE_SCREEN_HORIZONTAL_PADDING)
                    .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthTop - innerPadding.calculateTopPadding()))
            AuthTitleBlock(
                title = stringResource(R.string.auth_phone_title),
                description = stringResource(R.string.auth_phone_description),
                titleFontSize = 26.sp,
                titleLineHeight = 36.sp,
            )
            Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthTitleForm))
            Text(
                text = stringResource(R.string.auth_phone_label),
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontSize = PHONE_LABEL_TEXT_SIZE,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.extraColors.strongText,
            )
            Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthFieldLabelInput))
            PhoneNumberInput(
                phoneNumber = phoneNumber,
                onPhoneNumberChange = onPhoneNumberChange,
            )
            Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
            Row(
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.width(PHONE_SECURITY_ICON_SIZE),
                    tint = MaterialTheme.extraColors.secondaryText,
                )
                Text(
                    text = stringResource(R.string.auth_phone_security_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extraColors.strongText,
                )
            }
            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
                Text(
                    text = statusMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PhoneNumberInput(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = PHONE_INPUT_FOCUSED_BORDER
    val phoneNumberLabel = stringResource(R.string.auth_phone_label)
    val clearLabel = stringResource(R.string.common_clear)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = PHONE_INPUT_HEIGHT)
                .shadow(
                    elevation = PHONE_INPUT_FOCUS_ELEVATION,
                    shape = RoundedCornerShape(PHONE_INPUT_RADIUS),
                    ambientColor = PHONE_INPUT_FOCUS_SHADOW,
                    spotColor = PHONE_INPUT_FOCUS_SHADOW,
                ).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(PHONE_INPUT_RADIUS))
                .border(PHONE_INPUT_BORDER_WIDTH, borderColor, RoundedCornerShape(PHONE_INPUT_RADIUS))
                .padding(horizontal = PHONE_INPUT_HORIZONTAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.auth_phone_country_code),
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = PHONE_COUNTRY_TEXT_SIZE,
                    fontWeight = FontWeight.Medium,
                ),
            color = MaterialTheme.extraColors.strongText,
        )
        Spacer(modifier = Modifier.width(PHONE_INPUT_COUNTRY_GAP))
        Box(
            modifier =
                Modifier
                    .width(PHONE_INPUT_DIVIDER_WIDTH)
                    .height(PHONE_INPUT_DIVIDER_HEIGHT)
                    .background(MaterialTheme.extraColors.subtleDivider),
        )
        Spacer(modifier = Modifier.width(PHONE_INPUT_FIELD_GAP))
        BasicTextField(
            value = phoneNumber,
            onValueChange = { enteredPhoneNumber ->
                onPhoneNumberChange(
                    enteredPhoneNumber
                        .filter { digit -> digit in '0'..'9' }
                        .take(PHONE_NUMBER_MAX_LENGTH),
                )
            },
            modifier =
                Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = phoneNumberLabel
                    },
            singleLine = true,
            textStyle =
                TextStyle(
                    color = MaterialTheme.extraColors.strongText,
                    fontSize = PHONE_INPUT_TEXT_SIZE,
                    fontWeight = FontWeight.SemiBold,
                ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            cursorBrush = SolidColor(PHONE_INPUT_FOCUSED_BORDER),
            visualTransformation = PHONE_NUMBER_VISUAL_TRANSFORMATION,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (phoneNumber.isEmpty()) {
                        Text(
                            text = stringResource(R.string.auth_phone_hint),
                            color = MaterialTheme.extraColors.strongText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = PHONE_INPUT_TEXT_SIZE),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (phoneNumber.isNotEmpty()) {
            Box(
                modifier =
                    Modifier
                        .width(PHONE_CLEAR_TARGET_SIZE)
                        .height(PHONE_CLEAR_TARGET_SIZE)
                        .semantics {
                            contentDescription = clearLabel
                        }.clickable(role = Role.Button, onClickLabel = clearLabel, onClick = {
                            onPhoneNumberChange("")
                        }),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(PHONE_CLEAR_ICON_SIZE)
                            .height(PHONE_CLEAR_ICON_SIZE)
                            .background(PHONE_CLEAR_BACKGROUND, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.auth_phone_clear_symbol),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = PHONE_CLEAR_TEXT_SIZE,
                                fontWeight = FontWeight.Bold,
                            ),
                        color = MaterialTheme.extraColors.strongText,
                    )
                }
            }
        }
    }
}

private fun formatPhoneNumberForDisplay(phoneNumber: String): String =
    when {
        phoneNumber.length <= PHONE_NUMBER_AREA_CODE_LENGTH -> {
            phoneNumber
        }

        phoneNumber.length <= PHONE_NUMBER_FIRST_PART_END -> {
            phoneNumber.substring(0, PHONE_NUMBER_AREA_CODE_LENGTH) +
                PHONE_NUMBER_SEPARATOR +
                phoneNumber.substring(PHONE_NUMBER_AREA_CODE_LENGTH)
        }

        else -> {
            phoneNumber.substring(0, PHONE_NUMBER_AREA_CODE_LENGTH) +
                PHONE_NUMBER_SEPARATOR +
                phoneNumber.substring(PHONE_NUMBER_AREA_CODE_LENGTH, PHONE_NUMBER_FIRST_PART_END) +
                PHONE_NUMBER_SEPARATOR +
                phoneNumber.substring(PHONE_NUMBER_FIRST_PART_END)
        }
    }

private val PHONE_NUMBER_VISUAL_TRANSFORMATION =
    VisualTransformation { phoneNumber ->
        TransformedText(
            text = AnnotatedString(formatPhoneNumberForDisplay(phoneNumber.text)),
            offsetMapping = PHONE_NUMBER_OFFSET_MAPPING,
        )
    }

private val PHONE_NUMBER_OFFSET_MAPPING =
    object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int =
            when {
                offset <= PHONE_NUMBER_AREA_CODE_LENGTH -> offset
                offset <= PHONE_NUMBER_FIRST_PART_END -> offset + 1
                else -> offset + 2
            }

        override fun transformedToOriginal(offset: Int): Int =
            when {
                offset <= PHONE_NUMBER_AREA_CODE_LENGTH -> offset
                offset <= PHONE_NUMBER_FIRST_PART_END + 1 -> offset - 1
                else -> offset - 2
            }
    }

@Composable
private fun PhoneLinkButton(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(PHONE_LINK_BUTTON_HEIGHT)
                .shadow(
                    PHONE_LINK_BUTTON_ELEVATION,
                    RoundedCornerShape(PHONE_LINK_BUTTON_RADIUS),
                    spotColor = PHONE_LINK_BUTTON_SHADOW,
                ).background(
                    Brush.linearGradient(PHONE_LINK_BUTTON_GRADIENT),
                    RoundedCornerShape(PHONE_LINK_BUTTON_RADIUS),
                ).clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.auth_phone_link),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun AuthPhoneNumberScreenPreview() {
    HeartGuardTheme {
        AuthPhoneNumberScreen("01012345678", {}, {})
    }
}

@Preview(name = "Compact phone · 200% text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 2f)
@Composable
private fun AuthPhoneNumberScreenCompactPreview() {
    HeartGuardTheme {
        AuthPhoneNumberScreen("01012345678", {}, {})
    }
}

private val PHONE_SCREEN_MAX_WIDTH = 402.dp
private val PHONE_SCREEN_HORIZONTAL_PADDING = 24.dp
private val PHONE_SCREEN_BOTTOM_PADDING = 16.dp
private val PHONE_INPUT_HEIGHT = 58.dp
private val PHONE_INPUT_RADIUS = 16.dp
private val PHONE_INPUT_BORDER_WIDTH = 1.5.dp
private val PHONE_INPUT_FOCUSED_BORDER = Color(0xFF2E80ED)
private val PHONE_INPUT_FOCUS_SHADOW = Color(0x1F2E80ED)
private val PHONE_INPUT_FOCUS_ELEVATION = 4.dp
private val PHONE_INPUT_HORIZONTAL_PADDING = 18.dp
private val PHONE_INPUT_COUNTRY_GAP = 12.dp
private val PHONE_INPUT_FIELD_GAP = 12.dp
private val PHONE_INPUT_DIVIDER_WIDTH = 1.dp
private val PHONE_INPUT_DIVIDER_HEIGHT = 18.dp
private val PHONE_COUNTRY_TEXT_SIZE = 15.sp
private val PHONE_LABEL_TEXT_SIZE = 13.sp
private val PHONE_INPUT_TEXT_SIZE = 17.sp
private val PHONE_CLEAR_TARGET_SIZE = 48.dp
private val PHONE_CLEAR_ICON_SIZE = 18.dp
private val PHONE_CLEAR_BACKGROUND = Color(0xFFD5DBE5)
private val PHONE_CLEAR_TEXT_SIZE = 12.sp
private val PHONE_SECURITY_ICON_SIZE = 14.dp
private val PHONE_LINK_BUTTON_HEIGHT = 54.dp
private val PHONE_LINK_BUTTON_RADIUS = 16.dp
private val PHONE_LINK_BUTTON_ELEVATION = 8.dp
private val PHONE_LINK_BUTTON_SHADOW = Color(0x472E80ED)
private val PHONE_LINK_BUTTON_GRADIENT = listOf(Color(0xFF3B8CF5), Color(0xFF1F6FE0))
private const val PHONE_NUMBER_AREA_CODE_LENGTH = 3
private const val PHONE_NUMBER_FIRST_PART_END = 7
private const val PHONE_NUMBER_MAX_LENGTH = 11
private const val PHONE_NUMBER_SEPARATOR = "-"
