package com.trustmesh.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidence_entries",
    foreignKeys = [
        ForeignKey(
            entity = InteractionEntity::class,
            parentColumns = ["id"],
            childColumns = ["interactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["interactionId"])]
)
data class EvidenceEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val interactionId: String,
    val content: String,
    val orderIndex: Int
)
