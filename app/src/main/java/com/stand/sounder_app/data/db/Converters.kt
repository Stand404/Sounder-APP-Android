package com.stand.sounder_app.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stand.sounder_app.data.model.AudioItem
import com.stand.sounder_app.data.model.LoopMode
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromAudioItemList(value: List<AudioItem>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAudioItemList(value: String): List<AudioItem> {
        val type = TypeToken.getParameterized(List::class.java, AudioItem::class.java).type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromPlayMode(value: PlayMode): String {
        return value.name
    }

    @TypeConverter
    fun toPlayMode(value: String): PlayMode {
        return try {
            PlayMode.valueOf(value)
        } catch (e: Exception) {
            PlayMode.OVERLAY
        }
    }

    @TypeConverter
    fun fromOrderMode(value: OrderMode): String {
        return value.name
    }

    @TypeConverter
    fun toOrderMode(value: String): OrderMode {
        return try {
            OrderMode.valueOf(value)
        } catch (e: Exception) {
            OrderMode.ORDER
        }
    }

    @TypeConverter
    fun fromLoopMode(value: LoopMode): String {
        return value.name
    }

    @TypeConverter
    fun toLoopMode(value: String): LoopMode {
        return try {
            LoopMode.valueOf(value)
        } catch (e: Exception) {
            LoopMode.SINGLE
        }
    }
}
