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
import com.androidide.engine.BuildPipelineEngine
import com.androidide.project.AndroidProject
import com.androidide.ui.theme.IdeColors
import com.androidide.ui.viewmodel.MainViewModel

/**
 * BuildScreen — Displays the real-time build pipeline output.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen(
    viewModel: MainViewModel,
    project: AndroidProject
) {
    val buildLog by viewModel.buildLog.collectAsState()
    val isBuilding by viewModel.isBuilding.collectAsState()
    val lastApkPath by viewModel.lastApkPath.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom as log grows
    LaunchedEffect(buildLog.size) {
        if (buildLog.isNotEmpty()) {
            listState.animateScrollToItem(buildLog.size - 1)
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
                    Icon(Icons.Default.Build, contentDescription = null,
                        tint = IdeColors.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Build Console", color = IdeColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text(project.name, color = IdeColors.TextSecondary, fontSize = 12.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = IdeColors.Surface),
            actions = {
                if (isBuilding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).padding(end = 4.dp),
                        color = IdeColors.Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = { viewModel.clearBuildLog() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear",
                            tint = IdeColors.TextSecondary)
                    }
                    lastApkPath?.let {
                        IconButton(onClick = { viewModel.installApk() }) {
                            Icon(Icons.Default.InstallMobile, contentDescription = "Install APK",
                                tint = IdeColors.Success)
                        }
                    }
                    Button(
                        onClick = { viewModel.buildDebug() },
                        colors = ButtonDefaults.buttonColors(containerColor = IdeColors.Primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Build Debug", fontSize = 12.sp)
                    }
                }
            }
        )

        // Build stats bar
        if (buildLog.isNotEmpty()) {
            BuildStatsBar(buildLog, lastApkPath)
        }

        // Log output
        if (buildLog.isEmpty() && !isBuilding) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BuildCircle, contentDescription = null,
                        tint = IdeColors.TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Ready to build", color = IdeColors.TextSecondary, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Press \"Build Debug\" to start a real build",
                        color = IdeColors.TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(buildLog) { event ->
                    BuildEventRow(event)
                }
            }
        }
    }
}

@Composable
private fun BuildStatsBar(
    log: List<BuildPipelineEngine.BuildEvent>,
    apkPath: String?
) {
    val errors = log.count { it.isError }
    val warnings = log.count { it.message.contains("warning", ignoreCase = true) && !it.isError }
    val buildSuccess = log.any { it.type == BuildPipelineEngine.BuildEvent.Type.BUILD_SUCCESS }
    val buildFailed  = log.any { it.type == BuildPipelineEngine.BuildEvent.Type.BUILD_FAILED }

    Surface(color = IdeColors.Surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                buildSuccess -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = IdeColors.Success, modifier = Modifier.size(16.dp))
                    Text("BUILD SUCCESSFUL", color = IdeColors.Success,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                buildFailed -> {
                    Icon(Icons.Default.Error, contentDescription = null,
                        tint = IdeColors.Error, modifier = Modifier.size(16.dp))
                    Text("BUILD FAILED", color = IdeColors.Error,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (errors > 0) Text("$errors errors", color = IdeColors.Error, fontSize = 11.sp)
            if (warnings > 0) Text("$warnings warnings", color = IdeColors.Warning, fontSize = 11.sp)
            apkPath?.let {
                val name = it.substringAfterLast("/")
                Text(name, color = IdeColors.TextSecondary, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }
    }
    Divider(color = IdeColors.Border)
}

@Composable
private fun BuildEventRow(event: BuildPipelineEngine.BuildEvent) {
    val (color, prefix) = when (event.type) {
        BuildPipelineEngine.BuildEvent.Type.STEP_START   -> IdeColors.TextSecondary to "›"
        BuildPipelineEngine.BuildEvent.Type.STEP_SUCCESS -> IdeColors.Success to "✓"
        BuildPipelineEngine.BuildEvent.Type.STEP_FAILED  -> IdeColors.Error   to "✗"
        BuildPipelineEngine.BuildEvent.Type.BUILD_SUCCESS-> IdeColors.Success to "█"
        BuildPipelineEngine.BuildEvent.Type.BUILD_FAILED -> IdeColors.Error   to "█"
        BuildPipelineEngine.BuildEvent.Type.LOG          ->
            if (event.isError) IdeColors.Error else IdeColors.TextPrimary to " "
    }

    val bg = when (event.type) {
        BuildPipelineEngine.BuildEvent.Type.BUILD_SUCCESS -> IdeColors.SuccessBg
        BuildPipelineEngine.BuildEvent.Type.BUILD_FAILED  -> IdeColors.ErrorBg
        BuildPipelineEngine.BuildEvent.Type.STEP_START    -> IdeColors.Surface
        else -> IdeColors.Background
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            prefix,
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Column {
            if (event.tool.isNotBlank()) {
                Text(
                    "[${event.tool}]",
                    color = IdeColors.Primary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                event.message,
                color = color,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }
    }
}
