package com.vahak.mehrban.domain.repository

import android.content.Context
import android.os.Environment
import com.vahak.mehrban.data.remote.AppUpdateApi
import com.vahak.mehrban.data.remote.DownloadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class UpdateDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AppUpdateApi
) {

    fun downloadApk(fileName: String, downloadUrl: String): Flow<DownloadState> = flow {
        try {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(directory, fileName)

            var downloadedLength = if (file.exists()) file.length() else 0L
            var rangeHeader = if (downloadedLength > 0) "bytes=$downloadedLength-" else null

            var response: Response<ResponseBody>? = null

            // 🚀 THE FIX: Try to resume. If the server aborts or says 416, catch it and reset.
            try {
                response = api.downloadApk(downloadUrl, rangeHeader)
                if (response.code() == 416 || !response.isSuccessful) {
                    throw Exception("Invalid range or server error: ${response.code()}")
                }
            } catch (e: Exception) {
                // The file on the device is corrupted or the server rejected the range.
                // DELETE IT and start from scratch.
                if (file.exists()) file.delete()
                downloadedLength = 0L
                rangeHeader = null

                // Second attempt: Clean download from 0 bytes
                response = api.downloadApk(downloadUrl, rangeHeader)
            }

            if (response == null || !response.isSuccessful || response.body() == null) {
                emit(DownloadState.Error("خطا در برقراری ارتباط با سرور"))
                return@flow
            }

            val body = response.body()!!
            val isPartialContent = response.code() == 206

            val totalLength = if (isPartialContent) body.contentLength() + downloadedLength else body.contentLength()

            var appendToFile = isPartialContent
            if (!isPartialContent && file.exists()) {
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

            emit(DownloadState.Success(file))

        } catch (e: Exception) {
            emit(DownloadState.Error("ارتباط قطع شد. لطفاً دوباره تلاش کنید."))
        }
    }.flowOn(Dispatchers.IO)
}