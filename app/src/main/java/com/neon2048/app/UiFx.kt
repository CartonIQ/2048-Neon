package com.neon2048.app

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.TextView
import androidx.core.content.ContextCompat

object UiFx {
    /** Paints a TextView with the neon cyan -> violet -> pink gradient. */
    fun neonGradient(tv: TextView) {
        tv.post {
            val w = tv.paint.measureText(tv.text.toString()).coerceAtLeast(1f)
            val colors = intArrayOf(
                ContextCompat.getColor(tv.context, R.color.neon_cyan),
                Color.parseColor("#A06CFF"),
                Color.parseColor("#FF4DE3")
            )
            tv.paint.shader = LinearGradient(0f, 0f, w, 0f, colors, null, Shader.TileMode.CLAMP)
            tv.invalidate()
        }
    }
}
