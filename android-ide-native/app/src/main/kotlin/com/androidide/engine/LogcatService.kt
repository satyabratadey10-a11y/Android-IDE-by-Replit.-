package com.androidide.engine

import android.util.Log
import com.androidide.jni.NativeBridge
import com.androidide.toolchain.ToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException

/**
 * LogcatService.kt
 *
 * Provides real-time logcat streaming using ProcessBuilder with the
 * bundled `adb` binary. Falls back to device-local `logcat` if adb
 * is not available (which is the case when the IDE runs ON the device).
 *
 * On-device logcat:
 *   When Android IDE runs directly on the Android device being developed on,
 *   it can read /proc logs and run `logcat` directly without adb.
 *   We use ProcessBuilder pointing at /system/bin/logcat for this path.
 */
class LogcatService(private val toolchain: ToolchainManager) {

    companion object {
        private const val TAG = "LogcatService"
        private const val LOCAL_LOGCAT = "/system/bin/logcat"
        private const val BUF_SIZE = 8192
    }

    data class LogEntry(
        val rawLine: String,
        val timestamp: String,
        val pid: String,
        val tid: String,
        val level: LogLevel,
        val tag: String,
        val message: String
    )

    enum class LogLevel(val char: Char) {
        VERBOSE('V'), DEBUG('D'), INFO('I'), WARN('W'), ERROR('E'), FATAL('F'), SILENT('S');

        companion object {
            fun from(c: Char): LogLevel = values().find { it.char == c.uppercaseChar() } ?: VERBOSE
        }
    }

    data class LogFilter(
        val minLevel: LogLevel = LogLevel.VERBOSE,
        val tag: String = "",        // empty = all tags
        val pid: String = ""         // empty = all PIDs
    )

    /**
     * Stream logcat output as a Flow of [LogEntry].
     * Cancelling the coroutine collecting this flow will terminate the logcat process.
     *
     * @param filter    Filter to apply
     * @param useAdb    If true, use `adb logcat`; if false, use device-local `logcat`
     * @param device    ADB device serial (only used when useAdb=true)
     */
    fun stream(
        filter: LogFilter = LogFilter(),
        useAdb: Boolean = false,
        device: String = ""
    ): Flow<LogEntry> = flow {
        val args = buildLogcatArgs(filter, useAdb, device)
        val binary = if (useAdb) toolchain.toolPath("adb") else LOCAL_LOGCAT

        Log.i(TAG, "Starting logcat: $binary ${args.joinToString(" ")}")

        val process = try {
            ProcessBuilder(listOf(binary) + args)
                .redirectErrorStream(false)
                .start()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start logcat: ${e.message}")
            return@flow
        }

        try {
            val reader = BufferedReader(InputStreamReader(process.inputStream), BUF_SIZE)
            while (isActive) {
                val line = reader.readLine() ?: break
                val entry = parseLine(line)
                if (passesFilter(entry, filter)) emit(entry)
            }
        } finally {
            process.destroy()
            try { process.waitFor() } catch (_: InterruptedException) {}
            Log.i(TAG, "Logcat stream ended")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Dump existing logcat buffer (non-streaming, for display on open).
     */
    suspend fun dump(filter: LogFilter = LogFilter(), maxLines: Int = 2000): List<LogEntry> {
        val result = NativeBridge.execCommand(
            binary = LOCAL_LOGCAT,
            argv = buildLogcatArgs(filter, false, "", dump = true, maxLines = maxLines).toTypedArray(),
            workingDir = ""
        )
        return result[0].lines()
            .filter { it.isNotBlank() }
            .map { parseLine(it) }
            .filter { passesFilter(it, filter) }
    }

    /**
     * Clear the logcat buffer.
     */
    fun clearBuffer() {
        NativeBridge.execCommand(LOCAL_LOGCAT, arrayOf("-c"), "")
    }

    // ---- Internals ----

    private fun buildLogcatArgs(
        filter: LogFilter,
        useAdb: Boolean,
        device: String,
        dump: Boolean = false,
        maxLines: Int = 0
    ): List<String> {
        return buildList {
            if (useAdb) {
                if (device.isNotBlank()) { add("-s"); add(device) }
                add("logcat")
            }
            add("-v"); add("threadtime")
            if (dump) add("-d")
            if (maxLines > 0) { add("-T"); add("$maxLines") }

            // Log filter spec: TAG:LEVEL *:S
            val levelChar = filter.minLevel.char.toString()
            if (filter.tag.isNotBlank()) {
                add("${filter.tag}:$levelChar")
                add("*:S")
            } else {
                add("*:$levelChar")
            }
        }
    }

    private fun passesFilter(entry: LogEntry, filter: LogFilter): Boolean {
        if (entry.level.ordinal < filter.minLevel.ordinal) return false
        if (filter.tag.isNotBlank() && !entry.tag.contains(filter.tag, ignoreCase = true)) return false
        if (filter.pid.isNotBlank() && entry.pid != filter.pid) return false
        return true
    }

    /**
     * Parse a logcat `threadtime` format line:
     * MM-DD HH:MM:SS.mmm  PID   TID  Level Tag: Message
     */
    private fun parseLine(raw: String): LogEntry {
        return try {
            // Example: "04-01 12:34:56.789  1234  5678 D MyTag: Hello"
            val timestamp = raw.substring(0, 18).trim()
            val rest = raw.substring(18).trim()
            val parts = rest.split("\\s+".toRegex(), limit = 4)
            val pid   = parts.getOrElse(0) { "?" }
            val tid   = parts.getOrElse(1) { "?" }
            val lvlStr = parts.getOrElse(2) { "V" }
            val level = LogLevel.from(lvlStr.firstOrNull() ?: 'V')
            val tagAndMsg = parts.getOrElse(3) { raw }
            val colonIdx = tagAndMsg.indexOf(':')
            val tag = if (colonIdx > 0) tagAndMsg.substring(0, colonIdx).trim() else "?"
            val msg = if (colonIdx >= 0) tagAndMsg.substring(colonIdx + 1).trim() else tagAndMsg
            LogEntry(raw, timestamp, pid, tid, level, tag, msg)
        } catch (e: Exception) {
            LogEntry(raw, "", "?", "?", LogLevel.VERBOSE, "?", raw)
        }
    }
}
