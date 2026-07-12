package com.stand.sounder_app.ui.components

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.data.download.DownloadState
import com.stand.sounder_app.data.model.RemoteResource

@Composable
fun ShopCard(
    resource: RemoteResource,
    isInstalled: Boolean = false,
    resumable: Boolean = false,
    downloadState: DownloadState? = null,
    onCardClick: () -> Unit,
    onToggleInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onCardClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            ResourceImage(
                icon = resource.icon,
                displayName = resource.displayName.ifEmpty { resource.name },
                modifier = Modifier.size(56.dp),
                cornerRadius = 12.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = resource.displayName.ifEmpty { resource.name },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                if (resource.size.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = resource.size,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 安装按钮（参考 InstallButton 紧凑版设计）
            InstallButton(
                isInstalled = isInstalled,
                resumable = resumable,
                downloadState = downloadState,
                wide = false,
                onToggleInstall = onToggleInstall
            )
        }
    }
}
