package com.stand.sounder_app.data.download

import android.util.Log
import com.google.gson.Gson
import com.stand.sounder_app.data.model.AudioItem
import com.stand.sounder_app.data.model.RemoteAudioItem
import com.stand.sounder_app.data.model.RemoteResource
import com.stand.sounder_app.data.repository.ResourceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** 全局下载状态 */
enum class DownloadStatus { IDLE, DOWNLOADING, PAUSED, COMPLETED, FAILED }

/** 全局下载快照，供 UI 和 ViewModel 读取 */
data class DownloadState(
    val resourceId: String,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val progress: Float = 0f
)

data class DownloadProgress(
    val resourceId: String,
    val bytesWritten: Long = 0L,
    val totalBytes: Long = 0L,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val errorMessage: String = ""
)

/**
 * 断点续传记录（.download 文件序列化模型，参照 C# DownloadRecordData）。
 * 持久化每个已完成的音频项与图标状态，进程被杀后重入可恢复进度与已下载文件。
 */
data class DownloadRecord(
    val resourceId: String = "",
    val resourceName: String = "",
    val displayName: String = "",
    val description: String = "",
    val icon: String = "",
    val size: String = "",
    val publishDate: String = "",
    val totalAudioCount: Int = 0,
    val downloadedAudioItems: List<DownloadedAudioItem> = emptyList(),
    val iconDownloaded: Boolean = false,
    val lastUpdateTime: Long = 0L
)

/** 已完成的单个音频项（参照 C# DownloadedAudioItemData） */
data class DownloadedAudioItem(
    val id: String = "",
    val name: String = "",
    val src: String = "",
    val durationMs: Long = 0L,
    val orderIndex: Int = 0
)

private val gson = Gson()

