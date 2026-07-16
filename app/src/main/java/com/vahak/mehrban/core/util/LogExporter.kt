package com.vahak.mehrban.core.util

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object LogExporter {

    suspend fun exportLogsAndShare(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Define files
                val rawLogFile = File(context.cacheDir, "raw_logs.txt")
                val logTxtFile = File(context.cacheDir, "modern_family.log")
                val logZipFile = File(context.cacheDir, "modern_family_logs.zip")

                // 2. Clean old files
                if (rawLogFile.exists()) rawLogFile.delete()
                if (logTxtFile.exists()) logTxtFile.delete()
                if (logZipFile.exists()) logZipFile.delete()

                // 3. Dump the raw logcat output directly to the raw file
                val myPid = Process.myPid()
                val process = Runtime.getRuntime()
                    .exec("logcat -d -v threadtime -t 10000 --pid=$myPid View:S -f ${rawLogFile.absolutePath}")
                process.waitFor()

                // 4. 🚀 THE FIX: Read the raw log and prepend the Level to the VERY start of the line
                // This allows Notepad++ to color the ENTIRE line from left to right.
                logTxtFile.bufferedWriter().use { writer ->
                    rawLogFile.forEachLine { line ->
                        // Split the line to find the 5th token (which is the log level: I, D, W, E)
                        val tokens = line.trim().split(Regex("\\s+"), limit = 6)
                        val level =
                            if (tokens.size >= 5 && tokens[4].length == 1) tokens[4] else " "

                        // Write it out as: "[E] | 07-25 12:51..."
                        writer.write("[$level] | $line\n")
                    }
                }

                // Cleanup raw file immediately
                rawLogFile.delete()

                // 5. Compress
                zipFile(sourceFile = logTxtFile, zipFile = logZipFile)
                logTxtFile.delete()

                // 6. Share
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    logZipFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "App Logs (Zipped .log)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    val chooser =
                        Intent.createChooser(shareIntent, "ارسال لاگ‌های فشرده به توسعه‌دهنده")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }

            } catch (e: Exception) {
                Timber.tag("LogExporter").e(e, "Failed to export and compress logs")
            }
        }
    }

    private fun zipFile(sourceFile: File, zipFile: File) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            FileInputStream(sourceFile).use { fis ->
                val zipEntry = ZipEntry(sourceFile.name)
                zos.putNextEntry(zipEntry)
                val buffer = ByteArray(4096)
                var length: Int
                while (fis.read(buffer).also { length = it } >= 0) {
                    zos.write(buffer, 0, length)
                }
                zos.closeEntry()
            }
        }
    }
}