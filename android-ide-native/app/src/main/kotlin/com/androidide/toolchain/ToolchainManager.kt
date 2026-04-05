package com.androidide.toolchain

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.androidide.jni.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * ToolchainManager.kt
 *
 * Extracts pre-bundled build tool binaries from assets/ to the app's
 * private data directory, sets executable permissions, and sets the
 * toolchain path in the native layer.
 *
 * BINARY BUNDLING INSTRUCTIONS:
 * ==============================
 * Place these pre-built ARM64 binaries in:
 *   app/src/main/assets/toolchain/arm64-v8a/
 *
 * Required binaries:
 *   - aapt2         (from Android SDK build-tools)
 *   - d8            (from Android SDK build-tools)
 *   - zipalign      (from Android SDK build-tools)
 *   - apksigner     (from Android SDK build-tools — shell wrapper + jar)
 *   - aidl          (from Android SDK build-tools)
 *   - dexdump       (from Android SDK build-tools)
 *   - clang         (from Android NDK toolchains/llvm/prebuilt/linux-x86_64/bin/)
 *   - clang++       (NDK)
 *   - lld           (NDK)
 *   - llvm-ar       (NDK)
 *   - llvm-strip    (NDK)
 *   - llvm-objdump  (NDK)
 *   - adb           (from Android SDK platform-tools)
 *   - java          (Termux-built or GraalVM native-image)
 *   - keytool       (bundled with java)
 *
 * Required JARs (in assets/toolchain/jars/):
 *   - r8.jar        (from Android SDK build-tools/r8.jar)
 *   - bundletool.jar (from bundletool GitHub releases)
 *   - apksigner.jar (from Android SDK lib/apksigner.jar)
 *
 * Required SDK files (in assets/sdk/):
 *   - android.jar  (from Android SDK platforms/android-XX/android.jar)
 */
class ToolchainManager(private val context: Context) {

    companion object {
        private const val TAG = "ToolchainManager"

        // ABI for binary extraction — matches device architecture
        private const val ABI = "arm64-v8a"

        // All binary names that need extraction and chmod +x
        private val TOOL_BINARIES = listOf(
            "aapt2", "d8", "zipalign", "aidl", "dexdump",
            "clang", "clang++", "lld", "llvm-ar", "llvm-strip",
            "llvm-objdump", "llvm-nm", "adb", "java", "keytool",
            "jar", "javac", "jarsigner", "sqlite3"
        )

        private val TOOL_JARS = listOf(
            "r8.jar", "bundletool.jar", "apksigner.jar"
        )

        private val SDK_FILES = listOf(
            "android.jar"
        )
    }

    /** Root directory where all tools are installed */
    val toolchainDir: File get() = File(context.filesDir, "toolchain/bin")
    val jarsDir: File get() = File(context.filesDir, "toolchain/jars")
    val sdkDir: File get() = File(context.filesDir, "sdk")
    val projectsDir: File get() = File(context.filesDir, "projects")

    val androidJar: String get() = File(sdkDir, "android.jar").absolutePath
    val bundletoolJar: String get() = File(jarsDir, "bundletool.jar").absolutePath
    val apksignerJar: String get() = File(jarsDir, "apksigner.jar").absolutePath
    val r8Jar: String get() = File(jarsDir, "r8.jar").absolutePath

    var isReady = false
        private set

    /**
     * Extract and prepare all toolchain binaries.
     * Call this once on app startup (from a coroutine).
     */
    suspend fun setup(onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Setting up toolchain in: ${toolchainDir.absolutePath}")

        toolchainDir.mkdirs()
        jarsDir.mkdirs()
        sdkDir.mkdirs()
        projectsDir.mkdirs()

        val assets = context.assets

        // Extract binaries
        onProgress("Extracting build tools...")
        for (tool in TOOL_BINARIES) {
            val assetPath = "toolchain/$ABI/$tool"
            val outputFile = File(toolchainDir, tool)
            if (!outputFile.exists()) {
                try {
                    extractAsset(assets, assetPath, outputFile)
                    NativeBridge.chmodExec(outputFile.absolutePath)
                    Log.i(TAG, "Extracted: $tool")
                    onProgress("Extracted: $tool")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to extract $tool: ${e.message}")
                    onProgress("Warning: $tool not bundled")
                }
            }
        }

        // Extract JARs
        onProgress("Extracting JARs...")
        for (jar in TOOL_JARS) {
            val assetPath = "toolchain/jars/$jar"
            val outputFile = File(jarsDir, jar)
            if (!outputFile.exists()) {
                try {
                    extractAsset(assets, assetPath, outputFile)
                    Log.i(TAG, "Extracted JAR: $jar")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to extract $jar: ${e.message}")
                }
            }
        }

        // Extract SDK files
        onProgress("Extracting Android SDK...")
        for (sdkFile in SDK_FILES) {
            val assetPath = "sdk/$sdkFile"
            val outputFile = File(sdkDir, sdkFile)
            if (!outputFile.exists()) {
                try {
                    extractAsset(assets, assetPath, outputFile)
                    Log.i(TAG, "Extracted SDK: $sdkFile")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to extract SDK file $sdkFile: ${e.message}")
                }
            }
        }

        // Tell native layer where the tools are
        NativeBridge.setToolchainDir(toolchainDir.absolutePath)

        isReady = true
        onProgress("Toolchain ready.")
        Log.i(TAG, "Toolchain setup complete.")
    }

    /** Check if a specific tool binary exists and is executable */
    fun hasTool(name: String): Boolean {
        val file = File(toolchainDir, name)
        return file.exists() && file.canExecute()
    }

    /** Absolute path to a tool binary */
    fun toolPath(name: String): String = File(toolchainDir, name).absolutePath

    /** List all available tools */
    fun availableTools(): List<String> {
        return TOOL_BINARIES.filter { hasTool(it) }
    }

    // --------------------------------------------------------
    // Internal helpers
    // --------------------------------------------------------

    private fun extractAsset(assets: AssetManager, assetPath: String, outputFile: File) {
        assets.open(assetPath).use { input ->
            FileOutputStream(outputFile).use { output ->
                val buf = ByteArray(8192)
                var n: Int
                while (input.read(buf).also { n = it } >= 0) {
                    output.write(buf, 0, n)
                }
            }
        }
    }
}
