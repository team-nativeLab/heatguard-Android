package com.nativelap.heartguard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val HeartGuardShapes = Shapes(
    small = RoundedCornerShape(HeartGuardRadius.Small),
    medium = RoundedCornerShape(HeartGuardRadius.Button),
    large = RoundedCornerShape(HeartGuardRadius.Card),
)
