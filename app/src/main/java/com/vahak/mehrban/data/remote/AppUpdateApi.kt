package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url


import java.io.File

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class Success(val apkFile: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
data class AppVersionDto(
    @SerializedName("latestVersionCode")
    val latestVersionCode: Int,

    @SerializedName("latestVersionName")
    val latestVersionName: String,

    @SerializedName("releaseNotes")
    val releaseNotes: String,

    @SerializedName("downloadUrl")
    val downloadUrl: String,

    @SerializedName("isForced")
    val isForced: Boolean
)

interface AppUpdateApi {

    @GET("api/identity/v1/app-config/version")
    suspend fun getAppVersion(): Response<AppVersionDto>

    @Streaming
    @GET
    suspend fun downloadApk(
        @Url fileUrl: String,
        @Header("Range") range: String? = null
    ): Response<ResponseBody>
}