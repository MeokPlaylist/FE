package com.example.meokpli.Main.Feed

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

object PresignedUploader {
    private val client by lazy { OkHttpClient.Builder().build() } // 인증 헤더 금지(프리사인드 가정)

    private class UriRequestBody(
        private val context: Context,
        private val uri: Uri,
        private val mime: String,
        private val onProgress: ((sent: Long, total: Long) -> Unit)? = null
    ) : RequestBody() {
        override fun contentType() = mime.toMediaTypeOrNull()

        override fun contentLength(): Long {
            val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
            val len = afd?.length ?: -1
            afd?.close()
            return len // -1이면 chunked로 전송됨
        }

        override fun writeTo(sink: BufferedSink) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val src = input.source()
                var total = 0L
                val totalLen = contentLength()
                while (true) {
                    val read = src.read(sink.buffer, 8 * 1024)
                    if (read == -1L) break
                    total += read
                    onProgress?.invoke(total, totalLen)
                    sink.flush()
                }
            }
        }
    }

    /**
     * uris와 urls 크기가 같다고 가정하고, 같은 인덱스끼리 업로드합니다.
     * 기본 PUT. 서버가 POST를 요구하면 method="POST".
     */
    suspend fun uploadAll(
        context: Context,
        uris: List<Uri>,
        urls: List<String>,
        method: String = "PUT",
        headers: Map<String, String> = emptyMap(),
        onProgress: ((index: Int, sent: Long, total: Long) -> Unit)? = null
    ): List<Boolean> = withContext(Dispatchers.IO) {
        require(uris.size == urls.size) { "uris(${uris.size})와 urls(${urls.size}) 개수가 달라요." }
        val results = MutableList(uris.size) { false }

        for (i in uris.indices) {
            val uri = uris[i]
            val url = urls[i]
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val body = UriRequestBody(context, uri, mime) { sent, total ->
                onProgress?.invoke(
                    i,
                    sent,
                    total
                )
            }

            val reqBuilder = Request.Builder().url(url)
            headers.forEach { (k, v) -> reqBuilder.header(k, v) }
            when (method) {
                "PUT" -> reqBuilder.put(body)
                "POST" -> reqBuilder.post(body)
                else -> reqBuilder.method(method, body)
            }

            client.newCall(reqBuilder.build()).execute().use { resp ->
                results[i] = resp.isSuccessful
            }
        }
        results
    }
}