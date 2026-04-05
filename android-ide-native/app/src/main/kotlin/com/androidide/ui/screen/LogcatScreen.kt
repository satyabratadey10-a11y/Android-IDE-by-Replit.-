package com.androidide.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidide.jni.NativeBridge
import com.androidide.ui.theme.IdeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * LogcatScreen — Real logcat output via `adb logcat`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatScreen() {
    val scope = rememberCoroutineScope()
    val logLines = remember { mutableStateListOf<LogLine>() }
    val listState = rememberLazyListState()
    var isRunning by remember { mutableStateOf(false) }
    var filterTag by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("V") }
    val levels = listOf("V", "D", "I", "W", "E", "F")

    // Auto-scroll
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) listState.animateScrollToItem(logLines.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeColors.Background)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = null,
                        tint = IdeColors.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Logcat", color = IdeColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    if (isRunning) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = IdeColors.Primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = IdeColors.Surface),
            actions = {
                if (!isRunning) {
                    IconButton(onClick = {
                        isRunning = true
                        logLines.clear()
                        scope.launch(Dispatchers.IO) {
                            val levelArg = "*:$selectedLevel"
                            val args = buildList {
                                add("logcat")
                                add("-v"); add("time")
                                if (filterTag.isNotBlank()) {
                                    add("$filterTag:$selectedLevel"); add("*:S")
                                } else {
                                    add(levelArg)
                                }
                            }.toTypedArray()
                            val result = NativeBridge.adbCommand(args)
                            result[0].lines().forEach { line ->
                                if (line.isNotBlank()) logLines.add(parseLogLine(line))
                            }
                        }
                    }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start",
                            tint = IdeColors.Success)
                    }
                } else {
                    IconButton(onClick = { isRunning = false }) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop",
                            tint = IdeColors.Error)
                    }
                }
                IconButton(onClick = { logLines.clear() }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear",
                        tint = IdeColors.TextSecondary)
                }
            }
        )

        // Filter bar
        Surface(color = IdeColors.Surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filterTag,
                    onValueChange = { filterTag = it },
                    placeholder = { Text("Filter tag", fontSize = 11.sp, color = IdeColors.TextSecondary) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = IdeColors.TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IdeColors.Primary,
                        unfocusedBorderColor = IdeColors.Border
                    ),
                    singleLine = true
                )
                levels.forEach { level ->
                    val selected = selectedLevel == level
                    TextButton(
                        onClick = { selectedLevel = level },
                        modifier = Modifier.size(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (selected) levelColor(level).copy(alpha = 0.2f)
                            else IdeColors.Background
                        )
                    ) {
                        Text(level, color = levelColor(level), fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        Divider(color = IdeColors.Border)

        // Log output
        val filtered = if (filterTag.isBlank()) logLines
        else logLines.filter { it.tag.contains(filterTag, ignoreCase = true) }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (isRunning) "Waiting for logcat output..." else "Press ▶ to start logcat",
                    color = IdeColors.TextSecondary
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
            ) {
                items(filtered) { line ->
                    LogcatRow(line)
                }
            }
        }
    }
}

@Composable
private fun LogcatRow(line: LogLine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            line.level,
            color = levelColor(line.level),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(14.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            line.time,
            color = IdeColors.TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(60.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            line.tag,
            color = IdeColors.Primary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(4.dp))
        Text(
            line.message,
            color = levelColor(line.level),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}

data class LogLine(
    val time: String,
    val level: String,
    val tag: String,
    val pid: String,
    val message: String
)

private fun parseLogLine(raw: String): LogLine {
    // Format: MM-DD HH:MM:SS.mmm PID TID level/tag: message
    return try {
        val timeEnd = raw.indexOf(' ', 10)
        val time = raw.substring(0, timeEnd).takeLast(12)
        val rest = raw.substring(timeEnd).trim()
        val parts = rest.split("\\s+".toRegex(), limit = 4)
        val levelTag = if (parts.size > 2) parts[2] else "V/?"
        val level = levelTag.substringBefore("/").take(1)
        val tag = levelTag.substringAfter("/").substringBefore(":")
        val message = if (parts.size > 3) parts[3].trimStart(':').trim() else rest
        LogLine(time, level, tag, parts.getOrElse(0) { "?" }, message)
    } catch (e: Exception) {
        LogLine("", "V", "?", "?", raw)
    }
}

@Composable
private fun levelColor(level: String) = when (level.uppercase()) {
    "E", "F" -> IdeColors.Error
    "W" -> IdeColors.Warning
    "I" -> IdeColors.Success
    "D" -> IdeColors.Primary
    else -> IdeColors.TextSecondary
}
