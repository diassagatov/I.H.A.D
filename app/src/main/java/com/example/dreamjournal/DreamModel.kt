package com.example.dreamjournal

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "dreams")
data class DreamEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val interpretation: String = "",
    val status: String = "Draft",
    val createdAt: Long = System.currentTimeMillis()
)

data class DreamUiState(
    val dreams: List<DreamEntity> = emptyList(),
    val selectedDream: DreamEntity? = null,
    val titleInput: String = "",
    val descriptionInput: String = "",
    val interpretationInput: String = "",
    val statusInput: String = "Draft",
    val titleError: String? = null,
    val descriptionError: String? = null,
    val userMessage: String? = null,
    val lastDeletedDream: DreamEntity? = null,
    val isEditing: Boolean = false,
    val editingDreamId: String? = null
)
