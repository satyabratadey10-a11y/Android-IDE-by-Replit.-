package com.androidide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidide.project.AndroidProject
import com.androidide.ui.screen.*
import com.androidide.ui.theme.IdeColors
import com.androidide.ui.viewmodel.MainViewModel

/**
 * MainActivity.kt
 *
 * Root activity for Android IDE Native.
 * Sets up Jetpack Compose UI with a bottom navigation bar
 * that hosts all IDE screens: Editor, Build, Terminal, Debugger, Logcat, Devices.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AndroidIDEApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidIDEApp(viewModel: MainViewModel) {
    val currentProject by viewModel.currentProject.collectAsState()

    // Top-level: if no project, show Home (project manager)
    // Otherwise, show IDE with nav
    if (currentProject == null) {
        HomeScreen(
            viewModel = viewModel,
            onOpenProject = { project -> viewModel.openProject(project) }
        )
    } else {
        IDEWorkspace(viewModel, currentProject!!)
    }
}

enum class IDETab(val label: String, val icon: ImageVector) {
    EDITOR("Editor",    Icons.Default.Code),
    SEARCH("Search",    Icons.Default.Search),
    BUILD("Build",      Icons.Default.Build),
    TERMINAL("Terminal",Icons.Default.Terminal),
    DEBUGGER("Debug",   Icons.Default.BugReport),
    LOGCAT("Logcat",    Icons.Default.List),
    DEVICES("Devices",  Icons.Default.PhoneAndroid)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDEWorkspace(
    viewModel: MainViewModel,
    project: AndroidProject
) {
    var selectedTab by remember { mutableStateOf(IDETab.EDITOR) }
    val isBuilding by viewModel.isBuilding.collectAsState()

    Scaffold(
        containerColor = IdeColors.Background,
        bottomBar = {
            Surface(color = IdeColors.Surface, tonalElevation = 0.dp) {
                NavigationBar(
                    containerColor = IdeColors.Surface,
                    contentColor = IdeColors.Primary,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(60.dp)
                ) {
                    IDETab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        val badgeCount = if (tab == IDETab.BUILD && isBuilding) 1 else 0

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                if (badgeCount > 0) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = IdeColors.Primary)
                                    }) {
                                        Icon(tab.icon, contentDescription = tab.label,
                                            modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.label,
                                        modifier = Modifier.size(20.dp))
                                }
                            },
                            label = { Text(tab.label, fontSize = 9.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IdeColors.Primary,
                                selectedTextColor = IdeColors.Primary,
                                unselectedIconColor = IdeColors.TextSecondary,
                                unselectedTextColor = IdeColors.TextSecondary,
                                indicatorColor = Color(0xFF1A2F1A)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            when (selectedTab) {
                IDETab.EDITOR -> EditorScreen(
                    viewModel = viewModel,
                    project = project,
                    onBuildClick = { selectedTab = IDETab.BUILD },
                    onTerminalClick = { selectedTab = IDETab.TERMINAL }
                )
                IDETab.SEARCH -> SearchScreen(
                    toolchain = viewModel.toolchain,
                    project = project,
                    onFileClick = { file, _ ->
                        viewModel.openFile(file)
                        selectedTab = IDETab.EDITOR
                    }
                )
                IDETab.BUILD -> BuildScreen(
                    viewModel = viewModel,
                    project = project
                )
                IDETab.TERMINAL -> TerminalScreen(viewModel = viewModel)
                IDETab.DEBUGGER -> DebuggerScreen(viewModel = viewModel, project = project)
                IDETab.LOGCAT -> LogcatScreen()
                IDETab.DEVICES -> DeviceManagerScreen()
            }
        }
    }
}

@Composable
private fun darkColorScheme(): ColorScheme = darkColorScheme(
    primary = IdeColors.Primary,
    background = IdeColors.Background,
    surface = IdeColors.Surface,
    onBackground = IdeColors.TextPrimary,
    onSurface = IdeColors.TextPrimary
)
