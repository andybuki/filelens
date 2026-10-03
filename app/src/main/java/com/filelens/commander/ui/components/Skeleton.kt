package com.filelens.commander.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun FileListSkeleton(
    modifier: Modifier = Modifier,
    rowCount: Int = 8
) {
    val pulse = useShimmerAlpha()
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items((0 until rowCount).toList(), key = { it }) { i ->
            SkeletonRow(pulse = pulse, index = i)
        }
    }
}

/**
 * Placeholder body for the inspector while the analyzer is still working on
 * a freshly-selected file. Mirrors the real overview layout (tile + title +
 * trust card + a couple of data rows + an action button) so the transition
 * to real content doesn't reshuffle the panel.
 */
@Composable
fun InspectorSkeleton(modifier: Modifier = Modifier) {
    val pulse = useShimmerAlpha()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(width = 48.dp, height = 48.dp, radius = 12.dp, alpha = pulse)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBlock(width = 180.dp, height = 18.dp, radius = 5.dp, alpha = pulse)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBlock(width = 110.dp, height = 12.dp, radius = 4.dp, alpha = pulse * 0.85f)
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        SkeletonBlock(width = 0.dp, height = 72.dp, radius = 14.dp, alpha = pulse, fillWidth = true)
        Spacer(modifier = Modifier.height(16.dp))
        repeat(4) { idx ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBlock(width = (60 + idx * 12).dp, height = 12.dp, radius = 4.dp, alpha = pulse * 0.8f)
                Spacer(modifier = Modifier.weight(1f))
                SkeletonBlock(width = (90 + ((idx * 17) % 60)).dp, height = 12.dp, radius = 4.dp, alpha = pulse)
            }
            if (idx < 3) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        SkeletonBlock(width = 0.dp, height = 46.dp, radius = 10.dp, alpha = pulse, fillWidth = true)
    }
}

@Composable
private fun SkeletonRow(pulse: Float, index: Int) {
    val nameWidths = listOf(180.dp, 140.dp, 220.dp, 160.dp, 200.dp)
    val nameWidth = nameWidths[index % nameWidths.size]
    val subWidth = (90 + (index * 13) % 60).dp
    val showBadge = index % 3 != 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBlock(
            width = 34.dp,
            height = 34.dp,
            radius = 9.dp,
            alpha = pulse
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBlock(width = nameWidth, height = 14.dp, radius = 4.dp, alpha = pulse)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBlock(width = subWidth, height = 11.dp, radius = 4.dp, alpha = pulse * 0.85f)
        }
        if (showBadge) {
            Spacer(modifier = Modifier.width(8.dp))
            SkeletonBlock(width = 34.dp, height = 16.dp, radius = 5.dp, alpha = pulse)
        }
    }
}

@Composable
private fun SkeletonBlock(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    radius: androidx.compose.ui.unit.Dp,
    alpha: Float,
    fillWidth: Boolean = false
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha.coerceIn(0.3f, 1f))
    val sizeMod = if (fillWidth) {
        Modifier.fillMaxWidth().height(height)
    } else {
        Modifier.size(width = width, height = height)
    }
    Box(
        modifier = sizeMod
            .clip(RoundedCornerShape(radius))
            .background(base)
    )
}

@Composable
private fun useShimmerAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton-pulse"
    )
    return pulse
}
