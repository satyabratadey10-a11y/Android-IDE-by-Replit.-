package com.androidide.engine

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * ApkPackager.kt
 *
 * Handles adding compiled DEX files into an APK.
 * An APK is a ZIP file, so we open the existing APK (produced by aapt2 link)
 * and add/replace the classes.dex entry.
 */
object ApkPackager {

    /**
     * Add [dexFile] to [apkFile] as "classes.dex".
     * The original APK is modified in-place via a temp file swap.
     */
    fun addDexToApk(apkFile: File, dexFile: File) {
        val tempFile = File(apkFile.parent, "${apkFile.name}.tmp")
        ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
            // Copy all existing entries except classes.dex
            ZipFile(apkFile).use { zin ->
                val entries = zin.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name == "classes.dex") continue
                    zos.putNextEntry(ZipEntry(entry.name))
                    zin.getInputStream(entry).copyTo(zos)
                    zos.closeEntry()
                }
            }
            // Add the new DEX
            zos.putNextEntry(ZipEntry("classes.dex"))
            dexFile.inputStream().copyTo(zos)
            zos.closeEntry()
        }
        tempFile.renameTo(apkFile)
    }

    /**
     * Add multiple DEX shards (classes.dex, classes2.dex, …) to an APK.
     */
    fun addMultipleDexToApk(apkFile: File, dexFiles: List<File>) {
        val tempFile = File(apkFile.parent, "${apkFile.name}.tmp")
        ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
            ZipFile(apkFile).use { zin ->
                val entries = zin.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.matches(Regex("classes\\d*\\.dex"))) continue
                    zos.putNextEntry(ZipEntry(entry.name))
                    zin.getInputStream(entry).copyTo(zos)
                    zos.closeEntry()
                }
            }
            dexFiles.forEachIndexed { index, dexFile ->
                val entryName = if (index == 0) "classes.dex" else "classes${index + 1}.dex"
                zos.putNextEntry(ZipEntry(entryName))
                dexFile.inputStream().copyTo(zos)
                zos.closeEntry()
            }
        }
        tempFile.renameTo(apkFile)
    }

    /**
     * Add native .so libraries to the APK under lib/<abi>/<libname>.so
     */
    fun addNativeLibsToApk(apkFile: File, soFiles: Map<String, File>) {
        val tempFile = File(apkFile.parent, "${apkFile.name}.tmp")
        ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
            ZipFile(apkFile).use { zin ->
                val entries = zin.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (soFiles.containsKey(entry.name)) continue
                    zos.putNextEntry(ZipEntry(entry.name))
                    zin.getInputStream(entry).copyTo(zos)
                    zos.closeEntry()
                }
            }
            soFiles.forEach { (entryPath, soFile) ->
                zos.putNextEntry(ZipEntry(entryPath))
                soFile.inputStream().copyTo(zos)
                zos.closeEntry()
            }
        }
        tempFile.renameTo(apkFile)
    }

    /**
     * Extract an entry from an APK to a file.
     */
    fun extractFromApk(apkFile: File, entryName: String, outputFile: File) {
        ZipFile(apkFile).use { zip ->
            val entry = zip.getEntry(entryName) ?: throw IllegalArgumentException("Entry not found: $entryName")
            outputFile.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input ->
                outputFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    /**
     * List all entries in an APK.
     */
    fun listApkContents(apkFile: File): List<String> {
        val entries = mutableListOf<String>()
        ZipFile(apkFile).use { zip ->
            val iter = zip.entries()
            while (iter.hasMoreElements()) {
                entries.add(iter.nextElement().name)
            }
        }
        return entries
    }
}
