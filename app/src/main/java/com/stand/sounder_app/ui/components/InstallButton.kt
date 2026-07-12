package com.stand.sounder_app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stand.sounder_app.data.download.DownloadState
import com.stand.sounder_app.R
import com.stand.sounder_app.data.download.DownloadStatus
import com.stand.sounder_app.ui.theme.StatusInstalledText
import kotlinx.coroutines.delay

/**
 * 安装按钮，参考 vs_project 的 InstallButton（Compact / Wide）设计。
 *
 * 三种状态：
 * - 空闲（未安装/未下载）：蓝色圆角按钮，点击开始安装/下载
 * - 下载中：蓝色底 + 进度填充层 + 百分比，点击暂停
 * - 已暂停：橙色填充 + "继续"，点击继续
 *
 * @param wide   true=宽屏大按钮（详情页，高度 48dp，带下载图标）；
 *              false=紧凑小按钮（列表项，88x32）
 */
@Composable
fun InstallButton(
    isInstalled: Boolean = false,
    resumable: Boolean = false,
    downloadState: DownloadState? = null,
    wide: Boolean = false,
    onToggleInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = downloadState?.status
    val progress = downloadState?.progress ?: 0f

    // installState: 0=空闲, 1=下载中/暂停, 2=已安装
    var installState by remember(isInstalled, status, resumable) {
        mutableIntStateOf(
            when {
                isInstalled && status != DownloadStatus.DOWNLOADING && status != DownloadStatus.PAUSED -> 2
                status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED -> 1
                else -> 0
            }
        )
    }

    val isPaused = status == DownloadStatus.PAUSED || (installState == 0 && resumable)
    val targetProgress = when (installState) {
        0 -> 0f
        1 -> progress.coerceIn(0f, 1f)
        else -> 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 300),
        label = "installProgress"
    )

    val shape = RoundedCornerShape(50)
    val height = if (wide) 48.dp else 32.dp
    val width = if (wide) Modifier.fillMaxWidth() else Modifier.width(88.dp)
    val textSize = if (wide) 15.sp else 11.sp
    val percentSize = if (wide) 16.sp else 10.sp
    val iconSize = if (wide) 20.dp else 0.dp
    val accentBlue = Color(0xFF4A9EFF)
    val accentBlueHover = Color(0xFF3B8BF0)
    val pauseOrange = Color(0xFFFFA726)

    val commonModifier = modifier
        .then(width)
        .height(height)

    when (installState) {
        0 -> {
            if (resumable) {
                // 可继续：橙色「继续」按钮（与暂停态一致）
                Box(
                    modifier = commonModifier
                        .clip(shape)
                        .background(pauseOrange)
                        .clickable { onToggleInstall() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "继续",
                        fontSize = textSize,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 空闲：蓝色圆角按钮
                Box(
                    modifier = commonModifier
                        .clip(shape)
                        .background(accentBlue)
                        .clickable { onToggleInstall() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (wide) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(iconSize)
                            )
                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.width(8.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.install),
                            fontSize = textSize,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        1 -> {
            // 活跃状态（下载中 / 已暂停）：整体可点击切换 暂停/继续
            Box(
                modifier = commonModifier
                    .clip(shape)
                    .background(if (isPaused) pauseOrange else accentBlueHover)
                    .clickable { onToggleInstall() }
            ) {
                if (!isPaused) {
                    // 进度填充层（直角，仅外层按钮圆角）
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RectangleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accentBlue, Color(0xFF60A5FA))
                                )
                            )
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPaused) "继续" else "${(animatedProgress * 100).toInt()}%",
                        fontSize = if (isPaused) textSize else percentSize,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // 下载完成标记
            if (progress >= 1f && !isPaused) {
                LaunchedEffect(Unit) {
                    delay(200)
                    installState = 2
                }
            }
        }
        else -> {
            // 已安装
            Box(
                modifier = commonModifier,
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.installed),
                    tint = StatusInstalledText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
