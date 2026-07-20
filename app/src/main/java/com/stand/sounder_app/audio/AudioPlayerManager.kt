package com.stand.sounder_app.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.stand.sounder_app.data.model.AudioItem
import com.stand.sounder_app.data.model.LoopMode
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 后台播放任务信息
 */
data class PlaybackTaskInfo(
    val resourceId: String = "",
    val displayName: String = "",
    val shortName: String = "",
    val icon: String = "",
    val currentAudioName: String = "",
    val playModeText: String = "",
    val activePlayerCount: Int = 0,
    val isActive: Boolean = false,
    val createdAt: Long = 0L
)

data class AudioPlayState(
    val resourceId: String = "",
    val currentAudioId: String = "",
    val currentAudioName: String = "",
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val playMode: PlayMode = PlayMode.OVERLAY,
    val orderMode: OrderMode = OrderMode.ORDER
)

class AudioPlayerManager(private val context: Context) {

    // 实例计数器，保证每次 play() 生成唯一 instanceId
    private var instanceCounter = 0L
    // instanceId -> MediaPlayer
    private val activePlayers = mutableMapOf<String, MediaPlayer>()
    // instanceId -> audioId
    private val instanceAudioIdMap = mutableMapOf<String, String>()
    // audioId -> 全部活跃的 instanceId
    private val audioIdInstances = mutableMapOf<String, MutableSet<String>>()
    // instanceId -> resourceId（用于按资源统计/停止任务）
    private val instanceResourceMap = mutableMapOf<String, String>()

    // MediaPlayer 对象池
    private val playerPool = mutableListOf<MediaPlayer>()
    private val maxPoolSize = 5

    private val _playState = MutableStateFlow(AudioPlayState())
    val playState: StateFlow<AudioPlayState> = _playState.asStateFlow()

    // 后台播放任务（按资源聚合）
    private val resourceTasks = mutableMapOf<String, PlaybackTaskInfo>()
    private val resourceInstanceCount = mutableMapOf<String, Int>()
    private val _tasks = MutableStateFlow<List<PlaybackTaskInfo>>(emptyList())
    val tasks: StateFlow<List<PlaybackTaskInfo>> = _tasks.asStateFlow()

    private var playMode: PlayMode = PlayMode.OVERLAY
    private var orderMode: OrderMode = OrderMode.ORDER
    private var loopMode: LoopMode = LoopMode.SINGLE
    private var currentAudioList: List<AudioItem> = emptyList()
    private var currentIndex: Int = 0

    /** 每个资源的循环状态（支持多资源并发循环，参照 C# _loopStates） */
    private data class LoopState(
        val resourceId: String,
        val audioList: List<AudioItem>,
        var index: Int,
        val loopMode: LoopMode,
        val orderMode: OrderMode,
        var instanceId: String = ""
    )
    private val loopStates = mutableMapOf<String, LoopState>()

    /** 仅更新播放设置（不停止当前播放） */
    fun setPlaySettings(mode: PlayMode, order: OrderMode, loop: LoopMode = LoopMode.SINGLE) {
        playMode = mode
        orderMode = order
        loopMode = loop
    }

