package com.stand.sounder_app.ui.screens.tasks

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stand.sounder_app.ui.components.ResourceImage
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.audio.PlaybackTaskInfo
import com.stand.sounder_app.R
import com.stand.sounder_app.ui.components.EmptyState
import com.stand.sounder_app.ui.components.SearchBox
import com.stand.sounder_app.ui.theme.StopColor
import com.stand.sounder_app.ui.theme.StopColorStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerScreen(
    bottomBarVisible: Boolean = false
) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TaskManagerHeader(statusText = statusText)

                SearchBox(
                    value = keyword,
                    onValueChange = { keyword = it },
                    placeholder = stringResource(R.string.search_task_hint)
                )

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
                            contentPadding = PaddingValues(bottom = 88.dp),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                        ) {
                            item { Spacer(Modifier.height(4.dp)) }
                            items(filteredTasks, key = { it.resourceId }) { task ->
                                TaskItemCard(
                                    task = task,
                                    onStop = { audioPlayer.stopResource(task.resourceId) }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }

            // 底部居中停止全部按钮（参考 DetailStopButton）
            if (filteredTasks.isNotEmpty()) {
                Surface(
                    onClick = { audioPlayer.stopAll() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (bottomBarVisible) 86.dp else 20.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.linearGradient(
                                    listOf(StopColorStart, StopColor)
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_stop),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.stop_all),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    task: PlaybackTaskInfo,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        ResourceImage(
            icon = task.icon,
            displayName = task.displayName,
            modifier = Modifier.size(48.dp),
            cornerRadius = 12.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // 首行：名称 + 模式徽标 + 实例数徽标（参考 TaskManagerView）
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.shortName.ifEmpty { stringResource(R.string.unnamed_resource) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.weight(1f))
                // 播放模式徽标
                if (task.playModeText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp)
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.task_instance_count, task.activePlayerCount),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.displayName.ifEmpty { stringResource(R.string.unnamed_resource) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            if (task.currentAudioName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.currentAudioName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        OutlinedIconButton(
            onClick = onStop,
            shape = CircleShape,
            border = BorderStroke(1.5.dp, StopColor),
            colors = IconButtonDefaults.outlinedIconButtonColors(
                contentColor = StopColor
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_stop),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
