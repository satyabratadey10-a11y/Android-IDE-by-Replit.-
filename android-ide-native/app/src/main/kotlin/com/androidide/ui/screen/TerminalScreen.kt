package com.androidide.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidide.engine.TerminalEngine
import com.androidide.ui.theme.IdeColors
import com.androidide.ui.viewmodel.MainViewModel

/**
 * TerminalScreen — Real interactive terminal backed by native process execution.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(viewModel: MainViewModel) {
    val output by viewModel.terminalOutput.collectAsState()
    val toolchainReady by viewModel.toolchainReady.collectAsState()
    var input by remember { mutableStateOf("") }
    val historyIndex = remember { mutableStateOf(-1) }
    val commandHistory = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    // Auto-scroll
    LaunchedEffect(output.size) {
        if (output.isNotEmpty()) listState.animateScrollToItem(output.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E14))
    ) {
        // Top bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null,
                        tint = IdeColors.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Terminal", color = IdeColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1117)),
            actions = {
                IconButton(onClick = { viewModel.clearTerminal() }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear",
                        tint = IdeColors.TextSecondary)
                }
            }
        )

        // Terminal output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // Welcome banner
            item {
                Text(
                    "Android IDE Terminal — type 'help' for commands\n",
                    color = IdeColors.Primary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            items(output) { line ->
                TerminalLine(line)
            }
        }

        Divider(color = Color(0xFF21262D))

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1117))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$ ", color = IdeColors.Primary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            TextField(
                value = input,
                onValueChange = {
                    input = it
                    historyIndex.value = -1
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = IdeColors.TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = IdeColors.Primary
                ),
                singleLine = true,
                placeholder = {
                    if (!toolchainReady)
                        Text("Setting up toolchain...", color = IdeColors.TextSecondary,
                            fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (input.isNotBlank() && toolchainReady) {
                            commandHistory.add(0, input)
                            viewModel.executeCommand(input)
                            input = ""
                        }
                    }
                ),
                enabled = toolchainReady
            )
            IconButton(
                onClick = {
                    if (input.isNotBlank() && toolchainReady) {
                        commandHistory.add(0, input)
                        viewModel.executeCommand(input)
                        input = ""
                    }
                },
                enabled = toolchainReady && input.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Run",
                    tint = if (toolchainReady && input.isNotBlank()) IdeColors.Primary
                    else IdeColors.TextSecondary,
                    modifier = Modifier.size(18.dp))
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun TerminalLine(output: TerminalEngine.TerminalOutput) {
    if (output.text.isBlank()) return
    val color = when {
        output.text.startsWith("$ ") -> IdeColors.Primary
        output.isError -> IdeColors.Error
        output.text.startsWith("[") -> IdeColors.Warning
        output.text.contains("error", ignoreCase = true) -> IdeColors.Error
        output.text.contains("warning", ignoreCase = true) -> IdeColors.Warning
        output.text.contains("success", ignoreCase = true) -> IdeColors.Success
        else -> IdeColors.TextPrimary
    }
    Text(
        output.text,
        color = color,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 18.sp,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}
