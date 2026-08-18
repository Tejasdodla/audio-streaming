package com.fifthsense.audiostream.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fifthsense.audiostream.components.BleDeviceCard
import com.fifthsense.audiostream.components.GlassCard
import com.fifthsense.audiostream.components.StatsPanel
import com.fifthsense.audiostream.components.VolumeSlider
import com.fifthsense.audiostream.model.BleAudioDevice
import com.fifthsense.audiostream.model.BleConnectionState
import com.fifthsense.audiostream.model.StreamStats
import com.fifthsense.audiostream.theme.CyanNeon
import com.fifthsense.audiostream.theme.DarkBackground
import com.fifthsense.audiostream.theme.DarkSurface
import com.fifthsense.audiostream.theme.DarkSurfaceVariant
import com.fifthsense.audiostream.theme.GreenSuccess
import com.fifthsense.audiostream.theme.TextMuted
import com.fifthsense.audiostream.theme.TextPrimary
import com.fifthsense.audiostream.theme.TextSecondary
import com.fifthsense.audiostream.theme.VioletElectric

@Composable
fun DashboardScreen(
    scannedDevices: List<BleAudioDevice>,
    connectedDevice: BleAudioDevice?,
    isScanning: Boolean,
    stats: StreamStats,
    volume: Int,
    onScanClicked: () -> Unit,
    onConnectDevice: (BleAudioDevice) -> Unit,
    onDisconnectDevice: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onPlayFlashAsset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Scan Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isScanning) CyanNeon.copy(alpha = 0.6f) else Color(0x2200F5D4)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isScanning) CyanNeon.copy(alpha = 0.2f) else Color(0x15FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = CyanNeon,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = "Bluetooth",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = if (isScanning) "Scanning..." else "Bluetooth Sink",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isScanning) "Searching for nRF54L15" else "Ready to connect",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onScanClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) Color(0x3300F5D4) else CyanNeon,
                            contentColor = if (isScanning) CyanNeon else DarkBackground
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isScanning) "Stop" else "Scan")
                    }
                }
            }
        }

        // Active Connected Device
        if (connectedDevice != null && connectedDevice.connectionState == BleConnectionState.CONNECTED) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = GreenSuccess.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GreenSuccess.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothConnected,
                                        contentDescription = "Connected",
                                        tint = GreenSuccess,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = connectedDevice.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${connectedDevice.address} • Connected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GreenSuccess
                                    )
                                }
                            }

                            Button(
                                onClick = onDisconnectDevice,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x22FF5470),
                                    contentColor = com.fifthsense.audiostream.theme.CoralAccent
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Disconnect")
                            }
                        }

                        // Hardware Diagnostics Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onPlayFlashAsset(0) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanNeon.copy(alpha = 0.15f),
                                    contentColor = CyanNeon
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Boot Tune", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = { onPlayFlashAsset(1) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VioletElectric.copy(alpha = 0.2f),
                                    contentColor = Color(0xFFC084FC)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Button Melody", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Scanned Device List
        if (scannedDevices.isNotEmpty()) {
            item {
                Text(
                    text = "AVAILABLE DEVICES",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanNeon
                )
            }

            items(scannedDevices) { device ->
                BleDeviceCard(
                    device = device,
                    onConnectClicked = { onConnectDevice(device) },
                    onDisconnectClicked = onDisconnectDevice,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Volume Control
        item {
            VolumeSlider(
                volume = volume,
                onVolumeChanged = onVolumeChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Metrics Stats Panel
        item {
            StatsPanel(
                stats = stats,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
