package com.stand.sounder_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.data.download.DownloadStatus
import com.stand.sounder_app.data.model.AudioItem
import com.stand.sounder_app.data.model.LoopMode
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode
import com.stand.sounder_app.data.model.RemoteResource
import com.stand.sounder_app.data.model.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.stand.sounder_app.R
import java.io.File

data class DetailUiState(
    val resource: Resource? = null,
    val remoteResource: RemoteResource? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isInstalling: Boolean = false,
    val isDownloadComplete: Boolean = false,
    val isInterruptMode: Boolean = false,
    val loadingAudioIndices: Set<Int> = emptySet(),
    /** 活跃的播放实例 ID 集合（每个实例对应一次点击播放） */
    val playingInstanceIds: Set<String> = emptySet(),
    /** 由 ViewModel 同步更新的播放索引集合（供 UI 使用） */
    val playingIndices: Set<Int> = emptySet(),
    val isAudioPlaying: Boolean = false,
    val currentAudioName: String = "",
    val downloadProgress: Float = 0f
)

class DetailViewModel : ViewModel() {

    private val repository = MyApp.instance.resourceRepository
    private val audioPlayer = MyApp.instance.audioPlayerManager
    private val downloadManager = MyApp.instance.downloadManager

    private var currentAudioList: List<AudioItem> = emptyList()

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    /** instanceId -> index 映射 */
    private val instanceIndexMap = mutableMapOf<String, Int>()

    private var resourceId: String = ""
    private var mode: String = ""

