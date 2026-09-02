package com.routecj.driver.presentation.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import com.routecj.driver.R

enum class LogoVariant {
    DARK_BG,
    LIGHT_BG,
    TRANSPARENT_WHITE,
    TRANSPARENT_NAVY,
    ICON_ONLY
}

/**
 * Master RouteCJ Brand Logo Component.
 * Enforces the brand typography:
 *     ROUTE
 *      CJ
 * with ROUTE dominant, CJ centered below, and the integrated route/GPS symbol.
 * Uses Android Vector Drawables exclusively (no Canvas, no Bitmaps, no system font dependency).
 */
@Composable
fun RouteCJLogo(
    modifier: Modifier = Modifier,
    variant: LogoVariant = LogoVariant.DARK_BG,
    height: Dp = 150.dp,
    showSymbol: Boolean = true,
    routeFontSize: TextUnit? = null,
    cjFontSize: TextUnit? = null,
    showSlogan: Boolean = false
) {
    val logoDrawableRes = when (variant) {
        LogoVariant.ICON_ONLY -> R.drawable.ic_routecj_symbol
        LogoVariant.LIGHT_BG, LogoVariant.TRANSPARENT_NAVY -> R.drawable.ic_routecj_logo_light
        else -> R.drawable.ic_routecj_logo_dark
    }

    Icon(
        painter = painterResource(id = logoDrawableRes),
        contentDescription = "RouteCJ Master Brand Logo",
        tint = Color.Unspecified,
        modifier = modifier.height(height)
    )
}
