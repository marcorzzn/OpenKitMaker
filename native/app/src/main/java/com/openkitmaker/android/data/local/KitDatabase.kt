// Implements: SCR-HOME / SCR-SAVED / FUN-SAVE-LOCAL / FUN-OFFLINE-MODE
package com.openkitmaker.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [KitEntity::class, TemplateEntity::class],
    version = 2,
    exportSchema = false
)
abstract class KitDatabase : RoomDatabase() {
    abstract fun kitDao(): KitDao
}
