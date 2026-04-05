package com.androidide.jni

/**
 * NativeBridge.kt
 *
 * Kotlin interface to the C++ JNI layer.
 * Every function here corresponds to a native implementation in jni_bridge.cpp.
 *
 * Return type for all tool functions: Array<String>
 *   [0] = stdout
 *   [1] = stderr
 *   [2] = exit code (as string)
 */
object NativeBridge {

    init {
        System.loadLibrary("androidide_native")
    }

    // ---- Setup ----
    external fun setToolchainDir(dir: String)
    external fun chmodExec(path: String): Int

    // ---- Generic execution (terminal) ----
    external fun execCommand(binary: String, argv: Array<String>, workingDir: String): Array<String>

    // ---- AAPT2 ----
    external fun aapt2Compile(inputDir: String, outputDir: String): Array<String>
    external fun aapt2Link(manifest: String, compiledRes: String, androidJar: String, outputApk: String): Array<String>

    // ---- D8 ----
    external fun d8Compile(classesJar: String, outputDir: String, minApi: String, releaseMode: Boolean): Array<String>

    // ---- R8 ----
    external fun r8Optimize(classesJar: String, outputDir: String, androidJar: String, proguardRules: String, minApi: String): Array<String>

    // ---- zipalign ----
    external fun zipalign(inputApk: String, outputApk: String): Array<String>

    // ---- apksigner ----
    external fun apksignerSign(apkPath: String, keystorePath: String, keyAlias: String, ksPass: String, keyPass: String): Array<String>
    external fun apksignerVerify(apkPath: String): Array<String>

    // ---- keytool ----
    external fun keytoolGenKey(keystorePath: String, alias: String, storepass: String, keypass: String, dname: String): Array<String>

    // ---- clang / NDK ----
    external fun clangCompile(sourceFile: String, outputObj: String, sysroot: String, targetTriple: String): Array<String>

    // ---- AIDL ----
    external fun aidlCompile(aidlFile: String, includeDir: String, outputDir: String, cppMode: Boolean): Array<String>

    // ---- bundletool ----
    external fun bundletoolBuild(bundletoolJar: String, baseZip: String, outputAab: String): Array<String>

    // ---- dexdump ----
    external fun dexdump(dexFile: String): Array<String>

    // ---- objdump ----
    external fun objdump(binary: String, disassemble: Boolean): Array<String>

    // ---- adb ----
    external fun adbCommand(args: Array<String>): Array<String>

    // ---- file utils ----
    external fun extractAsset(assetPath: String, outputPath: String): Boolean
    external fun getFileSize(path: String): Long

    // Convenience helpers
    fun Array<String>.stdout() = this[0]
    fun Array<String>.stderr() = this[1]
    fun Array<String>.exitCode() = this[2].toIntOrNull() ?: -1
    fun Array<String>.success() = exitCode() == 0
}
