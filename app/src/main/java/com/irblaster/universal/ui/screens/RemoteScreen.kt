package com.irblaster.universal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BoltOutlined
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irblaster.universal.data.DeviceBrand
import com.irblaster.universal.data.IrSignal
import com.irblaster.universal.ui.theme.DarkBg
import com.irblaster.universal.ui.theme.DarkCard
import com.irblaster.universal.ui.theme.NeonCyan
import com.irblaster.universal.ui.theme.NeonGreen
import com.irblaster.universal.ui.theme.NeonPurple
import com.irblaster.universal.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun RemoteScreen(
    viewModel: MainViewModel,
    categoryId: String,
    onBack: () -> Unit,
) {
    val category = viewModel.categories.find { it.id == categoryId } ?: return
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val lastSignal by viewModel.lastTransmitted.collectAsState()
    val brand = selectedBrand ?: category.brands.firstOrNull()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(50); visible = true }

    var flashSignal by remember { mutableStateOf<IrSignal?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.TopCenter)
                .blur(100.dp)
                .background(NeonPurple.copy(alpha = 0.08f), CircleShape)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(52.dp))

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    category.icon + " " + category.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Brand selector
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(category.brands) { _, b ->
                    BrandChip(
                        brand = b,
                        selected = b == (selectedBrand ?: category.brands.firstOrNull()),
                        onClick = { viewModel.selectBrand(b) }
                    )
                }
            }

            // Last signal indicator
            AnimatedVisibility(visible = lastSignal != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(NeonGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, NeonGreen.copy(0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "ارسال شد: ${lastSignal?.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonGreen
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Signal buttons grid
            val currentBrand = selectedBrand ?: category.brands.firstOrNull()
            if (currentBrand != null) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(currentBrand.signals) { index, signal ->
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(300, delayMillis = index * 50)) +
                                    scaleIn(tween(300, delayMillis = index * 50))
                        ) {
                            SignalButton(
                                signal = signal,
                                isFlashing = flashSignal == signal,
                                onClick = {
                                    flashSignal = signal
                                    viewModel.transmitSignal(signal)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrandChip(brand: DeviceBrand, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) NeonCyan.copy(alpha = 0.2f) else DarkCard
            )
            .border(
                1.dp,
                if (selected) NeonCyan else Color.White.copy(0.15f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            brand.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) NeonCyan else Color.White.copy(0.7f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun SignalButton(signal: IrSignal, isFlashing: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed || isFlashing) 0.92f else 1f, label = "scale")

    LaunchedEffect(isFlashing) {
        if (isFlashing) {
            pressed = true
            delay(150)
            pressed = false
        }
    }

    val isPower = signal.name.contains("Power", ignoreCase = true) ||
            signal.name.contains("روشن", ignoreCase = true)
    val glowColor = when {
        isPower -> Color(0xFFFF6D00)
        signal.name.contains("Vol", ignoreCase = true) -> NeonCyan
        signal.name.contains("Mute", ignoreCase = true) -> Color(0xFFFF5252)
        else -> NeonPurple.copy(alpha = 0.8f)
    }

    Box(
        modifier = Modifier.scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(if (isFlashing) 12.dp else 6.dp)
                .background(glowColor.copy(if (isFlashing) 0.4f else 0.15f), RoundedCornerShape(14.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    glowColor.copy(if (isFlashing) 1f else 0.5f),
                    RoundedCornerShape(14.dp)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(DarkCard)
                .clickable { onClick() }
                .padding(vertical = 14.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isPower) {
                    Icon(
                        Icons.Default.BoltOutlined,
                        contentDescription = null,
                        tint = glowColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    signal.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = glowColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (signal.description.isNotEmpty()) {
                    Text(
                        signal.description,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp),
                        color = Color.White.copy(0.4f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