    /** 监听本地已安装资源变化，自动同步状态 */
    init {
        viewModelScope.launch {
            repository.getAllLocalResources().collect { localList ->
                val currentId = resourceId
                if (currentId.isEmpty()) return@collect
                val local = localList.firstOrNull { it.id == currentId }
                if (local != null) {
                    currentAudioList = local.audioList
                    // 正在安装中，或下载管理器显示活跃状态时，不标记完成
                    if (!_uiState.value.isInstalling && !downloadManager.isDownloadActive(currentId)) {
                        _uiState.value = _uiState.value.copy(
                            resource = local,
                            isDownloadComplete = true
                        )
                    }
                }
            }
        }

        // 监听全局下载状态变化，实时更新进度
        viewModelScope.launch {
            downloadManager.stateChanges.collect { state ->
                val currentId = resourceId
                if (currentId.isEmpty()) return@collect
                if (state.resourceId == currentId) {
                    when (state.status) {
                        DownloadStatus.DOWNLOADING -> {
                            _uiState.value = _uiState.value.copy(
                                isInstalling = true,
                                downloadProgress = state.progress
                            )
                        }
                        DownloadStatus.PAUSED -> {
                            _uiState.value = _uiState.value.copy(
                                isInstalling = false,
                                downloadProgress = state.progress
                            )
                        }
                        DownloadStatus.COMPLETED -> {
                            // Room DB 已在 installResourceWithLocalData 中写入，
                            // 但 Room Flow 可能在 isInstalling = true 时已提前消费，
                            // 此处主动读取本地资源并更新 UI。
                            _uiState.value = _uiState.value.copy(
                                isInstalling = false,
                                downloadProgress = 1f
                            )
                            viewModelScope.launch {
                                repository.getLocalResourceById(currentId)?.let { local ->
                                    currentAudioList = local.audioList
                                    _uiState.value = _uiState.value.copy(
                                        resource = local,
                                        isDownloadComplete = true
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun loadResource(id: String, m: String) {
        resourceId = id
        mode = m
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            if (m == "local") {
                val local = repository.getLocalResourceById(id)
                if (local != null) {
                    currentAudioList = local.audioList
                    _uiState.value = _uiState.value.copy(
                        resource = local,
                        isLoading = false,
                        isDownloadComplete = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = MyApp.instance.getString(R.string.resource_not_found)
                    )
                }
            } else {
                val result = repository.getRemoteResourceDetail(id)
                result.fold(
                    onSuccess = { remote ->
                        currentAudioList = remote.audioList.map {
                            AudioItem(id = it.id, name = it.name, src = it.url, duration = it.duration)
                        }
                        val localResource = repository.getLocalResourceById(id)
                        // 下载管理器中有活跃状态 → 未完成，同步实时进度
                        val activeState = downloadManager.getDownloadState(id)
                        val isActive = activeState != null && activeState.status in setOf(
                            DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED
                        )
                        val isActuallyComplete = localResource != null && !isActive
                        _uiState.value = _uiState.value.copy(
                            remoteResource = remote,
                            resource = localResource,
                            isLoading = false,
                            isDownloadComplete = isActuallyComplete,
                            isInstalling = isActive && activeState.status == DownloadStatus.DOWNLOADING,
                            downloadProgress = activeState?.progress ?: 0f
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: MyApp.instance.getString(R.string.load_failed)
                        )
                    }
                )
            }
        }
    }

    /**
     * 切换安装/暂停/继续。统一委托给全局 DownloadManager：
     * 安装任务在应用级作用域中执行，离开详情页后下载仍会持续，
     * 进度与状态通过 stateChanges 实时同步回本 ViewModel。
     */
    fun toggleInstall() {
        val remote = _uiState.value.remoteResource ?: return
        val state = downloadManager.getDownloadState(remote.id)
        // 乐观更新，让底部按钮立即响应（真正的状态由 stateChanges 同步）
        if (state?.status == DownloadStatus.DOWNLOADING) {
            _uiState.value = _uiState.value.copy(isInstalling = false)
        } else {
            _uiState.value = _uiState.value.copy(isInstalling = true, downloadProgress = 0f)
        }
        // 传入详情页已加载的 remoteResource，避免 installResourceById 中重复请求远程详情
        downloadManager.toggleDownload(remote.id, MyApp.instance.filesDir, existingResource = remote)
    }

    fun togglePlay(index: Int) {
        val audioList = currentAudioList
        if (index < 0 || index >= audioList.size) return

        val state = _uiState.value
        val isInterrupt = state.isInterruptMode
        val currentMode = state.resource?.playMode ?: PlayMode.OVERLAY

        // 打断模式或替换模式：先停止所有（替换模式由 AudioPlayerManager.playReplace 内部 stopAll 停声音，
        // 但 stopAll 不触发 onFinish 回调，此处需同步清理 UI 状态，避免旧索引残留）
        if (isInterrupt || currentMode == PlayMode.REPLACE) {
            audioPlayer.stopAll()
            instanceIndexMap.clear()
            _uiState.value = _uiState.value.copy(
                playingInstanceIds = emptySet(),
                playingIndices = emptySet(),
                loadingAudioIndices = emptySet(),
                isAudioPlaying = false
            )
        }
        // 正常模式（叠加）：不停止任何已有播放，直接叠加

        // 显示加载状态（已播放项不再显示加载动画）
        val alreadyPlaying = index in state.playingIndices
        if (!alreadyPlaying) {
            _uiState.value = _uiState.value.copy(
                loadingAudioIndices = _uiState.value.loadingAudioIndices + index
            )
        }
        _uiState.value = _uiState.value.copy(isAudioPlaying = true)

        val currentOrder = state.resource?.orderMode ?: OrderMode.ORDER
        val currentLoop = state.resource?.loopMode ?: LoopMode.SINGLE

        viewModelScope.launch {
            // 将远程音频解析为本地缓存路径（已缓存直接使用，否则下载到缓存后使用）
            val resolvedList = resolveLocalAudioList(audioList)

            val displayName = _uiState.value.resource?.displayName
                ?: _uiState.value.remoteResource?.displayName ?: ""
            audioPlayer.setPlaySettings(currentMode, currentOrder, currentLoop)
            audioPlayer.play(
                audioList = resolvedList,
                resourceId = resourceId,
                displayName = displayName,
                startIndex = index,
                onPlaying = { instanceId ->
                    // MediaPlayer 异步准备完成开始播放
                    instanceIndexMap[instanceId] = index
                    val s = _uiState.value
                    _uiState.value = s.copy(
                        loadingAudioIndices = s.loadingAudioIndices - index,
                        playingInstanceIds = s.playingInstanceIds + instanceId,
                        playingIndices = instanceIndexMap.values.toSet(),
                        isAudioPlaying = true,
                        currentAudioName = audioList[index].name
                    )
                },
                onFinish = { instanceId ->
                    // 自然播放完成：仅移除该实例
                    instanceIndexMap.remove(instanceId)
                    val s = _uiState.value
                    val remaining = s.playingInstanceIds - instanceId
                    _uiState.value = s.copy(
                        playingInstanceIds = remaining,
                        playingIndices = instanceIndexMap.values.toSet(),
                        isAudioPlaying = remaining.isNotEmpty() || s.loadingAudioIndices.isNotEmpty()
                    )
                },
                onLoopIndexChanged = { idx ->
                    // 列表循环切歌时持久化索引，跨重启衔接
                    viewModelScope.launch {
                        repository.updateCurrentAudioIndex(resourceId, idx)
                        val r = _uiState.value.resource
                        if (r != null) {
                            _uiState.value = _uiState.value.copy(resource = r.copy(currentAudioIndex = idx))
                        }
                    }
                }
            )
        }
    }

    /**
     * 将音频列表中的远程 URL 解析为本地缓存路径。
     * - 若音频已在 audio_cache 中缓存，直接使用本地路径；
     * - 若未缓存，下载到缓存目录后使用本地路径；
     * - 下载失败或已是本地路径则保持原样。
     */
    private suspend fun resolveLocalAudioList(audioList: List<AudioItem>): List<AudioItem> {
        val cacheDir = File(MyApp.instance.filesDir, "audio_cache")
        cacheDir.mkdirs()
        return audioList.map { audio ->
            if (!audio.src.startsWith("http")) return@map audio
            val cacheKey = audio.src.hashCode().toUInt().toString(16)
            val cacheFile = File(cacheDir, cacheKey)
            if (cacheFile.exists()) {
                audio.copy(src = cacheFile.absolutePath)
            } else {
                val result = downloadManager.download(resourceId, audio.src, cacheFile)
                if (result.isSuccess) audio.copy(src = cacheFile.absolutePath) else audio
            }
        }
    }

    /** 停止当前资源的播放（浮动按钮调用） */
    fun stopAudio() {
        audioPlayer.stopResource(resourceId)
        instanceIndexMap.clear()
        _uiState.value = _uiState.value.copy(
            playingInstanceIds = emptySet(),
            playingIndices = emptySet(),
            loadingAudioIndices = emptySet(),
            isAudioPlaying = false,
            currentAudioName = ""
        )
    }

    fun setInterruptMode(interrupt: Boolean) {
        _uiState.value = _uiState.value.copy(isInterruptMode = interrupt)
    }

    fun updatePlayMode(mode: PlayMode) {
        val resource = _uiState.value.resource ?: return
        // 先同步更新 UI 状态，保证「循环」子选项立即出现（不依赖协程调度/DB 写入）
        _uiState.value = _uiState.value.copy(resource = resource.copy(playMode = mode))
        viewModelScope.launch {
            repository.updatePlayMode(resource.id, mode)
        }
    }

    fun updateOrderMode(mode: OrderMode) {
        val resource = _uiState.value.resource ?: return
        _uiState.value = _uiState.value.copy(resource = resource.copy(orderMode = mode))
        viewModelScope.launch {
            repository.updateOrderMode(resource.id, mode)
        }
    }

    fun updateLoopMode(mode: LoopMode) {
        val resource = _uiState.value.resource ?: return
        _uiState.value = _uiState.value.copy(resource = resource.copy(loopMode = mode))
        viewModelScope.launch {
            repository.updateLoopMode(resource.id, mode)
        }
    }
}
