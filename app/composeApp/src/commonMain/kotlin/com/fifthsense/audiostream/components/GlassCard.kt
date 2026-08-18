package com.fifthsense.audiostream.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fifthsense.audiostream.theme.DarkCardBg
import com.fifthsense.audiostream.theme.GlassBorder

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    backgroundColor: Color = DarkCardBg,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x1AFFFFFF),
                            Color(0x05FFFFFF),
                            Color(0x00FFFFFF)
                        )
                    )
                )
        ) {
            content()
        }
    }
}
