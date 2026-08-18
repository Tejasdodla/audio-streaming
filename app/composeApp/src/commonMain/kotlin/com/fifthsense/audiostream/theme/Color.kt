package com.fifthsense.audiostream.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Background & Surface Palettes (Deep Obsidian & Glass)
val DarkBackground = Color(0xFF0A0D14)
val DarkSurface = Color(0xFF121824)
val DarkSurfaceVariant = Color(0xFF1B2333)
val DarkCardBg = Color(0xCC161E2E)
val GlassBorder = Color(0x3300F5D4)
val GlassBorderSecondary = Color(0x227B2CBF)

// High-Tech Cyber Accents
val CyanNeon = Color(0xFF00F5D4)
val CyanGlow = Color(0x6600F5D4)
val VioletElectric = Color(0xFF7B2CBF)
val VioletGlow = Color(0x667B2CBF)
val CoralAccent = Color(0xFFFF5470)
val AmberWarning = Color(0xFFFFBE0B)
val GreenSuccess = Color(0xFF10B981)

// Text Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Visualizer & Control Gradients
val NeonGradient = Brush.horizontalGradient(
    colors = listOf(CyanNeon, VioletElectric)
)

val CardGradients = Brush.verticalGradient(
    colors = listOf(Color(0xFF1A2234), Color(0xFF0F1420))
)

val VisualizerGradient = Brush.verticalGradient(
    colors = listOf(CyanNeon, VioletElectric, CoralAccent)
)

val GlowActiveBrush = Brush.radialGradient(
    colors = listOf(Color(0x5500F5D4), Color.Transparent)
)