    /**
     * 播放指定音频，返回唯一 instanceId（可用于后续停止该实例）
     * @param resourceId 资源唯一标识（用于任务聚合）
     * @param displayName 资源显示名
     * @param onPlaying 回调参数为 instanceId
     * @param onFinish 回调参数为 instanceId
     */
    fun play(
        audioList: List<AudioItem>,
        resourceId: String,
        displayName: String,
        shortName: String = "",
        icon: String = "",
        startIndex: Int = 0,
        onPlaying: ((String) -> Unit)? = null,
        onFinish: ((String) -> Unit)? = null,
        /** 列表循环切歌时回调，供调用方持久化当前索引（跨重启衔接） */
        onLoopIndexChanged: ((Int) -> Unit)? = null
    ): String? {
        if (audioList.isEmpty()) return null
        currentAudioList = audioList
        currentIndex = startIndex.coerceIn(0, audioList.lastIndex)

        // 非循环模式下切换播放，清理可能残留的循环状态，避免旧循环误续播
        if (playMode != PlayMode.LOOP) {
            loopStates.remove(resourceId)
        }

        ensureTask(resourceId, displayName, shortName, icon)

        return when (playMode) {
            PlayMode.OVERLAY -> playOverlay(audioList[currentIndex], resourceId, onPlaying, onFinish)
            PlayMode.REPLACE -> playReplace(audioList[currentIndex], resourceId, onPlaying, onFinish)
            PlayMode.LOOP -> startLoop(
                resourceId = resourceId,
                audioList = audioList,
                startIndex = currentIndex,
                onPlaying = onPlaying,
                onFinish = onFinish,
                onLoopIndexChanged = onLoopIndexChanged
            )
        }
    }

    /** 叠加模式：每次调用创建独立实例 */
    private fun playOverlay(
        audioItem: AudioItem,
        resourceId: String,
        onPlaying: ((String) -> Unit)? = null,
        onFinish: ((String) -> Unit)? = null
    ): String? {
        val instanceId = nextInstanceId(audioItem.id)
        val player = createPlayer(audioItem,
            onPlaying = { onPlaying?.invoke(instanceId) },
            onCompletion = {
                recycleInstance(instanceId)
                onFinish?.invoke(instanceId)
            }
        ) ?: return null
        registerInstance(instanceId, audioItem.id, resourceId, player)
        updateState(resourceId, audioItem, isPlaying = true)
        return instanceId
    }

    /** 替换模式：停止全部后创建新实例 */
    private fun playReplace(
        audioItem: AudioItem,
        resourceId: String,
        onPlaying: ((String) -> Unit)? = null,
        onFinish: ((String) -> Unit)? = null
    ): String? {
        stopAll()
        val instanceId = nextInstanceId(audioItem.id)
        val player = createPlayer(audioItem,
            onPlaying = { onPlaying?.invoke(instanceId) },
            onCompletion = {
                recycleInstance(instanceId)
                onFinish?.invoke(instanceId)
            }
        ) ?: return null
        registerInstance(instanceId, audioItem.id, resourceId, player)
        updateState(resourceId, audioItem, isPlaying = true)
        return instanceId
    }

    /**
     * 循环模式入口：先停止该资源自身的旧实例（仅本资源，不干扰其它资源叠加播放），
     * 再按 loopMode 启动单曲/列表循环。参照 C# StartLoop（使用 Overlay 式独立实例，
     * 由每次播放完成事件驱动续播，从而支持多资源并发循环）。
     */
    private fun startLoop(
        resourceId: String,
        audioList: List<AudioItem>,
        startIndex: Int,
        onPlaying: ((String) -> Unit)?,
        onFinish: ((String) -> Unit)?,
        onLoopIndexChanged: ((Int) -> Unit)?
    ): String? {
        // 停止该资源当前的播放实例，但【保留任务聚合】（由后续 registerInstance 重新计数），
        // 避免 stopResource 把任务清掉，导致「任务管理」里看不到本循环任务、且无法停止。
        val ids = activePlayers.keys.filter { instanceResourceMap[it] == resourceId }
        for (id in ids) {
            activePlayers.remove(id)?.let { recyclePlayer(it) }
            instanceResourceMap.remove(id)
            instanceAudioIdMap.remove(id)
            audioIdInstances.forEach { (_, set) -> set.remove(id) }
        }
        resourceInstanceCount.remove(resourceId)
        loopStates.remove(resourceId)
        val index = startIndex.coerceIn(0, audioList.lastIndex)
        val state = LoopState(
            resourceId = resourceId,
            audioList = audioList,
            index = index,
            loopMode = loopMode,
            orderMode = orderMode
        )
        loopStates[resourceId] = state
        val item = audioList.getOrNull(index) ?: return null
        return playLoopItem(state, item, onPlaying, onFinish, onLoopIndexChanged)
    }

