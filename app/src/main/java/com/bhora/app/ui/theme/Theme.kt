package com.bhora.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BhoraDarkColorScheme = darkColorScheme(
    primary = BhoraColors.Primary,
    onPrimary = BhoraColors.OnPrimary,
    primaryContainer = BhoraColors.PrimaryContainer,
    onPrimaryContainer = BhoraColors.OnPrimaryContainer,
    secondary = BhoraColors.Secondary,
    onSecondary = BhoraColors.OnSecondary,
    secondaryContainer = BhoraColors.SecondaryContainer,
    onSecondaryContainer = BhoraColors.OnSecondaryContainer,
    tertiary = BhoraColors.Tertiary,
    onTertiary = BhoraColors.OnTertiary,
    tertiaryContainer = BhoraColors.TertiaryContainer,
    onTertiaryContainer = BhoraColors.OnTertiaryContainer,
    background = BhoraColors.Surface,
    onBackground = BhoraColors.OnSurface,
    surface = BhoraColors.Surface,
    onSurface = BhoraColors.OnSurface,
    surfaceVariant = BhoraColors.SurfaceVariant,
    onSurfaceVariant = BhoraColors.OnSurfaceVariant,
    outline = BhoraColors.Outline,
    outlineVariant = BhoraColors.OutlineVariant,
    error = BhoraColors.Error,
    onError = BhoraColors.OnError,
    errorContainer = BhoraColors.ErrorContainer,
    onErrorContainer = BhoraColors.OnErrorContainer
)

private val BhoraSharpShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

@Composable
fun BhoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BhoraDarkColorScheme,
        typography = BhoraMaterialTypography,
        shapes = BhoraSharpShapes,
        content = content
    )
}
