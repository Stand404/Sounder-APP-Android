package com.stand.sounder_app.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.stand.sounder_app.R
import com.stand.sounder_app.data.model.AudioItem
import com.stand.sounder_app.data.model.LoopMode
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode
import com.stand.sounder_app.data.model.Resource
import com.stand.sounder_app.ui.components.ContextMenu
import com.stand.sounder_app.ui.components.ShortcutPermissionSheet
import com.stand.sounder_app.util.ShortcutPermissionChecker
import com.stand.sounder_app.ui.components.ContextMenuItem
import com.stand.sounder_app.ui.components.PillOption
import com.stand.sounder_app.ui.components.PillToggle
import com.stand.sounder_app.util.formatByteSize
import com.stand.sounder_app.viewmodel.DetailViewModel
import com.stand.sounder_app.viewmodel.PersonalResourceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun DetailScreen(
    resourceId: String,
    mode: String,
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(resourceId, mode) {
        viewModel.loadResource(resourceId, mode)
    }

    val showBottomBar =
        mode == "cloud" && uiState.remoteResource != null && uiState.error == null && !uiState.isLoading && !uiState.isDownloadComplete

    var showDetailMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val personalVM: PersonalResourceViewModel = viewModel()

    // 导出文件选择器：当 shareRequest 发射时，打开系统保存对话框
    var pendingExportZip by remember { mutableStateOf<File?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val zipFile = pendingExportZip ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        zipFile.inputStream().use { `in` -> `in`.copyTo(out) }
                    }
                    zipFile.delete()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        personalVM.shareRequest.collect { zipFile ->
            pendingExportZip = zipFile
            exportLauncher.launch("${zipFile.nameWithoutExtension}.zip")
        }
    }

    // 收集 personalVM 的 toast 消息（添加到桌面等操作反馈）
    val detailSnackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    LaunchedEffect(Unit) {
        personalVM.toast.collect { message ->
            detailSnackbarHostState.showSnackbar(message)
        }
    }

    // 监听「创建桌面快捷方式」被厂商权限拒绝，弹出引导去设置
    var showShortcutGuide by remember { mutableStateOf<Resource?>(null) }
    LaunchedEffect(Unit) {
        personalVM.shortcutPermissionGuide.collect { resource ->
            showShortcutGuide = resource
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
            snackbarHost = {
                androidx.compose.material3.SnackbarHost(detailSnackbarHostState)
            },
            bottomBar = {
                if (showBottomBar) {
                    DetailInstallBottomBar(
                        isInstalled = uiState.isDownloadComplete,
                        downloadState = com.stand.sounder_app.MyApp.instance.downloadManager.getDownloadState(
                            resourceId
                        ),
                        onToggleInstall = { viewModel.toggleInstall() }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 顶部栏：返回 + 右上角菜单
                Box(modifier = Modifier.fillMaxWidth()) {
                    DetailBackHeader(onBack = onBack)
                    val localResource = uiState.resource
                    if (localResource != null) {
                        IconButton(
                            onClick = { showDetailMenu = true },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more),
                                contentDescription = stringResource(R.string.more_actions)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (uiState.isLoading) {
                        DetailSkeleton()
                    } else if (uiState.error != null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = uiState.error ?: stringResource(R.string.load_failed),
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                                TextButton(onClick = { viewModel.loadResource(resourceId, mode) }) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    } else {
                        val resource = uiState.resource
                        val remoteResource = uiState.remoteResource
                        val isInstalled = resource != null

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                DetailHeader(
                                    icon = resource?.icon ?: remoteResource?.icon ?: "",
                                    displayName = resource?.displayName
                                        ?: remoteResource?.displayName ?: "",
                                    description = resource?.description
                                        ?: remoteResource?.description ?: "",
                                    size = resource?.let { formatByteSize(it.size) }
                                        ?: remoteResource?.size?.takeIf { it.isNotEmpty() },
                                    publishDate = resource?.publishDate?.takeIf { it.isNotEmpty() }
                                        ?: remoteResource?.publishDate?.takeIf { it.isNotEmpty() },
                                )
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    val audioList: List<AudioItem> = resource?.audioList
                                        ?: remoteResource?.audioList?.map {
                                            AudioItem(
                                                id = it.id,
                                                name = it.name,
                                                src = it.url,
                                                duration = it.duration
                                            )
                                        }
                                        ?: emptyList()

                                    if (audioList.isEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 40.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = stringResource(R.string.no_audio_in_package),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.5f
                                                )
                                            )
                                        }
                                    } else {
                                        if (!isInstalled) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.online_preview),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 16.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                PillToggle(
                                                    options = listOf(
                                                        PillOption(
                                                            false,
                                                            stringResource(R.string.normal_mode)
                                                        ),
                                                        PillOption(
                                                            true,
                                                            stringResource(R.string.interrupt_mode)
                                                        )
                                                    ),
                                                    selectedValue = uiState.isInterruptMode,
                                                    onSelect = { viewModel.setInterruptMode(it) }
                                                )
                                            }
                                        }

                                        if (isInstalled) {
                                            ResourceSettingSection(
                                                playMode = resource?.playMode ?: PlayMode.OVERLAY,
                                                orderMode = resource?.orderMode ?: OrderMode.ORDER,
                                                loopMode = resource?.loopMode ?: LoopMode.SINGLE,
                                                onPlayModeChange = { viewModel.updatePlayMode(it) },
                                                onOrderModeChange = { viewModel.updateOrderMode(it) },
                                                onLoopModeChange = { viewModel.updateLoopMode(it) }
                                            )
                                        }

                                        audioList.forEachIndexed { index, audioItem ->
                                            AudioItemRow(
                                                name = audioItem.name,
                                                duration = audioItem.duration,
                                                isPlaying = index in uiState.playingIndices,
                                                isLoading = index in uiState.loadingAudioIndices,
                                                isFirst = index == 0,
                                                onClick = { viewModel.togglePlay(index) }
                                            )
                                        }
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(88.dp)) }
                        }
                    }
                }
            }

        }

        DetailStopButton(
            isVisible = uiState.isAudioPlaying,
            bottomBarVisible = showBottomBar,
            onStop = { viewModel.stopAudio() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 右上角菜单（仅已安装资源显示）
        val localResource = uiState.resource
        if (localResource != null) {
            ContextMenu(
                expanded = showDetailMenu,
                onDismiss = { showDetailMenu = false },
                items = listOf(
                    ContextMenuItem(
                        label = stringResource(R.string.edit_resource),
                        onClick = {
                            showDetailMenu = false
                            onNavigateToEdit(localResource.id)
                        }
                    ),
                    ContextMenuItem(
                        label = stringResource(R.string.export_package),
                        onClick = {
                            showDetailMenu = false
                            personalVM.exportResource(localResource.id)
                        }
                    ),
                    ContextMenuItem(
                        label = stringResource(R.string.add_to_desktop),
                        onClick = {
                            showDetailMenu = false
                            personalVM.addToDesktop(context, localResource)
                        }
                    )
                )
            )
        }

        showShortcutGuide?.let { res ->
            ShortcutPermissionSheet(
                resource = res,
                onDismiss = { showShortcutGuide = null },
                onGoToSettings = {
                    ShortcutPermissionChecker.openShortcutSettings(context)
                    showShortcutGuide = null
                },
                onTryAnyway = {
                    personalVM.addToDesktopAnyway(context, res)
                    showShortcutGuide = null
                }
            )
        }
    }
}