package com.justmx.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.justmx.player.databinding.ActivityPlayerBinding
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import okhttp3.OkHttpClient

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var binding: ActivityPlayerBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        val videoUrl: Uri? = intent?.data
        if (videoUrl == null) {
            finish()
            return
        }
        val title: String? = intent?.getStringExtra("title")

        setupPlayer(videoUrl, title)
        setupDpPadControls()
    }

    private fun setupPlayer(videoUrl: Uri, title: String?) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(60_000, 120_000, 2_500, 5_000)
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val okHttpClient = OkHttpClient()
        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("JustMX/1.0")
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // NextLib: FFmpeg SOLO para audio; video queda en hardware.
        val renderers = NextRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val exoPlayer = ExoPlayer.Builder(this, renderers)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        // Preferir español si existe
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setPreferredAudioLanguage("spa")
            .setPreferredTextLanguage("spa")
            .build()

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(videoUrl)

        title?.let {
            mediaItemBuilder.setMediaMetadata(
                MediaMetadata.Builder().setTitle(it).build()
            )
        }

        val mediaItem = mediaItemBuilder.build()

        binding?.playerView?.player = exoPlayer
        binding?.playerView?.visibility = android.view.View.VISIBLE
        binding?.playerView?.isFocusable = true
        binding?.playerView?.requestFocus()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()

        player = exoPlayer
    }

    // Navegación por D-pad para Android TV
    private fun setupDpPadControls() {
        binding?.playerView?.setOnKeyListener { _, keyCode, _ ->
            val exoPlayer = player ?: return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    exoPlayer.play()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    exoPlayer.pause()
                    true
                }
            }
            false
        }
    }

    override fun onStop() {
        super.onStop()
        if (intent.getBooleanExtra("return_result", false)) {
            val resultIntent = Intent()
            resultIntent.putExtra("position", player?.currentPosition ?: 0L)
            resultIntent.putExtra("duration", player?.duration ?: 0L)
            setResult(RESULT_OK, resultIntent)
        }

        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
        binding = null
    }
}
