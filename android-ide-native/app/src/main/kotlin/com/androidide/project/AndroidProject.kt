package com.androidide.project

import java.io.File

/**
 * AndroidProject.kt
 *
 * Represents an Android project on disk.
 * Mirrors a real Android Studio project layout.
 */
data class AndroidProject(
    val id: String,
    val name: String,
    val packageName: String,
    val dir: File,
    val minSdk: Int = 24,
    val targetSdk: Int = 34,
    val compileSdk: Int = 34,
    val useKotlin: Boolean = true,
    val useNdk: Boolean = false
) {
    val srcDir: File get() = File(dir, "app/src/main")
    val javaDir: File get() = File(srcDir, if (useKotlin) "kotlin" else "java")
    val resDir: File get() = File(srcDir, "res")
    val manifestFile: File get() = File(srcDir, "AndroidManifest.xml")
    val buildDir: File get() = File(dir, "build")
    val outputApkPath: String get() = File(buildDir, "app-debug.apk").absolutePath
    val outputAabPath: String get() = File(buildDir, "app-release.aab").absolutePath

    /** All source files in the project */
    fun sourceFiles(): List<File> {
        return javaDir.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .toList()
    }

    /** All resource files */
    fun resourceFiles(): List<File> {
        return resDir.walkTopDown()
            .filter { it.isFile }
            .toList()
    }

    fun exists(): Boolean = dir.exists() && manifestFile.exists()

    fun toJson(): String = """
        {
          "id": "$id",
          "name": "$name",
          "packageName": "$packageName",
          "path": "${dir.absolutePath}",
          "minSdk": $minSdk,
          "targetSdk": $targetSdk,
          "useKotlin": $useKotlin,
          "useNdk": $useNdk
        }
    """.trimIndent()
}