    /** 为循环状态播放单个音频，返回 instanceId */
    private fun playLoopItem(
        state: LoopState,
        audioItem: AudioItem,
        onPlaying: ((String) -> Unit)?,
        onFinish: ((String) -> Unit)?,
        onLoopIndexChanged: ((Int) -> Unit)?
    ): String? {
        val resourceId = state.resourceId
        val instanceId = nextInstanceId(audioItem.id)
        val player = createPlayer(
            audioItem,
            onPlaying = { onPlaying?.invoke(instanceId) },
            onCompletion = {
                onLoopCompletion(instanceId, state, onPlaying, onFinish, onLoopIndexChanged)
            }
        ) ?: return null
        registerInstance(instanceId, audioItem.id, resourceId, player)
        state.instanceId = instanceId
        updateState(resourceId, audioItem, isPlaying = true)
        return instanceId
    }

    /** 循环实例播放完成：单曲 seek 重播 / 列表切下一首 */
    private fun onLoopCompletion(
        instanceId: String,
        state: LoopState,
        onPlaying: ((String) -> Unit)?,
        onFinish: ((String) -> Unit)?,
        onLoopIndexChanged: ((Int) -> Unit)?
    ) {
        // 实例已不再属于该资源（被停止/回收）→ 不再续播，避免误触发
        if (instanceResourceMap[instanceId] != state.resourceId) return

        try {
            when (state.loopMode) {
                LoopMode.SINGLE -> {
                    activePlayers[instanceId]?.let { player ->
                        player.seekTo(0)
                        player.start()
                        onPlaying?.invoke(instanceId)
                    }
                }
                LoopMode.LIST -> {
                    val next = computeNextLoopIndex(state)
                    state.index = next
                    onLoopIndexChanged?.invoke(next)
                    val nextItem = state.audioList.getOrNull(next) ?: return
                    val newPlayer = createPlayer(
                        nextItem,
                        onPlaying = { onPlaying?.invoke(instanceId) },
                        onCompletion = {
                            onLoopCompletion(instanceId, state, onPlaying, onFinish, onLoopIndexChanged)
                        }
                    ) ?: run {
                        recycleInstance(instanceId)
                        loopStates.remove(state.resourceId)
                        onFinish?.invoke(instanceId)
                        return
                    }
                    // 复用同一 instanceId，替换其内部播放器与音频映射
                    attachNewPlayer(instanceId, nextItem, newPlayer, state.resourceId)
                }
            }
        } catch (_: Exception) {
            recycleInstance(instanceId)
            loopStates.remove(state.resourceId)
            onFinish?.invoke(instanceId)
        }
    }

    /** 计算列表循环的下一索引（顺序/随机），参照 C# SelectAudio */
    private fun computeNextLoopIndex(state: LoopState): Int {
        val size = state.audioList.size
        if (size <= 1) return state.index
        return if (state.orderMode == OrderMode.RANDOM) {
            // 随机取一个不同于当前的下标
            var r = (state.audioList.indices).random()
            if (r == state.index) r = (r + 1) % size
            r
        } else {
            (state.index + 1) % size
        }
    }

