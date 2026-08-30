package com.example.tripmate.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Maps 1:1 to the Tailwind spacing scale defined in DESIGN.md
 * base: 4, xs: 8, sm: 12, md: 16, lg: 24, xl: 32, 2xl: 48, 3xl: 64
 * marginMobile: 20
 */
object Dimens {
    val base = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp

    val marginMobile = 20.dp
    val gutter = 16.dp

    // Radii (from DESIGN.md `rounded` tokens)
    val radiusSm = 4.dp        // rounded-sm (0.25rem)
    val radiusDefault = 8.dp   // DEFAULT (0.5rem)
    val radiusMd = 12.dp       // md (0.75rem) — inputs & chips
    val radiusLg = 16.dp       // lg (1rem)
    val radiusXl = 24.dp       // xl (1.5rem)
    val radiusCard = 20.dp     // Standard cards per DESIGN.md "Shapes" section
    val radiusFull = 999.dp    // pill buttons
}
