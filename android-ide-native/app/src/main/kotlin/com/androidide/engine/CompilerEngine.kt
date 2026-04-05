package com.androidide.engine

import com.androidide.jni.NativeBridge
import com.androidide.toolchain.ToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * CompilerEngine.kt
 *
 * Wraps Kotlin (kotlinc) and Java (javac) compilation.
 * Both are invoked as real external processes using ProcessBuilder + the
 * bundled Java binary. The kotlin-compiler.jar must be in assets/toolchain/jars/.
 */
class CompilerEngine(private val toolchain: ToolchainManager) {

    companion object {
        private const val KOTLIN_COMPILER_JAR = "kotlin-compiler.jar"
        private const val KOTLIN_STDLIB_JAR   = "kotlin-stdlib.jar"
    }

    /**
     * Compile all Kotlin/Java sources in [sourceDir].
     * Produces a classes.jar in [classesDir].
     *
     * Uses kotlinc bundled as an executable JAR, invoked via `java -jar`.
     */
    suspend fun compileKotlin(
        sourceDir: File,
        classesDir: File,
        androidJar: String,
        aidlGenDir: File? = null
    ): List<String> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        val javaPath = toolchain.toolPath("java")
        val compilerJar = File(toolchain.jarsDir, KOTLIN_COMPILER_JAR)
        val stdlibJar   = File(toolchain.jarsDir, KOTLIN_STDLIB_JAR)

        if (!compilerJar.exists()) {
            logs.add("Warning: kotlin-compiler.jar not bundled, skipping kotlinc")
            return@withContext logs
        }

        // Gather all .kt and .java source files
        val sources = mutableListOf<File>()
        sourceDir.walkTopDown().forEach { f ->
            if (f.extension == "kt" || f.extension == "java") sources.add(f)
        }
        aidlGenDir?.walkTopDown()?.filter { it.extension == "java" }?.forEach { sources.add(it) }

        if (sources.isEmpty()) {
            logs.add("No source files found in ${sourceDir.absolutePath}")
            return@withContext logs
        }

        val outputJar = File(classesDir, "classes.jar")
        classesDir.mkdirs()

        // Build classpath: android.jar + stdlib
        val cp = buildList {
            add(androidJar)
            if (stdlibJar.exists()) add(stdlibJar.absolutePath)
        }.joinToString(":")

        // kotlinc arguments
        val args = buildList {
            add("-jar"); add(compilerJar.absolutePath)  // java -jar kotlinc
            sources.forEach { add(it.absolutePath) }
            add("-cp"); add(cp)
            add("-d"); add(outputJar.absolutePath)
            add("-jvm-target"); add("17")
            add("-no-stdlib")   // we manage stdlib ourselves
        }.toTypedArray()

        val result = NativeBridge.execCommand(javaPath, args, classesDir.absolutePath)
        logs.add("[kotlinc] exit=${result[2]}")
        if (result[0].isNotBlank()) logs.add(result[0])
        if (result[1].isNotBlank()) logs.add(result[1])

        logs
    }

    /**
     * Compile Java sources with javac.
     */
    suspend fun compileJava(
        sourceDir: File,
        classesDir: File,
        androidJar: String
    ): List<String> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        val javacPath = toolchain.toolPath("javac")

        val sources = sourceDir.walkTopDown()
            .filter { it.extension == "java" }
            .map { it.absolutePath }
            .toList()

        if (sources.isEmpty()) {
            logs.add("No Java sources found.")
            return@withContext logs
        }

        classesDir.mkdirs()

        val args = buildList {
            add("-cp"); add(androidJar)
            add("-d"); add(classesDir.absolutePath)
            add("-source"); add("17")
            add("-target"); add("17")
            addAll(sources)
        }.toTypedArray()

        val result = NativeBridge.execCommand(javacPath, args, sourceDir.absolutePath)
        logs.add("[javac] exit=${result[2]}")
        if (result[0].isNotBlank()) logs.add(result[0])
        if (result[1].isNotBlank()) logs.add(result[1])

        // If javac succeeded, jar the classes
        if (result[2] == "0") {
            val jarResult = jarClasses(classesDir)
            logs.addAll(jarResult)
        }

        logs
    }

    private fun jarClasses(classesDir: File): List<String> {
        val logs = mutableListOf<String>()
        val jarPath = toolchain.toolPath("jar")
        val outputJar = File(classesDir, "classes.jar")

        val classFiles = classesDir.walkTopDown()
            .filter { it.extension == "class" }
            .toList()

        if (classFiles.isEmpty()) return logs

        val args = arrayOf("cf", outputJar.absolutePath, "-C", classesDir.absolutePath, ".")
        val result = NativeBridge.execCommand(jarPath, args, classesDir.absolutePath)
        logs.add("[jar] exit=${result[2]}")
        if (result[0].isNotBlank()) logs.add(result[0])
        if (result[1].isNotBlank()) logs.add(result[1])
        return logs
    }
}
