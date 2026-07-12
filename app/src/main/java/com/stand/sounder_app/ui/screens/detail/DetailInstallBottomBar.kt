package com.stand.sounder_app.ui.screens.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.data.download.DownloadState
import com.stand.sounder_app.ui.components.InstallButton

/**
 * 详情页底部安装按钮，悬浮卡片样式，复用 InstallButton（宽屏版）。
 */
@Composable
fun DetailInstallBottomBar(
    isInstalled: Boolean = false,
    downloadState: DownloadState? = null,
    onToggleInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        InstallButton(
            isInstalled = isInstalled,
            downloadState = downloadState,
            wide = true,
            onToggleInstall = onToggleInstall
        )
    }
}
