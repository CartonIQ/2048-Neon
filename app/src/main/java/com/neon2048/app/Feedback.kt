package com.neon2048.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** Haptics plus tiny synthesized sound effects (generated in code, no audio files needed). */
object Feedback {
    private const val RATE = 22050
    private val handler = Handler(Looper.getMainLooper())
    private val cache = HashMap<String, ShortArray>()

    fun click(view: View) {
        haptic(view, HapticFeedbackConstants.VIRTUAL_KEY)
        sound(view.context, "click") { synth(listOf(1400.0 to 22), 0.30) }
    }

    fun tab(view: View) {
        haptic(view, HapticFeedbackConstants.KEYBOARD_TAP)
        sound(view.context, "tab") { synth(listOf(880.0 to 30, 1175.0 to 40), 0.30) }
    }

    fun moved(view: View) {
        haptic(view, HapticFeedbackConstants.KEYBOARD_TAP)
        sound(view.context, "move") { synth(listOf(196.0 to 45), 0.30) }
    }

    fun merged(view: View, value: Int) {
        haptic(view, HapticFeedbackConstants.CONTEXT_CLICK)
        val step = (ln(value.toDouble()) / ln(2.0)).roundToInt().coerceIn(1, 14)
        sound(view.context, "merge$step") {
            val f = 261.63 * 2.0.pow(((step - 1) * 2) / 12.0)
            synth(listOf(f to 70, f * 1.5 to 120), 0.40)
        }
    }

    fun won(view: View) {
        haptic(view, HapticFeedbackConstants.LONG_PRESS)
        sound(view.context, "won") {
            synth(listOf(523.25 to 110, 659.25 to 110, 783.99 to 110, 1046.5 to 280), 0.45)
        }
    }

    fun over(view: View) {
        haptic(view, HapticFeedbackConstants.LONG_PRESS)
        sound(view.context, "over") {
            synth(listOf(392.0 to 150, 329.63 to 150, 261.63 to 150, 220.0 to 320), 0.40)
        }
    }

    private fun haptic(view: View, type: Int) {
        if (Prefs.hapticsOn(view.context)) view.performHapticFeedback(type)
    }

    private fun sound(context: Context, key: String, build: () -> ShortArray) {
        if (!Prefs.soundOn(context)) return
        try {
            val data = cache.getOrPut(key, build)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            if (track.state != AudioTrack.STATE_INITIALIZED) {
                track.release()
                return
            }
            track.write(data, 0, data.size)
            track.play()
            val lifetime = data.size * 1000L / RATE + 150L
            handler.postDelayed({
                try {
                    track.release()
                } catch (e: Exception) {
                    // ignore
                }
            }, lifetime)
        } catch (e: Exception) {
            // Sound is optional; never crash the game because of it.
        }
    }

    private fun synth(notes: List<Pair<Double, Int>>, volume: Double): ShortArray {
        val total = notes.fold(0) { acc, n -> acc + RATE * n.second / 1000 }
        val out = ShortArray(total)
        var pos = 0
        for ((freq, ms) in notes) {
            val n = RATE * ms / 1000
            for (i in 0 until n) {
                val t = i.toDouble() / RATE
                val attack = min(1.0, i / (RATE * 0.004))
                val decay = exp(-4.5 * i / n)
                val wave = sin(2 * PI * freq * t) * 0.75 + sin(4 * PI * freq * t) * 0.2
                out[pos + i] = (wave * attack * decay * volume * 32767.0).toInt().toShort()
            }
            pos += n
        }
        return out
    }
}
