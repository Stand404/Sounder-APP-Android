package com.stand.sounder_app.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.R
import com.stand.sounder_app.util.ResourcePackageUtils
import com.stand.sounder_app.util.ShortcutPermState
import com.stand.sounder_app.util.ShortcutPermissionChecker
import com.stand.sounder_app.util.formatByteSize
import com.stand.sounder_app.data.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stand.sounder_app.data.model.AudioItem
import com.stand.sounder_app.data.model.LoopMode
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode
import com.stand.sounder_app.util.ResourcePackageUtils.resourceDir
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

class PersonalResourceViewModel : ViewModel() {

    private val repository = MyApp.instance.resourceRepository
    private val appContext: Context = MyApp.instance

    private val _resources = MutableStateFlow<List<Resource>>(emptyList())
    val resources: StateFlow<List<Resource>> = _resources.asStateFlow()

    /** 存在未完成 .download 记录（可继续安装）的资源 ID 集合 */
    private val _resumableIds = MutableStateFlow<Set<String>>(emptySet())
    val resumableIds: StateFlow<Set<String>> = _resumableIds.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deleteMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val deleteMessage: SharedFlow<String> = _deleteMessage.asSharedFlow()

    /** 轻提示消息（克隆/导出/桌面/文件夹操作的反馈） */
    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    /** 创建桌面快捷方式被厂商权限拒绝 / 需询问时，请求 UI 弹出引导去设置 */
    private val _shortcutPermissionGuide = MutableSharedFlow<Resource>(extraBufferCapacity = 1)
    val shortcutPermissionGuide: SharedFlow<Resource> = _shortcutPermissionGuide.asSharedFlow()

    /** 导出完成后请求分享的 zip 文件 */
    private val _shareRequest = MutableSharedFlow<File>(extraBufferCapacity = 1)
    val shareRequest: SharedFlow<File> = _shareRequest.asSharedFlow()

    init {
        loadResources()
    }

