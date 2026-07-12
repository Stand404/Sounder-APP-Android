package com.stand.sounder_app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stand.sounder_app.data.model.AudioItem
import com.stand.sounder_app.data.model.LoopMode
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode

@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val displayName: String,
    val description: String = "",
    val icon: String = "",
    val audioList: List<AudioItem> = emptyList(),
    val size: Long = 0L,
    val publishDate: String = "",
    val installDate: Long = System.currentTimeMillis(),
    val playMode: PlayMode = PlayMode.OVERLAY,
    val orderMode: OrderMode = OrderMode.ORDER,
    val loopMode: LoopMode = LoopMode.SINGLE,
    val currentAudioIndex: Int = 0
)
