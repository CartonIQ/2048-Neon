package com.neon2048.app

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object Prefs {
    private const val FILE = "neon2048_prefs"

    const val KEY_BEST = "best_score"
    const val KEY_NIGHT = "night_mode"
    const val KEY_TILE_THEME = "tile_theme"
    const val KEY_BOARD = "saved_board"
    const val KEY_SCORE = "saved_score"
    const val KEY_KEEP = "keep_playing"
    const val KEY_SAVED = "score_saved"
    const val KEY_WIN_RECORDED = "win_recorded"
    const val KEY_SOUND = "sound_on"
    const val KEY_HAPTICS = "haptics_on"
    const val KEY_HOWTO_SEEN = "howto_seen"

    const val STAT_GAMES = "stat_games"
    const val STAT_WINS = "stat_wins"
    const val STAT_MOVES = "stat_moves"
    const val STAT_BEST_TILE = "stat_best_tile"
    const val STAT_TOTAL_SCORE = "stat_total_score"

    const val NIGHT_LIGHT = 0
    const val NIGHT_DARK = 1
    const val NIGHT_SYSTEM = 2

    fun get(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun soundOn(context: Context): Boolean = get(context).getBoolean(KEY_SOUND, true)

    fun hapticsOn(context: Context): Boolean = get(context).getBoolean(KEY_HAPTICS, true)

    fun nightMode(context: Context): Int =
        when (get(context).getInt(KEY_NIGHT, NIGHT_SYSTEM)) {
            NIGHT_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            NIGHT_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
}
