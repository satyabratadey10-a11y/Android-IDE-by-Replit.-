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
import com.androidide.jni.NativeBridge
import com.androidide.ui.theme.IdeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * DeviceManagerScreen — Lists connected devices via real `adb devices`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagerScreen() {
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<AdbDevice>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<AdbDevice?>(null) }
    var deviceProps by remember { mutableStateOf("") }

    fun refreshDevices() {
        isRefreshing = true
        scope.launch(Dispatchers.IO) {
            val result = NativeBridge.adbCommand(arrayOf("devices", "-l"))
            devices = parseAdbDevices(result[0])
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) { refreshDevices() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeColors.Background)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null,
                        tint = IdeColors.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Device Manager", color = IdeColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = IdeColors.Surface),
            actions = {
                IconButton(onClick = { refreshDevices() }, enabled = !isRefreshing) {
                    if (isRefreshing)
                        CircularProgressIndicator(modifier = Modifier.size(20.dp),
                            color = IdeColors.Primary, strokeWidth = 2.dp)
                    else
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh",
                            tint = IdeColors.Primary)
                }
            }
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Device list panel
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(IdeColors.Surface)
            ) {
                Text(
                    "Connected Devices (${devices.size})",
                    color = IdeColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
                Divider(color = IdeColors.Border)

                if (devices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DevicesOther, contentDescription = null,
                                tint = IdeColors.TextSecondary, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No devices connected", color = IdeColors.TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Connect a device or start an emulator",
                                color = IdeColors.TextSecondary, fontSize = 10.sp)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(devices) { device ->
                            DeviceListItem(
                                device = device,
                                isSelected = device == selectedDevice,
                                onClick = {
                                    selectedDevice = device
                                    scope.launch(Dispatchers.IO) {
                                        val props = NativeBridge.adbCommand(
                                            arrayOf("-s", device.serial, "shell", "getprop"))
                                        deviceProps = props[0]
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = IdeColors.Border)

            // Device details panel
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val device = selectedDevice
                if (device == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a device", color = IdeColors.TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    Text(device.model, color = IdeColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(device.serial, color = IdeColors.TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))

                    // Quick actions
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickAction("Reboot") {
                            scope.launch(Dispatchers.IO) {
                                NativeBridge.adbCommand(arrayOf("-s", device.serial, "reboot"))
                            }
                        }
                        QuickAction("Shell") {
                            scope.launch(Dispatchers.IO) {
                                val r = NativeBridge.adbCommand(arrayOf("-s", device.serial, "shell", "uname", "-a"))
                                deviceProps = r[0]
                            }
                        }
                        QuickAction("Screenshot") {
                            scope.launch(Dispatchers.IO) {
                                NativeBridge.adbCommand(arrayOf("-s", device.serial, "shell", "screencap", "/sdcard/screen.png"))
                                NativeBridge.adbCommand(arrayOf("-s", device.serial, "pull", "/sdcard/screen.png"))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("System Properties", color = IdeColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = IdeColors.Surface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val props = deviceProps
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            item {
                                Text(
                                    props.ifEmpty { "Loading properties..." },
                                    color = IdeColors.TextPrimary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: AdbDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) IdeColors.SelectionBg else IdeColors.Surface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (device.type == "emulator") Icons.Default.Computer else Icons.Default.PhoneAndroid,
            contentDescription = null,
            tint = if (device.state == "device") IdeColors.Success else IdeColors.Error,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(device.model, color = IdeColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(device.serial, color = IdeColors.TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(device.state, color = if (device.state == "device") IdeColors.Success else IdeColors.Error,
                fontSize = 10.sp)
        }
    }
    Divider(color = IdeColors.Border)
}

@Composable
private fun QuickAction(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = IdeColors.Primary),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(IdeColors.Border)
        )
    ) {
        Text(label, fontSize = 11.sp)
    }
}

data class AdbDevice(
    val serial: String,
    val state: String,
    val model: String,
    val type: String
)

private fun parseAdbDevices(output: String): List<AdbDevice> {
    return output.lines()
        .drop(1) // skip "List of devices attached"
        .filter { it.isNotBlank() && !it.startsWith("*") }
        .mapNotNull { line ->
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 2) return@mapNotNull null
            val serial = parts[0]
            val state = parts[1]
            val model = parts.find { it.startsWith("model:") }
                ?.substringAfter("model:") ?: serial
            val type = if (serial.startsWith("emulator")) "emulator" else "device"
            AdbDevice(serial, state, model, type)
        }
}

