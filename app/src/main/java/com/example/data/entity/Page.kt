package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class Page(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val orderIndex: Int,
    val originalPath: String,
    val processedPath: String,
    val cornersJson: String? = null,
    val filter: String = "ORIGINAL", // ORIGINAL, AUTO, MAGIC_COLOR, BW, GRAYSCALE
    val rotation: Int = 0,
    val ocrText: String? = null
)
