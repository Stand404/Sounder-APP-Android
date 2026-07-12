package com.stand.sounder_app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.stand.sounder_app.data.model.Resource
import com.stand.sounder_app.R

/**
 * 个人资源列表卡片 —— 参考 Win PersonalResourceListItem 布局
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResourceCard(
    resource: Resource,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    resumable: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 资源图标 —— 参考 Win: 56x56, CornerRadius 12
            ResourceImage(
                icon = resource.icon,
                displayName = resource.displayName.ifEmpty { resource.name },
                modifier = Modifier.size(56.dp),
                cornerRadius = 12.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 资源信息 —— 参考 Win: Spacing=3
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // DisplayName
                Text(
                    text = resource.displayName.ifEmpty { resource.name },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Description
                if (resource.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = resource.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 元信息行 —— Win: Orientation=Horizontal Spacing=12 FontSize=11 TextTertiary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (resource.size > 0) {
                        Text(
                            text = formatSize(resource.size),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    if (resource.audioList.isNotEmpty()) {
                        if (resource.size > 0) {
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = "${resource.audioList.size} 个音频",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    if (resumable) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.resumable),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFA726)
                        )
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.1f MB".format(bytes.toDouble() / (1024 * 1024))
    }
}
