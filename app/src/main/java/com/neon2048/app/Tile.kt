package com.neon2048.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils

enum class Direction { LEFT, RIGHT, UP, DOWN }

enum class TileTheme {
    NEON, CLASSIC, PASTEL;

    companion object {
        fun fromIndex(index: Int): TileTheme = values().getOrElse(index) { NEON }
    }
}

/** One tile travelling from cell [from] to cell [to] (row-major indices). */
class TileMove(val from: Int, val to: Int, val value: Int)

class MoveResult(
    val moved: Boolean,
    val gained: Int,
    val merged: Set<Int>,
    val moves: List<TileMove>
)

/** Colors, drawables and text styling for a tile value in a given tile theme. */
object Tile {
    private val NEON = mapOf(
        0 to "#1F2430", 2 to "#2A2F3F", 4 to "#313A4A", 8 to "#FF5C8A",
        16 to "#FF7EB3", 32 to "#FFB347", 64 to "#FFD93D", 128 to "#4DFFB8",
        256 to "#00F0FF", 512 to "#4D9EFF", 1024 to "#A06CFF", 2048 to "#FF4DE3"
    )
    private val CLASSIC = mapOf(
        0 to "#40CDC1B4", 2 to "#EEE4DA", 4 to "#EDE0C8", 8 to "#F2B179",
        16 to "#F59563", 32 to "#F67C5F", 64 to "#F65E3B", 128 to "#EDCF72",
        256 to "#EDCC61", 512 to "#EDC850", 1024 to "#EDC53F", 2048 to "#EDC22E"
    )
    private val PASTEL = mapOf(
        0 to "#1A808080", 2 to "#FDE2E4", 4 to "#FAD2E1", 8 to "#FFC8DD",
        16 to "#BDE0FE", 32 to "#A2D2FF", 64 to "#CDB4DB", 128 to "#CAFFBF",
        256 to "#FDFFB6", 512 to "#FFD6A5", 1024 to "#9BF6FF", 2048 to "#FFADAD"
    )

    private fun table(theme: TileTheme): Map<Int, String> = when (theme) {
        TileTheme.NEON -> NEON
        TileTheme.CLASSIC -> CLASSIC
        TileTheme.PASTEL -> PASTEL
    }

    fun fillColor(theme: TileTheme, value: Int): Int {
        val t = table(theme)
        val key = if (value > 2048) 2048 else value
        return Color.parseColor(t[key] ?: t.getValue(0))
    }

    fun textColor(context: Context, theme: TileTheme, value: Int): Int {
        val bright = ColorUtils.calculateLuminance(fillColor(theme, value)) > 0.4
        return ContextCompat.getColor(
            context,
            if (bright) R.color.tile_text_dark else R.color.tile_text_light
        )
    }

    fun textSizeSp(value: Int): Float = when {
        value < 100 -> 28f
        value < 1000 -> 24f
        value < 10000 -> 20f
        else -> 16f
    }

    fun background(context: Context, theme: TileTheme, value: Int): Drawable {
        if (theme == TileTheme.NEON) {
            val res = when (value) {
                0 -> R.drawable.bg_tile_default
                2 -> R.drawable.bg_tile_2
                4 -> R.drawable.bg_tile_4
                8 -> R.drawable.bg_tile_8
                16 -> R.drawable.bg_tile_16
                32 -> R.drawable.bg_tile_32
                64 -> R.drawable.bg_tile_64
                128 -> R.drawable.bg_tile_128
                256 -> R.drawable.bg_tile_256
                512 -> R.drawable.bg_tile_512
                1024 -> R.drawable.bg_tile_1024
                else -> R.drawable.bg_tile_2048
            }
            return ContextCompat.getDrawable(context, res)!!
        }
        val d = GradientDrawable()
        d.shape = GradientDrawable.RECTANGLE
        d.cornerRadius = 12f * context.resources.displayMetrics.density
        val fill = fillColor(theme, value)
        d.orientation = GradientDrawable.Orientation.TOP_BOTTOM
        d.setColors(intArrayOf(ColorUtils.blendARGB(fill, Color.WHITE, 0.14f), fill))
        return d
    }
}
