package com.stand.sounder_app.viewmodel

import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.data.model.AudioItem
import com.stand.sounder_app.data.model.PickerAudioItem
import com.stand.sounder_app.data.model.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import androidx.core.graphics.scale
import androidx.core.net.toUri

data class EditUiState(
    val resource: Resource? = null,
    val isLoading: Boolean = true,
    val displayName: String = "",
    val editName: String = "",
    val description: String = "",
    val icon: String = "",
    /** 图标版本号，每次更换图标时更新，用于强制刷新图片缓存（同一路径 icon.png 内容变化时） */
    val iconVersion: Long = 0L,
    val audioList: List<AudioItem> = emptyList(),
    val selectedAudioIndices: Set<Int> = emptySet(),
    val isAllSelected: Boolean = false,
    val playingIndex: Int = -1,
    val showIconPicker: Boolean = false,
    val showAudioPicker: Boolean = false,
    val pickerSearchText: String = "",
    val pickerAudioItems: List<PickerAudioItem> = emptyList(),
    val selectedPickerItems: Set<String> = emptySet(),
    val installedIcons: List<IconOption> = emptyList()
)

/** 图标拾取项 —— 已安装资源提供的图标 */
data class IconOption(
    val icon: String,
    val displayName: String
)

class EditViewModel : ViewModel() {

    private val repository = MyApp.instance.resourceRepository

