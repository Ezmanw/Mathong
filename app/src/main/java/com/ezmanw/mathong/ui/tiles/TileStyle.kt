package com.ezmanw.mathong.ui.tiles

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import com.ezmanw.mathong.data.FaceStyle
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.data.TileTheme

/** Tonal color roles for each suit, harmonized to the app's color scheme. */
data class SuitColors(
    val red: TonalPair,
    val green: TonalPair,
    val blue: TonalPair,
    val orange: TonalPair,
    val purple: TonalPair,
    val neutral: TonalPair,
)

/** Everything needed to draw a tile. */
data class TileStyle(
    val palette: TilePalette,
    val face: FaceStyle,
    val showIndex: Boolean,
    val suits: SuitColors,
    val cornerFraction: Float,
)

object TileStyles {
    // Lab hue angles for the classic suit colors.
    private const val RED = 30.0
    private const val GREEN = 145.0
    private const val BLUE = 275.0
    private const val ORANGE = 62.0
    private const val PURPLE = 318.0

    fun suits(scheme: ColorScheme, dark: Boolean): SuitColors {
        val primaryHue = ColorMath.toLch(scheme.primary).h
        fun pair(hue: Double, chroma: Double): TonalPair {
            val h = ColorMath.harmonize(hue, primaryHue)
            return if (dark) {
                TonalPair(ColorMath.fromLch(80.0, chroma * 0.75, h), ColorMath.fromLch(32.0, chroma * 0.45, h))
            } else {
                TonalPair(ColorMath.fromLch(42.0, chroma, h), ColorMath.fromLch(87.0, chroma * 0.42, h))
            }
        }
        return SuitColors(
            red = pair(RED, 70.0),
            green = pair(GREEN, 55.0),
            blue = pair(BLUE, 60.0),
            orange = pair(ORANGE, 65.0),
            purple = pair(PURPLE, 55.0),
            neutral = if (dark) {
                TonalPair(ColorMath.fromLch(90.0, 6.0, primaryHue), ColorMath.fromLch(34.0, 8.0, primaryHue))
            } else {
                TonalPair(ColorMath.fromLch(22.0, 8.0, primaryHue), ColorMath.fromLch(88.0, 10.0, primaryHue))
            },
        )
    }

    /** Tile body derived from the color scheme: a light face over two primary-tinted edge tones. */
    fun materialPalette(scheme: ColorScheme, dark: Boolean): TilePalette {
        val hue = ColorMath.toLch(scheme.primary).h
        return if (dark) {
            val face = ColorMath.fromLch(22.0, 6.0, hue)
            TilePalette(face, face, ColorMath.fromLch(40.0, 22.0, hue), ColorMath.fromLch(28.0, 18.0, hue), face)
        } else {
            val face = ColorMath.fromLch(99.0, 2.0, hue)
            TilePalette(face, face, ColorMath.fromLch(82.0, 22.0, hue), ColorMath.fromLch(62.0, 30.0, hue), face)
        }
    }

    fun build(settings: Settings, scheme: ColorScheme): TileStyle {
        val dark = scheme.surface.luminance() < 0.5f
        val material = settings.faceStyle == FaceStyle.MATERIAL
        return TileStyle(
            palette = if (material) materialPalette(scheme, dark) else settings.tileTheme.palette(),
            face = settings.faceStyle,
            showIndex = settings.showIndices,
            suits = suits(scheme, dark),
            cornerFraction = if (material) 0.2f else 0.12f,
        )
    }
}

@Composable
fun rememberTileStyle(settings: Settings): TileStyle {
    val scheme = MaterialTheme.colorScheme
    return remember(settings.faceStyle, settings.tileTheme, settings.showIndices, scheme) {
        TileStyles.build(settings, scheme)
    }
}

/** Convenience for previews that override a single setting. */
@Composable
fun rememberTileStyle(face: FaceStyle, theme: TileTheme = TileTheme.JADE, showIndex: Boolean = false): TileStyle =
    rememberTileStyle(Settings(faceStyle = face, tileTheme = theme, showIndices = showIndex))

