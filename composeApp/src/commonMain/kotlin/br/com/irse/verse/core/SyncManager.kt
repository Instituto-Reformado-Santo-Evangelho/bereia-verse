package br.com.irse.verse.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SyncManager(
    private val notesRepository: NotesRepository,
    private val cloudProvider: CloudSyncProvider?,
    private val dispatchers: CoroutineDispatchers
) {
    private val scope = CoroutineScope(dispatchers.io + SupervisorJob())
    
    val syncState: StateFlow<CloudSyncState> = cloudProvider?.syncState ?: MutableStateFlow(CloudSyncState.IDLE).asStateFlow()
    val isAuthorized: StateFlow<Boolean> = cloudProvider?.isAuthorized ?: MutableStateFlow(false).asStateFlow()

    suspend fun authorize() = cloudProvider?.authorize()
    suspend fun signOut() = cloudProvider?.signOut()
    
    fun startAutoSync() {
        if (cloudProvider == null) return
        
        scope.launch {
            // Monitora autorização e dispara sync inicial
            cloudProvider.isAuthorized.collect { authorized ->
                if (authorized) {
                    performFullSync()
                }
            }
        }

        // Sync periódico a cada 5 minutos
        scope.launch {
            while (isActive) {
                delay(5 * 60 * 1000)
                if (cloudProvider.isAuthorized.value) {
                    performFullSync()
                }
            }
        }
    }

    suspend fun performFullSync() = withContext(dispatchers.io) {
        if (cloudProvider == null || !cloudProvider.isAuthorized.value) return@withContext
        
        try {
            // 1. Download das notas da nuvem
            val cloudNotes = cloudProvider.downloadNotes()
            val localNotes = notesRepository.notes.value
            
            // 2. Mesclar mudanças
            // Lógica simples: updatedAt mais recente vence
            cloudNotes.forEach { cloudNote ->
                val localNote = localNotes.find { it.id == cloudNote.id }
                if (localNote == null || cloudNote.updatedAt > localNote.updatedAt) {
                    notesRepository.saveNote(cloudNote.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
            
            // 3. Upload de notas locais pendentes
            notesRepository.notes.value.filter { it.syncStatus == SyncStatus.PENDING }.forEach { localNote ->
                cloudProvider.uploadNote(localNote)
                notesRepository.saveNote(localNote.copy(syncStatus = SyncStatus.SYNCED))
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