    private fun loadResources() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllLocalResources().collect { list ->
                _resources.value = list
                val filesDir = MyApp.instance.filesDir
                _resumableIds.value = list.mapNotNull { r ->
                    if (MyApp.instance.downloadManager.hasPendingDownload(r.id, filesDir)) r.id else null
                }.toSet()
                _isLoading.value = false
            }
        }
    }

    fun deleteResource(resourceId: String) {
        viewModelScope.launch {
            val app = MyApp.instance
            val audioDir = File(app.filesDir, "audio/$resourceId")

            // 统计待删除文件数及总大小
            var fileCount = 0
            var totalBytes = 0L
            if (audioDir.exists()) {
                audioDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        fileCount++
                        totalBytes += file.length()
                    }
                }
            }

            // 删除本地文件
            if (audioDir.exists()) {
                audioDir.deleteRecursively()
            }
            // 删除 installed_icons 中的对应图标
            val iconFile = File(app.filesDir, "installed_icons/$resourceId.jpg")
            if (iconFile.exists()) {
                iconFile.delete()
            }
            // 清理 DownloadManager 中的全局状态
            app.downloadManager.cleanupResource(resourceId)
            // 删除 Room 中的元数据
            repository.deleteResource(resourceId)

            // 提示消息
            val sizeStr = formatByteSize(totalBytes)
            _deleteMessage.tryEmit(appContext.getString(R.string.deleted_files, fileCount, sizeStr))
        }
    }

    fun refresh() {
        loadResources()
    }

    // ===== 克隆资源 =====

    fun cloneResource(resourceId: String) {
        viewModelScope.launch {
            val source = repository.getLocalResourceById(resourceId) ?: run {
                _toast.tryEmit(appContext.getString(R.string.resource_not_found_clone))
                return@launch
            }
            val newId = "local_${System.currentTimeMillis()}_${(0..9999).random()}"
            val newDir = resourceDir(newId)
            val srcDir = resourceDir(resourceId)

            // 拷贝音频/图标等本地文件
            runCatching { ResourcePackageUtils.copyDirectory(srcDir, newDir) }

            val newAudioList = source.audioList.map { audio ->
                val newSrc = if (audio.src.startsWith(srcDir.absolutePath)) {
                    audio.src.replaceFirst(srcDir.absolutePath, newDir.absolutePath)
                } else {
                    audio.src
                }
                AudioItem(id = audio.id, name = audio.name, src = newSrc, duration = audio.duration)
            }

            val newIcon = if (source.icon.startsWith(srcDir.absolutePath)) {
                source.icon.replaceFirst(srcDir.absolutePath, newDir.absolutePath)
            } else {
                source.icon
            }

            val clone = source.copy(
                id = newId,
                displayName = "${source.displayName} (副本)",
                name = "${source.name} (副本)",
                audioList = newAudioList,
                icon = newIcon,
                installDate = System.currentTimeMillis()
            )
            repository.saveResource(clone)
            refresh()
            _toast.tryEmit(appContext.getString(R.string.cloned_as, clone.displayName))
        }
    }

    // ===== 导出资源（打包为 zip，与 C# 完全一致） =====

    fun exportResource(resourceId: String) {
        viewModelScope.launch {
            val source = repository.getLocalResourceById(resourceId) ?: run {
                _toast.tryEmit(appContext.getString(R.string.resource_not_found_export))
                return@launch
            }
            val safeName = sanitizeFileName(source.displayName)
            val cacheDir = File(appContext.cacheDir, "exports")
            cacheDir.mkdirs()
            val zipFile = File(cacheDir, "$safeName.zip")
            if (zipFile.exists()) zipFile.delete()

            val ok = withContext(Dispatchers.IO) {
                ResourcePackageUtils.createExportZip(source, zipFile)
            }

            if (ok) {
                _shareRequest.tryEmit(zipFile)
            } else {
                _toast.tryEmit(appContext.getString(R.string.export_failed))
            }
        }
    }

    // ===== 创建新资源 =====

    /** 创建新资源并导航到编辑页面（参照 C# CreateResource） */
    fun createResource(callback: (String) -> Unit) {
        viewModelScope.launch {
            val newId = "local_${System.currentTimeMillis()}_${(0..9999).random()}"
            val installDir = resourceDir(newId)
            installDir.mkdirs()

            // 复制 App 默认图标作为资源图标（参照 C# 复制 Assets/sounder.jpg）
            val defaultIcon = withContext(Dispatchers.IO) {
                runCatching {
                    val src = android.graphics.BitmapFactory.decodeResource(
                        appContext.resources, R.mipmap.ic_launcher
                    ) ?: return@runCatching ""
                    val iconFile = File(installDir, "icon.jpg")
                    FileOutputStream(iconFile).use { fos ->
                        src.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
                    }
                    iconFile.absolutePath
                }.getOrDefault("")
            }

            val resource = Resource(
                id = newId,
                name = appContext.getString(R.string.new_resource_name),
                displayName = appContext.getString(R.string.new_resource_name),
                description = appContext.getString(R.string.new_resource_desc),
                icon = defaultIcon,
                audioList = emptyList(),
                size = 0L,
                publishDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                installDate = System.currentTimeMillis(),
                isInstalled = true,
                playMode = PlayMode.OVERLAY,
                orderMode = OrderMode.ORDER,
                loopMode = LoopMode.SINGLE,
                currentAudioIndex = 0
            )
            repository.saveResource(resource)
            refresh()
            _toast.tryEmit(appContext.getString(R.string.created, resource.displayName))
            callback(newId)
        }
    }

    // ===== 导入资源包 =====

    /** 从 zip 文件 Uri 导入资源包（参照 C# ImportResourceAsync） */
    fun importResourceFromUri(context: Context, uri: Uri, callback: (String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    importZip(context, uri)
                }
            }
            result.onSuccess { newId ->
                refresh()
                _toast.tryEmit(appContext.getString(R.string.import_success))
                callback(newId)
            }.onFailure { e ->
                val errMsg = e.message ?: appContext.getString(R.string.unknown_error)
                _toast.tryEmit(appContext.getString(R.string.import_failed, errMsg))
            }
        }
    }

    /**
     * 在 IO 线程上解析 zip 并导入。parseImportZip 直接解压到安装目录，
     * 返回的路径已是最终路径，无需额外复制。
     */
    private suspend fun importZip(context: Context, uri: Uri): String {
        val newId = "local_${System.currentTimeMillis()}_${(0..9999).random()}"
        val installDir = resourceDir(newId)
        installDir.mkdirs()

        val result = withContext(Dispatchers.IO) {
            ResourcePackageUtils.parseImportZip(context, uri, installDir)
        }

        // 标准化图标名称
        var iconPath = ""
        if (result.iconPath.isNotBlank()) {
            val iconSrc = File(result.iconPath)
            val destIcon = File(installDir, "icon.jpg")
            if (iconSrc != destIcon) {
                iconSrc.renameTo(destIcon)
            }
            iconPath = destIcon.absolutePath
            // 同时复制到 installed_icons 公开文件夹供图标选择器使用
            try {
                destIcon.copyTo(File(MyApp.instance.filesDir, "installed_icons/${newId}.jpg"), overwrite = true)
            } catch (_: Exception) { }
        }

        // 标准化音频文件名并补全 ID
        val audioFiles = result.audioFiles.mapIndexed { i, audio ->
            var srcPath = audio.src
            if (srcPath.isNotBlank()) {
                val audioSrc = File(srcPath)
                val destAudio = File(installDir, "audio_$i.${audioSrc.extension}")
                if (audioSrc != destAudio) {
                    audioSrc.renameTo(destAudio)
                }
                srcPath = destAudio.absolutePath
            }
            audio.copy(id = "${newId}_audio_$i", src = srcPath)
        }

        // 计算总大小
        val totalSize = installDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

        // 删除 manifest.json（已不再需要）
        File(installDir, "manifest.json").delete()

        val resource = Resource(
            id = newId,
            name = result.displayName,
            displayName = result.displayName,
            description = result.description,
            icon = iconPath,
            audioList = audioFiles,
            size = totalSize,
            publishDate = result.publishDate,
            installDate = System.currentTimeMillis(),
            isInstalled = true,
            playMode = PlayMode.OVERLAY,
            orderMode = OrderMode.ORDER,
            loopMode = LoopMode.SINGLE,
            currentAudioIndex = 0
        )
        repository.saveResource(resource)
        return newId
    }

    // ===== 添加到桌面（固定快捷方式） =====

    /**
     * 添加到桌面：先检测厂商「创建桌面快捷方式」权限。
     * - 已允许 / 未知（无法检测）：直接请求固定快捷方式。
     * - 被拒绝 / 需询问：弹出引导，让用户去系统设置开启权限。
     */
    fun addToDesktop(context: Context, resource: Resource) {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                ShortcutPermissionChecker.check(appContext)
            }
            when (state) {
                ShortcutPermState.GRANTED, ShortcutPermState.UNKNOWN -> {
                    performAddToDesktop(context, resource)
                }
                ShortcutPermState.DENIED, ShortcutPermState.ASK -> {
                    _shortcutPermissionGuide.tryEmit(resource)
                }
            }
        }
    }

    /** 绕过权限检测，强制尝试添加（供引导页「仍然尝试」使用） */
    fun addToDesktopAnyway(context: Context, resource: Resource) {
        performAddToDesktop(context, resource)
    }

    private fun performAddToDesktop(context: Context, resource: Resource) {
        val shortcutManager = runCatching {
            context.getSystemService(ShortcutManager::class.java)
        }.getOrNull()
        if (shortcutManager != null) {
            val shortcutId = "resource_${resource.id}"
            runCatching {
                val intent = Intent(context, com.stand.sounder_app.shortcut.ShortcutPlayActivity::class.java).apply {
                    action = com.stand.sounder_app.shortcut.ShortcutPlayActivity.ACTION_PLAY_SHORTCUT
                    putExtra(com.stand.sounder_app.shortcut.ShortcutPlayReceiver.EXTRA_RESOURCE_ID, resource.id)
                }
                // 优先使用资源自己的图标，否则回退到 App 图标
                val shortcut = android.content.pm.ShortcutInfo.Builder(context, shortcutId)
                    .setShortLabel(resource.name)
                    .setLongLabel(resource.name)
                    .setIcon(buildShortcutIcon(context, resource.icon))
                    .setIntent(intent)
                    .build()
                shortcutManager.requestPinShortcut(shortcut, null)
            }
        } else {
            // 启动器不支持固定快捷方式：回退旧版广播
            runCatching { sendLegacyShortcut(context, resource) }
        }
        _toast.tryEmit(appContext.getString(R.string.shortcut_requested, resource.name))
    }

    /**
     * 旧版「添加到桌面」广播，兼容不支持 ShortcutManager 的启动器。
     * 需要 com.android.launcher.permission.INSTALL_SHORTCUT 权限（已在 Manifest 声明）。
     * 使用字符串字面量避免 EXTRA_SHORTCUT_* 等已废弃常量告警。
     */
    private fun sendLegacyShortcut(context: Context, resource: Resource) {
        val shortcutIntent = Intent(context, com.stand.sounder_app.shortcut.ShortcutPlayActivity::class.java).apply {
            action = com.stand.sounder_app.shortcut.ShortcutPlayActivity.ACTION_PLAY_SHORTCUT
            putExtra(com.stand.sounder_app.shortcut.ShortcutPlayReceiver.EXTRA_RESOURCE_ID, resource.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val iconBitmap = createScaledShortcutIcon(context, resource.icon)
        val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
            putExtra("android.intent.extra.shortcut.NAME", resource.name)
            putExtra("android.intent.extra.shortcut.INTENT", shortcutIntent)
            putExtra("android.intent.extra.shortcut.ICON", iconBitmap)
        }
        context.sendBroadcast(addIntent)
        _toast.tryEmit(appContext.getString(R.string.shortcut_sent))
    }

    /**
     * 生成适合旧版桌面快捷方式的图标：保留原图标，并在其四周预留透明空白区域，
     * 避免启动器直接拉伸原图导致「放大」观感。
     * 原图记为 150x150 正方形，居中绘制在更大的标准画布内（占画布 67%，四周留白）。
     */
    private fun createScaledShortcutIcon(
        context: Context,
        iconPath: String
    ): android.graphics.Bitmap {
        val density = context.resources.displayMetrics.density
        val canvasSize = (108 * density).toInt().coerceAtLeast(1) // 标准快捷方式画布
        val innerRatio = 0.67f                               // 原图占据画布比例，其余为周围空白
        val innerSize = (canvasSize * innerRatio).toInt().coerceAtLeast(1)

        val output = createBitmap(canvasSize, canvasSize)
        val canvas = android.graphics.Canvas(output)

        // 取得原图：资源专属图标优先，否则回退到 App 图标
        val src = if (iconPath.isNotBlank()) {
            val file = File(iconPath)
            if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
        } else null
            ?: android.graphics.BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        if (src != null) {
            // 将原图缩放到 innerSize 并居中绘制，四周自然形成透明空白
            val inner = src.scale(innerSize, innerSize)
            val left = (canvasSize - innerSize) / 2f
            val top = (canvasSize - innerSize) / 2f
            canvas.drawBitmap(inner, left, top, null)
        }
        return output
    }

    /**
     * 构建快捷方式图标：复用「原图居中 + 四周留白」的位图并包装为自适应图标。
     * 这样在 Android 8+ 的 ShortcutManager(requestPinShortcut) 路径下也不会被拉伸放大。
     */
    private fun buildShortcutIcon(context: Context, iconPath: String): android.graphics.drawable.Icon {
        val bitmap = createScaledShortcutIcon(context, iconPath)
        return android.graphics.drawable.Icon.createWithAdaptiveBitmap(bitmap)
    }

    // ===== 辅助（委托 ResourcePackageUtils） =====

    private fun sanitizeFileName(name: String): String =
        ResourcePackageUtils.sanitizeFileName(name)
}
