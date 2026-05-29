package com.vahak.mehrban.core.util

import android.os.Build
import android.util.Log
import com.vahak.mehrban.BuildConfig
import com.vahak.mehrban.core.data.local.dao.CrashLogDao
import com.vahak.mehrban.core.data.local.entity.CrashLogEntity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class GlobalCrashHandler(
    private val crashLogDao: CrashLogDao,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 1. Extract the full stack trace into a String
            val stringWriter = StringWriter()
            throwable.printStackTrace(PrintWriter(stringWriter))
            val stackTrace = stringWriter.toString()

            // 2. Create the payload
            val crashLog = CrashLogEntity(
                appVersion = BuildConfig.VERSION_NAME + " (${BuildConfig.VERSION_CODE})",
                androidVersion = Build.VERSION.RELEASE,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                exceptionType = throwable.javaClass.simpleName,
                stackTrace = stackTrace
            )

            // 3. Save to Room synchronously (We are dying, no time for async!)
            crashLogDao.insertCrashLogSync(crashLog)
            Log.e("MehrbanTelemetry", "Crash saved to local database successfully.")

        } catch (e: Exception) {
            // If the crash handler itself crashes, we just print to logcat
            Log.e("MehrbanTelemetry", "Failed to save crash log", e)
        } finally {
            // 4. Pass the crash back to Android so it shows the "App has stopped" dialog
            // or restarts the process properly.
            defaultHandler?.uncaughtException(thread, throwable) ?: exitProcess(1)
        }
    }
}