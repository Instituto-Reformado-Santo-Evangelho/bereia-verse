package br.com.irse.verse.core

import kotlinx.serialization.Serializable

@Serializable
enum class SyncStatus { PENDING, SYNCED, CONFLICT }

@Serializable
data class Note(
    val id: String,
    val verseId: Int? = null, // Nulo para notas livres
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
