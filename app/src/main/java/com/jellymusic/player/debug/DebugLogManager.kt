package com.jellymusic.player.debug

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogManager {
    private const val TAG = "DebugLogManager"
    private var loggingJob: Job? = null
    private var logcatProcess: java.lang.Process? = null
    private var currentLogFile: File? = null
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    fun isLogging(): Boolean = loggingJob?.isActive == true

    fun getCurrentLogFilePath(): String? = currentLogFile?.absolutePath

    fun getLogsDirectory(context: Context): File {
        // Main ROM primary storage: /storage/emulated/0/LOGS
        val primaryDir = File(Environment.getExternalStorageDirectory(), "LOGS")
        if (primaryDir.exists() || primaryDir.mkdirs()) {
            return primaryDir
        }
        // Fallback: Documents/LOGS
        val docsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "LOGS")
        if (docsDir.exists() || docsDir.mkdirs()) {
            return docsDir
        }
        // App external files fallback
        val appDir = context.getExternalFilesDir("LOGS") ?: File(context.filesDir, "LOGS")
        appDir.mkdirs()
        return appDir
    }

    fun startLogging(context: Context, scope: CoroutineScope) {
        if (isLogging()) return

        val logsDir = getLogsDirectory(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val logFile = File(logsDir, "jellymusic_$timeStamp.log")
        currentLogFile = logFile

        // Catch crashes and write directly to log file
        if (previousHandler == null) {
            previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    logFile.appendText("\n\n[FATAL EXCEPTION] Thread: ${thread.name}\n${Log.getStackTraceString(throwable)}\n")
                } catch (_: Exception) {}
                previousHandler?.uncaughtException(thread, throwable)
            }
        }

        loggingJob = scope.launch(Dispatchers.IO) {
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(logFile, true)
                fos.write("=== JellyMusic Debug LogCat Started: ${Date()} ===\n".toByteArray())
                fos.write("App Version: 1.0.0, Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n".toByteArray())
                fos.write("Log File: ${logFile.absolutePath}\n\n".toByteArray())
                fos.flush()

                val pid = Process.myPid()
                val process = try {
                    ProcessBuilder("logcat", "-v", "threadtime", "--pid=$pid").redirectErrorStream(true).start()
                } catch (_: Exception) {
                    ProcessBuilder("logcat", "-v", "threadtime").redirectErrorStream(true).start()
                }
                logcatProcess = process

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    fos.write((line + "\n").toByteArray())
                    fos.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in logcat streaming: ${e.message}", e)
            } finally {
                try {
                    fos?.close()
                } catch (_: Exception) {}
                stopLogcatProcess()
            }
        }
    }

    fun stopLogging() {
        loggingJob?.cancel()
        loggingJob = null
        stopLogcatProcess()
    }

    private fun stopLogcatProcess() {
        try {
            logcatProcess?.destroy()
        } catch (_: Exception) {}
        logcatProcess = null
    }
}
