package com.androidide.engine

import com.androidide.jni.NativeBridge
import com.androidide.project.AndroidProject
import com.androidide.toolchain.ToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * NdkBuildEngine.kt
 *
 * Drives the NDK (C/C++) build pipeline using real clang + lld binaries.
 *
 * Pipeline:
 *   1. Compile each .cpp / .c file with clang → .o
 *   2. Link all .o files with lld → libNATIVE.so
 *   3. Strip symbols (release mode)
 *   4. Add .so to APK under lib/<abi>/
 */
class NdkBuildEngine(
    private val toolchain: ToolchainManager,
    private val project: AndroidProject
) {
    companion object {
        // Supported ABI targets
        val SUPPORTED_ABIS = listOf(
            AbiTarget("arm64-v8a",  "aarch64-linux-android21"),
            AbiTarget("armeabi-v7a","armv7a-linux-androideabi21"),
            AbiTarget("x86_64",     "x86_64-linux-android21")
        )

        private const val SYSROOT_RELATIVE =
            "toolchains/llvm/prebuilt/linux-x86_64/sysroot"
    }

    data class AbiTarget(val abi: String, val triple: String)

    data class NdkBuildEvent(
        val type: Type,
        val message: String,
        val abi: String = ""
    ) {
        enum class Type { LOG, COMPILING, LINKING, STRIPPING, SUCCESS, FAILED }
    }

    /**
     * Build all C/C++ sources in the project for every requested ABI.
     * Returns a map of ABI → .so file (ready to be added to APK).
     */
    fun buildNativeLibs(
        abis: List<AbiTarget> = listOf(SUPPORTED_ABIS.first()),
        releaseMode: Boolean = false
    ): Flow<NdkBuildEvent> = flow {
        val cppDir = File(project.srcDir, "cpp")
        if (!cppDir.exists()) {
            emit(NdkBuildEvent(NdkBuildEvent.Type.LOG, "No cpp/ directory found, skipping NDK build"))
            return@flow
        }

        val sources = cppDir.walkTopDown()
            .filter { it.extension == "cpp" || it.extension == "c" }
            .toList()

        if (sources.isEmpty()) {
            emit(NdkBuildEvent(NdkBuildEvent.Type.LOG, "No C/C++ source files found"))
            return@flow
        }

        val ndkSysroot = findNdkSysroot()
        if (ndkSysroot == null) {
            emit(NdkBuildEvent(NdkBuildEvent.Type.FAILED,
                "NDK sysroot not found. Bundle NDK toolchain in assets/toolchain/."))
            return@flow
        }

        emit(NdkBuildEvent(NdkBuildEvent.Type.LOG,
            "NDK build: ${sources.size} source files, ${abis.size} ABI(s)"))

        for (abi in abis) {
            emit(NdkBuildEvent(NdkBuildEvent.Type.LOG, "Building for ${abi.abi}...", abi.abi))

            val objDir = File(project.buildDir, "obj/${abi.abi}").apply { mkdirs() }
            val libsDir = File(project.buildDir, "libs/${abi.abi}").apply { mkdirs() }

            val objFiles = mutableListOf<File>()

            // ---- Compile each source file ----
            for (src in sources) {
                val objName = src.nameWithoutExtension + ".o"
                val objFile = File(objDir, objName)

                emit(NdkBuildEvent(NdkBuildEvent.Type.COMPILING,
                    "Compiling: ${src.name}", abi.abi))

                val result = NativeBridge.clangCompile(
                    sourceFile = src.absolutePath,
                    outputObj  = objFile.absolutePath,
                    sysroot    = ndkSysroot,
                    targetTriple = abi.triple
                )

                if (result[2] != "0") {
                    emit(NdkBuildEvent(NdkBuildEvent.Type.FAILED,
                        "Compile error in ${src.name}:\n${result[1]}", abi.abi))
                    return@flow
                }
                if (result[0].isNotBlank()) emit(NdkBuildEvent(NdkBuildEvent.Type.LOG, result[0], abi.abi))
                objFiles.add(objFile)
            }

            // ---- Link → .so ----
            val libName = "lib${project.name.lowercase().replace(" ", "_")}.so"
            val soFile = File(libsDir, libName)
            emit(NdkBuildEvent(NdkBuildEvent.Type.LINKING, "Linking: $libName", abi.abi))

            val linkResult = NativeBridge.execCommand(
                binary = toolchain.toolPath("clang"),
                argv   = buildList {
                    add("-fuse-ld=lld"); add("-shared")
                    add("-o"); add(soFile.absolutePath)
                    add("--sysroot"); add(ndkSysroot)
                    add("-target"); add(abi.triple)
                    add("-fPIC")
                    if (releaseMode) add("-O2")
                    objFiles.forEach { add(it.absolutePath) }
                    add("-llog"); add("-landroid")
                }.toTypedArray(),
                workingDir = objDir.absolutePath
            )

            if (linkResult[2] != "0") {
                emit(NdkBuildEvent(NdkBuildEvent.Type.FAILED,
                    "Link error:\n${linkResult[1]}", abi.abi))
                return@flow
            }

            // ---- Strip in release mode ----
            if (releaseMode) {
                emit(NdkBuildEvent(NdkBuildEvent.Type.STRIPPING, "Stripping symbols...", abi.abi))
                val strippedFile = File(libsDir, "stripped_$libName")
                NativeBridge.execCommand(
                    binary = toolchain.toolPath("llvm-strip"),
                    argv   = arrayOf("--strip-unneeded", "-o",
                                     strippedFile.absolutePath, soFile.absolutePath),
                    workingDir = libsDir.absolutePath
                )
                if (strippedFile.exists()) strippedFile.renameTo(soFile)
            }

            emit(NdkBuildEvent(NdkBuildEvent.Type.SUCCESS,
                "Built ${abi.abi}/${libName} (${soFile.length() / 1024}KB)", abi.abi))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Package all built .so files into an existing APK.
     */
    fun packageNativeLibsIntoApk(
        apkFile: File,
        abis: List<AbiTarget>
    ) {
        val soMap = mutableMapOf<String, File>()
        for (abi in abis) {
            val libsDir = File(project.buildDir, "libs/${abi.abi}")
            libsDir.listFiles()?.filter { it.extension == "so" }?.forEach { so ->
                soMap["lib/${abi.abi}/${so.name}"] = so
            }
        }
        if (soMap.isNotEmpty()) {
            ApkPackager.addNativeLibsToApk(apkFile, soMap)
        }
    }

    private fun findNdkSysroot(): String? {
        // Look for extracted NDK sysroot in toolchain directory
        val candidate = File(toolchain.toolchainDir.parent, "sysroot")
        return if (candidate.exists()) candidate.absolutePath else null
    }
}
