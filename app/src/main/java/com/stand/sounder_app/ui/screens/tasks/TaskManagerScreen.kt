package com.stand.sounder_app.ui.screens.tasks

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.audio.PlaybackTaskInfo
import com.stand.sounder_app.R
import com.stand.sounder_app.ui.components.EmptyState
import com.stand.sounder_app.ui.components.SearchBox
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerScreen() {
    val audioPlayer = MyApp.instance.audioPlayerManager
    val tasks by audioPlayer.tasks.collectAsState()

    val statusText = if (tasks.isEmpty()) MyApp.instance.getString(R.string.no_tasks)
    else MyApp.instance.getString(R.string.task_count, tasks.size)

    var keyword by remember { mutableStateOf("") }
    val filteredTasks = remember(tasks, keyword) {
        if (keyword.isBlank()) tasks
        else tasks.filter {
            it.displayName.contains(keyword, ignoreCase = true) ||
                it.currentAudioName.contains(keyword, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TaskManagerHeader(statusText = statusText)

            SearchBox(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = stringResource(R.string.search_task_hint)
            )

            if (filteredTasks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = { audioPlayer.stopAll() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_stop),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.stop_all))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (tasks.isEmpty()) {
                EmptyState(
                    modifier = Modifier.padding(bottom = 72.dp),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_schedule),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    title = stringResource(R.string.no_tasks),
                    subtitle = stringResource(R.string.task_management_desc)
                )
            } else if (filteredTasks.isEmpty()) {
                EmptyState(
                    modifier = Modifier.padding(bottom = 72.dp),
                    title = stringResource(R.string.no_matching_tasks),
                    subtitle = stringResource(R.string.try_different_keywords)
                )
            } else {
                LazyColumn(
                            contentPadding = PaddingValues(bottom = 72.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(filteredTasks, key = { it.resourceId }) { task ->
                        TaskItemCard(
                            task = task,
                            onStop = { audioPlayer.stopResource(task.resourceId) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TaskItemCard(
    task: PlaybackTaskInfo,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        if (task.icon.isNotEmpty()) {
            GlideImage(
                model = task.icon,
                contentDescription = task.displayName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = task.shortName.ifEmpty { task.displayName.take(1) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // 首行：名称 + 模式徽标 + 实例数徽标（参考 TaskManagerView）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = task.displayName.ifEmpty { stringResource(R.string.unnamed_resource) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                // 播放模式徽标
                if (task.playModeText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.playModeText,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                // 实例数徽标
                if (task.activePlayerCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.task_instance_count, task.activePlayerCount),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            if (task.currentAudioName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.now_playing, task.currentAudioName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(onClick = onStop) {
            Icon(
                painter = painterResource(R.drawable.ic_stop),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.stop_playback))
        }
    }
}
