package com.vahak.parentcontroll.core.util

import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object LogExporter {

    /**
     * Extracts the last N lines of Logcat, compresses them into a ZIP file,
     * deletes the raw text file to save space, and opens the Share sheet.
     */
    suspend fun exportLogsAndShare(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Define file paths in the cache directory
                val logTxtFile = File(context.cacheDir, "modern_family_logs.txt")
                val logZipFile = File(context.cacheDir, "modern_family_logs.zip")

                // 2. Self-Cleaning: Delete old files so we don't bloat the user's storage
                if (logTxtFile.exists()) logTxtFile.delete()
                if (logZipFile.exists()) logZipFile.delete()

                logTxtFile.createNewFile()

                // 3. Dump ONLY the last 5000 lines to prevent massive files
                // -d = dump and exit
                // -v threadtime = include timestamps
                // -t 5000 = only fetch the latest 5000 events
                val myPid = Process.myPid()
                // This tells logcat to silence the "View" tag completely, but keep everything else
                val process = Runtime.getRuntime().exec("logcat -d -v threadtime -t 10000 --pid=$myPid View:S -f ${logTxtFile.absolutePath}")
                process.waitFor()

                // 4. Compress the text file into a highly compressed ZIP archive
                zipFile(sourceFile = logTxtFile, zipFile = logZipFile)

                // 5. Cleanup: Delete the heavy uncompressed text file immediately
                if (logTxtFile.exists()) logTxtFile.delete()

                // 6. Get the secure URI for the ZIP file using our FileProvider
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    logZipFile
                )

                // 7. Create the Share Intent (Notice the MIME type is now application/zip)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "App Logs (Zipped)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                // 8. Launch the Share Menu on the main UI thread
                withContext(Dispatchers.Main) {
                    val chooser = Intent.createChooser(shareIntent, "ارسال لاگ‌های فشرده به توسعه‌دهنده")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }

            } catch (e: Exception) {
                Log.e("LogExporter", "Failed to export and compress logs", e)
            }
        }
    }

    /**
     * Standard Java helper function to compress a single file into a .zip archive.
     */
    private fun zipFile(sourceFile: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            FileInputStream(sourceFile).use { fis ->
                // Name the file *inside* the zip archive
                val zipEntry = ZipEntry(sourceFile.name)
                zos.putNextEntry(zipEntry)

                val buffer = ByteArray(4096) // 4KB buffer for fast writing
                var length: Int
                while (fis.read(buffer).also { length = it } >= 0) {
                    zos.write(buffer, 0, length)
                }
                zos.closeEntry()
            }
        }
    }
}