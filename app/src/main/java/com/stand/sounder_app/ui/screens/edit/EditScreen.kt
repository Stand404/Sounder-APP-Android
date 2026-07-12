package com.stand.sounder_app.ui.screens.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.stand.sounder_app.R
import com.stand.sounder_app.ui.components.LoadingSkeleton
import com.stand.sounder_app.ui.theme.AccentBlue
import com.stand.sounder_app.util.formatAudioDuration
import com.stand.sounder_app.viewmodel.EditViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun EditScreen(
    resourceId: String,
    onBack: () -> Unit,
    viewModel: EditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(resourceId) {
        viewModel.loadResource(resourceId)
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
            EditHeader(
                onBack = onBack
            )
            if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                LoadingSkeleton(modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            val listState = rememberLazyListState()
            var draggingItemId by remember { mutableStateOf<String?>(null) }
            var dragAccumulator by remember { mutableFloatStateOf(0f) }
            var dragOffset by remember { mutableFloatStateOf(0f) }
            var itemHeightPx by remember { mutableFloatStateOf(0f) }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 72.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // ===== 基本信息区块 =====
                item {
                    BasicInfoSection(
                        icon = uiState.icon,
                        iconVersion = uiState.iconVersion,
                        displayName = uiState.displayName,
                        editName = uiState.editName,
                        description = uiState.description,
                        onIconClick = { viewModel.openIconPicker() },
                        onDisplayNameChange = { viewModel.updateDisplayName(it) },
                        onEditNameChange = { viewModel.updateEditName(it) },
                        onDescriptionChange = { viewModel.updateDescription(it) },
                        focusManager = focusManager
                    )
                }

                // ===== 音频列表标题栏 =====
                item {
                    AudioListHeader(
                        count = uiState.audioList.size,
                        selectedCount = uiState.selectedAudioIndices.size,
                        isAllSelected = uiState.isAllSelected,
                        onToggleSelectAll = { viewModel.toggleSelectAllAudios() },
                        onDeleteSelected = {
                            viewModel.removeSelectedAudios()
                            focusManager.clearFocus()
                        },
                        onAddAudio = {
                            viewModel.openAudioPicker()
                            focusManager.clearFocus()
                        }
                    )
                }

                // ===== 音频列表项 =====
                if (uiState.audioList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无音频，点击上方按钮添加",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.audioList,
                        key = { _, item -> item.id }
                    ) { index, audioItem ->
                        val isDragging = draggingItemId == audioItem.id
                        AudioEditRow(
                            audioItem = audioItem,
                            isSelected = index in uiState.selectedAudioIndices,
                            isPlaying = index == uiState.playingIndex,
                            isDragging = isDragging,
                            onToggleSelect = { viewModel.toggleAudioSelection(index) },
                            onTogglePlay = { viewModel.togglePreview(index) },
                            onNameChange = { viewModel.updateAudioName(index, it) },
                            modifier = Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffset else 0f
                                    scaleX = if (isDragging) 1.02f else 1f
                                    scaleY = if (isDragging) 1.02f else 1f
                                }
                                .onGloballyPositioned { coords ->
                                    if (itemHeightPx <= 0f) {
                                        itemHeightPx = coords.size.height.toFloat()
                                    }
                                }
                                .pointerInput(audioItem.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingItemId = audioItem.id
                                            dragAccumulator = 0f
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val list = viewModel.uiState.value.audioList
                                            val currentIndex =
                                                list.indexOfFirst { it.id == audioItem.id }
                                            if (currentIndex < 0) return@detectDragGesturesAfterLongPress
                                            dragAccumulator += dragAmount.y
                                            dragOffset += dragAmount.y
                                            if (itemHeightPx > 0f && dragAccumulator > itemHeightPx && currentIndex < list.lastIndex) {
                                                viewModel.moveAudioItem(currentIndex, currentIndex + 1)
                                                dragAccumulator -= itemHeightPx
                                                dragOffset -= itemHeightPx
                                            } else if (itemHeightPx > 0f && dragAccumulator < -itemHeightPx && currentIndex > 0) {
                                                viewModel.moveAudioItem(currentIndex, currentIndex - 1)
                                                dragAccumulator += itemHeightPx
                                                dragOffset += itemHeightPx
                                            }
                                        },
                                        onDragEnd = { draggingItemId = null },
                                        onDragCancel = { draggingItemId = null }
                                    )
                                }
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
    }

    // ===== 图标选择弹窗 =====
    val iconFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.pickIconFromUri(it) }
    }

    if (uiState.showIconPicker) {
        IconPickerSheet(
            currentIcon = uiState.icon,
            options = uiState.installedIcons,
            onSelectIcon = { viewModel.updateIcon(it) },
            onPickFromFile = { iconFilePickerLauncher.launch("image/*") },
            onDismiss = { viewModel.closeIconPicker() }
        )
    }

    // ===== 音频选择弹窗 =====
    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            viewModel.addAudioFromUri(uri)
        }
    }

    if (uiState.showAudioPicker) {
        AudioPickerSheet(
            searchText = uiState.pickerSearchText,
            items = uiState.pickerAudioItems,
            selectedIds = uiState.selectedPickerItems,
            onSearchChange = { viewModel.updatePickerSearch(it) },
            onToggleItem = { viewModel.togglePickerItem(it) },
            onPickFromFile = { audioFilePickerLauncher.launch("audio/*") },
            onConfirm = { viewModel.confirmPickerSelection() },
            onDismiss = { viewModel.cancelPicker() }
        )
    }
}

