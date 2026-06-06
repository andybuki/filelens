package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.filelens.commander.R
import com.filelens.commander.ui.theme.FlBorder
import com.filelens.commander.ui.theme.FlBorder2
import com.filelens.commander.ui.theme.FlInk
import com.filelens.commander.ui.theme.FlOnAccent
import com.filelens.commander.ui.theme.FlSurface
import com.filelens.commander.ui.theme.FlSurface2
import com.filelens.commander.ui.theme.FlText
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.ui.theme.RiskDangerTint

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
            if (label != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlText
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    headline: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Filled.FolderOpen,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FlSurface2)
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            DashedArt(icon = icon)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium.copy(color = FlInk),
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlText,
                    textAlign = TextAlign.Center
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = FlOnAccent
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    )
                ) {
                    if (actionIcon != null) {
                        Icon(actionIcon, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun NoResultsState(
    query: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            SolidArt(icon = Icons.Filled.SearchOff)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.state_no_matches_headline),
                style = MaterialTheme.typography.titleMedium.copy(color = FlInk),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (query.isNotEmpty()) stringResource(R.string.state_no_matches_subtitle, query) else stringResource(R.string.state_no_matches_no_query),
                style = MaterialTheme.typography.bodyMedium,
                color = FlText,
                textAlign = TextAlign.Center
            )
            if (onClear != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.action_clear_search), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(RiskDangerTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = RiskDanger,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.state_error_headline),
                style = MaterialTheme.typography.titleMedium.copy(color = FlInk),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = FlText,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.action_retry), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DashedArt(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(132.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(FlSurface)
            .drawDashedBorder(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FlBorder2,
            modifier = Modifier.size(52.dp)
        )
    }
}

@Composable
private fun SolidArt(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(132.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(FlSurface)
            .border(width = 1.dp, color = FlBorder, shape = RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FlBorder2,
            modifier = Modifier.size(52.dp)
        )
    }
}

private fun Modifier.drawDashedBorder(): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
    drawRoundRect(
        color = FlBorder2,
        style = stroke,
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
    )
}
