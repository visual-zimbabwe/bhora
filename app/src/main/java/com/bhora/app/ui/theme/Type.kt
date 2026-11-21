package com.bhora.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bhora.app.R

val EbGaramondFontFamily = FontFamily(
    Font(resId = R.font.eb_garamond_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.eb_garamond_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
)

val ManropeFontFamily = FontFamily(
    Font(resId = R.font.manrope_light, weight = FontWeight.Light, style = FontStyle.Normal),
    Font(resId = R.font.manrope_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.manrope_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.manrope_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal)
)

object BhoraTextStyles {
    val DisplayBrand = TextStyle(
        fontFamily = EbGaramondFontFamily,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.02).em
    )

    val MatchupTeam = TextStyle(
        fontFamily = EbGaramondFontFamily,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em
    )

    val MatchupTeamCompact = TextStyle(
        fontFamily = EbGaramondFontFamily,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em
    )

    val MatchupSeparator = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontStyle = FontStyle.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.30.em
    )

    val KickoffTime = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.12.em
    )

    val StatusEditorial = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontStyle = FontStyle.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.em
    )

    val FilterEditorial = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontStyle = FontStyle.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.25.em
    )

    val RegionEyebrow = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontStyle = FontStyle.Normal,
        fontSize = 9.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.25.em
    )

    val RegionEntry = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Normal,
        fontSize = 11.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.16.em
    )

    val BodyMuted = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Light,
        fontStyle = FontStyle.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.06.em
    )
}

val BhoraMaterialTypography = Typography(
    displayLarge = BhoraTextStyles.DisplayBrand,
    headlineLarge = BhoraTextStyles.MatchupTeam,
    bodyLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em
    ),
    labelMedium = BhoraTextStyles.StatusEditorial
)
