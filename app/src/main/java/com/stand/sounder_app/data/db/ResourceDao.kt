package com.stand.sounder_app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.stand.sounder_app.data.model.LoopMode
import com.stand.sounder_app.data.model.OrderMode
import com.stand.sounder_app.data.model.PlayMode
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {

    @Query("SELECT * FROM resources ORDER BY installDate DESC")
    fun getAllResources(): Flow<List<ResourceEntity>>

    @Query("SELECT * FROM resources WHERE id = :id")
    suspend fun getResourceById(id: String): ResourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResources(resources: List<ResourceEntity>)

    @Update
    suspend fun updateResource(resource: ResourceEntity)

    @Delete
    suspend fun deleteResource(resource: ResourceEntity)

    @Query("DELETE FROM resources WHERE id = :id")
    suspend fun deleteResourceById(id: String)

    @Query("SELECT COUNT(*) FROM resources")
    suspend fun getResourceCount(): Int

    @Query("SELECT audioList FROM resources WHERE id = :id")
    suspend fun getAudioListJson(id: String): String?

    @Query("UPDATE resources SET playMode = :playMode WHERE id = :id")
    suspend fun updatePlayMode(id: String, playMode: PlayMode)

    @Query("UPDATE resources SET orderMode = :orderMode WHERE id = :id")
    suspend fun updateOrderMode(id: String, orderMode: OrderMode)

    @Query("UPDATE resources SET loopMode = :loopMode WHERE id = :id")
    suspend fun updateLoopMode(id: String, loopMode: LoopMode)

    @Query("UPDATE resources SET currentAudioIndex = :index WHERE id = :id")
    suspend fun updateCurrentAudioIndex(id: String, index: Int)

    @Query("UPDATE resources SET displayName = :name, description = :description, icon = :icon WHERE id = :id")
    suspend fun updateResourceInfo(id: String, name: String, description: String, icon: String)

    @Query("UPDATE resources SET icon = :icon WHERE id = :id")
    suspend fun updateIcon(id: String, icon: String)

    @Query("DELETE FROM resources")
    suspend fun deleteAll()
}
