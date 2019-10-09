package com.nuuls.axel.nuulsimageuploader

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object NuulsUploader {
    private val client = OkHttpClient()
    private val MEDIA_TYPE = "image/png".toMediaType()
    private const val URL = "https://i.nuuls.com/upload"
    private val TAG = NuulsUploader::class.java.simpleName

    suspend fun upload(file: File): String? = withContext(Dispatchers.IO) {
        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xd", "justNameItXD.png", file.asRequestBody(MEDIA_TYPE))
                .build()
        val request = Request.Builder()
                .url(URL)
                .post(requestBody)
                .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext response.body?.string()
            }
        } catch (t: Throwable) {
            Log.e(TAG, Log.getStackTraceString(t))
        }

        return@withContext null
    }
}