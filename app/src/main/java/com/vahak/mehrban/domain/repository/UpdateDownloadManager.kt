package com.vahak.mehrban.domain.repository

import android.content.Context
import android.os.Environment
import com.vahak.mehrban.data.remote.AppUpdateApi
import com.vahak.mehrban.data.remote.DownloadError
import com.vahak.mehrban.data.remote.DownloadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class UpdateDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context, // 🚀 Kept for file system access
    private val api: AppUpdateApi
) {

    fun downloadApk(fileName: String, downloadUrl: String): Flow<DownloadState> = flow {
        try {
            Timber.d("Initiating APK download process, fileName: %s", fileName)
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(directory, fileName)

            var downloadedLength = if (file.exists()) file.length() else 0L
            var rangeHeader = if (downloadedLength > 0) "bytes=$downloadedLength-" else null

            var response: Response<ResponseBody>?

            try {
                Timber.d("Requesting APK file chunks, rangeHeader: %s", rangeHeader)
                response = api.downloadApk(downloadUrl, rangeHeader)
                if (response.code() == 416 || !response.isSuccessful) {
                    throw Exception("Invalid range or server error: ${response.code()}")
                }
            } catch (e: Exception) {
                Timber.w(
                    e, "Invalid byte range or server rejection, resetting to clean download state"
                )
                if (file.exists()) file.delete()
                downloadedLength = 0L
                rangeHeader = null
                response = api.downloadApk(downloadUrl, rangeHeader)
            }

            if (!response.isSuccessful || response.body() == null) {
                Timber.e(
                    "Failed to download APK, server responded with error status: %d",
                    response.code()
                )
                emit(DownloadState.Error(DownloadError.CONNECTION_FAILED)) // 🚀 Clean Error
                return@flow
            }

            val body = response.body()!!
            val isPartialContent = response.code() == 206
            val totalLength =
                if (isPartialContent) body.contentLength() + downloadedLength else body.contentLength()

            var appendToFile = isPartialContent
            if (!isPartialContent && file.exists()) {
                Timber.d("Removing existing file due to complete download strategy shift")
                file.delete()
                appendToFile = false
            }

            var currentLength = if (appendToFile) downloadedLength else 0L

            body.byteStream().use { inputStream ->
                FileOutputStream(file, appendToFile).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var lastEmittedProgress = -1

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        currentLength += bytesRead

                        val progress = ((currentLength * 100) / totalLength).toInt()

                        if (progress != lastEmittedProgress) {
                            lastEmittedProgress = progress
                            emit(DownloadState.Downloading(progress, currentLength, totalLength))
                        }
                    }
                }
            }
            Timber.i("APK downloaded successfully, totalBytes: %d", totalLength)
            emit(DownloadState.Success(file))

        } catch (e: Exception) {
            Timber.e(e, "System failure during APK download stream processing")
            emit(DownloadState.Error(DownloadError.CONNECTION_LOST))
        }
    }.flowOn(Dispatchers.IO)
}