package com.stand.sounder_app.data.repository

import com.stand.sounder_app.data.api.ApiService
import com.stand.sounder_app.data.db.ResourceDao
import com.stand.sounder_app.data.db.ResourceEntity
import com.stand.sounder_app.data.model.AudioItem
import com.stand.sounder_app.data.model.LoopMode
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode
import com.stand.sounder_app.data.model.RemoteResource
import com.stand.sounder_app.data.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ResourceRepository(
    private val resourceDao: ResourceDao,
    private val apiService: ApiService
) {

    /** 获取所有本地资源（实时流） */
    fun getAllLocalResources(): Flow<List<Resource>> {
        return resourceDao.getAllResources().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /** 根据 ID 获取本地资源 */
    suspend fun getLocalResourceById(id: String): Resource? {
        return resourceDao.getResourceById(id)?.toDomain()
    }

    /** 安装资源（从远程下载后保存到本地） */
    suspend fun installResource(remote: RemoteResource): Resource {
        val entity = ResourceEntity(
            id = remote.id,
            name = remote.name,
            displayName = remote.displayName.ifEmpty { remote.name },
            description = remote.description,
            icon = remote.icon,
            audioList = remote.audioList.map { audio ->
                AudioItem(
                    id = audio.id,
                    name = audio.name,
                    src = audio.url,
                    duration = audio.duration
                )
            },
            size = parseSizeString(remote.size),
            publishDate = remote.publishDate,
            installDate = System.currentTimeMillis(),
            playMode = PlayMode.OVERLAY,
            orderMode = OrderMode.ORDER,
            loopMode = LoopMode.SINGLE,
            currentAudioIndex = 0
        )
        resourceDao.insertResource(entity)
        return entity.toDomain()
    }

    /**
     * 安装资源（使用已下载的本地音频列表和图标路径）。
     * 参照 C# SaveResourceJson：所有文件下载完成后才写入 DB，
     * 避免下载中途资源就出现在"我的资源"页面。
     */
    suspend fun installResourceWithLocalData(
        remote: RemoteResource,
        localAudioList: List<AudioItem>,
        localIcon: String
    ): Resource {
        val entity = ResourceEntity(
            id = remote.id,
            name = remote.name,
            displayName = remote.displayName.ifEmpty { remote.name },
            description = remote.description,
            icon = localIcon,
            audioList = localAudioList,
            size = parseSizeString(remote.size),
            publishDate = remote.publishDate,
            installDate = System.currentTimeMillis(),
            playMode = PlayMode.OVERLAY,
            orderMode = OrderMode.ORDER,
            loopMode = LoopMode.SINGLE,
            currentAudioIndex = 0
        )
        resourceDao.insertResource(entity)
        return entity.toDomain()
    }

    /** 删除本地资源 */
    suspend fun deleteResource(id: String) {
        resourceDao.deleteResourceById(id)
    }

    /** 更新资源信息 */
    suspend fun updateResourceInfo(id: String, name: String, description: String, icon: String) {
        resourceDao.updateResourceInfo(id, name, description, icon)
    }

    /** 仅更新资源图标 */
    suspend fun updateIcon(id: String, icon: String) {
        resourceDao.updateIcon(id, icon)
    }

    /** 更新音频列表（通过实体替换方式） */
    suspend fun updateAudioList(id: String, audioList: List<AudioItem>) {
        val entity = resourceDao.getResourceById(id) ?: return
        resourceDao.insertResource(entity.copy(audioList = audioList))
    }

    /** 更新播放模式 */
    suspend fun updatePlayMode(id: String, playMode: PlayMode) {
        resourceDao.updatePlayMode(id, playMode)
    }

    /** 更新顺序模式 */
    suspend fun updateOrderMode(id: String, orderMode: OrderMode) {
        resourceDao.updateOrderMode(id, orderMode)
    }

    /** 更新循环模式细分（单曲/列表） */
    suspend fun updateLoopMode(id: String, loopMode: LoopMode) {
        resourceDao.updateLoopMode(id, loopMode)
    }

    /** 持久化当前播放索引（循环/顺序跨重启衔接） */
    suspend fun updateCurrentAudioIndex(id: String, index: Int) {
        resourceDao.updateCurrentAudioIndex(id, index.coerceAtLeast(0))
    }

    /** 保存完整资源（用于编辑保存） */
    suspend fun saveResource(resource: Resource) {
        val entity = ResourceEntity(
            id = resource.id,
            name = resource.name,
            displayName = resource.displayName,
            description = resource.description,
            icon = resource.icon,
            audioList = resource.audioList,
            size = resource.size,
            publishDate = resource.publishDate,
            installDate = resource.installDate,
            playMode = resource.playMode,
            orderMode = resource.orderMode,
            loopMode = resource.loopMode,
            currentAudioIndex = resource.currentAudioIndex
        )
        resourceDao.insertResource(entity)
    }

    // ===== 远程 API =====

    /** 获取远程资源列表（分页） */
    suspend fun getRemoteResourceList(page: Int, limit: Int = 10): Result<List<RemoteResource>> {
        return try {
            val response = apiService.getResourceList(page, limit)
            if (response.isSuccess) {
                Result.success(response.data?.items ?: emptyList())
            } else {
                Result.failure(Exception(response.message.ifEmpty { "加载失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 获取远程资源详情 */
    suspend fun getRemoteResourceDetail(id: String): Result<RemoteResource> {
        return try {
            val response = apiService.getResourceDetail(id)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "加载失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 搜索远程资源 */
    suspend fun searchRemoteResources(
        keyword: String,
        page: Int = 1,
        limit: Int = 10
    ): Result<List<RemoteResource>> {
        return try {
            val response = apiService.searchResources(keyword, page, limit)
            if (response.isSuccess) {
                Result.success(response.data?.items ?: emptyList())
            } else {
                Result.failure(Exception(response.message.ifEmpty { "搜索失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 是否有更多数据 */
    suspend fun hasMoreRemoteResources(page: Int, limit: Int = 10): Boolean {
        return try {
            val response = apiService.getResourceList(page, limit)
            val data = response.data ?: return false
            data.page * data.size < data.total
        } catch (e: Exception) {
            false
        }
    }
}

/** 将 API 返回的格式化大小字符串（如 "24.40KB"）解析为字节数 */
private fun parseSizeString(size: String): Long {
    if (size.isEmpty()) return 0L
    return try {
        val value = size.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: return 0L
        val unit = size.filter { it.isLetter() }.uppercase()
        when (unit) {
            "B" -> value.toLong()
            "KB" -> (value * 1024).toLong()
            "MB" -> (value * 1024 * 1024).toLong()
            "GB" -> (value * 1024 * 1024 * 1024).toLong()
            else -> 0L
        }
    } catch (_: Exception) {
        0L
    }
}

/** Room Entity -> Domain Model */
private fun ResourceEntity.toDomain(): Resource {
    return Resource(
        id = id,
        name = name,
        displayName = displayName,
        description = description,
        icon = icon,
        audioList = audioList,
        size = size,
        publishDate = publishDate,
        installDate = installDate,
        isInstalled = true,
        playMode = playMode,
        orderMode = orderMode,
        loopMode = loopMode,
        currentAudioIndex = currentAudioIndex
    )
}
