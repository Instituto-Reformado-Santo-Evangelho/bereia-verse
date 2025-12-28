package br.com.irse.verse.core

import kotlinx.coroutines.flow.StateFlow

interface CloudSyncProvider {
    val syncState: StateFlow<CloudSyncState>
    val isAuthorized: StateFlow<Boolean>
    
    suspend fun authorize()
    suspend fun signOut()
    
    suspend fun uploadNote(note: Note)
    suspend fun downloadNotes(): List<Note>
    suspend fun deleteNote(noteId: String)
}

enum class CloudSyncState {
    IDLE, SYNCING, SUCCESS, ERROR
}