    /** 列表循环切换音频时，用新播放器替换同一 instanceId 的内部状态（保持任务计数不变） */
    private fun attachNewPlayer(instanceId: String, audioItem: AudioItem, player: MediaPlayer, resourceId: String) {
        val oldAudioId = instanceAudioIdMap[instanceId]
        if (oldAudioId != null) {
            audioIdInstances[oldAudioId]?.remove(instanceId)
            if (audioIdInstances[oldAudioId]?.isEmpty() == true) audioIdInstances.remove(oldAudioId)
        }
        val oldPlayer = activePlayers.put(instanceId, player)
        instanceAudioIdMap[instanceId] = audioItem.id
        audioIdInstances.getOrPut(audioItem.id) { mutableSetOf() }.add(instanceId)
        oldPlayer?.let { recyclePlayer(it) }
        updateState(resourceId, audioItem, isPlaying = true)
        // 同步任务卡片的「当前音频名」（与 C# taskInfo.CurrentAudioName 一致）
        resourceTasks[resourceId]?.let { t ->
            resourceTasks[resourceId] = t.copy(currentAudioName = audioItem.name)
            emitTasks()
        }
    }

    // ===== 实例管理 =====

    private fun nextInstanceId(audioId: String): String = "${audioId}_${++instanceCounter}"

    private fun registerInstance(instanceId: String, audioId: String, resourceId: String, player: MediaPlayer) {
        activePlayers[instanceId] = player
        instanceAudioIdMap[instanceId] = audioId
        instanceResourceMap[instanceId] = resourceId
        audioIdInstances.getOrPut(audioId) { mutableSetOf() }.add(instanceId)
        bumpTask(resourceId, +1)
    }

    /** 回收某个实例（播放结束调用） */
    private fun recycleInstance(instanceId: String) {
        val player = activePlayers.remove(instanceId) ?: return
        val audioId = instanceAudioIdMap.remove(instanceId)
        val resourceId = instanceResourceMap.remove(instanceId)
        if (audioId != null) {
            audioIdInstances[audioId]?.remove(instanceId)
            if (audioIdInstances[audioId]?.isEmpty() == true) {
                audioIdInstances.remove(audioId)
            }
        }
        recyclePlayer(player)
        if (resourceId != null) bumpTask(resourceId, -1)
        if (activePlayers.isEmpty()) {
            _playState.value = AudioPlayState()
        }
    }

    /** 停止指定 audioId 的所有播放实例 */
    fun stopAudio(audioId: String): List<String> {
        val instanceIds = audioIdInstances.remove(audioId) ?: return emptyList()
        val stopped = mutableListOf<String>()
        val resourceIds = mutableSetOf<String>()
        for (id in instanceIds) {
            activePlayers.remove(id)?.let { recyclePlayer(it) }
            instanceResourceMap.remove(id)?.let { resourceIds.add(it) }
            instanceAudioIdMap.remove(id)
            stopped.add(id)
        }
        resourceIds.forEach { bumpTask(it, -1) }
        if (activePlayers.isEmpty()) {
            _playState.value = AudioPlayState()
        }
        return stopped
    }

    /** 停止指定资源的全部播放（任务管理用） */
    fun stopResource(resourceId: String): List<String> {
        val ids = activePlayers.keys.filter { instanceResourceMap[it] == resourceId }
        for (id in ids) {
            activePlayers.remove(id)?.let { recyclePlayer(it) }
            instanceAudioIdMap.remove(id)
            instanceResourceMap.remove(id)
            audioIdInstances.forEach { (_, set) -> set.remove(id) }
        }
        resourceTasks.remove(resourceId)
        resourceInstanceCount.remove(resourceId)
        loopStates.remove(resourceId)
        emitTasks()
        if (activePlayers.isEmpty()) {
            _playState.value = AudioPlayState()
        }
        return ids
    }

    /** 指定资源是否正在播放（存在活跃实例） */
    fun isResourcePlaying(resourceId: String): Boolean {
        return (resourceInstanceCount[resourceId] ?: 0) > 0
    }

    /** 停止所有播放 */
    fun stopAll(): List<String> {
        val allIds = activePlayers.keys.toList()
        activePlayers.values.forEach { recyclePlayer(it) }
        activePlayers.clear()
        instanceAudioIdMap.clear()
        audioIdInstances.clear()
        instanceResourceMap.clear()
        resourceTasks.clear()
        resourceInstanceCount.clear()
        loopStates.clear()
        emitTasks()
        _playState.value = AudioPlayState()
        return allIds
    }

