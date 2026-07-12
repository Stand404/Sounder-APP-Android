package com.stand.sounder_app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 资源包数据模型
 */
data class Resource(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String = "",
    val icon: String = "",
    val audioList: List<AudioItem> = emptyList(),
    val size: Long = 0L,
    val publishDate: String = "",
    val installDate: Long = 0L,
    val isInstalled: Boolean = false,
    val playMode: PlayMode = PlayMode.OVERLAY,
    val orderMode: OrderMode = OrderMode.ORDER,
    /** 循环模式细分：单曲循环 / 列表循环（仅 playMode == LOOP 时生效） */
    val loopMode: LoopMode = LoopMode.SINGLE,
    /** 当前播放索引（循环/顺序模式下持久化，跨重启衔接） */
    val currentAudioIndex: Int = 0
)

data class AudioItem(
    val id: String,
    val name: String,
    val src: String,
    val duration: Long = 0L
)

enum class PlayMode {
    @SerializedName("overlay") OVERLAY,
    @SerializedName("replace") REPLACE,
    @SerializedName("loop") LOOP
}

enum class OrderMode {
    @SerializedName("order") ORDER,
    @SerializedName("random") RANDOM
}

/** 循环模式细分（参照 C# LoopMode） */
enum class LoopMode {
    @SerializedName("single") SINGLE, // 单曲循环：锁定当前音频反复播放
    @SerializedName("list") LIST      // 列表循环：按顺序/随机在列表中轮播
}

/**
 * 音频拾取器列表项 —— 参照 Win PickerAudioItem
 */
data class PickerAudioItem(
    val id: String,
    val name: String,
    val src: String,
    val duration: Long = 0L,
    val sourceName: String = ""
)
