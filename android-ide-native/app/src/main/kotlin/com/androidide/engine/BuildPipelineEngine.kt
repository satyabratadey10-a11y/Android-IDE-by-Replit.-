package com.androidide.engine

import android.util.Log
import com.androidide.jni.NativeBridge
import com.androidide.jni.NativeBridge.exitCode
import com.androidide.jni.NativeBridge.stderr
import com.androidide.jni.NativeBridge.stdout
import com.androidide.jni.NativeBridge.success
import com.androidide.project.AndroidProject
import com.androidide.toolchain.ToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * BuildPipelineEngine.kt
 *
 * Orchestrates the REAL Android build pipeline using actual tool binaries.
 * Each step invokes a real binary via the native JNI layer.
 *
 * Pipeline:
 *   1. AIDL compilation
 *   2. Java/Kotlin compilation (kotlinc / javac)
 *   3. AAPT2 compile (resources)
 *   4. AAPT2 link (generate R.java + base APK)
 *   5. D8/R8 DEX compilation
 *   6. APK packaging (zip resources + dex)
 *   7. zipalign
 *   8. apksigner
 *   9. Verify
 */
class BuildPipelineEngine(
    private val toolchain: ToolchainManager,
    private val project: AndroidProject
) {

    companion object {
        private const val TAG = "BuildPipelineEngine"
        const val MIN_API = "24"
    }

    data class BuildEvent(
        val type: Type,
        val message: String,
        val tool: String = "",
        val isError: Boolean = false
    ) {
        enum class Type { LOG, STEP_START, STEP_SUCCESS, STEP_FAILED, BUILD_SUCCESS, BUILD_FAILED }
    }

    /**
     * Run the full build pipeline, emitting real-time events.
     */
    fun buildDebug(): Flow<BuildEvent> = flow {
        emit(BuildEvent(BuildEvent.Type.LOG, "=== Android IDE Build Started ==="))
        emit(BuildEvent(BuildEvent.Type.LOG, "Project: ${project.name}"))
        emit(BuildEvent(BuildEvent.Type.LOG, "Package: ${project.packageName}"))

        val buildDir = File(project.dir, "build")
        val aidlOutDir = File(buildDir, "aidl_output").apply { mkdirs() }
        val classesDir = File(buildDir, "classes").apply { mkdirs() }
        val resDir = File(buildDir, "compiled_res").apply { mkdirs() }
        val dexDir = File(buildDir, "dex").apply { mkdirs() }
        val apkUnaligned = File(buildDir, "app-unaligned.apk")
        val apkAligned = File(buildDir, "app-aligned.apk")
        val apkSigned = File(buildDir, "app-debug.apk")
        val keystoreFile = File(project.dir, "debug.keystore")

        // ---- Step 1: AIDL ----
        emit(BuildEvent(BuildEvent.Type.STEP_START, "Compiling AIDL interfaces...", "aidl"))
        val aidlDir = File(project.srcDir, "aidl")
        if (aidlDir.exists()) {
            aidlDir.walkTopDown().filter { it.extension == "aidl" }.forEach { aidlFile ->
                val result = NativeBridge.aidlCompile(
                    aidlFile.absolutePath,
                    aidlDir.absolutePath,
                    aidlOutDir.absolutePath,
                    false
                )
                emit(BuildEvent(BuildEvent.Type.LOG, result.stdout(), "aidl"))
                if (!result.success()) {
                    emit(BuildEvent(BuildEvent.Type.STEP_FAILED, result.stderr(), "aidl", true))
                    return@flow
                }
            }
        }
        emit(BuildEvent(BuildEvent.Type.STEP_SUCCESS, "AIDL done", "aidl"))

        // ---- Step 2: Kotlin/Java compilation ----
        emit(BuildEvent(BuildEvent.Type.STEP_START, "Compiling Kotlin/Java sources...", "kotlinc"))
        val compilerResult = CompilerEngine(toolchain).compileKotlin(
            sourceDir = project.srcDir,
            classesDir = classesDir,
            androidJar = toolchain.androidJar,
            aidlGenDir = aidlOutDir
        )
        compilerResult.forEach { line ->
            emit(BuildEvent(BuildEvent.Type.LOG, line, "kotlinc"))
        }
        if (!File(classesDir, "classes.jar").exists()) {
            // Try javac fallback
            val javacResult = CompilerEngine(toolchain).compileJava(
                sourceDir = project.srcDir,
                classesDir = classesDir,
                androidJar = toolchain.androidJar
            )
            javacResult.forEach { emit(BuildEvent(BuildEvent.Type.LOG, it, "javac")) }
        }

        val classesJar = File(classesDir, "classes.jar")
        if (!classesJar.exists()) {
            emit(BuildEvent(BuildEvent.Type.STEP_FAILED, "Compilation failed — no classes.jar produced", isError = true))
            return@flow
        }
        emit(BuildEvent(BuildEvent.Type.STEP_SUCCESS, "Compilation succeeded", "kotlinc"))

        // ---- Step 3: AAPT2 compile resources ----
        emit(BuildEvent(BuildEvent.Type.STEP_START, "Compiling resources with AAPT2...", "aapt2"))
        val resResult = NativeBridge.aapt2Compile(
            inputDir = File(project.srcDir, "res").absolutePath,
            outputDir = resDir.absolutePath
        )
        emit(BuildEvent(BuildEvent.Type.LOG, resResult.stdout(), "aapt2"))
        if (!resResult.success()) {
            emit(BuildEvent(BuildEvent.Type.STEP_FAILED, resResult.stderr(), "aapt2", true))
            return@flow
        }
        emit(BuildEvent(BuildEvent.Type.STEP_SUCCESS, "Resources compiled", "aapt2"))

        // ---- Step 4: AAPT2 link ----
        emit(BuildEvent(BuildEvent.Type.STEP_START, "Linking resources...", "aapt2 link"))
        val linkResult = NativeBridge.aapt2Link(
            manifest = File(project.srcDir, "AndroidManifest.xml").absolutePath,
            compiledRes = resDir.absolutePath,
            androidJar = toolchain.androidJar,
            outputApk = apkUnaligned.absolutePath
        )
        emit(BuildEvent(BuildEvent.Type.LOG, linkResult.stdout(), "aapt2 link"))
        if (!linkResult.success()) {
            emit(BuildEvent(BuildEvent.Type.STEP_FAILED, linkResult.stderr(), "aapt2 link", true))
            return@flow
        }
        emit(BuildEvent(BuildEvent.Type.STEP_SUCCESS, "Resources linked", "aapt2 link"))

        // ---- Step 5: D8 DEX compilation ----
        emit(BuildEvent(BuildEvent.Type.STEP_START, "Converting to DEX with D8...", "d8"))
        val d8Result = NativeBridge.d8Compile(
            classesJar = classesJar.absolutePath,
            outputDir = dexDir.absolutePath,
            minApi = MIN_API,
            releaseMode = false
        )
        emit(BuildEvent(BuildEvent.Type.LOG, d8Result.stdout(), "d8"))
        if (!d8Result.success()) {
            emit(BuildEvent(BuildEvent.Type.STEP_FAILED, d8Result.stderr(), "d8", true))
            return@flow
        }
        val dexFile = File(dexDir, "classes.dex")
        if (!dexFile.exists()) {
            emit(BuildEvent(BuildEvent.Type.STEP_FAILED, "d8: classes.dex not produced", isError = true))
            return@flow
        }
        emit(BuildEvent(BuildEvent.Type.STEP_SUCCESS, "DEX compiled (${dexFile.length() / 1024}KB)", "d8"))

        // ---- Step 6: Add DEX to APK ----
        emit(BuildEvent(BuildEvent.Type.STEP_START, "Packaging APK...", "apkbuilder"))
        ApkPackager.addDexToApk(apkUnaligned, dexFile)
        emit(BuildEvent(BuildEvent.Type.STEP_SUCCESS, "APK packaged", "apkbuilder"))

        // ---- Step 7: zipalign ----
        emit(BuildEvent(BuildEvent.Type.STEP_START, "Aligning APK with zipalign...", "zipalign"))
        val alignResult = NativeBridge.zipalign(apkUnaligned.absolutePath, apkAligned.absolutePath)
        emit(BuildEvent(BuildEvent.Type.LOG, alignResult.stdout(), "zipalign"))
        if (!alignResult.success()) {
            emit(BuildEvent(BuildEvent.Type.STEP_FAILED, alignResult.stderr(), "zipalign", true))
            return@flow
        }
        emit(BuildEvent(BuildEvent.Type.STEP_SUCCESS, "APK aligned", "zipalign"))

        // ---- Step 8: Generate debug keystore if needed ----
        if (!keystoreFile.exists()) {
            emit(BuildEvent(BuildEvent.Type.LOG, "Generating debug keystore...", "keytool"))
            NativeBridge.keytoolGenKey(
                keystorePath = keystoreFile.absolutePath,
                alias = "androiddebugkey",
                storepass = "android",
                keypass = "android",
                dname = "CN=Android Debug,O=Android,C=US"
            )
        }

        // ---- Step 9: apksigner ----
        emit(BuildEvent(BuildEvent.Type.STEP_START, "Signing APK...", "apksigner"))
        val signResult = NativeBridge.apksignerSign(
            apkPath = apkAligned.absolutePath,
            keystorePath = keystoreFile.absolutePath,
            keyAlias = "androiddebugkey",
            ksPass = "pass:android",
            keyPass = "pass:android"
        )
        apkAligned.copyTo(apkSigned, overwrite = true)
        emit(BuildEvent(BuildEvent.Type.LOG, signResult.stdout(), "apksigner"))
        if (!signResult.success()) {
            emit(BuildEvent(BuildEvent.Type.STEP_FAILED, signResult.stderr(), "apksigner", true))
            return@flow
        }
        emit(BuildEvent(BuildEvent.Type.STEP_SUCCESS, "APK signed", "apksigner"))

        // ---- Step 10: Verify ----
        emit(BuildEvent(BuildEvent.Type.STEP_START, "Verifying APK signature...", "apksigner verify"))
        val verifyResult = NativeBridge.apksignerVerify(apkSigned.absolutePath)
        emit(BuildEvent(BuildEvent.Type.LOG, verifyResult.stdout(), "apksigner verify"))

        val apkSize = apkSigned.length() / 1024
        emit(BuildEvent(BuildEvent.Type.BUILD_SUCCESS,
            "BUILD SUCCESSFUL\nAPK: ${apkSigned.absolutePath}\nSize: ${apkSize}KB"))
        Log.i(TAG, "Build successful: ${apkSigned.absolutePath}")
    }.flowOn(Dispatchers.IO)

    /**
     * Install the built APK to device via adb.
     */
    suspend fun installToDevice(apkPath: String): String {
        val result = NativeBridge.adbCommand(arrayOf("install", "-r", apkPath))
        return if (result.success()) "Install SUCCESS\n${result.stdout()}"
        else "Install FAILED\n${result.stderr()}"
    }
}
