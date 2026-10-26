// Implements: SCR-HOME / SCR-CATALOG / SCR-SAVED / FUN-SELECT-TEMPLATE / FUN-SAVE-LOCAL / FUN-OFFLINE-MODE
package com.openkitmaker.native.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KitDao {
    @Query("SELECT * FROM kits ORDER BY updated_at DESC")
    fun observeAllKits(): Flow<List<KitEntity>>

    @Query("SELECT * FROM kits ORDER BY updated_at DESC LIMIT 3")
    fun observeRecentKits(): Flow<List<KitEntity>>

    @Query("SELECT * FROM kits WHERE id = :kitId LIMIT 1")
    suspend fun getKitById(kitId: String): KitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKit(entity: KitEntity)

    @Query("UPDATE kits SET name = :newName, updated_at = :updatedAt WHERE id = :kitId")
    suspend fun renameKit(kitId: String, newName: String, updatedAt: String)

    @Query("DELETE FROM kits WHERE id = :kitId")
    suspend fun deleteKitById(kitId: String)

    @Query("SELECT * FROM templates ORDER BY name ASC")
    fun observeTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun countTemplates(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TemplateEntity>)
}
