package com.androidide.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidide.project.AndroidProject
import com.androidide.ui.theme.IdeColors
import com.androidide.ui.viewmodel.MainViewModel

/**
 * HomeScreen — Project manager / landing page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenProject: (AndroidProject) -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val toolchainReady by viewModel.toolchainReady.collectAsState()
    val setupLog by viewModel.setupLog.collectAsState()

    var showNewProjectDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeColors.Background)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = IdeColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Android IDE",
                        color = IdeColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = IdeColors.Surface
            ),
            actions = {
                IconButton(onClick = { showNewProjectDialog = true },
                    enabled = toolchainReady) {
                    Icon(Icons.Default.Add, contentDescription = "New Project",
                        tint = IdeColors.Primary)
                }
            }
        )

        // Toolchain status banner
        if (!toolchainReady) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = IdeColors.Surface
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = IdeColors.Primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        setupLog.ifEmpty { "Setting up toolchain..." },
                        color = IdeColors.TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Divider(color = IdeColors.Border)
        }

        // Project list
        if (projects.isEmpty() && toolchainReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = IdeColors.TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No projects yet",
                        color = IdeColors.TextSecondary,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showNewProjectDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = IdeColors.Primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("New Project")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onOpenProject(project) },
                        onDelete = { viewModel.deleteProject(project) }
                    )
                }
            }
        }
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { name, pkg, minSdk, useKotlin, useNdk ->
                viewModel.createProject(name, pkg, minSdk, useKotlin, useNdk)
                showNewProjectDialog = false
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: AndroidProject,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = IdeColors.Surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Android,
                contentDescription = null,
                tint = IdeColors.Primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    color = IdeColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    project.packageName,
                    color = IdeColors.TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("minSdk ${project.minSdk}")
                    if (project.useKotlin) Chip("Kotlin")
                    if (project.useNdk) Chip("NDK")
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = IdeColors.Error)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete project?", color = IdeColors.TextPrimary) },
            text = { Text("\"${project.name}\" will be permanently deleted.",
                color = IdeColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = IdeColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = IdeColors.TextSecondary)
                }
            },
            containerColor = IdeColors.Surface
        )
    }
}

@Composable
private fun Chip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = IdeColors.Background
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = IdeColors.TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("MyApp") }
    var packageName by remember { mutableStateOf("com.example.myapp") }
    var minSdk by remember { mutableStateOf("24") }
    var useKotlin by remember { mutableStateOf(true) }
    var useNdk by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Android Project", color = IdeColors.TextPrimary) },
        containerColor = IdeColors.Surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("App Name", color = IdeColors.TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = IdeColors.TextPrimary,
                        unfocusedTextColor = IdeColors.TextPrimary,
                        focusedBorderColor = IdeColors.Primary,
                        unfocusedBorderColor = IdeColors.Border
                    )
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name", color = IdeColors.TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = IdeColors.TextPrimary,
                        unfocusedTextColor = IdeColors.TextPrimary,
                        focusedBorderColor = IdeColors.Primary,
                        unfocusedBorderColor = IdeColors.Border
                    )
                )
                OutlinedTextField(
                    value = minSdk,
                    onValueChange = { minSdk = it },
                    label = { Text("Min SDK", color = IdeColors.TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = IdeColors.TextPrimary,
                        unfocusedTextColor = IdeColors.TextPrimary,
                        focusedBorderColor = IdeColors.Primary,
                        unfocusedBorderColor = IdeColors.Border
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = useKotlin, onCheckedChange = { useKotlin = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = IdeColors.Primary))
                    Spacer(Modifier.width(8.dp))
                    Text("Use Kotlin", color = IdeColors.TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = useNdk, onCheckedChange = { useNdk = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = IdeColors.Primary))
                    Spacer(Modifier.width(8.dp))
                    Text("Include NDK (C++)", color = IdeColors.TextPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && packageName.isNotBlank()) {
                        onCreate(name, packageName, minSdk.toIntOrNull() ?: 24, useKotlin, useNdk)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IdeColors.Primary)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = IdeColors.TextSecondary)
            }
        }
    )
}
