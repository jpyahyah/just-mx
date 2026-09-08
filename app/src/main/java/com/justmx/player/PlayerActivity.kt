package com.justmx.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.justmx.opensubtitles.MovieHashCalculator
import com.justmx.opensubtitles.OpenSubtitlesClient
import com.justmx.player.databinding.ActivityPlayerBinding
import com.justmx.player.ui.settings.SettingsActivity
import com.justmx.player.ui.settings.SettingsDataStore
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var binding: ActivityPlayerBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        val videoUrl: Uri? = intent?.data

        // Configurar listeners de settings (siempre visibles)
        binding?.btnFloatingSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding?.btnSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding?.playerView?.setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }

        if (videoUrl == null) {
            // Mostrar overlay de bienvenida con botón de settings
            binding?.overlayContainer?.visibility = View.VISIBLE
            binding?.tvMessage?.text = "JUST MX\n\nNo hay video para reproducir.\nAbre un video desde Stremio o usa el botón de ajustes."
            binding?.playerView?.visibility = View.GONE
            return
        }

        val title: String? = intent?.getStringExtra("title")

        setupPlayer(videoUrl, title)
        setupDpPadControls()

        // Auto-descargar subtítulos si está configurado
        if (SettingsDataStore.isAutoSubtitleDownloadEnabled(this)) {
            fetchSubtitlesForUrl(videoUrl)
        }
    }

    private fun setupPlayer(videoUrl: Uri, title: String?) {
        val (minBuf, maxBuf, bufferTime) = SettingsDataStore.getBufferDurations(this)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(minBuf, maxBuf, bufferTime, bufferTime / 2)
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val okHttpClient = OkHttpClient()
        val httpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("JustMX/1.0")
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val renderers = NextRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val exoPlayer = ExoPlayer.Builder(this, renderers)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        // Audio passthrough/bitstream
        val audioLang = SettingsDataStore.getPreferredAudioLanguage(this)
        val subLang = SettingsDataStore.getPreferredSubtitleLanguage(this)
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setPreferredAudioLanguage(audioLang)
            .setPreferredTextLanguage(subLang)
            .build()

        // Audio attributes
        val audioAttrs = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MOVIE)
            .build()
        exoPlayer.setAudioAttributes(audioAttrs, true)

        val mediaItemBuilder = MediaItem.Builder().setUri(videoUrl)
        title?.let {
            mediaItemBuilder.setMediaMetadata(MediaMetadata.Builder().setTitle(it).build())
        }
        val mediaItem = mediaItemBuilder.build()

        binding?.playerView?.player = exoPlayer
        binding?.playerView?.visibility = View.VISIBLE
        binding?.overlayContainer?.visibility = View.GONE
        binding?.playerView?.isFocusable = true
        binding?.playerView?.requestFocus()

        // Show controller when paused, hide when playing
        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    binding?.playerView?.hideController()
                    binding?.btnFloatingSettings?.visibility = View.GONE
                } else {
                    binding?.playerView?.showController()
                    binding?.btnFloatingSettings?.visibility = View.VISIBLE
                }
            }
        })

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()

        player = exoPlayer
    }

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

    /**
     * Fase 3: Calcular moviehash de OpenSubtitles leyendo por Range los primeros/últimos 64KB
     * de la URL de Real-Debrid, consultar la API y sidecar el .srt descargado.
     */
    private fun fetchSubtitlesForUrl(videoUrl: Uri) {
        val url = videoUrl.toString()
        if (!url.startsWith("http://") && !url.startsWith("https://")) return

        lifecycleScope.launch {
            val okHttpClient = OkHttpClient()

            // 1. HEAD para obtener Content-Length
            val headRequest = Request.Builder().url(url).head().build()
            val headResponse = try {
                okHttpClient.newCall(headRequest).execute()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            if (headResponse?.isSuccessful != true) return@launch
            val contentLength = headResponse.header("Content-Length")?.toLongOrNull() ?: 0L
            headResponse.close()
            if (contentLength <= 0) return@launch

            // 2. Range request para primeros 64KB
            val headRange = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-65535")
                .build()
            val headStreamResponse = okHttpClient.newCall(headRange).execute()
            if (!headStreamResponse.isSuccessful) return@launch
            val headInputStream = headStreamResponse.body?.byteStream() ?: return@launch

            // 3. Range request para últimos 64KB
            val tailStart = maxOf(0L, contentLength - 65536)
            val tailRange = Request.Builder()
                .url(url)
                .header("Range", "bytes=$tailStart-${contentLength - 1}")
                .build()
            val tailStreamResponse = okHttpClient.newCall(tailRange).execute()
            if (!tailStreamResponse.isSuccessful) return@launch
            val tailInputStream = tailStreamResponse.body?.byteStream() ?: return@launch

            // 4. Calcular moviehash
            val hash = try {
                MovieHashCalculator.computeHash(contentLength, headInputStream, tailInputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            headInputStream.close()
            tailInputStream.close()
            headStreamResponse.close()
            tailStreamResponse.close()

            if (hash == null) return@launch
            if (hash == "0000000000000000" || hash.length != 16) return@launch

            // 5. Consultar OpenSubtitles
            val client = OpenSubtitlesClient()
            val subtitleUrl = client.findSubtitlesByHash(hash, listOf("spa", "eng"))

            if (subtitleUrl != null) {
                // 6. Sidecar el subtítulo
                addSubtitleToPlayer(subtitleUrl)
            }
        }
    }

    /**
     * Agrega un subtítulo externo al reproductor.
     * @param subtitleUrl URL directa al .srt descargado (link de OpenSubtitles).
     */
    private fun addSubtitleToPlayer(subtitleUrl: String) {
        val exoPlayer = player ?: return

        val subtitleUri = Uri.parse(subtitleUrl)
        val mediaItem = exoPlayer.currentMediaItem
        val updatedItem = mediaItem?.buildUpon()
            ?.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage("spa")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
            ?.build()

        if (updatedItem != null) {
            exoPlayer.setMediaItem(updatedItem, true)
            exoPlayer.prepare()
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
