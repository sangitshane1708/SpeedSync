package com.example.speedsync

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            toggleService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpeedSyncScreen(
                        onToggle = { checkPermissionsAndToggle() }
                    )
                }
            }
        }
    }

    private fun checkPermissionsAndToggle() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            perms.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun toggleService() {
        val intent = Intent(this, SpeedService::class.java)
        if (SpeedService.isRunning.value) {
            stopService(intent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}

@Composable
fun SpeedSyncScreen(onToggle: () -> Unit) {
    val speed by SpeedService.currentSpeed.collectAsState()
    val isRunning by SpeedService.isRunning.collectAsState()

    var minSpeed by remember { mutableFloatStateOf(SpeedService.minSpeedThreshold) }
    var maxSpeed by remember { mutableFloatStateOf(SpeedService.maxSpeedThreshold) }
    var minVol by remember { mutableFloatStateOf(SpeedService.minVolRatio * 100) }
    var maxVol by remember { mutableFloatStateOf(SpeedService.maxVolRatio * 100) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SpeedSync",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Current Speed", fontSize = 14.sp)
                    Text(
                        text = "${speed.roundToInt()} km/h",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Min Speed Threshold: ${minSpeed.roundToInt()} km/h", fontWeight = FontWeight.SemiBold)
            Slider(
                value = minSpeed,
                onValueChange = {
                    minSpeed = it
                    SpeedService.minSpeedThreshold = it
                },
                valueRange = 0f..50f
            )

            Text(text = "Max Speed Threshold: ${maxSpeed.roundToInt()} km/h", fontWeight = FontWeight.SemiBold)
            Slider(
                value = maxSpeed,
                onValueChange = {
                    maxSpeed = it
                    SpeedService.maxSpeedThreshold = it
                },
                valueRange = 30f..120f
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Min Volume: ${minVol.roundToInt()}%", fontWeight = FontWeight.SemiBold)
            Slider(
                value = minVol,
                onValueChange = {
                    minVol = it
                    SpeedService.minVolRatio = it / 100f
                },
                valueRange = 0f..50f
            )

            Text(text = "Max Volume: ${maxVol.roundToInt()}%", fontWeight = FontWeight.SemiBold)
            Slider(
                value = maxVol,
                onValueChange = {
                    maxVol = it
                    SpeedService.maxVolRatio = it / 100f
                },
                valueRange = 50f..100f
            )
        }

        Button(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isRunning) "STOP SPEEDSNC" else "START SPEEDSNC",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}