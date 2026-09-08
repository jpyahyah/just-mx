package com.justmx.player.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.justmx.player.R
import com.justmx.player.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBufferSpinner()
        setupLanguageInputs()
        setupPassthroughToggle()
        setupAutoSubtitleToggle()
        setupAFRToggle()

        binding.btnSave.setOnClickListener {
            Toast.makeText(this, "Ajustes guardados", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBufferSpinner() {
        val buffers = arrayOf("Normal", "Grande", "Enorme")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, buffers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBuffer.adapter = adapter

        val prefs = SettingsDataStore.getPrefs(this)
        val current = prefs.getString("buffer_size", SettingsDataStore.BUFFER_LARGE)
        val selectedIndex = when (current) {
            SettingsDataStore.BUFFER_NORMAL -> 0
            SettingsDataStore.BUFFER_ENORMOUS -> 2
            else -> 1
        }
        binding.spinnerBuffer.setSelection(selectedIndex)

        binding.btnSave.setOnClickListener {
            val selected = when (binding.spinnerBuffer.selectedItemPosition) {
                0 -> SettingsDataStore.BUFFER_NORMAL
                2 -> SettingsDataStore.BUFFER_ENORMOUS
                else -> SettingsDataStore.BUFFER_LARGE
            }
            SettingsDataStore.getPrefs(this).edit()
                .putString("buffer_size", selected).apply()
        }
    }

    private fun setupLanguageInputs() {
        val prefs = SettingsDataStore.getPrefs(this)
        binding.etAudioLanguage.setText(prefs.getString("preferred_audio_language", "spa"))
        binding.etSubtitleLanguage.setText(prefs.getString("preferred_subtitle_language", "spa"))

        binding.btnSave.setOnClickListener {
            prefs.edit()
                .putString("preferred_audio_language", binding.etAudioLanguage.text.toString())
                .putString("preferred_subtitle_language", binding.etSubtitleLanguage.text.toString())
                .apply()
        }
    }

    private fun setupPassthroughToggle() {
        val prefs = SettingsDataStore.getPrefs(this)
        binding.switchPassthrough.isChecked = prefs.getBoolean("audio_passthrough", false)
    }

    private fun setupAutoSubtitleToggle() {
        val prefs = SettingsDataStore.getPrefs(this)
        binding.switchAutoSubtitle.isChecked = prefs.getBoolean("auto_subtitle_download", true)
    }

    private fun setupAFRToggle() {
        val prefs = SettingsDataStore.getPrefs(this)
        binding.switchAFR.isChecked = prefs.getBoolean("frame_rate_matching", false)
    }
}
