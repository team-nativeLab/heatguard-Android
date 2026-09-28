package com.nativelap.heartguard.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize

/** Keeps screen content readable on wide windows while preserving a full-size layout viewport. */
fun Modifier.heartGuardResponsivePage(containerColor: Color): Modifier =
    fillMaxSize()
        .background(containerColor)
        .wrapContentWidth(align = Alignment.CenterHorizontally)
        .widthIn(max = HeartGuardComponentSize.ResponsiveContentMaxWidth)
