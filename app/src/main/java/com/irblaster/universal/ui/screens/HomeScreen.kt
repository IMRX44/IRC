package com.irblaster.universal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irblaster.universal.ui.components.CategoryCard
import com.irblaster.universal.ui.components.PulsingDot
import com.irblaster.universal.ui.theme.DarkBg
import com.irblaster.universal.ui.theme.NeonCyan
import com.irblaster.universal.ui.theme.NeonPurple
import com.irblaster.universal.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onCategoryClick: (String) -> Unit,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Background glow blobs
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .blur(80.dp)
                .background(NeonCyan.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .blur(80.dp)
                .background(NeonPurple.copy(alpha = 0.05f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(52.dp))

            // Header
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "IR Blaster",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            brush = Brush.linearGradient(listOf(NeonCyan, NeonPurple))
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingDot(
                                color = if (viewModel.transmitter.hasIrBlaster) NeonCyan else Color.Red,
                                size = 8.dp
                            )
                            Text(
                                "  " + if (viewModel.transmitter.hasIrBlaster) "IR Blaster آماده" else "IR Blaster یافت نشد",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (viewModel.transmitter.hasIrBlaster) NeonCyan else Color.Red
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = onScanClick) {
                            Icon(Icons.Default.BlurOn, contentDescription = "Scan", tint = NeonCyan)
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White.copy(0.6f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, delayMillis = 200))
            ) {
                Text(
                    "ریموت کنترل هوشمند برای همه دستگاه‌ها",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "دسته‌بندی دستگاه‌ها",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(viewModel.categories) { index, category ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(400, delayMillis = index * 100 + 300)) +
                                slideInVertically(tween(400, delayMillis = index * 100 + 300)) { 60 }
                    ) {
                        CategoryCard(
                            emoji = category.icon,
                            name = category.name,
                            brandCount = category.brands.size,
                            onClick = { onCategoryClick(category.id) }
                        )
                    }
                }
            }
        }
    }
}
