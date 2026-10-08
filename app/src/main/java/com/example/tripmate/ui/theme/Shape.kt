package com.example.tripmate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material3 shape slots mapped to DESIGN.md's `rounded` scale
val TripMateShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.radiusSm),   // 4dp
    small = RoundedCornerShape(Dimens.radiusMd),        // 12dp — inputs & chips
    medium = RoundedCornerShape(Dimens.radiusLg),        // 16dp
    large = RoundedCornerShape(Dimens.radiusCard),       // 20dp — standard cards
    extraLarge = RoundedCornerShape(28.dp)              // standard dialogs
)
val TripPilotShapes = TripMateShapes