    /** 预览专用的单例 MediaPlayer */
    private var previewPlayer: MediaPlayer? = null

    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    fun loadResource(resourceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val resource = repository.getLocalResourceById(resourceId)
            if (resource != null) {
                val baseAudioList = sanitizeAudioIds(resource.audioList)
                _uiState.value = _uiState.value.copy(
                    resource = resource,
                    isLoading = false,
                    displayName = resource.displayName,
                    editName = resource.name,
                    description = resource.description,
                    icon = resource.icon,
                    audioList = baseAudioList,
                    pickerAudioItems = baseAudioList.map {
                        PickerAudioItem(
                            id = "${resource.id}::${it.id}", name = it.name, src = it.src,
                            duration = it.duration, sourceName = resource.displayName
                        )
                    }
                )
            }
        }
    }

    /** 规整音频 id：空白或重复的 id 会被替换为唯一非空值，避免 Lazy 列表键冲突 */
    private fun sanitizeAudioIds(list: List<AudioItem>): List<AudioItem> {
        val seen = mutableSetOf<String>()
        return list.map { item ->
            var id = item.id
            while (id.isBlank() || id in seen) {
                id = "audio_${UUID.randomUUID()}"
            }
            seen.add(id)
            if (id == item.id) item else item.copy(id = id)
        }
    }

    fun updateDisplayName(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name)
        scheduleAutoSave()
    }

    fun updateEditName(name: String) {
        _uiState.value = _uiState.value.copy(editName = name)
        scheduleAutoSave()
    }

    fun updateDescription(desc: String) {
        _uiState.value = _uiState.value.copy(description = desc)
        scheduleAutoSave()
    }

    fun updateIcon(icon: String) {
        _uiState.value = _uiState.value.copy(
            icon = icon,
            iconVersion = System.currentTimeMillis(),
            showIconPicker = false
        )
        scheduleAutoSave()
    }

    /**
     * 从系统文件选择器 Uri 选取图标，自动裁剪为 150×150 正方形并保存到资源自身目录
     */
    fun pickIconFromUri(uri: Uri) {
        val resourceId = _uiState.value.resource?.id ?: return
        viewModelScope.launch {
            try {
                val context = MyApp.instance
                val destDir = File(context.filesDir, "resources/$resourceId")
                destDir.mkdirs()
                val destFile = File(destDir, "icon.png")

                // 读取原始 Bitmap
                val inputStream = context.contentResolver.openInputStream(uri)
                val original = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (original != null) {
                    // 中心裁剪为 150×150 正方形
                    val size = 150
                    val cropSize = minOf(original.width, original.height)
                    val x = (original.width - cropSize) / 2
                    val y = (original.height - cropSize) / 2
                    val cropped = android.graphics.Bitmap.createBitmap(original, x, y, cropSize, cropSize)
                    val resized = cropped.scale(size, size)

                    destFile.outputStream().use { output ->
                        resized.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
                    }

                    if (cropped !== original) cropped.recycle()
                    resized.recycle()
                    original.recycle()
                }

                // 使用已保存的文件路径作为图标
                updateIcon(destFile.absolutePath)
            } catch (_: Exception) {
                // 处理失败时回退：直接用源 URI
                updateIcon(uri.toString())
            }
        }
    }

    /**
     * 从已安装图标库中选择图标，复制到资源自身目录后使用（不污染 installed_icons）
     */
    fun pickIconFromInstalled(installedPath: String) {
        val resourceId = _uiState.value.resource?.id ?: return
        viewModelScope.launch {
            try {
                val context = MyApp.instance
                val destDir = File(context.filesDir, "resources/$resourceId")
                destDir.mkdirs()
                val source = File(installedPath)
                val extension = source.extension.takeIf { it.isNotBlank() } ?: "png"
                val destFile = File(destDir, "icon.$extension")
                source.copyTo(destFile, overwrite = true)
                updateIcon(destFile.absolutePath)
            } catch (_: Exception) {
                // 回退：直接用原路径
                updateIcon(installedPath)
            }
        }
    }

    // ===== 音频选择/多选 =====

    fun toggleAudioSelection(index: Int) {
        val current = _uiState.value.selectedAudioIndices
        val updated = if (index in current) current - index else current + index
        _uiState.value = _uiState.value.copy(
            selectedAudioIndices = updated,
            isAllSelected = updated.size == _uiState.value.audioList.size
        )
    }

    fun toggleSelectAllAudios() {
        val allIndices = _uiState.value.audioList.indices.toSet()
        val isAll = _uiState.value.isAllSelected
        _uiState.value = _uiState.value.copy(
            selectedAudioIndices = if (isAll) emptySet() else allIndices,
            isAllSelected = !isAll
        )
    }

    fun removeSelectedAudios() {
        val sorted = _uiState.value.selectedAudioIndices.sortedDescending()
        val list = _uiState.value.audioList.toMutableList()
        sorted.forEach { index ->
            if (index in list.indices) list.removeAt(index)
        }
        _uiState.value = _uiState.value.copy(
            audioList = list,
            selectedAudioIndices = emptySet(),
            isAllSelected = false
        )
        scheduleAutoSave()
    }

    // ===== 音频预览 =====

    fun togglePreview(index: Int) {
        val state = _uiState.value
        val audioList = state.audioList
        if (index < 0 || index >= audioList.size) return

        // 点击正在播放的项 → 停止
        if (state.playingIndex == index) {
            previewPlayer?.let {
                try { it.stop(); it.reset() } catch (_: Exception) { }
            }
            _uiState.value = state.copy(playingIndex = -1)
            return
        }

        // 播放新的音频：复用已有实例，reset 后重新设置
        val player = previewPlayer ?: MediaPlayer().also { previewPlayer = it }
        try {
            player.reset()
            val audioItem = audioList[index]
            val uri = if (audioItem.src.startsWith("http")) {
                audioItem.src.toUri()
            } else {
                Uri.fromFile(File(audioItem.src))
            }
            player.setDataSource(MyApp.instance, uri)
            player.setOnPreparedListener { mp ->
                _uiState.value = _uiState.value.copy(playingIndex = index)
                mp.start()
            }
            player.setOnCompletionListener {
                _uiState.value = _uiState.value.copy(playingIndex = -1)
            }
            player.setOnErrorListener { _, _, _ ->
                _uiState.value = _uiState.value.copy(playingIndex = -1)
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(playingIndex = -1)
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewPlayer?.let {
            try { it.release() } catch (_: Exception) { }
            previewPlayer = null
        }
    }

    // ===== 音频内联编辑 =====

    fun updateAudioName(index: Int, name: String) {
        val list = _uiState.value.audioList.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(name = name)
            _uiState.value = _uiState.value.copy(audioList = list)
            scheduleAutoSave()
        }
    }

    // ===== 图标选择 =====

    fun openIconPicker() {
        viewModelScope.launch {
            val currentId = _uiState.value.resource?.id
            val iconsDir = File(MyApp.instance.filesDir, "installed_icons")
            val icons = if (iconsDir.isDirectory) {
                iconsDir.listFiles()
                    ?.filter { it.isFile && it.nameWithoutExtension != currentId }
                    ?.mapNotNull { file ->
                        IconOption(icon = file.absolutePath, displayName = file.nameWithoutExtension)
                    } ?: emptyList()
            } else emptyList()
            _uiState.value = _uiState.value.copy(
                showIconPicker = true,
                installedIcons = icons
            )
        }
    }

    fun closeIconPicker() {
        _uiState.value = _uiState.value.copy(showIconPicker = false)
    }

    // ===== 音频选择器 =====

    fun openAudioPicker() {
        viewModelScope.launch {
            val allLocal = repository.getAllLocalResources().first().flatMap { resource ->
                resource.audioList.mapIndexed { index, audio ->
                    // 拼接 index 保证 id 唯一（旧数据中 audio.id 可能为空导致键冲突）
                    PickerAudioItem(
                        id = "${resource.id}::${audio.id}::$index",
                        name = audio.name,
                        src = audio.src,
                        duration = audio.duration,
                        sourceName = resource.displayName
                    )
                }
            }
            _uiState.value = _uiState.value.copy(
                showAudioPicker = true,
                pickerAudioItems = allLocal,
                pickerSearchText = "",
                selectedPickerItems = emptySet()
            )
        }
    }

    fun updatePickerSearch(text: String) {
        _uiState.value = _uiState.value.copy(pickerSearchText = text)
    }

    fun togglePickerItem(audioId: String) {
        val current = _uiState.value.selectedPickerItems
        _uiState.value = _uiState.value.copy(
            selectedPickerItems = if (audioId in current) current - audioId else current + audioId
        )
    }

    fun confirmPickerSelection() {
        val selectedIds = _uiState.value.selectedPickerItems
        val existingIds = _uiState.value.audioList.map { it.id }.toSet()
        val itemsToAdd = _uiState.value.pickerAudioItems
            .filter { it.id in selectedIds && it.id !in existingIds }
            .map { AudioItem(id = it.id, name = it.name, src = it.src, duration = it.duration) }
        _uiState.value = _uiState.value.copy(
            audioList = _uiState.value.audioList + itemsToAdd,
            showAudioPicker = false
        )
        scheduleAutoSave()
    }

    fun cancelPicker() {
        _uiState.value = _uiState.value.copy(showAudioPicker = false)
    }

    // ===== 排序 =====

    fun moveAudioItem(from: Int, to: Int) {
        val list = _uiState.value.audioList.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
            _uiState.value = _uiState.value.copy(audioList = list)
            scheduleAutoSave()
        }
    }

    // ===== 保存 =====

    /** 自动保存：修改后延迟一小段时间再写盘，避免每次输入都写入 */
    private var autoSaveJob: Job? = null

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(400)
            performSave()
        }
    }

    private suspend fun performSave() {
        val state = _uiState.value
        val resource = state.resource ?: return
        // 重新计算资源总大小（图标 + 所有音频文件）
        val totalSize = withContext(Dispatchers.IO) {
            val iconFile = File(state.icon)
            val iconSize = if (iconFile.exists()) iconFile.length() else 0L
            val audioSize = state.audioList.sumOf { audio ->
                val f = File(audio.src)
                if (f.exists()) f.length() else 0L
            }
            iconSize + audioSize
        }
        repository.saveResource(
            resource.copy(
                name = state.editName,
                displayName = state.displayName,
                description = state.description,
                icon = state.icon,
                audioList = state.audioList,
                size = totalSize
            )
        )
    }

    /**
     * 从系统文件选择器 Uri 添加音频（参照 Win AddAudioFromFileAsync）
     */
    fun addAudioFromUri(uri: Uri) {
        val resourceId = _uiState.value.resource?.id ?: return
        viewModelScope.launch {
            try {
                val context = MyApp.instance
                val audioDir = File(context.filesDir, "resources/$resourceId/audio")
                audioDir.mkdirs()

                val audioId = "audio_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
                val extension = getExtensionFromUri(context, uri)
                val destFile = File(audioDir, "$audioId$extension")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                var durationMs = 0L
                try {
                    val mediaMetadataRetriever = android.media.MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(context, uri)
                    val durationStr = mediaMetadataRetriever.extractMetadata(
                        android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                    )
                    durationMs = durationStr?.toLongOrNull() ?: 0L
                    mediaMetadataRetriever.release()
                } catch (_: Exception) {
                }

                val name = destFile.nameWithoutExtension
                val newItem = AudioItem(
                    id = audioId,
                    name = name,
                    src = destFile.absolutePath,
                    duration = durationMs
                )

                _uiState.value = _uiState.value.copy(
                    audioList = _uiState.value.audioList + newItem
                )
                scheduleAutoSave()
            } catch (_: Exception) {
            }
        }
    }

    private fun getExtensionFromUri(context: android.content.Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri) ?: return ".mp3"
        return when {
            mimeType.contains("mp3") -> ".mp3"
            mimeType.contains("wav") -> ".wav"
            mimeType.contains("ogg") -> ".ogg"
            mimeType.contains("aac") -> ".aac"
            mimeType.contains("flac") -> ".flac"
            mimeType.contains("wma") -> ".wma"
            else -> ".mp3"
        }
    }
}
