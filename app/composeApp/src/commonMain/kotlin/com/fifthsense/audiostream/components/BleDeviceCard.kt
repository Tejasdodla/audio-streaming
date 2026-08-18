package com.fifthsense.audiostream.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
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
import com.fifthsense.audiostream.model.BleAudioDevice
import com.fifthsense.audiostream.model.BleConnectionState
import com.fifthsense.audiostream.theme.CoralAccent
import com.fifthsense.audiostream.theme.CyanNeon
import com.fifthsense.audiostream.theme.DarkBackground
import com.fifthsense.audiostream.theme.DarkSurfaceVariant
import com.fifthsense.audiostream.theme.GreenSuccess
import com.fifthsense.audiostream.theme.TextMuted
import com.fifthsense.audiostream.theme.TextPrimary
import com.fifthsense.audiostream.theme.TextSecondary
import com.fifthsense.audiostream.theme.VioletElectric

@Composable
fun BleDeviceCard(
    device: BleAudioDevice,
    onConnectClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = device.connectionState == BleConnectionState.CONNECTED
    val isConnecting = device.connectionState == BleConnectionState.CONNECTING ||
            device.connectionState == BleConnectionState.DISCOVERING_SERVICES

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (isConnected) CyanNeon.copy(alpha = 0.6f) else Color(0x2200F5D4)
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
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Device Icon with glow badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) CyanNeon.copy(alpha = 0.15f) else DarkSurfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint = if (isConnected) CyanNeon else VioletElectric,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = device.name.ifEmpty { "nRF54L15 Audio Sink" },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        if (device.isTargetDevice) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyanNeon.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "nRF54L15",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanNeon
                                )
                            }
                        }
                    }

                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RSSI: ${device.rssi} dBm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Text(
                            text = "•",
                            color = TextMuted
                        )

                        Text(
                            text = when (device.connectionState) {
                                BleConnectionState.CONNECTED -> "Ready (2M PHY, DLE)"
                                BleConnectionState.CONNECTING -> "Negotiating MTU..."
                                BleConnectionState.DISCOVERING_SERVICES -> "Discovering I2S Service..."
                                BleConnectionState.DISCONNECTING -> "Disconnecting..."
                                BleConnectionState.DISCONNECTED -> "Available"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (device.connectionState) {
                                BleConnectionState.CONNECTED -> GreenSuccess
                                BleConnectionState.CONNECTING,
                                BleConnectionState.DISCOVERING_SERVICES -> CyanNeon
                                else -> TextMuted
                            }
                        )
                    }
                }
            }

            // Connect / Disconnect Action Button
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = CyanNeon,
                    strokeWidth = 2.5.dp
                )
            } else if (isConnected) {
                Button(
                    onClick = onDisconnectClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralAccent.copy(alpha = 0.2f),
                        contentColor = CoralAccent
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Disconnect")
                }
            } else {
                Button(
                    onClick = onConnectClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeon,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Connect")
                }
            }
        }
    }
}
