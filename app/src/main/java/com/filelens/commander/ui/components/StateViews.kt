package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlAccentTint
import com.filelens.commander.ui.theme.FlInk2
import com.filelens.commander.ui.theme.FlInk3
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
                color = FlAccent,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
            if (label != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlInk2
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
    onAction: (() -> Unit)? = null
) {
    StateScaffold(
        modifier = modifier,
        icon = icon,
        iconTint = FlAccent,
        iconBackground = FlAccentTint,
        headline = headline,
        subtitle = subtitle,
        actionLabel = actionLabel,
        onAction = onAction
    )
}

@Composable
fun NoResultsState(
    query: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    EmptyState(
        modifier = modifier,
        icon = Icons.Filled.SearchOff,
        headline = "No results",
        subtitle = if (query.isNotEmpty()) "Nothing matched “$query” in this folder." else null,
        actionLabel = if (onClear != null) "Clear search" else null,
        onAction = onClear
    )
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    StateScaffold(
        modifier = modifier,
        icon = Icons.Filled.Warning,
        iconTint = RiskDanger,
        iconBackground = RiskDangerTint,
        headline = "Something went wrong",
        subtitle = message,
        actionLabel = if (onRetry != null) "Try again" else null,
        onAction = onRetry
    )
}

@Composable
private fun StateScaffold(
    modifier: Modifier,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBackground: androidx.compose.ui.graphics.Color,
    headline: String,
    subtitle: String?,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlInk3,
                    textAlign = TextAlign.Center
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onAction) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
