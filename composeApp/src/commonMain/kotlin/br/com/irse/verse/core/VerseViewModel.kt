package br.com.irse.verse.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class VerseViewModel(
    private val parser: BibleParser,
    private val database: BibleDatabase,
    private val snapshotHandler: SnapshotHandler,
    private val settingsRepository: SettingsRepository,
    private val searchUseCase: SearchUseCase,
    private val historyRepository: HistoryRepository,
    private val notesRepository: NotesRepository,
    private val syncManager: SyncManager,
    private val dispatchers: CoroutineDispatchers = CoroutineDispatchers()
) {
    private val viewModelScope = CoroutineScope(dispatchers.main + SupervisorJob())
    
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState = _errorState.asStateFlow()

    fun clearError() { _errorState.value = null }

    private val _detectedVerses = MutableStateFlow<List<Pair<VerseRequest, String?>>>(emptyList())
    val detectedVerses = _detectedVerses.asStateFlow()

    private var currentOriginalVerses: List<Pair<VerseRequest, String?>> = emptyList()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _noteSearchResults = MutableStateFlow<List<Note>>(emptyList())
    val noteSearchResults = _noteSearchResults.asStateFlow()

    enum class SearchScope { VERSES, NOTES }
    private val _searchScope = MutableStateFlow(SearchScope.VERSES)
    val searchScope = _searchScope.asStateFlow()

    val history = historyRepository.history
    val notes = notesRepository.notes

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isInternalUpdate = MutableStateFlow(false)
    val isInternalUpdate = _isInternalUpdate.asStateFlow()

    private val backStack = java.util.Stack<List<Pair<VerseRequest, String?>>>()
    private val forwardStack = java.util.Stack<List<Pair<VerseRequest, String?>>>()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward = _canGoForward.asStateFlow()

    val fontSize = settingsRepository.settings.map { it.fontSize }.stateIn(viewModelScope, SharingStarted.Lazily, 16)
    val fontFamily = settingsRepository.settings.map { it.fontFamily }.stateIn(viewModelScope, SharingStarted.Lazily, "sans-serif")
    val lineHeight = settingsRepository.settings.map { it.lineHeight }.stateIn(viewModelScope, SharingStarted.Lazily, 1.4f)
    val showFireAnimation = settingsRepository.settings.map { it.showFireAnimation }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val animatedWindow = settingsRepository.settings.map { it.animatedWindow }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val signature = settingsRepository.settings.map { it.signature }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    private val _textColor = MutableStateFlow(androidx.compose.ui.graphics.Color(0xFF333333))
    val textColor = _textColor.asStateFlow()

    private val _borderColor = MutableStateFlow(androidx.compose.ui.graphics.Color.LightGray)
    val borderColor = _borderColor.asStateFlow()

    private val _backgroundColor = MutableStateFlow(androidx.compose.ui.graphics.Color.White)
    val backgroundColor = _backgroundColor.asStateFlow()

    data class SnapshotTemplate(
        val id: String, 
        val displayName: String, 
        val backgroundBrush: androidx.compose.ui.graphics.Brush, 
        val contentColor: androidx.compose.ui.graphics.Color,
        val fontFamilyName: String = "SansSerif",
        val borderColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent
    )

    val templatesList = listOf(
        SnapshotTemplate(
            id = "classic", displayName = "Clássico", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFFFFC107), androidx.compose.ui.graphics.Color(0xFFFFD54F))), 
            contentColor = androidx.compose.ui.graphics.Color(0xFF333333)
        ),
        SnapshotTemplate(
            id = "dark_modern", displayName = "Dark", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF1E1E1E), androidx.compose.ui.graphics.Color(0xFF2C2C2C))), 
            contentColor = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
        ),
        SnapshotTemplate(
            id = "sunset", displayName = "Pôr do Sol", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFFff512f), androidx.compose.ui.graphics.Color(0xFFdd2476))), 
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        SnapshotTemplate(
            id = "ocean", displayName = "Oceano", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF2193b0), androidx.compose.ui.graphics.Color(0xFF6dd5ed))), 
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        SnapshotTemplate(
            id = "elegant", displayName = "Elegante", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFFF5F5DC), androidx.compose.ui.graphics.Color(0xFFE8E8C8))), 
            contentColor = androidx.compose.ui.graphics.Color(0xFF2C1B18), fontFamilyName = "Serif"
        ),
        SnapshotTemplate(
            id = "night", displayName = "Noite", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.radialGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF1A237E), androidx.compose.ui.graphics.Color(0xFF000000))), 
            contentColor = androidx.compose.ui.graphics.Color.White
        )
    )

    private val _selectedTemplate = MutableStateFlow(templatesList.first())
    val selectedTemplate = _selectedTemplate.asStateFlow()

    // Estado do Editor de Notas persistente
    private val _isNoteEditorOpen = MutableStateFlow(false)
    val isNoteEditorOpen = _isNoteEditorOpen.asStateFlow()

    private val _editingVerseRequest = MutableStateFlow<VerseRequest?>(null)
    val editingVerseRequest = _editingVerseRequest.asStateFlow()

    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote = _editingNote.asStateFlow()

    // Sync States exposed from Manager/Provider
    val syncState = syncManager.syncState
    val isSyncAuthorized = syncManager.isAuthorized

    fun loginToDrive() {
        viewModelScope.launch {
            try {
                syncManager.authorize()
            } catch (e: Exception) {
                _errorState.value = "Erro ao conectar Google Drive: ${e.message}"
            }
        }
    }

    fun logoutDrive() {
        viewModelScope.launch {
            try {
                syncManager.signOut()
            } catch (e: Exception) {
                _errorState.value = "Erro ao desconectar: ${e.message}"
            }
        }
    }

    fun openNoteEditor(request: VerseRequest? = null, note: Note? = null) {
        _editingVerseRequest.value = request
        _editingNote.value = note
        _isNoteEditorOpen.value = true
    }

    fun closeNoteEditor() {
        _isNoteEditorOpen.value = false
        _editingVerseRequest.value = null
        _editingNote.value = null
    }

    fun setTemplate(template: SnapshotTemplate) { _selectedTemplate.value = template }

    init {
        viewModelScope.launch {
            try {
                historyRepository.loadHistory()
                settingsRepository.loadSettings()
                notesRepository.loadNotes()
            } catch (e: Exception) {
                _errorState.value = "Falha ao carregar dados iniciais: ${e.message}"
            }
        }

        viewModelScope.launch {
            _searchQuery.debounce(300).distinctUntilChanged().filter { it.length >= 2 }.collect { query -> performSearch(query) }
        }
    }

    private fun updateNavigationState() {
        _canGoBack.value = !backStack.isEmpty()
        _canGoForward.value = !forwardStack.isEmpty()
    }

    private fun pushToBackStack() {
        if (currentOriginalVerses.isNotEmpty()) {
            backStack.push(currentOriginalVerses)
            forwardStack.clear()
            updateNavigationState()
        }
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            val current = currentOriginalVerses
            if (current.isNotEmpty()) forwardStack.push(current)
            val previous = backStack.pop()
            _detectedVerses.value = previous
            currentOriginalVerses = previous
            updateNavigationState()
            _isInternalUpdate.value = true
        }
    }

    fun navigateForward() {
        if (forwardStack.isNotEmpty()) {
            val current = currentOriginalVerses
            if (current.isNotEmpty()) backStack.push(current)
            val next = forwardStack.pop()
            _detectedVerses.value = next
            currentOriginalVerses = next
            updateNavigationState()
            _isInternalUpdate.value = true
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.length < 2) {
            _searchResults.value = emptyList()
            _noteSearchResults.value = emptyList()
        }
    }

    fun setSearchScope(scope: SearchScope) {
        _searchScope.value = scope
        // Re-executa a busca com a query atual no novo escopo
        val currentQuery = _searchQuery.value
        if (currentQuery.length >= 2) {
             viewModelScope.launch { performSearch(currentQuery) }
        } else {
             _searchResults.value = emptyList()
             _noteSearchResults.value = emptyList()
        }
    }

    private suspend fun performSearch(query: String) {
        try {
            if (_searchScope.value == SearchScope.NOTES) {
                _noteSearchResults.value = notesRepository.searchNotes(query)
                _searchResults.value = emptyList()
            } else {
                _searchResults.value = searchUseCase.execute(query)
                _noteSearchResults.value = emptyList()
            }
        } catch (e: Exception) {
            _errorState.value = "Erro na busca: ${e.message}"
        }
    }
    
    fun selectVerse(verseId: Int) {
        pushToBackStack()
        _isInternalUpdate.value = true
        val req = parser.repository.getVerseRequest(verseId)
        if (req != null) {
            viewModelScope.launch {
                try {
                    val content = withContext(dispatchers.io) { database.getText(req.id) }
                    val results = listOf(req to content)
                    _detectedVerses.value = results
                    currentOriginalVerses = results
                } catch (e: Exception) {
                    _errorState.value = "Erro ao selecionar versículo: ${e.message}"
                }
            }
        }
    }

    fun processQuery(text: String, addToHistory: Boolean = true, isExternal: Boolean = false) {
        val wasInternal = !isExternal
        _isInternalUpdate.value = wasInternal
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val requests = withContext(dispatchers.io) { parser.processSelection(text) }
                if (requests.isNotEmpty()) {
                    val currentFirst = _detectedVerses.value.firstOrNull()?.first
                    val newFirst = requests.firstOrNull()
                    if (currentFirst?.id != newFirst?.id) pushToBackStack()
                    val results = withContext(dispatchers.io) { requests.map { req -> database.getText(req.id) to req }.map { it.second to it.first } }
                    _detectedVerses.value = results
                    currentOriginalVerses = results
                    if (addToHistory) {
                        val historyLabel = formatHistoryLabel(requests)
                        historyRepository.saveEntry(historyLabel)
                    }
                }
            } catch (e: Exception) {
                _errorState.value = "Erro ao processar texto: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun formatHistoryLabel(requests: List<VerseRequest>): String {
        if (requests.isEmpty()) return ""
        val first = requests.first()
        val last = requests.last()
        return if (requests.size == 1) "${first.book} ${first.chapter}:${first.verse}"
        else if (first.book == last.book && first.chapter == last.chapter) "${first.book} ${first.chapter}:${first.verse}-${last.verse}"
        else if (first.book == last.book) "${first.book} ${first.chapter}:${first.verse} - ${last.chapter}:${last.verse}"
        else {
            val uniqueBooks = requests.map { it.book }.distinct()
            if (uniqueBooks.size > 1) "${first.book} ${first.chapter}:${first.verse} (+${uniqueBooks.size - 1} livros)"
            else "${first.book} ${first.chapter}:${first.verse}..."
        }
    }

    fun selectVerse(request: VerseRequest) {
        pushToBackStack()
        viewModelScope.launch {
            try {
                val content = withContext(dispatchers.io) { database.getText(request.id) }
                val results = listOf(request to content)
                _detectedVerses.value = results
                currentOriginalVerses = results
            } catch (e: Exception) {
                _errorState.value = "Erro ao abrir versículo: ${e.message}"
            }
        }
    }

    fun loadContext(direction: Int) {
        val currentList = _detectedVerses.value
        if (currentList.isEmpty()) return
        viewModelScope.launch {
            try {
                val newVerses = currentList.toMutableList()
                if (direction < 0) {
                    val firstId = currentList.first().first.id
                    val prevId = firstId - 1
                    if (prevId > 0) {
                        val content = withContext(dispatchers.io) { database.getText(prevId) }
                        if (content != null) {
                            val req = parser.repository.getVerseRequest(prevId)
                            if (req != null) newVerses.add(0, req to content)
                        }
                    }
                } else {
                    val lastId = currentList.last().first.id
                    val nextId = lastId + 1
                    if (nextId < 32000) { 
                        val content = withContext(dispatchers.io) { database.getText(nextId) }
                        if (content != null) {
                            val req = parser.repository.getVerseRequest(nextId)
                            if (req != null) newVerses.add(req to content)
                        }
                    }
                }
                _detectedVerses.value = newVerses
                _isInternalUpdate.value = true
            } catch (e: Exception) {
                _errorState.value = "Erro ao carregar contexto: ${e.message}"
            }
        }
    }

    fun removeContext(direction: Int) {
        val currentList = _detectedVerses.value
        if (currentList.size <= 1) return
        val newVerses = currentList.toMutableList()
        if (direction < 0) newVerses.removeAt(0) else newVerses.removeAt(newVerses.lastIndex)
        _detectedVerses.value = newVerses
        _isInternalUpdate.value = true
    }
    
    fun refreshHistory() {
        viewModelScope.launch {
             try { historyRepository.loadHistory() } catch (e: Exception) { _errorState.value = "Erro ao atualizar histórico: ${e.message}" }
        }
    }

    fun saveNote(verseId: Int?, content: String) {
        viewModelScope.launch {
            try {
                val existingNote = verseId?.let { notesRepository.getNoteForVerse(it) }
                val now = currentTimeMillis()
                val note = Note(id = existingNote?.id ?: generateUuid(), verseId = verseId, content = content, createdAt = existingNote?.createdAt ?: now, updatedAt = now, syncStatus = SyncStatus.PENDING)
                notesRepository.saveNote(note)
            } catch (e: Exception) {
                _errorState.value = "Erro ao salvar nota: ${e.message}"
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try { notesRepository.deleteNote(noteId) } catch (e: Exception) { _errorState.value = "Erro ao excluir nota: ${e.message}" }
        }
    }

    fun getVerseReference(verseId: Int): String? {
        val req = parser.repository.getVerseRequest(verseId) ?: return null
        return "${req.book} ${req.chapter}:${req.verse}"
    }

    fun triggerSync() {
        viewModelScope.launch {
            try {
                syncManager.performFullSync()
            } catch (e: Exception) {
                _errorState.value = "Erro na sincronização: ${e.message}"
            }
        }
    }

    fun updateFontSize(size: Int) { viewModelScope.launch { settingsRepository.updateFontSize(size) } }
    fun updateFontFamily(family: String) { viewModelScope.launch { settingsRepository.updateFontFamily(family) } }
    fun updateLineHeight(height: Float) { viewModelScope.launch { settingsRepository.updateLineHeight(height) } }
    fun updateShowFireAnimation(enabled: Boolean) { viewModelScope.launch { settingsRepository.updateShowFireAnimation(enabled) } }
    fun updateAnimatedWindow(enabled: Boolean) { viewModelScope.launch { settingsRepository.updateAnimatedWindow(enabled) } }
    fun updateSignature(text: String) { viewModelScope.launch { settingsRepository.updateSignature(text) } }

    fun captureSnapshot() {
        val currentVerses = _detectedVerses.value
        if (currentVerses.isEmpty()) return
        val template = _selectedTemplate.value
        viewModelScope.launch {
            _isProcessing.value = true
            try { snapshotHandler.captureAndSave(currentVerses, template) } catch (e: Exception) { _errorState.value = "Erro ao capturar imagem: ${e.message}"; e.printStackTrace() } finally { _isProcessing.value = false }
        }
    }

    fun captureNoteSnapshot(note: Note? = null, editorContent: String? = null) {
        val targetNote = note ?: _editingNote.value
        val content = editorContent ?: targetNote?.content ?: return
        val ref = targetNote?.verseId?.let { getVerseReference(it) } ?: _editingVerseRequest.value?.let { "${it.book} ${it.chapter}:${it.verse}" }
        val template = _selectedTemplate.value
        val sign = signature.value

        viewModelScope.launch {
            _isProcessing.value = true
            try {
                snapshotHandler.captureNoteAndSave(content, ref, sign, template)
            } catch (e: Exception) {
                _errorState.value = "Erro ao capturar nota: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
