package br.com.irse.verse.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

class SyncManager(
    private val notesRepository: NotesRepository,
    private val cloudProvider: CloudSyncProvider?,
    private val dispatchers: CoroutineDispatchers
) {
    private val scope = CoroutineScope(dispatchers.io + SupervisorJob())
    private val syncMutex = Mutex()
    private var authSyncJob: Job? = null
    private var periodicSyncJob: Job? = null
    private val autoSyncIntervalMs = 5 * 60 * 1000L
    private val maxBackoffMs = 30 * 60 * 1000L
    
    val syncState: StateFlow<CloudSyncState> = cloudProvider?.syncState ?: MutableStateFlow(CloudSyncState.IDLE).asStateFlow()
    val isAuthorized: StateFlow<Boolean> = cloudProvider?.isAuthorized ?: MutableStateFlow(false).asStateFlow()

    suspend fun authorize() = cloudProvider?.authorize()
    suspend fun signOut() = cloudProvider?.signOut()
    fun onManualCodeEntered(code: String) = cloudProvider?.onManualCodeEntered(code)
    
    fun startAutoSync() {
        if (cloudProvider == null) return
        
        if (authSyncJob?.isActive != true) {
            authSyncJob = scope.launch {
            // Monitora autorização e dispara sync inicial
            cloudProvider.isAuthorized.collect { authorized ->
                    if (authorized) {
                        performFullSync()
                    }
                }
            }
        }

        // Sync periódico a cada 5 minutos
        if (periodicSyncJob?.isActive != true) {
            periodicSyncJob = scope.launch {
                var delayMs = autoSyncIntervalMs
                while (isActive) {
                    delay(delayMs)
                    if (cloudProvider.isAuthorized.value) {
                        performFullSync()
                        delayMs = if (syncState.value == CloudSyncState.ERROR) {
                            min(delayMs * 2, maxBackoffMs)
                        } else {
                            autoSyncIntervalMs
                        }
                    } else {
                        delayMs = autoSyncIntervalMs
                    }
                }
            }
        }
    }

    fun stopAutoSync() {
        authSyncJob?.cancel()
        periodicSyncJob?.cancel()
        authSyncJob = null
        periodicSyncJob = null
    }

    fun dispose() {
        stopAutoSync()
        scope.cancel()
    }

    suspend fun performFullSync() = withContext(dispatchers.io) {
        if (cloudProvider == null || !cloudProvider.isAuthorized.value) return@withContext

        syncMutex.withLock {
            // 1. Download das notas da nuvem
            val cloudNotes = cloudProvider.downloadNotes()
            val localNotes = notesRepository.getNotesForSync()
            val localNotesById = localNotes.associateBy { it.id }
            
            // 2. Mesclar mudanças
            cloudNotes.forEach { cloudNote ->
                val localNote = localNotesById[cloudNote.id]
                
                if (cloudNote.isDeleted) {
                    // Se está deletado na nuvem, garantimos que está deletado localmente
                    if (localNote != null) {
                        notesRepository.hardDeleteNote(cloudNote.id)
                    }
                    return@forEach
                }

                when {
                    localNote == null -> {
                        notesRepository.saveNote(cloudNote.copy(syncStatus = SyncStatus.SYNCED))
                    }
                    localNote.syncStatus == SyncStatus.CONFLICT -> { }
                    localNote.syncStatus == SyncStatus.PENDING -> {
                        if (cloudNote.updatedAt > localNote.updatedAt && cloudNote.content != localNote.content) {
                            notesRepository.saveNote(localNote.copy(syncStatus = SyncStatus.CONFLICT))
                        } else if (cloudNote.updatedAt > localNote.updatedAt) {
                            notesRepository.saveNote(cloudNote.copy(syncStatus = SyncStatus.SYNCED))
                        }
                    }
                    cloudNote.updatedAt > localNote.updatedAt -> {
                        notesRepository.saveNote(cloudNote.copy(syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
            
            // 3. Upload de mudanças locais pendentes (incluindo deleções)
            notesRepository.getNotesForSync().filter { it.syncStatus == SyncStatus.PENDING }.forEach { localNote ->
                cloudProvider.uploadNote(localNote)
                if (syncState.value != CloudSyncState.ERROR) {
                    if (localNote.isDeleted) {
                        // Se era uma deleção e foi enviada com sucesso, podemos remover localmente
                        notesRepository.hardDeleteNote(localNote.id)
                    } else {
                        notesRepository.saveNote(localNote.copy(syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
        }
    }
}
