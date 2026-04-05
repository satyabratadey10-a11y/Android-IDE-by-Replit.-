package com.androidide.engine

import com.androidide.jni.NativeBridge
import com.androidide.toolchain.ToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * TerminalEngine.kt
 *
 * Interactive terminal engine backed by REAL process execution.
 * Parses the user's command line and routes to the appropriate
 * native tool wrapper or falls back to generic exec.
 */
class TerminalEngine(private val toolchain: ToolchainManager) {

    data class TerminalOutput(val text: String, val isError: Boolean = false)

    private var currentDir: String = toolchain.projectsDir.absolutePath

    fun execute(commandLine: String): Flow<TerminalOutput> = flow {
        val trimmed = commandLine.trim()
        if (trimmed.isEmpty()) return@flow

        emit(TerminalOutput("$ $trimmed"))

        val parts = tokenize(trimmed)
        val cmd = parts.firstOrNull() ?: return@flow
        val args = parts.drop(1).toTypedArray()

        when (cmd) {
            "cd"    -> emit(handleCd(args))
            "pwd"   -> emit(TerminalOutput(currentDir))
            "ls"    -> emit(execTool("ls", *args, wd = currentDir))
            "cat"   -> emit(execTool("cat", *args, wd = currentDir))
            "mkdir" -> emit(execTool("mkdir", *args, wd = currentDir))
            "rm"    -> emit(execTool("rm", *args, wd = currentDir))
            "cp"    -> emit(execTool("cp", *args, wd = currentDir))
            "mv"    -> emit(execTool("mv", *args, wd = currentDir))
            "echo"  -> emit(TerminalOutput(args.joinToString(" ")))
            "chmod" -> emit(execTool("chmod", *args, wd = currentDir))
            "grep"  -> emit(execTool("grep", *args, wd = currentDir))
            "find"  -> emit(execTool("find", *args, wd = currentDir))
            "which" -> emit(execTool("which", *args, wd = currentDir))
            "env"   -> emit(execTool("env", wd = currentDir))
            "ps"    -> emit(execTool("ps", *args, wd = currentDir))
            "kill"  -> emit(execTool("kill", *args, wd = currentDir))
            "df"    -> emit(execTool("df", *args, wd = currentDir))
            "du"    -> emit(execTool("du", *args, wd = currentDir))
            "zip"   -> emit(execTool("zip", *args, wd = currentDir))
            "unzip" -> emit(execTool("unzip", *args, wd = currentDir))
            "curl"  -> emit(execTool("curl", *args, wd = currentDir))
            "wget"  -> emit(execTool("wget", *args, wd = currentDir))
            "tar"   -> emit(execTool("tar", *args, wd = currentDir))

            // Android build tools
            "aapt2"      -> emit(execToolchain("aapt2", *args))
            "d8"         -> emit(execToolchain("d8", *args))
            "zipalign"   -> emit(execToolchain("zipalign", *args))
            "apksigner"  -> emit(execToolchain("apksigner", *args))
            "aidl"       -> emit(execToolchain("aidl", *args))
            "dexdump"    -> emit(execToolchain("dexdump", *args))
            "adb"        -> emit(execToolchain("adb", *args))
            "java"       -> emit(execToolchain("java", *args))
            "javac"      -> emit(execToolchain("javac", *args))
            "clang"      -> emit(execToolchain("clang", *args))
            "clang++"    -> emit(execToolchain("clang++", *args))
            "llvm-strip" -> emit(execToolchain("llvm-strip", *args))
            "llvm-ar"    -> emit(execToolchain("llvm-ar", *args))
            "keytool"    -> emit(execToolchain("keytool", *args))

            // Builtins
            "help" -> emit(TerminalOutput(HELP_TEXT))
            "clear" -> emit(TerminalOutput("\u001b[2J\u001b[H"))  // ANSI clear

            else -> {
                // Try to find the binary in toolchain, then fallback to /system/bin
                val toolchainBin = toolchain.toolPath(cmd)
                val binary = when {
                    java.io.File(toolchainBin).canExecute() -> toolchainBin
                    else -> "/system/bin/$cmd"
                }
                emit(execBinary(binary, args, currentDir))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun handleCd(args: Array<String>): TerminalOutput {
        val target = when {
            args.isEmpty() -> toolchain.projectsDir.absolutePath
            args[0].startsWith("/") -> args[0]
            args[0] == ".." -> java.io.File(currentDir).parent ?: currentDir
            else -> "$currentDir/${args[0]}"
        }
        val dir = java.io.File(target)
        return if (dir.exists() && dir.isDirectory) {
            currentDir = dir.canonicalPath
            TerminalOutput("")
        } else {
            TerminalOutput("cd: $target: No such directory", isError = true)
        }
    }

    private fun execTool(name: String, vararg args: String, wd: String = currentDir): TerminalOutput {
        val binary = "/system/bin/$name"
        return execBinary(binary, args.toTypedArray(), wd)
    }

    private fun execToolchain(name: String, vararg args: String): TerminalOutput {
        val binary = toolchain.toolPath(name)
        return execBinary(binary, args.toTypedArray(), currentDir)
    }

    private fun execBinary(binary: String, args: Array<String>, wd: String): TerminalOutput {
        return try {
            val result = NativeBridge.execCommand(binary, args, wd)
            val out = result[0].trimEnd()
            val err = result[1].trimEnd()
            val combined = buildString {
                if (out.isNotEmpty()) append(out)
                if (err.isNotEmpty()) {
                    if (isNotEmpty()) append("\n")
                    append(err)
                }
            }
            TerminalOutput(combined, isError = result[2] != "0")
        } catch (e: Exception) {
            TerminalOutput("Error: ${e.message}", isError = true)
        }
    }

    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '
        for (ch in input) {
            when {
                inQuotes && ch == quoteChar -> inQuotes = false
                !inQuotes && (ch == '"' || ch == '\'') -> { inQuotes = true; quoteChar = ch }
                !inQuotes && ch == ' ' -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }

    private val HELP_TEXT = """
Android IDE Terminal — available commands:
  File ops:   ls, cd, pwd, cat, mkdir, rm, cp, mv, chmod, find, grep, du, df
  Archive:    zip, unzip, tar
  Network:    curl, wget
  Process:    ps, kill, env

Build tools:
  aapt2      Compile/link Android resources
  d8         Convert classes.jar → DEX
  zipalign   4-byte align APK
  apksigner  Sign APK
  aidl       Compile AIDL interfaces
  dexdump    Inspect DEX files
  adb        Android Debug Bridge
  java/javac Java runtime/compiler
  clang      C/C++ compiler (NDK)
  keytool    Keystore management

Type 'clear' to clear the terminal.
""".trimIndent()
}
