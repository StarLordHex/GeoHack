package org.geohack.game

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

data class Target(val alias: String, val discovered: String)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GeoHackApp() }
    }
}

@Composable
fun GeoHackApp() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.all { it }) context.startForegroundService(Intent(context, RadarService::class.java))
    }
    var savedRefresh by remember { mutableIntStateOf(0) }
    var page by remember { mutableStateOf("Radar") }
    val targets = remember { mutableStateListOf<Target>() }
    var reputation by remember { mutableIntStateOf(0) }
    var terminalOutput by remember { mutableStateOf("GEOHACK TRAINING TERMINAL\nType 'help' for commands.\n") }
    var command by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("GREEN") }
    val ink = if (selected == "AMBER") Color(0xFFFFBF00) else if (selected == "CYAN") Color.Cyan else Color(0xFF55FF88)
    val dark = Color(0xFF0A1110)
    MaterialTheme(colorScheme = darkColorScheme(primary = ink, background = dark, surface = Color(0xFF16211E))) {
        Column(Modifier.fillMaxSize().background(dark).padding(12.dp)) {
            Text("GEOHACK  /  v0.2", color = ink, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Radar", "Targets", "Terminal", "Ranks", "Profile").forEach { tab ->
                    TextButton(onClick = { page = tab }, contentPadding = PaddingValues(3.dp)) {
                        Text(tab, color = if (page == tab) ink else Color.LightGray)
                    }
                }
            }
            HorizontalDivider(color = ink)
            Spacer(Modifier.height(14.dp))
            when(page) {
                "Radar" -> Column {
                    Text("LOCAL ENCOUNTERS", color = ink)
                    Text("Experimental BLE discovery — both phones must opt in.", color = Color.Yellow)
                    Button(onClick = {
                        val required = mutableListOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
                        if (android.os.Build.VERSION.SDK_INT >= 33) required.add(Manifest.permission.POST_NOTIFICATIONS)
                        val missing = required.filter { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
                        if (missing.isEmpty()) context.startForegroundService(Intent(context, RadarService::class.java))
                        else permissionLauncher.launch(missing.toTypedArray())
                    }) { Text("Enable background radar") }
                    Button(onClick = { context.stopService(Intent(context, RadarService::class.java)) }) { Text("Stop radar") }
                    Button(onClick = { savedRefresh++ }) { Text("Refresh nearby tokens") }
                    val discovered = remember(savedRefresh) { context.getSharedPreferences("encounters", android.content.Context.MODE_PRIVATE).getStringSet("seen", emptySet()).orEmpty().toList() }
                    Text("BLE tokens seen: ${discovered.size}", color = Color.White)
                    discovered.take(12).forEach { Text(it.take(12), color = ink) }
                    Text("Tokens are temporary, not verified player identities. Server identity linking comes later.", color = Color.LightGray)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        val alias = "NODE_" + (targets.size + 101)
                        targets.add(Target(alias, "Demo encounter"))
                    }) { Text("Simulate nearby player") }
                    Text("Discovered: ${targets.size} players", color = Color.White)
                    Text("Real encounters will require proximity verification and mutual opt-in.", color = Color.LightGray)
                }
                "Targets" -> Column {
                    Text("DISCOVERED TARGETS", color = ink)
                    if (targets.isEmpty()) Text("No encounters yet. Try Radar demo.", color = Color.LightGray)
                    LazyColumn {
                        items(targets) { target ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(target.alias, color = ink)
                                    Text(target.discovered)
                                    Button(onClick = {
                                        page = "Terminal"
                                        terminalOutput += "\nTraining scenario loaded for ${target.alias}.\n"
                                    }) { Text("Open training scenario") }
                                }
                            }
                        }
                    }
                }
                "Terminal" -> Column {
                    Text("SANDBOXED COMMAND TUTORIAL", color = ink)
                    Text("This is a safe command interpreter, NOT a real shell.", color = Color.Yellow)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(8.dp)).padding(10.dp)) {
                        Text(terminalOutput, color = ink, fontFamily = FontFamily.Monospace)
                    }
                    Row {
                        BasicTextField(command, { command = it }, Modifier.weight(1f).background(Color(0xFF23332E)).padding(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = ink, fontFamily = FontFamily.Monospace),
                            singleLine = true)
                        Button(onClick = {
                            val response = when(command.trim().lowercase()) {
                                "help" -> "Commands: help, whoami, pwd, ls, cat intel.txt, mission, clear"
                                "whoami" -> "trainee"
                                "pwd" -> "/game/training"
                                "ls" -> "intel.txt  README"
                                "cat intel.txt" -> "Training clue: GEOHACK-101"
                                "mission" -> { reputation += 10; "Mission complete! +10 demo XP" }
                                "clear" -> { terminalOutput = ""; "" }
                                else -> "Unknown training command. Type help."
                            }
                            terminalOutput += "\n$ $command\n$response"
                            command = ""
                        }) { Text("RUN") }
                    }
                }
                "Ranks" -> Column {
                    Text("LEADERBOARD — LOCAL DEMO ONLY", color = ink)
                    Text("1. YOU — $reputation XP", color = Color.White)
                    Text("Online leaderboards will be server-validated.", color = Color.LightGray)
                }
                "Profile" -> Column {
                    Text("PLAYER PROFILE", color = ink)
                    Text("Alias: TRAINEE", color = Color.White)
                    Text("Reputation: $reputation XP", color = Color.White)
                    Text("Beginner protection: ACTIVE", color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("Terminal font colour", color = ink)
                    Row { listOf("GREEN", "AMBER", "CYAN").forEach { colour ->
                        TextButton(onClick = { selected = colour }) { Text(colour) }
                    } }
                    Text("Background images and accounts are planned for later versions.", color = Color.LightGray)
                }
            }
        }
    }
}
