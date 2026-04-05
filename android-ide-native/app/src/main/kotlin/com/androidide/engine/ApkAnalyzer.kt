package com.androidide.engine

import com.androidide.jni.NativeBridge
import com.androidide.toolchain.ToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/**
 * ApkAnalyzer.kt
 *
 * Real APK analysis using aapt2 dump, dexdump, and direct ZIP inspection.
 * Provides detailed breakdown of APK contents, permissions, DEX method count,
 * resource usage, and signing info.
 */
class ApkAnalyzer(private val toolchain: ToolchainManager) {

    data class ApkInfo(
        val path: String,
        val fileSizeBytes: Long,
        val packageName: String,
        val versionCode: Int,
        val versionName: String,
        val minSdk: Int,
        val targetSdk: Int,
        val permissions: List<String>,
        val activities: List<String>,
        val services: List<String>,
        val receivers: List<String>,
        val dexFiles: List<DexInfo>,
        val nativeLibs: List<NativeLibInfo>,
        val resources: List<ApkEntry>,
        val signingInfo: SigningInfo?
    ) {
        val totalMethodCount: Int get() = dexFiles.sumOf { it.methodCount }
        val fileSizeMb: Float get() = fileSizeBytes / (1024f * 1024f)
    }

    data class DexInfo(
        val name: String,
        val sizeBytes: Long,
        val methodCount: Int,
        val classCount: Int
    )

    data class NativeLibInfo(val abi: String, val name: String, val sizeBytes: Long)

    data class ApkEntry(val path: String, val sizeBytes: Long, val compressedBytes: Long)

    data class SigningInfo(
        val verified: Boolean,
        val scheme: String,
        val certificate: String
    )

    /**
     * Analyse [apkFile] and return a full [ApkInfo] report.
     */
    suspend fun analyze(apkFile: File): ApkInfo = withContext(Dispatchers.IO) {
        val entries = listEntries(apkFile)

        // Parse manifest via aapt2 dump
        val manifestInfo = dumpManifest(apkFile)

        // Analyse DEX files
        val dexEntries = entries.filter { it.path.matches(Regex("classes\\d*\\.dex")) }
        val dexInfoList = dexEntries.map { entry ->
            val tmpDex = extractEntryTemp(apkFile, entry.path)
            val dexInfo = analyzeDex(tmpDex, entry.name)
            tmpDex.delete()
            dexInfo
        }

        // Native libs
        val nativeLibs = entries
            .filter { it.path.startsWith("lib/") && it.path.endsWith(".so") }
            .map { entry ->
                val parts = entry.path.split("/")
                NativeLibInfo(
                    abi = parts.getOrElse(1) { "unknown" },
                    name = parts.lastOrNull() ?: entry.path,
                    sizeBytes = entry.sizeBytes
                )
            }

        // Signing
        val signing = verifySignature(apkFile)

        ApkInfo(
            path = apkFile.absolutePath,
            fileSizeBytes = apkFile.length(),
            packageName = manifestInfo["packageName"] ?: "unknown",
            versionCode = manifestInfo["versionCode"]?.toIntOrNull() ?: 0,
            versionName = manifestInfo["versionName"] ?: "unknown",
            minSdk = manifestInfo["minSdk"]?.toIntOrNull() ?: 0,
            targetSdk = manifestInfo["targetSdk"]?.toIntOrNull() ?: 0,
            permissions = manifestInfo.entries
                .filter { it.key.startsWith("permission") }
                .map { it.value },
            activities = manifestInfo.entries
                .filter { it.key.startsWith("activity") }
                .map { it.value },
            services = manifestInfo.entries
                .filter { it.key.startsWith("service") }
                .map { it.value },
            receivers = manifestInfo.entries
                .filter { it.key.startsWith("receiver") }
                .map { it.value },
            dexFiles = dexInfoList,
            nativeLibs = nativeLibs,
            resources = entries.filter { it.path.startsWith("res/") },
            signingInfo = signing
        )
    }

