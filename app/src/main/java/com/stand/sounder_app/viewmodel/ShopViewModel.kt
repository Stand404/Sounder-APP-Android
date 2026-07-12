package com.stand.sounder_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.R
import com.stand.sounder_app.data.download.DownloadState
import com.stand.sounder_app.data.model.RemoteResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ShopUiState(
    val resources: List<RemoteResource> = emptyList(),
    val installedIds: Set<String> = emptySet(),
    /** 存在未完成 .download 记录（可继续安装）的资源 */
    val pendingIds: Set<String> = emptySet(),
    val downloadStates: Map<String, DownloadState> = emptyMap(),
    val isLoading: Boolean = false,
    /** 下拉刷新专用标志，与 isLoading 解耦，避免初始骨架屏切回的问题 */
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

class ShopViewModel : ViewModel() {

    private val repository = MyApp.instance.resourceRepository
    private val downloadManager = MyApp.instance.downloadManager

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 20

    init {
        // 监听本地安装状态变化，排除正在下载中的资源，以及存在未完成下载记录（可继续）的资源
        viewModelScope.launch {
            repository.getAllLocalResources().collect { localList ->
                val dbIds = localList.map { it.id }.toSet()
                val activeDownloads = downloadManager.getActiveDownloadIds()
                val filesDir = MyApp.instance.filesDir
                val pending = dbIds.filter { downloadManager.hasPendingDownload(it, filesDir) }.toSet()
                _uiState.value = _uiState.value.copy(
                    installedIds = (dbIds - activeDownloads) - pending,
                    pendingIds = pending
                )
            }
        }

        // 监听全局下载状态变化，实时更新 UI
        viewModelScope.launch {
            downloadManager.stateChanges.collect { state ->
                val current = _uiState.value
                // 只关注当前列表中的资源
                if (current.resources.any { it.id == state.resourceId }) {
                    _uiState.value = current.copy(
                        downloadStates = current.downloadStates + (state.resourceId to state)
                    )
                    // 下载完成时更新 installedIds（从 Room Flow 自动触发）
                }
            }
        }

        loadResources()
    }

    fun loadResources() {
        viewModelScope.launch {
            val isRefresh = _uiState.value.resources.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isRefreshing = isRefresh,
                error = null
            )
            currentPage = 1

            val result = repository.getRemoteResourceList(currentPage, pageSize)
            result.fold(
                onSuccess = { list ->
                    // 进入页面时从全局下载状态快照同步进度/状态，保证正在下载或暂停的资源立即显示
                    val seededStates = downloadManager.getAllStates().filterKeys { id ->
                        list.any { it.id == id }
                    }
                    _uiState.value = _uiState.value.copy(
                        resources = list,
                        isLoading = false,
                        isRefreshing = false,
                        hasMore = list.size >= pageSize,
                        downloadStates = seededStates
                    )
                    // 列表加载完成后预加载前 10 项图标，减少滑动时的白块等待
                    preloadIcons(list.take(10))
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: MyApp.instance.getString(R.string.load_failed)
                    )
                }
            )
        }
    }

    /**
     * 预加载资源图标，让 Glide 提前缓存图片到磁盘/内存，
     * 当 LazyColumn 渲染卡片时图片已就绪，瞬间显示。
     */
    private suspend fun preloadIcons(list: List<RemoteResource>) = withContext(Dispatchers.IO) {
        val app = MyApp.instance
        list.forEach { resource ->
            if (resource.icon.isNotEmpty()) {
                try {
                    Glide.with(app)
                        .load(resource.icon)
                        .preload()
                } catch (_: Exception) {
                    // 单张预加载失败不影响整体
                }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            currentPage++

            val result = repository.getRemoteResourceList(currentPage, pageSize)
            result.fold(
                onSuccess = { list ->
                    val seededStates = _uiState.value.downloadStates + downloadManager.getAllStates()
                        .filterKeys { id -> list.any { it.id == id } }
                    _uiState.value = _uiState.value.copy(
                        resources = _uiState.value.resources + list,
                        isLoadingMore = false,
                        hasMore = list.size >= pageSize,
                        downloadStates = seededStates
                    )
                },
                onFailure = { error ->
                    currentPage--
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = error.message ?: MyApp.instance.getString(R.string.load_failed)
                    )
                }
            )
        }
    }

    /** 切换安装/暂停/继续（统一委托给全局 DownloadManager，下载在应用级作用域中持续） */
    fun toggleInstall(remoteResource: RemoteResource) {
        downloadManager.toggleDownload(remoteResource.id, MyApp.instance.filesDir)
    }
}