    // ===== 任务聚合 =====

    private fun ensureTask(resourceId: String, displayName: String, shortName: String, icon: String) {
        val currentAudioName = currentAudioList.getOrNull(currentIndex)?.name ?: ""
        val existing = resourceTasks[resourceId]
        val task = (existing ?: PlaybackTaskInfo(
            resourceId = resourceId,
            displayName = displayName,
            shortName = shortName,
            icon = icon,
            createdAt = System.currentTimeMillis()
        )).copy(
            playModeText = playModeText(),
            isActive = true,
            currentAudioName = currentAudioName
        )
        resourceTasks[resourceId] = task
        emitTasks()
    }

    private fun bumpTask(resourceId: String, delta: Int) {
        val count = (resourceInstanceCount[resourceId] ?: 0) + delta
        if (count <= 0) {
            resourceInstanceCount.remove(resourceId)
            resourceTasks.remove(resourceId)
        } else {
            resourceInstanceCount[resourceId] = count
            val t = resourceTasks[resourceId]
            if (t != null) {
                resourceTasks[resourceId] = t.copy(activePlayerCount = count, isActive = true)
            }
        }
        emitTasks()
    }

    private fun emitTasks() {
        _tasks.value = resourceTasks.values.sortedBy { it.createdAt }.toList()
    }

    private fun playModeText(): String {
        val mode = when (playMode) {
            PlayMode.OVERLAY -> "叠加"
            PlayMode.REPLACE -> "替换"
            PlayMode.LOOP -> when (loopMode) {
                LoopMode.SINGLE -> "单曲循环"
                LoopMode.LIST -> "列表循环"
            }
        }
        val order = when (orderMode) {
            OrderMode.ORDER -> "顺序"
            OrderMode.RANDOM -> "随机"
        }
        return "$mode · $order"
    }

    // ===== 对象池 =====

    private fun obtainPlayer(): MediaPlayer {
        if (playerPool.isNotEmpty()) {
            return playerPool.removeAt(playerPool.size - 1).apply {
                try { reset() } catch (_: Exception) {}
            }
        }
        return MediaPlayer()
    }

    private fun recyclePlayer(player: MediaPlayer) {
        try {
            if (player.isPlaying) player.stop()
            player.reset()
        } catch (_: Exception) {}
        if (playerPool.size < maxPoolSize) {
            playerPool.add(player)
        } else {
            player.release()
        }
    }

    // ===== 内部 =====

    private fun createPlayer(audioItem: AudioItem,
                             onPlaying: () -> Unit,
                             onCompletion: () -> Unit): MediaPlayer? {
        if (audioItem.src.isBlank()) {
            Log.e("AudioPlayerManager", "音频源地址为空: ${audioItem.name}")
            return null
        }
        val player = obtainPlayer()
        try {
            val uri = if (audioItem.src.startsWith("http")) {
                Uri.parse(audioItem.src)
            } else {
                Uri.fromFile(File(audioItem.src))
            }
            player.setDataSource(context, uri)
            player.setOnPreparedListener {
                it.start()
                onPlaying()
            }
            player.prepareAsync()
            player.setOnCompletionListener { onCompletion() }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return player
    }

    private fun updateState(resourceId: String, audioItem: AudioItem, isPlaying: Boolean) {
        _playState.value = AudioPlayState(
            resourceId = resourceId,
            currentAudioId = audioItem.id,
            currentAudioName = audioItem.name,
            isPlaying = isPlaying,
            duration = audioItem.duration,
            playMode = playMode,
            orderMode = orderMode
        )
    }

    /** 释放所有资源 */
    fun release() {
        stopAll()
        playerPool.forEach {
            try { it.release() } catch (_: Exception) {}
        }
        playerPool.clear()
    }
}
