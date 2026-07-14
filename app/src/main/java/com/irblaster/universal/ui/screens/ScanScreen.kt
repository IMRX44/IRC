package com.irblaster.universal.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irblaster.universal.data.BrandProfile
import com.irblaster.universal.ui.components.NeonButton
import com.irblaster.universal.ui.components.SignalWaveAnimation
import com.irblaster.universal.ui.theme.DarkBg
import com.irblaster.universal.ui.theme.DarkCard
import com.irblaster.universal.ui.theme.NeonCyan
import com.irblaster.universal.ui.theme.NeonGreen
import com.irblaster.universal.ui.theme.NeonPurple

@Composable
fun ScanScreen(
    viewModel: com.irblaster.universal.viewmodel.MainViewModel,
    categoryId: String,
    onBack: () -> Unit,
) {
    val smart by viewModel.smart.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        if (!smart.active) {
            BrandPickerView(
                categoryId = categoryId,
                profiles = viewModel.brandProfilesFor(categoryId),
                onBack = onBack,
                onPick = { viewModel.startSmartScan(categoryId, it.brand) }
            )
        } else if (smart.foundCode != null) {
            FoundView(smart, onBack = { viewModel.exitSmart(); onBack() },
                onRetry = { viewModel.startSmartScan(smart.categoryId, smart.brand) })
        } else {
            ScanningView(
                smart = smart,
                onBack = { viewModel.exitSmart() },
                onWorked = { viewModel.confirmWorked() },
                onPause = { viewModel.pauseSmart() },
                onResume = { viewModel.resumeSmart() },
                onNext = { viewModel.stepNext() },
                onPrev = { viewModel.stepPrev() },
                onResend = { viewModel.resendCurrent() },
            )
        }
    }
}

@Composable
private fun BrandPickerView(
    categoryId: String,
    profiles: List<BrandProfile>,
    onBack: () -> Unit,
    onPick: (BrandProfile) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(52.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("⚡ اسکن هوشمند Power", style = MaterialTheme.typography.titleLarge,
                color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "برند دستگاهت رو انتخاب کن. تمام کدهای روشن/خاموش اون برند خودکار و پشت‌سرهم فرستاده میشن — وقتی دستگاه واکنش نشون داد دکمه «✅ کار کرد» رو بزن.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(0.55f)
        )
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(profiles) { _, profile ->
                val isUniversal = profile.brand.startsWith("⚡")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (isUniversal) NeonGreen.copy(0.7f) else NeonCyan.copy(0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .background(if (isUniversal) NeonGreen.copy(0.08f) else DarkCard)
                        .clickable { onPick(profile) }
                        .padding(vertical = 18.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            profile.brand,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isUniversal) NeonGreen else Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${profile.codes.size} کد",
                            style = MaterialTheme.typography.labelMedium,
                            color = (if (isUniversal) NeonGreen else NeonCyan).copy(0.75f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanningView(
    smart: com.irblaster.universal.viewmodel.SmartScanState,
    onBack: () -> Unit,
    onWorked: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onResend: () -> Unit,
) {
    val progressAnim by animateFloatAsState(smart.progress, tween(120), label = "p")
    val infinite = rememberInfiniteTransition(label = "rot")
    val rotation by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "r"
    )

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.size(420.dp).align(Alignment.Center)
                .rotate(if (smart.running) rotation else 0f)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(listOf(NeonCyan.copy(0.10f), NeonPurple.copy(0.05f), Color.Transparent)),
                    CircleShape
                )
        )
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(52.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(smart.brand, style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            // Big pulsing power target
            Box(Modifier.fillMaxWidth().aspectRatio(1.4f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(220.dp).blur(40.dp)
                        .background(
                            (if (smart.running) NeonCyan else Color.Gray).copy(0.15f),
                            CircleShape
                        )
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                        if (smart.running) SignalWaveAnimation(NeonCyan) else Text("⏸", fontSize = 40.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "${smart.index + 1} / ${smart.codes.size}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                        color = Color.White, fontWeight = FontWeight.ExtraBold
                    )
                    AnimatedContent(
                        targetState = smart.current?.label ?: "",
                        transitionSpec = { fadeIn(tween(80)) togetherWith fadeOut(tween(80)) },
                        label = "lbl"
                    ) { label ->
                        Text(label, style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan, fontFamily = FontFamily.Monospace)
                    }
                    smart.current?.let {
                        Text(it.protocol, style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(0.4f))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progressAnim },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = NeonCyan, trackColor = NeonCyan.copy(0.15f), strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(6.dp))
            Text(
                if (smart.finished) "✓ همه کدها فرستاده شد — می‌تونی دوباره یا قبلی/بعدی رو امتحان کنی"
                else if (smart.running) "در حال ارسال با حداکثر سرعت سخت‌افزار…"
                else "متوقف شد — دستی جلو/عقب برو",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(0.5f)
            )

            Spacer(Modifier.weight(1f))

            // The BIG "it worked" button
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Box(Modifier.matchParentSize().blur(20.dp)
                    .background(NeonGreen.copy(0.25f), RoundedCornerShape(20.dp)))
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(2.dp, NeonGreen, RoundedCornerShape(20.dp))
                        .background(NeonGreen.copy(0.12f))
                        .clickable { onWorked() }
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("✅ کار کرد! (روشن/خاموش شد)", style = MaterialTheme.typography.titleMedium,
                        color = NeonGreen, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Transport controls
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleControl(Icons.Default.SkipPrevious, "قبلی", NeonPurple, Modifier.weight(1f), onPrev)
                if (smart.running) {
                    CircleControl(Icons.Default.Pause, "توقف", Color(0xFFFF9800), Modifier.weight(1f), onPause)
                } else {
                    CircleControl(Icons.Default.PlayArrow, "ادامه", NeonCyan, Modifier.weight(1f), onResume)
                }
                CircleControl(Icons.Default.Bolt, "دوباره", NeonGreen, Modifier.weight(1f), onResend)
                CircleControl(Icons.Default.SkipNext, "بعدی", NeonPurple, Modifier.weight(1f), onNext)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CircleControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .border(1.dp, color.copy(0.5f), RoundedCornerShape(14.dp))
            .background(DarkCard)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun FoundView(
    smart: com.irblaster.universal.viewmodel.SmartScanState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val code = smart.foundCode ?: return
    val infinite = rememberInfiniteTransition(label = "glow")
    val scale by infinite.animateFloat(
        0.95f, 1.05f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "s"
    )
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.scale(scale), contentAlignment = Alignment.Center) {
            Box(Modifier.size(160.dp).blur(50.dp).background(NeonGreen.copy(0.3f), CircleShape))
            Box(
                Modifier.size(130.dp).border(2.dp, NeonGreen, CircleShape)
                    .background(NeonGreen.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, null, tint = NeonGreen, modifier = Modifier.size(70.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("کد پیدا شد! 🎉", style = MaterialTheme.typography.headlineMedium,
            color = NeonGreen, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("${smart.brand} — ${code.protocol}",
            style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(code.label, style = MaterialTheme.typography.labelLarge,
            color = NeonCyan, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text("این کد Power دستگاه توئه. حالا می‌تونی از این استفاده کنی.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(0.5f), textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        NeonButton("✅ عالیه، بازگشت", onClick = onBack, color = NeonGreen,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        NeonButton("🔄 ادامه اسکن (این درست نبود)", onClick = onRetry,
            color = Color.White.copy(0.6f), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}
