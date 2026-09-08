package com.justmx.player.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi

@UnstableApi
object SettingsDataStore {

    private const val PREFS_NAME = "justmx_prefs"

    // Defaults
    const val BUFFER_NORMAL = "normal"
    const val BUFFER_LARGE = "large"
    const val BUFFER_ENORMOUS = "enormous"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Buffer sizes (ms) ---
    fun getBufferDurations(context: Context): Triple<Int, Int, Int> {
        val prefs = getPrefs(context)
        return when (prefs.getString("buffer_size", BUFFER_LARGE)) {
            BUFFER_NORMAL -> Triple(500, 1500, 500)      // ~1.5MB total
            BUFFER_ENORMOUS -> Triple(120_000, 240_000, 2_500)  // 120s/240s
            else -> Triple(60_000, 120_000, 2_500)      // Grande: 60s/120s
        }
    }

    // --- Preferred audio/sub language ---
    fun getPreferredAudioLanguage(context: Context): String {
        return getPrefs(context).getString("preferred_audio_language", "spa") ?: "spa"
    }

    fun getPreferredSubtitleLanguage(context: Context): String {
        return getPrefs(context).getString("preferred_subtitle_language", "spa") ?: "spa"
    }

    // --- Passthrough ---
    fun isAudioPassthroughEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("audio_passthrough", false)
    }

    // --- Auto subtitles ---
    fun isAutoSubtitleDownloadEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("auto_subtitle_download", true)
    }
}