class DownloadManager(
    private val cacheDir: File? = null,
    private val repository: ResourceRepository? = null
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                .build()
            chain.proceed(request)
        }
        .build()

    /**
     * 应用级协程作用域：安装任务运行于此，与任何页面的 ViewModel/Composable 生命周期解耦，
     * 因此即使从商店/搜索/详情页跳转到其它页面，下载也会继续在后台进行。
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("DownloadManager"))

    // 全局下载状态（跨页面共享）
    private val states = ConcurrentHashMap<String, DownloadState>()
    // 状态变更流（供 ViewModel 实时观察）
    private val _stateChanges = MutableSharedFlow<DownloadState>(extraBufferCapacity = 1)
    val stateChanges: SharedFlow<DownloadState> = _stateChanges.asSharedFlow()
    // 取消标志：true 表示取消/暂停当前下载
    private val cancelFlags = ConcurrentHashMap<String, AtomicBoolean>()
    // 启动守卫：避免同一资源被重复启动安装
    private val starting = ConcurrentHashMap<String, Boolean>()

    private val downloadProgress = ConcurrentHashMap<String, DownloadProgress>()
    private val listeners = ConcurrentHashMap<String, (DownloadProgress) -> Unit>()

    // ==================== 全局状态 API ====================

    /** 获取资源的全局下载状态 */
    fun getDownloadState(resourceId: String): DownloadState? = states[resourceId]

    /** 资源是否正在下载或已暂停（用于排除 Room Flow 中的错误"已安装"标识） */
    fun isDownloadActive(resourceId: String): Boolean {
        val state = states[resourceId] ?: return false
        return state.status in setOf(DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED)
    }

    /** 获取所有活跃下载的 ID（状态为 DOWNLOADING 或 PAUSED） */
    fun getActiveDownloadIds(): Set<String> {
        return states.filterValues { it.status in setOf(DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED) }.keys
    }

    /** 更新全局状态并发射到变更流 */
    private fun updateState(resourceId: String, status: DownloadStatus, progress: Float = 0f) {
        val newState = DownloadState(resourceId, status, progress)
        states[resourceId] = newState
        _stateChanges.tryEmit(newState)
    }

    /** 清理资源下载状态（删除资源时调用），并中止可能仍在后台运行的安装任务 */
    fun cleanupResource(resourceId: String) {
        // 先置位取消标志，让正在运行（应用级作用域内）的安装任务在安全点停止
        cancelFlags[resourceId]?.set(true)
        starting.remove(resourceId)
        cancelFlags.remove(resourceId)
        downloadProgress.remove(resourceId)
        listeners.remove(resourceId)
        states.remove(resourceId)
        Log.i("DownloadManager", "清理下载状态: id=$resourceId")
    }

    // ==================== 断点续传记录（.download，参照 C#） ====================

    /** .download 记录文件路径: filesDir/audio/{resourceId}/.download */
    private fun getDownloadRecordFile(resourceId: String, filesDir: File): File {
        return File(File(filesDir, "audio/$resourceId"), ".download")
    }

    /** 读取断点续传记录（进程重启后可恢复） */
    fun readDownloadRecord(resourceId: String, filesDir: File?): DownloadRecord? {
        val dir = filesDir ?: return null
        val file = getDownloadRecordFile(resourceId, dir)
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), DownloadRecord::class.java)
        } catch (e: Exception) {
            Log.e("DownloadManager", "读取 .download 失败(id=$resourceId): ${e.message}")
            null
        }
    }

    /** 写入断点续传记录（原子写：先写临时文件再重命名） */
    private fun saveDownloadRecord(resourceId: String, record: DownloadRecord, filesDir: File?) {
        if (filesDir == null) return
        val dir = File(filesDir, "audio/$resourceId")
        dir.mkdirs()
        val file = File(dir, ".download")
        val tmp = File(dir, ".download.tmp")
        try {
            tmp.writeText(gson.toJson(record))
            if (!tmp.renameTo(file)) {
                file.writeText(gson.toJson(record)) // 重命名失败则直接覆盖写
            }
        } catch (e: Exception) {
            Log.e("DownloadManager", "保存 .download 失败(id=$resourceId): ${e.message}")
        }
    }

    /** 下载完成后清理 .download 记录 */
    fun deleteDownloadRecord(resourceId: String, filesDir: File?) {
        if (filesDir == null) return
        val file = getDownloadRecordFile(resourceId, filesDir)
        if (file.exists()) file.delete()
    }

    /** 是否存在未完成的下载记录（供 UI 标记「可继续」） */
    fun hasPendingDownload(resourceId: String, filesDir: File?): Boolean {
        val dir = filesDir ?: return false
        return getDownloadRecordFile(resourceId, dir).exists()
    }

    /** 扫描所有未完成的下载记录，返回 resourceId 列表 */
    fun getPendingDownloads(filesDir: File?): List<String> {
        if (filesDir == null) return emptyList()
        val audioRoot = File(filesDir, "audio")
        if (!audioRoot.isDirectory) return emptyList()
        return audioRoot.listFiles { f -> f.isDirectory }?.mapNotNull { dir ->
            val rec = File(dir, ".download")
            if (rec.exists()) dir.name else null
        } ?: emptyList()
    }

    /** 将当前已完成音频项持久化到 .download（图标状态置否） */
    private fun persistAudioRecord(
        resourceId: String,
        filesDir: File,
        resource: RemoteResource?,
        completed: List<AudioItem>
    ) {
        if (resource == null) return
        val items = completed.map { a ->
            val orderIndex = resource.audioList.indexOfFirst { it.id == a.id }.coerceAtLeast(0)
            DownloadedAudioItem(
                id = a.id,
                name = a.name,
                src = a.src,
                durationMs = a.duration,
                orderIndex = orderIndex
            )
        }
        val record = DownloadRecord(
            resourceId = resource.id,
            resourceName = resource.name,
            displayName = resource.displayName,
            description = resource.description,
            icon = resource.icon,
            size = resource.size,
            publishDate = resource.publishDate,
            totalAudioCount = resource.audioList.size,
            downloadedAudioItems = items,
            iconDownloaded = false,
            lastUpdateTime = System.currentTimeMillis()
        )
        saveDownloadRecord(resourceId, record, filesDir)
    }

    /** 更新 .download 记录中的图标下载状态 */
    private fun markIconDownloaded(resourceId: String, filesDir: File, resource: RemoteResource?, downloaded: Boolean) {
        val existing = readDownloadRecord(resourceId, filesDir)
        val base = existing ?: buildBaseRecord(resourceId, resource) ?: return
        saveDownloadRecord(resourceId, base.copy(iconDownloaded = downloaded, lastUpdateTime = System.currentTimeMillis()), filesDir)
    }

    /** 仅含元数据的基线记录（无已完成音频时使用） */
    private fun buildBaseRecord(resourceId: String, resource: RemoteResource?): DownloadRecord? {
        if (resource == null) return null
        return DownloadRecord(
            resourceId = resource.id,
            resourceName = resource.name,
            displayName = resource.displayName,
            description = resource.description,
            icon = resource.icon,
            size = resource.size,
            publishDate = resource.publishDate,
            totalAudioCount = resource.audioList.size,
            lastUpdateTime = System.currentTimeMillis()
        )
    }

    /** 暂停下载 */
    fun pauseDownload(resourceId: String) {
        val state = states[resourceId] ?: return
        if (state.status != DownloadStatus.DOWNLOADING) return
        cancelFlags[resourceId]?.set(true)
        updateState(resourceId, DownloadStatus.PAUSED, state.progress)
        Log.i("DownloadManager", "下载已暂停: id=$resourceId")
    }

    // ==================== 全局启动 / 切换 API ====================

    /**
     * 在 DownloadManager 自有作用域中启动安装。所有下载任务都运行于此作用域，
     * 因此与调用方（ViewModel/页面）的生命周期无关，可跨页面持续进行。
     * 同一资源正在下载时忽略重复调用；暂停/失败/初始态则会开始或继续安装。
     */
    fun startInstall(resourceId: String, filesDir: File) {
        if (states[resourceId]?.status == DownloadStatus.DOWNLOADING) return
        // 使用启动守卫避免重复并发启动（快速连点等场景）
        if (starting.putIfAbsent(resourceId, true) != null) return
        scope.launch {
            try {
                installResourceById(resourceId = resourceId, filesDir = filesDir)
            } finally {
                starting.remove(resourceId)
            }
        }
    }

    /**
     * 切换下载/暂停（全局入口，供各页面统一调用）：
     * - DOWNLOADING → 暂停（cancelFlag 置位，installResourceById 会在安全点中止）
     * - 其余（IDLE / PAUSED / FAILED / null）→ 在全局作用域中开始/继续安装
     */
    fun toggleDownload(resourceId: String, filesDir: File) {
        when (getDownloadState(resourceId)?.status) {
            DownloadStatus.DOWNLOADING -> pauseDownload(resourceId)
            else -> startInstall(resourceId, filesDir)
        }
    }

    /** 获取当前所有全局下载状态快照（供页面进入时一次性同步 UI） */
    fun getAllStates(): Map<String, DownloadState> = states.toMap()

    /**
     * 应用启动时调用：扫描 audio/{id}/.download 记录，将未完成任务恢复为 PAUSED 状态
     * 并同步已下载进度，使商店/搜索/详情页进入时即可看到「可继续」状态与进度。
     * 正在下载中的资源（进程存活）不会被覆盖。
     */
    fun restorePendingDownloads(filesDir: File?) {
        if (filesDir == null) return
        val pending = getPendingDownloads(filesDir)
        for (id in pending) {
            if (states[id]?.status == DownloadStatus.DOWNLOADING) continue
            val record = readDownloadRecord(id, filesDir)
            val progress = if (record != null && record.totalAudioCount > 0) {
                record.downloadedAudioItems.size.toFloat() / record.totalAudioCount
            } else 0f
            updateState(id, DownloadStatus.PAUSED, progress.coerceIn(0f, 1f))
            Log.i("DownloadManager", "恢复未完成下载记录: id=$id, 进度=${(progress * 100).toInt()}%")
        }
    }

    // ==================== 安装流程 ====================

    /**
     * 根据资源 ID 完整安装：获取详情 → 保存元数据 → 下载音频 → 下载图标 → 更新本地路径。
     * 支持取消（pause 时中止正在进行的下载）。
     */
    suspend fun installResourceById(
        resourceId: String,
        filesDir: File,
        onProgress: (Float) -> Unit = {}
    ) {
        // 重置取消标志
        val cancelFlag = AtomicBoolean(false)
        cancelFlags[resourceId] = cancelFlag

        Log.i("DownloadManager", ">>> 安装资源开始: id=$resourceId")
        val repo = repository ?: return

        // 设置全局状态为下载中
        updateState(resourceId, DownloadStatus.DOWNLOADING, 0f)

        val detail = repo.getRemoteResourceDetail(resourceId)
        val fullResource = detail.getOrNull() ?: run {
            Log.e("DownloadManager", "获取资源详情失败: id=$resourceId")
            updateState(resourceId, DownloadStatus.FAILED, 0f)
            return
        }

        Log.i("DownloadManager", "获取详情成功: ${fullResource.displayName}, 音频文件数=${fullResource.audioList.size}")

        val audioCount = fullResource.audioList.size
        val hasIcon = fullResource.icon.isNotEmpty()
        val totalTasks = audioCount + (if (hasIcon) 1 else 0)

        // 下载音频
        val updatedAudioList = downloadResourceAudio(
            resourceId = resourceId,
            audioList = fullResource.audioList,
            filesDir = filesDir,
            cancelFlag = cancelFlag,
            resource = fullResource,
            onProgress = { audioProgress ->
                val completedAudio = (audioProgress * audioCount).toInt()
                val overallProgress = completedAudio.toFloat() / totalTasks
                updateState(resourceId, DownloadStatus.DOWNLOADING, overallProgress)
                onProgress(overallProgress)
            }
        )
        // 检查是否被暂停
        if (cancelFlag.get()) {
            Log.i("DownloadManager", "下载已暂停，中止安装: id=$resourceId")
            return
        }

        // 下载图标
        var localIcon = fullResource.icon
        if (hasIcon) {
            Log.i("DownloadManager", "开始下载图标: url=${fullResource.icon}")
            val iconResult = downloadResourceIcon(
                resourceId = resourceId,
                iconUrl = fullResource.icon,
                filesDir = filesDir,
                cancelFlag = cancelFlag,
                resource = fullResource
            )
            if (cancelFlag.get()) {
                Log.i("DownloadManager", "下载已暂停(图标)，中止安装: id=$resourceId")
                return
            }
            if (iconResult != null) {
                localIcon = iconResult
                Log.i("DownloadManager", "图标已保存: $localIcon")
            } else {
                Log.e("DownloadManager", "图标下载失败: url=${fullResource.icon}")
            }
            updateState(resourceId, DownloadStatus.DOWNLOADING, 1f)
            onProgress(1f)
        }

        // 所有文件下载完成，一次性写入 Room DB（参照 C# SaveResourceJson）
        repo.installResourceWithLocalData(fullResource, updatedAudioList, localIcon)
        Log.i("DownloadManager", "元数据已保存到 Room DB（含本地音频路径和图标）")

        // 标记完成
        updateState(resourceId, DownloadStatus.COMPLETED, 1f)
        deleteDownloadRecord(resourceId, filesDir)
        cancelFlags.remove(resourceId)

        val localCount = updatedAudioList.count { !it.src.startsWith("http") }
        val remoteCount = updatedAudioList.count { it.src.startsWith("http") }
        Log.i("DownloadManager", "<<< 安装完成: $localCount 个本地文件, $remoteCount 个仍远程, 图标=${if (hasIcon) (if (localIcon.startsWith("http")) "已下载" else "已有") else "无"}, 总计${updatedAudioList.size}个音频")
    }

    /**
     * 下载资源的所有音频文件到本地。支持通过 cancelFlag 取消，
     * 并参照 C# 实现从 .download 记录断点续传（进程被杀后重入可恢复已完成的音频项）。
     */
    suspend fun downloadResourceAudio(
        resourceId: String,
        audioList: List<RemoteAudioItem>,
        filesDir: File,
        cancelFlag: AtomicBoolean? = null,
        onProgress: (Float) -> Unit = {},
        resource: RemoteResource? = null
    ): List<AudioItem> {
        val audioDir = File(filesDir, "audio/$resourceId")
        if (audioList.isEmpty()) {
            Log.i("DownloadManager", "无音频文件需要下载")
            return emptyList()
        }
        Log.i("DownloadManager", "音频下载目录: ${audioDir.absolutePath}")

        val totalCount = audioList.size
        val results = mutableListOf<AudioItem>()
        val recoveredSet = mutableSetOf<String>()

        // 从 .download 记录恢复已完成的音频项（参照 C# 断点续传）
        val record = readDownloadRecord(resourceId, filesDir)
        if (record != null) {
            for (downloaded in record.downloadedAudioItems) {
                val localFile = File(downloaded.src)
                if (localFile.exists()) {
                    results.add(
                        AudioItem(
                            id = downloaded.id,
                            name = downloaded.name,
                            src = downloaded.src,
                            duration = downloaded.durationMs
                        )
                    )
                    recoveredSet.add(downloaded.id)
                    Log.i("DownloadManager", "   [断点续传] 已恢复: ${downloaded.name} (${downloaded.src})")
                }
            }
            if (results.isNotEmpty()) {
                onProgress(results.size.toFloat() / totalCount)
            }
        }

        for (index in audioList.indices) {
            // 每次下载前检查取消标志
            if (cancelFlag?.get() == true) {
                Log.i("DownloadManager", "检测到取消标志，中断音频下载")
                break
            }

            val audio = audioList[index]
            // 已通过 .download 记录恢复的跳过
            if (recoveredSet.contains(audio.id)) continue

            val ext = extractExtensionFromUrl(audio.url)
            val filename = "audio_${index + 1}$ext"
            val localFile = File(audioDir, filename)
            val progressStr = "[${index + 1}/$totalCount]"
            Log.i("DownloadManager", "$progressStr 开始处理音频: ${audio.name} | url=${audio.url} | 目标=${localFile.absolutePath}")

            // 文件已存在直接复用（与记录恢复互补的安全网）
            if (localFile.exists()) {
                Log.i("DownloadManager", "$progressStr 目标文件已存在, 跳过下载: size=${localFile.length()}bytes")
                results.add(
                    AudioItem(
                        id = audio.id,
                        name = audio.name,
                        src = localFile.absolutePath,
                        duration = audio.duration
                    )
                )
                onProgress(results.size.toFloat() / totalCount)
                persistAudioRecord(resourceId, filesDir, resource, results)
                continue
            }

            val startTime = System.currentTimeMillis()
            val result = download(
                resourceId = resourceId,
                url = audio.url,
                destFile = localFile,
                cancelFlag = cancelFlag
            )
            val elapsed = System.currentTimeMillis() - startTime

            if (result.isSuccess) {
                val size = localFile.length()
                Log.i("DownloadManager", "$progressStr ✓ 下载成功: ${audio.name} | size=${size}bytes | 耗时=${elapsed}ms | 路径=${localFile.absolutePath}")
                results.add(
                    AudioItem(
                        id = audio.id,
                        name = audio.name,
                        src = localFile.absolutePath,
                        duration = audio.duration
                    )
                )
                persistAudioRecord(resourceId, filesDir, resource, results)
            } else {
                val exception = result.exceptionOrNull()
                // 取消导致的异常，不视为错误
                if (exception is CancellationException) {
                    Log.i("DownloadManager", "$progressStr 下载已被取消: ${audio.name}")
                    break
                }
                val errMsg = exception?.message ?: "未知错误"
                Log.e("DownloadManager", "$progressStr ✗ 下载失败: ${audio.name} | 耗时=${elapsed}ms | 原因=$errMsg | url=${audio.url}")
                results.add(
                    AudioItem(
                        id = audio.id,
                        name = audio.name,
                        src = audio.url,
                        duration = audio.duration
                    )
                )
            }
            onProgress(results.size.toFloat() / totalCount)
        }

        val successCount = results.count { !it.src.startsWith("http") }
        Log.i("DownloadManager", "音频批量下载结束: $successCount/$totalCount 个成功")
        return results
    }

    /**
     * 下载资源图标到 audio/{resourceId}/ 目录下。
     */
    suspend fun downloadResourceIcon(
        resourceId: String,
        iconUrl: String,
        filesDir: File,
        cancelFlag: AtomicBoolean? = null,
        resource: RemoteResource? = null
    ): String? {
        if (iconUrl.isEmpty()) return null
        if (cancelFlag?.get() == true) return null

        val audioDir = File(filesDir, "audio/$resourceId")
        val filename = extractFilenameFromUrl(iconUrl, "icon")
        val iconFile = File(audioDir, filename)

        Log.i("DownloadManager", "处理图标: url=$iconUrl | 目标=${iconFile.absolutePath}")

        if (iconFile.exists()) {
            Log.i("DownloadManager", "图标已存在: size=${iconFile.length()}bytes | 路径=${iconFile.absolutePath}")
            markIconDownloaded(resourceId, filesDir, resource, true)
            return iconFile.absolutePath
        }

        val startTime = System.currentTimeMillis()
        val result = download(
            resourceId = resourceId,
            url = iconUrl,
            destFile = iconFile,
            cancelFlag = cancelFlag
        )
        val elapsed = System.currentTimeMillis() - startTime

        return if (result.isSuccess) {
            Log.i("DownloadManager", "✓ 图标下载成功: size=${iconFile.length()}bytes | 耗时=${elapsed}ms | 路径=${iconFile.absolutePath}")
            markIconDownloaded(resourceId, filesDir, resource, true)
            // 同时保存到 installed_icons 专用文件夹（供图标选择器使用）
            try {
                iconFile.copyTo(File(filesDir, "installed_icons/${resourceId}.jpg"), overwrite = true)
            } catch (_: Exception) { }
            iconFile.absolutePath
        } else {
            val exception = result.exceptionOrNull()
            if (exception is CancellationException) {
                Log.i("DownloadManager", "图标下载已被取消")
                return null
            }
            val errMsg = exception?.message ?: "未知错误"
            Log.e("DownloadManager", "✗ 图标下载失败: 耗时=${elapsed}ms | 原因=$errMsg | url=$iconUrl")
            null
        }
    }

    /** 从 URL 末尾提取文件名 */
    private fun extractFilenameFromUrl(url: String, fallback: String): String {
        return url.substringAfterLast('/').substringBefore('?')
            .ifEmpty { fallback }
    }

    /** 从 URL 中提取文件扩展名（含点号，如 .mp3），无扩展名则返回空字符串 */
    private fun extractExtensionFromUrl(url: String): String {
        val name = url.substringAfterLast('/').substringBefore('?')
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex >= 0) name.substring(dotIndex) else ""
    }

    /**
     * 下载文件到指定路径。支持通过 cancelFlag 取消。
     */
    suspend fun download(
        resourceId: String,
        url: String,
        destFile: File,
        cancelFlag: AtomicBoolean? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        if (cancelFlag?.get() == true) {
            return@withContext Result.failure(CancellationException("Download cancelled"))
        }

        // 检查全局缓存
        val cacheKey = url.hashCode().toUInt().toString(16)
        val cacheFile = cacheDir?.let { File(it, cacheKey) }
        if (cacheFile != null && cacheFile.exists()) {
            val cacheSize = cacheFile.length()
            Log.i("DownloadManager", "  缓存命中: key=$cacheKey | url=$url | size=${cacheSize}bytes")
            destFile.parentFile?.mkdirs()
            cacheFile.copyTo(destFile, overwrite = true)
            Log.i("DownloadManager", "  已从缓存复制到: ${destFile.absolutePath}")
            updateProgress(resourceId) {
                it.copy(isCompleted = true, totalBytes = cacheSize, bytesWritten = cacheSize)
            }
            return@withContext Result.success(destFile)
        }
        Log.i("DownloadManager", "  缓存未命中: key=$cacheKey, 开始网络下载")
        return@withContext doDownload(resourceId, url, destFile, cacheFile, cancelFlag)
    }

    /**
     * 实际执行网络下载（带失败重试 + 指数退避，参照 C# DownloadFileWithProgressAsync）。
     * 取消(CancellationException)立即上抛、不重试；其它异常重试最多 3 次。
     */
    private suspend fun doDownload(
        resourceId: String,
        url: String,
        destFile: File,
        cacheFile: File? = null,
        cancelFlag: AtomicBoolean? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val maxRetries = 3
        var lastException: Exception? = null
        for (attempt in 0 until maxRetries) {
            if (cancelFlag?.get() == true) {
                return@withContext Result.failure(CancellationException("Download cancelled"))
            }
            try {
                return@withContext doDownloadOnce(resourceId, url, destFile, cacheFile, cancelFlag)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    Log.w("DownloadManager", "    下载异常(第${attempt + 1}次重试): ${e.message} | url=$url")
                    delay(300L * (1 shl attempt)) // 指数退避: 300ms → 600ms → 1200ms
                }
            }
        }
        val err = lastException ?: Exception("下载失败")
        updateProgress(resourceId) { it.copy(isFailed = true, errorMessage = err.message ?: "未知错误") }
        Result.failure(err)
    }

    /** 单次网络下载（含临时文件与重命名），失败向上抛异常 */
    private fun doDownloadOnce(
        resourceId: String,
        url: String,
        destFile: File,
        cacheFile: File? = null,
        cancelFlag: AtomicBoolean? = null
    ): Result<File> {
        val request = Request.Builder().url(url).build()
        Log.i("DownloadManager", "    HTTP 请求开始: url=$url")
        val startTime = System.currentTimeMillis()
        client.newCall(request).execute().use { response ->
            val connTime = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                val error = "下载失败: HTTP ${response.code}"
                Log.e("DownloadManager", "    HTTP ${response.code} | 连接耗时=${connTime}ms | url=$url")
                updateProgress(resourceId) { it.copy(isFailed = true, errorMessage = error) }
                return Result.failure(Exception(error))
            }

            val body = response.body ?: return Result.failure(Exception("响应体为空"))
            val contentLength = body.contentLength()
            val lenStr = if (contentLength >= 0) "${contentLength}bytes" else "未知大小"

            Log.i("DownloadManager", "    HTTP 200 | 大小=$lenStr | 连接耗时=${connTime}ms")

            updateProgress(resourceId) { it.copy(totalBytes = if (contentLength >= 0) contentLength else 0) }

            // 下载到临时文件
            val tempFile = File(destFile.parentFile, "${destFile.name}.tmp")
            tempFile.parentFile?.mkdirs()
            FileOutputStream(tempFile).use { outputStream ->
                val inputStream = body.byteStream()
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                var chunkIndex = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    // 每次读取后检查取消标志
                    if (cancelFlag?.get() == true) {
                        Log.i("DownloadManager", "    检测到取消标志，中断正在进行的下载")
                        // 清理临时文件
                        tempFile.delete()
                        throw CancellationException("Download paused by user")
                    }
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    chunkIndex++
                    if (chunkIndex % 64 == 0) {
                        Log.v("DownloadManager", "    ... 已接收 ${totalRead}bytes")
                    }
                    val finalTotal = totalRead
                    updateProgress(resourceId) { it.copy(bytesWritten = finalTotal) }
                }
            }

            tempFile.renameTo(destFile)
            val totalTime = System.currentTimeMillis() - startTime
            Log.i("DownloadManager", "    已保存到: ${destFile.absolutePath} | size=${destFile.length()}bytes | 总耗时=${totalTime}ms")

            if (cacheFile != null) {
                cacheFile.parentFile?.mkdirs()
                destFile.copyTo(cacheFile, overwrite = true)
                Log.i("DownloadManager", "    已同步到缓存: ${cacheFile.absolutePath}")
            }

            updateProgress(resourceId) { it.copy(isCompleted = true) }
            return Result.success(destFile)
        }
    }

    private fun updateProgress(resourceId: String, update: (DownloadProgress) -> DownloadProgress) {
        val current = downloadProgress[resourceId] ?: DownloadProgress(resourceId = resourceId)
        val updated = update(current)
        downloadProgress[resourceId] = updated
        listeners[resourceId]?.invoke(updated)
    }
}
