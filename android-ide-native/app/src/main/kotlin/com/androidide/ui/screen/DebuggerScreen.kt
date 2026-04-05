package com.androidide.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.androidide.project.AndroidProject
import com.androidide.ui.theme.IdeColors
import com.androidide.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * DebuggerScreen — APK/DEX inspector + adb-based debugger controls.
 * Runs real dexdump, objdump, and adb commands via the native layer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebuggerScreen(
    viewModel: MainViewModel,
    project: AndroidProject
) {
    val scope = rememberCoroutineScope()
    var dexDumpOutput by remember { mutableStateOf("") }
    var adbOutput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("DEX Inspector", "ADB", "APK Info")
    val isBuilding by viewModel.isBuilding.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeColors.Background)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null,
                        tint = IdeColors.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Debugger", color = IdeColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = IdeColors.Surface)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = IdeColors.Surface,
            contentColor = IdeColors.Primary
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, fontSize = 12.sp, color = if (selectedTab == idx) IdeColors.Primary else IdeColors.TextSecondary) }
                )
            }
        }

        when (selectedTab) {
            0 -> DexInspectorTab(
                project = project,
                output = dexDumpOutput,
                onDump = {
                    scope.launch(Dispatchers.IO) {
                        val dexFile = java.io.File(project.buildDir, "dex/classes.dex")
                        if (dexFile.exists()) {
                            val result = NativeBridge.dexdump(dexFile.absolutePath)
                            dexDumpOutput = if (result[2] == "0") result[0] else "Error: ${result[1]}"
                        } else {
                            dexDumpOutput = "No DEX file found. Build the project first."
                        }
                    }
                }
            )
            1 -> AdbTab(
                output = adbOutput,
                onCommand = { cmd ->
                    scope.launch(Dispatchers.IO) {
                        val parts = cmd.trim().split("\\s+".toRegex()).drop(1).toTypedArray()
                        val result = NativeBridge.adbCommand(parts)
                        adbOutput += "\n$ adb ${parts.joinToString(" ")}\n"
                        adbOutput += if (result[0].isNotBlank()) result[0] else result[1]
                    }
                }
            )
            2 -> ApkInfoTab(project = project)
        }
    }
}

@Composable
private fun DexInspectorTab(
    project: AndroidProject,
    output: String,
    onDump: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("classes.dex inspector", color = IdeColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDump,
                colors = ButtonDefaults.buttonColors(containerColor = IdeColors.Primary)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Run dexdump", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = IdeColors.Surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                item {
                    Text(
                        output.ifEmpty { "Press 'Run dexdump' to inspect DEX bytecode.\nBuild the project first." },
                        color = if (output.startsWith("Error")) IdeColors.Error else IdeColors.TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdbTab(output: String, onCommand: (String) -> Unit) {
    var input by remember { mutableStateOf("adb ") }
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("ADB Command Runner", color = IdeColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        val quickCommands = listOf(
            "adb devices", "adb shell ps", "adb shell getprop ro.build.version.sdk",
            "adb logcat -d", "adb shell am start -n com.example/.MainActivity"
        )
        Text("Quick commands:", color = IdeColors.TextSecondary, fontSize = 11.sp)
        quickCommands.forEach { cmd ->
            TextButton(onClick = { onCommand(cmd) }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) {
                Text(cmd, color = IdeColors.Primary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = IdeColors.TextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IdeColors.Primary,
                    unfocusedBorderColor = IdeColors.Border
                ),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onCommand(input) },
                colors = ButtonDefaults.buttonColors(containerColor = IdeColors.Primary)) {
                Text("Run")
            }
        }
        Spacer(Modifier.height(8.dp))
        Surface(modifier = Modifier.fillMaxSize(), color = IdeColors.Surface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                item {
                    Text(
                        output.ifEmpty { "ADB output will appear here." },
                        color = IdeColors.TextPrimary,
                        fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ApkInfoTab(project: AndroidProject) {
    val apkFile = java.io.File(project.buildDir, "app-debug.apk")
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("APK Information", color = IdeColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (!apkFile.exists()) {
            Text("No APK found. Build the project first.", color = IdeColors.TextSecondary)
        } else {
            val entries = com.androidide.engine.ApkPackager.listApkContents(apkFile)
            val sizeMb = apkFile.length().toFloat() / (1024 * 1024)
            InfoRow("APK Path", apkFile.absolutePath)
            InfoRow("APK Size", "${"%.2f".format(sizeMb)} MB")
            InfoRow("Entry Count", "${entries.size}")
            Spacer(Modifier.height(8.dp))
            Text("Contents:", color = IdeColors.TextSecondary, fontSize = 12.sp)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries) { entry ->
                    Text("  $entry", color = IdeColors.TextPrimary,
                        fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label:", color = IdeColors.TextSecondary, fontSize = 12.sp,
            modifier = Modifier.width(100.dp))
        Text(value, color = IdeColors.TextPrimary, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace)
    }
}
