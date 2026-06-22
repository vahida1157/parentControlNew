package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url


import java.io.File

enum class DownloadError {
    CONNECTION_FAILED,
    CONNECTION_LOST,
    ALREADY_LATEST,
    GENERIC_ERROR
}

sealed class DownloadState {
    data class Downloading(val progress: Int, val currentLength: Long, val totalLength: Long) :
        DownloadState()

    data class Success(val file: File) : DownloadState()
    data class Error(val error: DownloadError) : DownloadState()
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
    suspend fun getAppVersion(
        @Query("currentVersionCode") versionCode: Int
    ): Response<AppVersionDto>

    @Streaming
    @GET
    suspend fun downloadApk(
        @Url fileUrl: String,
        @Header("Range") range: String? = null
    ): Response<ResponseBody>
}