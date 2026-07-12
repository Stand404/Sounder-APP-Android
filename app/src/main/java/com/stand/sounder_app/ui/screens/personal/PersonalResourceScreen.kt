package com.stand.sounder_app.ui.screens.personal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.painterResource
import com.stand.sounder_app.ui.components.ContextMenu
import com.stand.sounder_app.ui.components.ContextMenuItem
import com.stand.sounder_app.ui.components.EmptyState
import com.stand.sounder_app.ui.components.IosActionSheet
import com.stand.sounder_app.ui.components.IosActionSheetItem
import com.stand.sounder_app.ui.components.LoadingSkeleton
import com.stand.sounder_app.ui.components.ResourceCard
import com.stand.sounder_app.ui.components.SearchBox
import com.stand.sounder_app.R
import com.stand.sounder_app.data.model.Resource
import com.stand.sounder_app.ui.components.ShortcutPermissionSheet
import com.stand.sounder_app.util.ShortcutPermissionChecker
import com.stand.sounder_app.viewmodel.PersonalResourceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalResourceScreen(
    onResourceClick: (String) -> Unit,
    onEditResource: (String) -> Unit,
    onGoToShop: () -> Unit = {},
    /** 创建或导入资源后导航到编辑页 */
    onNavigateToEdit: (String) -> Unit = {},
    viewModel: PersonalResourceViewModel = viewModel()
) {
    val resources by viewModel.resources.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val resumableIds by viewModel.resumableIds.collectAsState()
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showShortcutGuide by remember { mutableStateOf<Resource?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 导入资源包 — 文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importResourceFromUri(context, it) { _ -> }
        }
    }

    // 按搜索词过滤
    val filteredResources = remember(resources, searchQuery) {
        if (searchQuery.isBlank()) resources
        else resources.filter { r ->
            r.displayName.contains(searchQuery, ignoreCase = true) ||
                    r.name.contains(searchQuery, ignoreCase = true) ||
                    r.description.contains(searchQuery, ignoreCase = true)
        }
    }

    // 监听删除结果消息
    LaunchedEffect(Unit) {
        viewModel.deleteMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // 监听操作轻提示
    LaunchedEffect(Unit) {
        viewModel.toast.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // 监听「创建桌面快捷方式」被厂商权限拒绝，弹出引导去设置
    LaunchedEffect(Unit) {
        viewModel.shortcutPermissionGuide.collectLatest { resource ->
            showShortcutGuide = resource
        }
    }

    // 导出文件选择器
    val scope = rememberCoroutineScope()
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
    // 监听导出完成，弹出系统保存对话框
    LaunchedEffect(Unit) {
        viewModel.shareRequest.collectLatest { zipFile ->
            pendingExportZip = zipFile
            exportLauncher.launch("${zipFile.nameWithoutExtension}.zip")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 72.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PersonalResourceHeader()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBox(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = stringResource(R.string.search_hint),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAddSheet = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_resource_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        LoadingSkeleton(
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    resources.isEmpty() -> {
                        EmptyState(
                            modifier = Modifier.padding(bottom = 72.dp),
                            title = stringResource(R.string.no_resources_yet),
                            subtitle = stringResource(R.string.go_shop_to_install),
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_music),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            action = {
                                Button(onClick = onGoToShop) {
                                    Text(stringResource(R.string.go_to_shop))
                                }
                            }
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 72.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item { Spacer(Modifier.height(4.dp)) }

                            if (filteredResources.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.no_match),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(filteredResources, key = { it.id }) { resource ->
                                    var showMenu by remember { mutableStateOf(false) }

                                    Box {
                                        ResourceCard(
                                            resource = resource,
                                            onClick = { onResourceClick(resource.id) },
                                            onLongClick = { showMenu = true },
                                            resumable = resource.id in resumableIds
                                        )

                                        ContextMenu(
                                            expanded = showMenu,
                                            onDismiss = { showMenu = false },
                                            items = listOf(
                                                ContextMenuItem(
                                                    label = stringResource(R.string.view_detail),
                                                    onClick = { onResourceClick(resource.id) }
                                                ),
                                                ContextMenuItem(
                                                    label = stringResource(R.string.edit_resource),
                                                    onClick = { onEditResource(resource.id) }
                                                ),
                                                ContextMenuItem(
                                                    label = stringResource(R.string.clone_resource),
                                                    onClick = { viewModel.cloneResource(resource.id) }
                                                ),
                                                ContextMenuItem(
                                                    label = stringResource(R.string.export_package),
                                                    onClick = { viewModel.exportResource(resource.id) }
                                                ),
                                                ContextMenuItem(
                                                    label = stringResource(R.string.add_to_desktop),
                                                    onClick = { viewModel.addToDesktop(context, resource) }
                                                ),
                                                ContextMenuItem(
                                                    label = stringResource(R.string.delete),
                                                    onClick = {
                                                        pendingDeleteId = resource.id
                                                        showDeleteDialog = true
                                                    },
                                                    isDestructive = true
                                                )
                                            )
                                        )
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }

        // 删除确认
        IosActionSheet(
            show = showDeleteDialog,
            onDismiss = { showDeleteDialog = false },
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            items = listOf(
                IosActionSheetItem(
                    label = stringResource(R.string.delete),
                    onClick = {
                        // 读取独立的 pendingDeleteId（不会被 onDismiss 清空），再清空状态
                        val idToDelete = pendingDeleteId
                        showDeleteDialog = false
                        pendingDeleteId = null
                        idToDelete?.let { viewModel.deleteResource(it) }
                    },
                    isDestructive = true
                )
            )
        )

        // 添加资源
        IosActionSheet(
            show = showAddSheet,
            onDismiss = { showAddSheet = false },
            items = listOf(
                IosActionSheetItem(
                    label = stringResource(R.string.create_new_resource),
                    onClick = {
                        showAddSheet = false
                        viewModel.createResource { newId -> onNavigateToEdit(newId) }
                    },
                    isBold = false
                ),
                IosActionSheetItem(
                    label = stringResource(R.string.import_package),
                    onClick = {
                        showAddSheet = false
                        importLauncher.launch(arrayOf("application/zip"))
                    },
                    isBold = false
                )
            )
        )

        showShortcutGuide?.let { res ->
            ShortcutPermissionSheet(
                resource = res,
                onDismiss = { showShortcutGuide = null },
                onGoToSettings = {
                    ShortcutPermissionChecker.openShortcutSettings(context)
                    showShortcutGuide = null
                },
                onTryAnyway = {
                    viewModel.addToDesktopAnyway(context, res)
                    showShortcutGuide = null
                }
            )
        }
    }
}