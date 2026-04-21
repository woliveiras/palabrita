package com.woliveiras.palabrita.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_config")
data class ModelConfigEntity(
    @PrimaryKey val id: Int = 1,
    val modelId: String = "none",
    val downloadState: String = "NOT_DOWNLOADED",
    val modelPath: String? = null,
    val sizeBytes: Long = 0,
    val selectedAt: Long = 0,
)
