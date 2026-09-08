package com.justmx.opensubtitles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Cliente para OpenSubtitles.com REST API.
 *
 * API: https://api.opensubtitles.com/api/v1/
 * Necesita API key configurada en BuildConfig via gradle property.
 */
class OpenSubtitlesClient {

    private val client = OkHttpClient()
    private val apiKey: String = com.justmx.player.BuildConfig.OPENSUBTITLES_API_KEY

    /**
     * Busca subtítulos por moviehash.
     * @param hash Hash de OpenSubtitles (16 hex chars)
     * @param languages Lista de códigos de idioma (ISO-639-2/B), ej: ["spa", "eng"]
     * @return URL de descarga del mejor .srt encontrado, o null si no hay.
     */
    suspend fun findSubtitlesByHash(
        hash: String,
        languages: List<String> = listOf("spa", "eng")
    ): String? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            return@withContext null
        }

        val langParam = languages.joinToString(",")
        val url = "https://api.opensubtitles.com/api/v1/subtitles?moviehash=$hash&languages=$langParam&page=1&per_page=5"

        val request = Request.Builder()
            .url(url)
            .addHeader("Api-Key", apiKey)
            .addHeader("Accept", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val results = json.optJSONArray("data") ?: return@withContext null
            if (results.length() == 0) return@withContext null

            // Tomar el primer resultado con file_id válido
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val fileId = item.optLong("id")
                if (fileId > 0) {
                    val downloadUrl = downloadSubtitle(fileId)
                    if (downloadUrl != null) return@withContext downloadUrl
                }
            }
            null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Descarga un subtítulo por file_id y devuelve el link directo.
     */
    private suspend fun downloadSubtitle(fileId: Long): String? = withContext(Dispatchers.IO) {
        val url = "https://api.opensubtitles.com/api/v1/download"
        val jsonBody = JSONObject()
        jsonBody.put("file_id", fileId)

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Api-Key", apiKey)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "JustMX/1.0")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            // El link de descarga directa
            json.optString("link", null)?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}