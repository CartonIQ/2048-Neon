package com.neon2048.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnEnd
import kotlin.random.Random

/** Neon confetti burst used to celebrate reaching the 2048 tile. Never intercepts touches. */
class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private class Piece(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        val color: Int,
        var rot: Float,
        val spin: Float
    )

    private val density = resources.displayMetrics.density
    private val pieces = ArrayList<Piece>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val palette = intArrayOf(
        0xFF00F0FF.toInt(), 0xFF7000FF.toInt(), 0xFFFF4DE3.toInt(),
        0xFFFFD93D.toInt(), 0xFF4DFFB8.toInt(), 0xFFFF5C8A.toInt()
    )
    private var animator: ValueAnimator? = null
    private var lastNanos = 0L
    private var progress = 0f

    fun burst() {
        if (width == 0 || height == 0) return
        cancel()
        pieces.clear()
        repeat(120) {
            pieces.add(
                Piece(
                    x = width * Random.nextFloat(),
                    y = -Random.nextFloat() * height * 0.3f,
                    vx = (Random.nextFloat() - 0.5f) * 420f * density,
                    vy = (200f + Random.nextFloat() * 600f) * density,
                    size = (6f + Random.nextFloat() * 8f) * density,
                    color = palette[Random.nextInt(palette.size)],
                    rot = Random.nextFloat() * 360f,
                    spin = (Random.nextFloat() - 0.5f) * 720f
                )
            )
        }
        lastNanos = System.nanoTime()
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 2800
        a.addUpdateListener { anim ->
            progress = anim.animatedValue as Float
            step()
            invalidate()
        }
        a.doOnEnd {
            pieces.clear()
            invalidate()
        }
        animator = a
        a.start()
    }

    fun cancel() {
        animator?.cancel()
        animator = null
        pieces.clear()
    }

    private fun step() {
        val now = System.nanoTime()
        val dt = ((now - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
        lastNanos = now
        val gravity = 1300f * density
        for (p in pieces) {
            p.vy += gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.spin * dt
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pieces.isEmpty()) return
        val alpha = if (progress < 0.75f) 255 else (255 * (1f - (progress - 0.75f) / 0.25f)).toInt().coerceIn(0, 255)
        for (p in pieces) {
            paint.color = p.color
            paint.alpha = alpha
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rot)
            canvas.drawRect(-p.size / 2f, -p.size / 4f, p.size / 2f, p.size / 4f, paint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        cancel()
        super.onDetachedFromWindow()
    }
}