    /**
     * Generate a text report from [ApkInfo].
     */
    fun generateReport(info: ApkInfo): String = buildString {
        appendLine("═══════════════════════════════════════")
        appendLine("  APK Analysis Report")
        appendLine("═══════════════════════════════════════")
        appendLine("Package:     ${info.packageName}")
        appendLine("Version:     ${info.versionName} (${info.versionCode})")
        appendLine("SDK:         minSdk=${info.minSdk}, targetSdk=${info.targetSdk}")
        appendLine("Size:        ${"%.2f".format(info.fileSizeMb)} MB")
        appendLine()
        appendLine("── DEX ──────────────────────────────")
        info.dexFiles.forEach { dex ->
            appendLine("  ${dex.name}: ${dex.classCount} classes, ${dex.methodCount} methods, ${dex.sizeBytes/1024}KB")
        }
        appendLine("  Total methods: ${info.totalMethodCount} / 65536")
        appendLine()
        appendLine("── Native Libs ──────────────────────")
        if (info.nativeLibs.isEmpty()) appendLine("  (none)")
        info.nativeLibs.forEach { lib ->
            appendLine("  [${lib.abi}] ${lib.name} (${lib.sizeBytes/1024}KB)")
        }
        appendLine()
        appendLine("── Permissions ──────────────────────")
        if (info.permissions.isEmpty()) appendLine("  (none)")
        info.permissions.forEach { appendLine("  $it") }
        appendLine()
        appendLine("── Components ───────────────────────")
        appendLine("  Activities: ${info.activities.size}")
        info.activities.forEach { appendLine("    $it") }
        appendLine("  Services: ${info.services.size}")
        info.services.forEach { appendLine("    $it") }
        appendLine("  Receivers: ${info.receivers.size}")
        info.receivers.forEach { appendLine("    $it") }
        appendLine()
        val sig = info.signingInfo
        if (sig != null) {
            appendLine("── Signing ──────────────────────────")
            appendLine("  Verified: ${sig.verified}")
            appendLine("  Scheme: ${sig.scheme}")
            appendLine("  Certificate: ${sig.certificate}")
        }
        appendLine("═══════════════════════════════════════")
    }

    // ---- Internals ----

    private fun listEntries(apkFile: File): List<ApkEntry> {
        val entries = mutableListOf<ApkEntry>()
        ZipFile(apkFile).use { zip ->
            val iter = zip.entries()
            while (iter.hasMoreElements()) {
                val e = iter.nextElement()
                entries.add(ApkEntry(e.name, e.size, e.compressedSize))
            }
        }
        return entries
    }

    private fun dumpManifest(apkFile: File): Map<String, String> {
        val result = NativeBridge.execCommand(
            binary = toolchain.toolPath("aapt2"),
            argv = arrayOf("dump", "xmltree", "--file", "AndroidManifest.xml", apkFile.absolutePath),
            workingDir = apkFile.parent
        )
        return parseAapt2ManifestDump(result[0])
    }

    private fun parseAapt2ManifestDump(dump: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var permIdx = 0; var actIdx = 0; var svcIdx = 0; var rcvIdx = 0
        dump.lines().forEach { line ->
            val trimmed = line.trim()
            fun attr(key: String) = Regex("""$key.*?Raw="([^"]+)"""").find(trimmed)?.groupValues?.get(1)
            when {
                trimmed.contains("package=") -> attr("package")?.let { map["packageName"] = it }
                trimmed.contains("versionCode=") -> attr("versionCode")?.let { map["versionCode"] = it }
                trimmed.contains("versionName=") -> attr("versionName")?.let { map["versionName"] = it }
                trimmed.contains("minSdkVersion=") -> attr("minSdkVersion")?.let { map["minSdk"] = it }
                trimmed.contains("targetSdkVersion=") -> attr("targetSdkVersion")?.let { map["targetSdk"] = it }
                trimmed.contains("uses-permission") -> attr("name")?.let { map["permission${permIdx++}"] = it }
                trimmed.contains("activity") && !trimmed.contains("alias") ->
                    attr("name")?.let { map["activity${actIdx++}"] = it }
                trimmed.contains("service") -> attr("name")?.let { map["service${svcIdx++}"] = it }
                trimmed.contains("receiver") -> attr("name")?.let { map["receiver${rcvIdx++}"] = it }
            }
        }
        return map
    }

    private fun analyzeDex(dexFile: File, name: String): DexInfo {
        val result = NativeBridge.dexdump(dexFile.absolutePath)
        val output = result[0]
        val methodCount = Regex("name\\s*:", RegexOption.IGNORE_CASE).findAll(output).count()
        val classCount = Regex("Class descriptor").findAll(output).count()
        return DexInfo(name, dexFile.length(), methodCount, classCount)
    }

    private fun verifySignature(apkFile: File): SigningInfo? {
        val result = NativeBridge.apksignerVerify(apkFile.absolutePath)
        val output = result[0] + result[1]
        val verified = result[2] == "0"
        val scheme = when {
            output.contains("v3", ignoreCase = true) -> "APK Signature Scheme v3"
            output.contains("v2", ignoreCase = true) -> "APK Signature Scheme v2"
            output.contains("v1", ignoreCase = true) -> "JAR Signature (v1)"
            else -> "Unknown"
        }
        val cert = Regex("Signer #\\d+ certificate DN: (.+)").find(output)?.groupValues?.get(1) ?: "N/A"
        return SigningInfo(verified, scheme, cert)
    }

    private fun extractEntryTemp(apkFile: File, entryName: String): File {
        val tmpFile = File.createTempFile("ide_dex_", ".dex")
        tmpFile.deleteOnExit()
        try {
            ApkPackager.extractFromApk(apkFile, entryName, tmpFile)
        } catch (_: Exception) {}
        return tmpFile
    }

    private val ApkEntry.name: String get() = path.substringAfterLast("/")
}
