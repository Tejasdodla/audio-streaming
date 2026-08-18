package com.fifthsense.audiostream.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.fifthsense.audiostream.theme.CyanNeon
import com.fifthsense.audiostream.theme.GreenSuccess
import com.fifthsense.audiostream.theme.TextMuted
import com.fifthsense.audiostream.theme.TextPrimary
import com.fifthsense.audiostream.theme.TextSecondary
import com.fifthsense.audiostream.theme.VioletElectric

@Composable
fun TopHeader(
    connectedDevice: BleAudioDevice?,
    isScanning: Boolean,
    onScanClicked: () -> Unit,
    onStatusPillClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "5TH SENSE AUDIO",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "nRF54L15 • MAX98357A I2S Receiver",
                style = MaterialTheme.typography.labelSmall,
                color = CyanNeon
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Connection Status Pill
            val isConnected = connectedDevice?.connectionState == BleConnectionState.CONNECTED
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isConnected) GreenSuccess.copy(alpha = 0.15f) else Color(0x15FFFFFF)
                    )
                    .clickable { onStatusPillClicked() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) GreenSuccess else Color(0xFF64748B))
                    )
                    Text(
                        text = if (isConnected) "CONNECTED" else "OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConnected) GreenSuccess else TextMuted
                    )
                }
            }

            // Scan button
            IconButton(
                onClick = onScanClicked,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x15FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Scan BLE",
                    tint = if (isScanning) CyanNeon else TextSecondary
                )
            }
        }
    }
}
