package com.androidide.engine

import com.androidide.jni.NativeBridge
import com.androidide.project.AndroidProject
import com.androidide.toolchain.ToolchainManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * SearchEngine.kt
 *
 * Code search across project files.
 * Uses the native `grep` binary for fast full-text search,
 * with a pure-Kotlin fallback for structural/symbol search.
 */
class SearchEngine(private val toolchain: ToolchainManager) {

    data class SearchResult(
        val file: File,
        val lineNumber: Int,
        val lineContent: String,
        val matchStart: Int,
        val matchEnd: Int
    )

    /**
     * Full-text search across all source files using native `grep`.
     */
    suspend fun grep(
        project: AndroidProject,
        query: String,
        caseSensitive: Boolean = false,
        regex: Boolean = false,
        fileExtensions: List<String> = listOf("kt", "java", "xml", "cpp", "c", "h")
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()

        val args = buildList {
            add("-rn")                    // recursive, print line numbers
            if (!caseSensitive) add("-i") // case insensitive
            if (!regex) add("-F")         // fixed string (not regex)
            // Include only requested file types
            fileExtensions.forEach { ext -> add("--include=*.$ext") }
            add(query)
            add(project.srcDir.absolutePath)
        }.toTypedArray()

        val result = NativeBridge.execCommand("/system/bin/grep", args, project.dir.absolutePath)
        if (result[2] == "0" || result[2] == "1") { // grep returns 1 when no matches
            parseGrepOutput(result[0], query, caseSensitive).also { results.addAll(it) }
        }

        results
    }

    /**
     * Find all usages of a symbol (class name, function name, variable).
     */
    suspend fun findUsages(
        project: AndroidProject,
        symbol: String
    ): List<SearchResult> = grep(project, symbol, caseSensitive = true)

    /**
     * Find all TODO/FIXME/HACK comments in the project.
     */
    suspend fun findTodos(project: AndroidProject): List<SearchResult> {
        return grep(project, "TODO|FIXME|HACK|XXX|BUG", regex = true)
    }

    /**
     * Find all files matching a pattern.
     */
    suspend fun findFiles(
        project: AndroidProject,
        namePattern: String
    ): List<File> = withContext(Dispatchers.IO) {
        val result = NativeBridge.execCommand(
            "/system/bin/find",
            arrayOf(project.dir.absolutePath, "-name", namePattern, "-type", "f"),
            project.dir.absolutePath
        )
        result[0].lines()
            .filter { it.isNotBlank() }
            .map { File(it.trim()) }
            .filter { it.exists() }
    }

    /**
     * Replace all occurrences in a file.
     */
    suspend fun replaceInFile(
        file: File,
        find: String,
        replace: String,
        caseSensitive: Boolean = true
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        val content = file.readText()
        val pattern = if (caseSensitive) find else find.lowercase()
        val newContent = buildString {
            var idx = 0
            while (idx < content.length) {
                val searchIn = if (caseSensitive) content else content.lowercase()
                val found = searchIn.indexOf(pattern, idx)
                if (found == -1) {
                    append(content.substring(idx))
                    break
                }
                append(content.substring(idx, found))
                append(replace)
                idx = found + find.length
                count++
            }
        }
        if (count > 0) file.writeText(newContent)
        count
    }

    /**
     * Replace all across project files.
     */
    suspend fun replaceAll(
        project: AndroidProject,
        find: String,
        replace: String,
        caseSensitive: Boolean = true,
        fileExtensions: List<String> = listOf("kt", "java", "xml")
    ): Map<File, Int> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<File, Int>()
        project.srcDir.walkTopDown()
            .filter { it.isFile && it.extension in fileExtensions }
            .forEach { file ->
                val count = replaceInFile(file, find, replace, caseSensitive)
                if (count > 0) results[file] = count
            }
        results
    }

    // ---- Internals ----

    private fun parseGrepOutput(
        raw: String,
        query: String,
        caseSensitive: Boolean
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        raw.lines().forEach { line ->
            if (line.isBlank()) return@forEach
            // Format: /path/to/file:lineNumber:lineContent
            val firstColon = line.indexOf(':')
            if (firstColon == -1) return@forEach
            val secondColon = line.indexOf(':', firstColon + 1)
            if (secondColon == -1) return@forEach

            val filePath = line.substring(0, firstColon)
            val lineNum = line.substring(firstColon + 1, secondColon).toIntOrNull() ?: return@forEach
            val content = line.substring(secondColon + 1)

            val searchIn = if (caseSensitive) content else content.lowercase()
            val searchFor = if (caseSensitive) query else query.lowercase()
            val matchStart = searchIn.indexOf(searchFor)
            val matchEnd = if (matchStart >= 0) matchStart + query.length else 0

            results.add(SearchResult(File(filePath), lineNum, content, matchStart, matchEnd))
        }
        return results
    }
}
