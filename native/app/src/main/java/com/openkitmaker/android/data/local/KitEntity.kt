// Implements: SCR-HOME / SCR-SAVED / FUN-SAVE-LOCAL / FUN-OFFLINE-MODE
package com.openkitmaker.android.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kits")
data class KitEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String,

    @ColumnInfo(name = "template_id")
    val templateId: String,

    @ColumnInfo(name = "layers_json")
    val layersJson: String,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String,

    @ColumnInfo(name = "preview_path")
    val previewPath: String? = null
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "default_layers_json")
    val defaultLayersJson: String
)