// ===== 基本信息区块 =====
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun BasicInfoSection(
    icon: String,
    iconVersion: Long = 0L,
    displayName: String,
    editName: String,
    description: String,
    onIconClick: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onEditNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    focusManager: FocusManager
) {
    val fieldShape = RoundedCornerShape(16.dp)

    Column(modifier = Modifier.padding(start = 16.dp, bottom = 16.dp, end = 16.dp)) {
            Text(
                text = "基本信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 图标 + 编辑字段
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // 图标（可点击打开选择器）
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onIconClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon.isNotEmpty()) {
                        // 用 iconVersion 作为缓存签名，避免同一路径(icon.png)内容变化后仍显示旧图
                        val iconSignature = remember(icon, iconVersion) {
                            "$icon-$iconVersion"
                        }
                        GlideImage(
                            model = icon,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        ) {
                            it.signature(com.bumptech.glide.signature.ObjectKey(iconSignature))
                                .skipMemoryCache(true)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .align(Alignment.BottomCenter),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("更换", fontSize = 10.sp, color = Color.White)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.ic_broken_image),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                            Text("设置图标", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 编辑字段 —— 每个输入框单独一行，SearchBox 风格
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 资源名称
                    EditSearchField(
                        value = displayName,
                        onValueChange = onDisplayNameChange,
                        placeholder = "资源名称",
                        shape = fieldShape,
                        singleLine = false,
                        minLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // 简称
                    EditSearchField(
                        value = editName,
                        onValueChange = onEditNameChange,
                        placeholder = "简称",
                        shape = fieldShape,
                        singleLine = false,
                        minLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // 资源描述
                    EditSearchField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        placeholder = "资源描述",
                        shape = fieldShape,
                        singleLine = false,
                        minLines = 2,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }
        }
}

@Composable
private fun EditSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    shape: RoundedCornerShape,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = shape
            ),
        singleLine = singleLine,
        minLines = minLines,
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        }
    )
}

// ===== 拖拽手柄（6 点样式，纯装饰，拖拽由整行长按手势触发） =====
@Composable
private fun DragHandleIcon() {
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Column(
        modifier = Modifier
            .width(20.dp)
            .padding(start = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .background(dotColor, CircleShape)
                    )
                }
            }
        }
    }
}

// ===== 音频编辑行 =====
@Composable
private fun AudioEditRow(
    audioItem: com.stand.sounder_app.data.model.AudioItem,
    isSelected: Boolean,
    isPlaying: Boolean,
    isDragging: Boolean = false,
    onToggleSelect: () -> Unit,
    onTogglePlay: () -> Unit,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 6.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 拖拽手柄（长按整行可拖拽排序）
            DragHandleIcon()

            Spacer(modifier = Modifier.width(8.dp))

            // 勾选框
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(20.dp),
                colors = CheckboxDefaults.colors(checkedColor = AccentBlue)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 名称（内联编辑，自定义紧凑样式，无边框）
            BasicTextField(
                value = audioItem.name,
                onValueChange = onNameChange,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp)
                    ),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        innerTextField()
                    }
                }
            )

            // 时长
            if (audioItem.duration > 0) {
                Text(
                    text = formatAudioDuration(audioItem.duration),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // 播放/暂停
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = if (isPlaying) painterResource(R.drawable.ic_pause) else painterResource(R.drawable.ic_play_arrow),
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = if (isPlaying) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}