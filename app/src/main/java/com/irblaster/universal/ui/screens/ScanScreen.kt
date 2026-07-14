package com.irblaster.universal.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irblaster.universal.ui.components.NeonButton
import com.irblaster.universal.ui.components.SignalWaveAnimation
import com.irblaster.universal.ui.theme.DarkBg
import com.irblaster.universal.ui.theme.DarkCard
import com.irblaster.universal.ui.theme.NeonCyan
import com.irblaster.universal.ui.theme.NeonGreen
import com.irblaster.universal.ui.theme.NeonPurple
import com.irblaster.universal.viewmodel.MainViewModel

@Composable
fun ScanScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val scanState by viewModel.scanState.collectAsState()
    val progress = if (scanState.totalSignals > 0)
        scanState.currentIndex.toFloat() / scanState.totalSignals else 0f
    val progressAnim by animateFloatAsState(progress, tween(200), label = "progress")

    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotation by infiniteTransition.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Animated background
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.Center)
                .rotate(if (scanState.isScanning) rotation else 0f)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        listOf(NeonCyan.copy(0.08f), NeonPurple.copy(0.05f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "⚡ اسکن خودکار IR",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "تمام سیگنال‌های ممکن را با سرعت بالا تست می‌کند",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.5f)
            )

            Spacer(Modifier.height(28.dp))

            // Main scan card
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(20.dp)
                        .background(
                            if (scanState.isScanning) NeonCyan.copy(0.12f) else Color.Transparent,
                            RoundedCornerShape(24.dp)
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(NeonCyan.copy(0.6f), NeonPurple.copy(0.3f))),
                            RoundedCornerShape(24.dp)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkCard)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Signal wave or idle state
                    Box(
                        modifier = Modifier.height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (scanState.isScanning) {
                            SignalWaveAnimation(NeonCyan)
                        } else {
                            Text("📡", fontSize = 48.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    AnimatedContent(
                        targetState = scanState.isScanning,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "status"
                    ) { isScanning ->
                        Text(
                            if (isScanning) "در حال اسکن..." else "آماده اسکن",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isScanning) NeonCyan else Color.White.copy(0.7f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    if (scanState.isScanning || scanState.currentIndex > 0) {
                        Text(
                            "${scanState.currentIndex} / ${scanState.totalSignals}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(Modifier.height(4.dp))

                        scanState.currentSignal?.let { signal ->
                            AnimatedContent(
                                targetState = signal.name,
                                transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                                label = "signame"
                            ) { name ->
                                Text(
                                    name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = { progressAnim },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonCyan,
                            trackColor = NeonCyan.copy(0.15f),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (scanState.markedWorking.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeonGreen.copy(0.1f), RoundedCornerShape(10.dp))
                                .border(1.dp, NeonGreen.copy(0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${scanState.markedWorking.size} سیگنال موفق علامت‌گذاری شد",
                                    color = NeonGreen,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Speed slider
            if (!scanState.isScanning) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تأخیر بین سیگنال‌ها", color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${scanState.delayMs}ms",
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = scanState.delayMs.toFloat(),
                        onValueChange = { },
                        valueRange = 30f..500f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = NeonCyan.copy(0.2f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("⚡ 30ms فوق سریع", color = Color.White.copy(0.4f), style = MaterialTheme.typography.labelMedium)
                        Text("🐢 500ms کند", color = Color.White.copy(0.4f), style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (scanState.isScanning) {
                    NeonButton(
                        text = "⏹ توقف",
                        onClick = { viewModel.pauseScan() },
                        color = Color(0xFFFF5252),
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = "✓ این کار کرد!",
                        onClick = { viewModel.markCurrentWorking() },
                        color = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    NeonButton(
                        text = "▶ شروع اسکن TV",
                        onClick = { viewModel.startBruteForceScan("tv") },
                        color = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = "▶ اسکن AC",
                        onClick = { viewModel.startBruteForceScan("ac") },
                        color = NeonPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!scanState.isScanning && (scanState.currentIndex > 0 || scanState.markedWorking.isNotEmpty())) {
                Spacer(Modifier.height(10.dp))
                NeonButton(
                    text = "🔄 شروع مجدد",
                    onClick = { viewModel.resetScan() },
                    color = Color.White.copy(0.6f)
                )
            }

            // Working signals list
            if (scanState.markedWorking.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "سیگنال‌های موفق",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(scanState.markedWorking) { signal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeonGreen.copy(0.08f), RoundedCornerShape(10.dp))
                                .border(1.dp, NeonGreen.copy(0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FlashOn, null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(signal.name, color = NeonGreen, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(signal.description, color = Color.White.copy(0.5f), style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
