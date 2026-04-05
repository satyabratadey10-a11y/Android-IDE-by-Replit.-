package com.androidide.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidide.project.AndroidProject
import com.androidide.ui.theme.IdeColors
import com.androidide.ui.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    project: AndroidProject,
    onBuildClick: () -> Unit,
    onTerminalClick: () -> Unit
) {
    val editorContent by viewModel.editorContent.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()

    var currentFile by remember { mutableStateOf<File?>(null) }
    var showFileTree by remember { mutableStateOf(true) }

    // Open first source file by default
    LaunchedEffect(project) {
        val files = project.sourceFiles()
        if (files.isNotEmpty() && currentFile == null) {
            currentFile = files.first()
            viewModel.openFile(files.first())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeColors.Background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        project.name,
                        color = IdeColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        currentFile?.name?.let { if (isDirty) "● $it" else it } ?: "No file",
                        color = IdeColors.TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = IdeColors.Surface),
            actions = {
                IconButton(onClick = {
                    currentFile?.let { viewModel.saveCurrentFile(it) }
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Save",
                        tint = if (isDirty) IdeColors.Warning else IdeColors.TextSecondary)
                }
                IconButton(onClick = onTerminalClick) {
                    Icon(Icons.Default.Terminal, contentDescription = "Terminal",
                        tint = IdeColors.TextSecondary)
                }
                IconButton(onClick = onBuildClick) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Build",
                        tint = IdeColors.Primary)
                }
            }
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // File tree (collapsible)
            if (showFileTree) {
                FileTree(
                    project = project,
                    currentFile = currentFile,
                    onFileClick = { file ->
                        if (isDirty) {
                            currentFile?.let { viewModel.saveCurrentFile(it) }
                        }
                        currentFile = file
                        viewModel.openFile(file)
                    },
                    onToggle = { showFileTree = false },
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                )
            } else {
                IconButton(
                    onClick = { showFileTree = true },
                    modifier = Modifier
                        .width(32.dp)
                        .fillMaxHeight()
                        .background(IdeColors.Surface)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Show files",
                        tint = IdeColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            // Editor area
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = IdeColors.Border)

            Box(modifier = Modifier.fillMaxSize().background(IdeColors.Background)) {
                val scrollState = rememberScrollState()
                Row(modifier = Modifier.fillMaxSize()) {
                    // Line numbers
                    val lines = editorContent.split("\n")
                    LazyColumn(
                        modifier = Modifier
                            .width(40.dp)
                            .fillMaxHeight()
                            .background(IdeColors.Surface)
                            .padding(end = 4.dp)
                    ) {
                        itemsIndexed(lines) { idx, _ ->
                            Text(
                                "${idx + 1}",
                                color = IdeColors.TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }

                    // Code area
                    val extension = currentFile?.extension ?: ""
                    BasicTextField(
                        value = editorContent,
                        onValueChange = viewModel::onEditorContentChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(scrollState)
                            .padding(8.dp),
                        textStyle = TextStyle(
                            color = IdeColors.TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(IdeColors.Primary),
                        decorationBox = { innerTextField ->
                            if (editorContent.isEmpty()) {
                                Text(
                                    "Open a file from the tree →",
                                    color = IdeColors.TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileTree(
    project: AndroidProject,
    currentFile: File?,
    onFileClick: (File) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(IdeColors.Surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "FILES",
                color = IdeColors.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onToggle, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Hide",
                    tint = IdeColors.TextSecondary, modifier = Modifier.size(14.dp))
            }
        }
        Divider(color = IdeColors.Border)

        val allFiles = project.sourceFiles() + project.resourceFiles()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(allFiles.size) { idx ->
                val file = allFiles[idx]
                val isSelected = file == currentFile
                val relPath = file.relativeTo(project.srcDir).path

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) IdeColors.SelectionBg else IdeColors.Surface)
                        .clickable { onFileClick(file) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (file.extension) {
                            "kt" -> Icons.Default.Code
                            "java" -> Icons.Default.Coffee
                            "xml" -> Icons.Default.Code
                            "json" -> Icons.Default.DataObject
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = when (file.extension) {
                            "kt" -> IdeColors.Kotlin
                            "java" -> IdeColors.Warning
                            "xml" -> IdeColors.Xml
                            else -> IdeColors.TextSecondary
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        file.name,
                        color = if (isSelected) IdeColors.Primary else IdeColors.TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

