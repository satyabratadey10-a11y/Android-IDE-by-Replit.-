package com.androidide.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidide.engine.SearchEngine
import com.androidide.project.AndroidProject
import com.androidide.toolchain.ToolchainManager
import com.androidide.ui.theme.IdeColors
import kotlinx.coroutines.launch

/**
 * SearchScreen — Full-text code search across the current project.
 * Uses native grep for speed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    toolchain: ToolchainManager,
    project: AndroidProject,
    onFileClick: (java.io.File, Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val engine = remember { SearchEngine(toolchain) }
    val focusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SearchEngine.SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchTime by remember { mutableStateOf(0L) }
    var replaceMode by remember { mutableStateOf(false) }
    var replaceText by remember { mutableStateOf("") }

    fun search() {
        if (query.isBlank()) return
        isSearching = true
        scope.launch {
            val start = System.currentTimeMillis()
            results = engine.grep(project, query, caseSensitive, useRegex)
            searchTime = System.currentTimeMillis() - start
            isSearching = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeColors.Background)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null,
                        tint = IdeColors.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Search", color = IdeColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = IdeColors.Surface),
            actions = {
                IconButton(onClick = { replaceMode = !replaceMode }) {
                    Icon(
                        if (replaceMode) Icons.Default.FindReplace else Icons.Default.SwapVert,
                        contentDescription = "Toggle replace",
                        tint = if (replaceMode) IdeColors.Primary else IdeColors.TextSecondary
                    )
                }
            }
        )

        // Search bar
        Surface(color = IdeColors.Surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text("Search in ${project.name}...",
                            color = IdeColors.TextSecondary, fontSize = 13.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = IdeColors.TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IdeColors.Primary,
                            unfocusedBorderColor = IdeColors.Border
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; results = emptyList() },
                                    modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = null,
                                        tint = IdeColors.TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() })
                    )
                    IconButton(
                        onClick = { search() },
                        enabled = query.isNotBlank() && !isSearching
                    ) {
                        if (isSearching)
                            CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                color = IdeColors.Primary, strokeWidth = 2.dp)
                        else
                            Icon(Icons.Default.Search, contentDescription = "Search",
                                tint = IdeColors.Primary)
                    }
                }

                // Options row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip("Aa", caseSensitive) { caseSensitive = it }
                    FilterChip(".*", useRegex) { useRegex = it }
                    Spacer(Modifier.weight(1f))
                    if (results.isNotEmpty()) {
                        Text("${results.size} results in ${searchTime}ms",
                            color = IdeColors.TextSecondary, fontSize = 10.sp)
                    }
                }

                // Replace row
                if (replaceMode) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = replaceText,
                            onValueChange = { replaceText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Replace with...",
                                color = IdeColors.TextSecondary, fontSize = 13.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = IdeColors.TextPrimary, fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IdeColors.Warning,
                                unfocusedBorderColor = IdeColors.Border
                            ),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    val replaced = engine.replaceAll(project, query, replaceText, caseSensitive)
                                    val total = replaced.values.sum()
                                    results = emptyList()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IdeColors.Warning),
                            enabled = query.isNotBlank() && !isSearching
                        ) { Text("Replace All", fontSize = 12.sp) }
                    }
                }
            }
        }
        Divider(color = IdeColors.Border)

        // Results
        if (results.isEmpty() && !isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ManageSearch, contentDescription = null,
                        tint = IdeColors.TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (query.isBlank()) "Enter a search term above"
                        else "No results for \"$query\"",
                        color = IdeColors.TextSecondary, fontSize = 14.sp
                    )
                }
            }
        } else {
            // Group by file
            val grouped = results.groupBy { it.file }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (file, fileResults) ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = IdeColors.Surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null,
                                    tint = when (file.extension) {
                                        "kt" -> IdeColors.Kotlin
                                        "java" -> IdeColors.Warning
                                        "xml" -> IdeColors.Xml
                                        else -> IdeColors.TextSecondary
                                    },
                                    modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    file.name,
                                    color = IdeColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("${fileResults.size}", color = IdeColors.TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                    items(fileResults) { result ->
                        SearchResultRow(result, query, caseSensitive) {
                            onFileClick(result.file, result.lineNumber)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun SearchResultRow(
    result: SearchEngine.SearchResult,
    query: String,
    caseSensitive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 24.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${result.lineNumber}",
            color = IdeColors.TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        Spacer(Modifier.width(8.dp))
        val annotated = buildAnnotatedString {
            val line = result.lineContent.trimStart()
            val lowerLine = if (caseSensitive) line else line.lowercase()
            val lowerQuery = if (caseSensitive) query else query.lowercase()
            var idx = 0
            while (idx < line.length) {
                val found = lowerLine.indexOf(lowerQuery, idx)
                if (found == -1) {
                    withStyle(SpanStyle(color = IdeColors.TextPrimary)) { append(line.substring(idx)) }
                    break
                }
                withStyle(SpanStyle(color = IdeColors.TextPrimary)) { append(line.substring(idx, found)) }
                withStyle(SpanStyle(color = IdeColors.Background,
                    background = IdeColors.Primary)) {
                    append(line.substring(found, found + query.length))
                }
                idx = found + query.length
            }
        }
        Text(
            annotated,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .clickable { onToggle(!selected) }
            .padding(2.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        color = if (selected) IdeColors.Primary.copy(alpha = 0.2f) else IdeColors.Background,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) IdeColors.Primary else IdeColors.Border
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = if (selected) IdeColors.Primary else IdeColors.TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

