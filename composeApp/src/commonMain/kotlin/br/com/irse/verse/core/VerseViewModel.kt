package br.com.irse.verse.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

import br.com.irse.verse.ui.theme.VerseColors
import org.jetbrains.compose.resources.StringResource
import verse.composeapp.generated.resources.*

data class UiError(
    val resource: StringResource,
    val args: List<Any> = emptyList()
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
class VerseViewModel(
    val parser: BibleParser,
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
    
    private val _errorState = MutableStateFlow<UiError?>(null)
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

    enum class NoteFilter { ALL, FREE, VERSE }
    private val _noteFilter = MutableStateFlow(NoteFilter.ALL)
    val noteFilter = _noteFilter.asStateFlow()

    val history = historyRepository.history
    val notes = notesRepository.notes

    val filteredNotes = combine(notes, _noteFilter) {
        noteList, filter ->
        when (filter) {
            NoteFilter.ALL -> noteList
            NoteFilter.FREE -> noteList.filter { it.verseId == null }
            NoteFilter.VERSE -> noteList.filter { it.verseId != null }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
    val showSnapshotAction = settingsRepository.settings.map { it.showSnapshotAction }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isTransparent = settingsRepository.settings.map { it.isTransparent }.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val isTransparencySupported = settingsRepository.settings.map { it.isTransparencySupported }.stateIn(viewModelScope, SharingStarted.Lazily, true)

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
        val borderColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
        val showLogo: Boolean = true,
        val logoAlpha: Float = 1.0f,
        val showFooter: Boolean = true,
        val useLogoBackground: Boolean = false,
        val textAlignment: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Justify,
        val backgroundImage: String? = null,
        val imageAlpha: Float = 0.6f
    )

    val templatesList = listOf(
        SnapshotTemplate(
            id = "note_pergaminho", displayName = "Pergaminho",
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFFE3DAC9), androidx.compose.ui.graphics.Color(0xFFF0E6D2))),
            contentColor = androidx.compose.ui.graphics.Color(0xFF3E2723), 
            backgroundImage = "note_bg_5.png",
            imageAlpha = 1.0f,
            showLogo = false,
            fontFamilyName = "Serif",
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_papiro", displayName = "Papiro Antigo",
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFFF5DEB3), androidx.compose.ui.graphics.Color(0xFFEEE8AA))),
            contentColor = androidx.compose.ui.graphics.Color(0xFF212121),
            backgroundImage = "note_bg_6.png",
            imageAlpha = 1.0f,
            showLogo = false,
            fontFamilyName = "Serif",
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_pedra", displayName = "Pedra Angular",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF424242), androidx.compose.ui.graphics.Color(0xFF212121))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "note_bg_8.png",
            imageAlpha = 0.9f,
            showLogo = false,
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_manuscrito", displayName = "Manuscrito",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF3E2723), androidx.compose.ui.graphics.Color(0xFF212121))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "note_bg_9.png",
            imageAlpha = 0.85f,
            showLogo = false,
            fontFamilyName = "Serif",
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_oliva", displayName = "Oliva",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF33691E), androidx.compose.ui.graphics.Color(0xFF1B5E20))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "note_bg_10.png",
            imageAlpha = 0.9f,
            showLogo = false,
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_terra", displayName = "Terra Prometida",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF5D4037), androidx.compose.ui.graphics.Color(0xFF3E2723))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "note_bg_11.png",
            imageAlpha = 0.9f,
            showLogo = false,
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_vinho", displayName = "Vinho Novo",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF880E4F), androidx.compose.ui.graphics.Color(0xFF4A148C))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "note_bg_12.png",
            imageAlpha = 0.9f,
            showLogo = false,
            fontFamilyName = "Serif",
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_noite", displayName = "Noite Estrelada",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF1A237E), androidx.compose.ui.graphics.Color(0xFF000051))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "note_bg_13.png",
            imageAlpha = 0.9f,
            showLogo = false,
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_deserto", displayName = "Deserto",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFFE65100), androidx.compose.ui.graphics.Color(0xFFFF6F00))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "note_bg_14.png",
            imageAlpha = 0.9f,
            showLogo = false,
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_sinai", displayName = "Monte Sinai",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF455A64), androidx.compose.ui.graphics.Color(0xFF263238))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "note_bg_15.png",
            imageAlpha = 0.9f,
            showLogo = false,
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_cedro", displayName = "Cedro",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF3E2723), androidx.compose.ui.graphics.Color(0xFF212121))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "note_bg_16.png",
            imageAlpha = 0.9f,
            showLogo = false,
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "note_marmore", displayName = "Mármore",
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFFFAFAFA), androidx.compose.ui.graphics.Color(0xFFF5F5F5))),
            contentColor = androidx.compose.ui.graphics.Color(0xFF212121),
            backgroundImage = "note_bg_17.png",
            imageAlpha = 1.0f,
            showLogo = false,
            fontFamilyName = "Serif",
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "grad_aurora", displayName = "Aurora",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF00c6ff), androidx.compose.ui.graphics.Color(0xFF0072ff))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            showLogo = false, textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "grad_crepusculo", displayName = "Crepúsculo",
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF141E30), androidx.compose.ui.graphics.Color(0xFF243B55))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            showLogo = false, fontFamilyName = "Serif", textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "grad_meianoite", displayName = "Meia-noite",
            backgroundBrush = androidx.compose.ui.graphics.Brush.radialGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF232526), androidx.compose.ui.graphics.Color(0xFF414345))),
            contentColor = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
            showLogo = false, textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "grad_real", displayName = "Realeza",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF5f2c82), androidx.compose.ui.graphics.Color(0xFF49a09d))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            showLogo = false, textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "nature_1", displayName = "Natureza 1",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xCC000000), androidx.compose.ui.graphics.Color(0x66000000))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "bg1.png", imageAlpha = 0.7f, showLogo = false
        ),
        SnapshotTemplate(
            id = "nature_2", displayName = "Natureza 2",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xD9000000), androidx.compose.ui.graphics.Color(0x4D000000))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "bg2.png", imageAlpha = 0.75f, showLogo = false
        ),
        SnapshotTemplate(
            id = "nature_3", displayName = "Natureza 3",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xE6000000), androidx.compose.ui.graphics.Color(0x66000000))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "bg3.png", imageAlpha = 0.65f, showLogo = false
        ),
        SnapshotTemplate(
            id = "nature_4", displayName = "Natureza 4",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xD9000000), androidx.compose.ui.graphics.Color(0x59000000))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "bg4.png", imageAlpha = 0.8f, showLogo = false
        ),
        SnapshotTemplate(
            id = "nature_5", displayName = "Natureza 5",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xCC000000), androidx.compose.ui.graphics.Color(0x4D000000))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "bg5.png", imageAlpha = 0.7f, showLogo = false
        ),
        SnapshotTemplate(
            id = "nature_6", displayName = "Natureza 6",
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xD9000000), androidx.compose.ui.graphics.Color(0x66000000))),
            contentColor = androidx.compose.ui.graphics.Color.White,
            backgroundImage = "bg6.png", imageAlpha = 0.75f, showLogo = false
        ),
        SnapshotTemplate(
            id = "classic", displayName = "Clássico", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(VerseColors.PrimaryAmber, androidx.compose.ui.graphics.Color(0xFFFFD54F))), 
            contentColor = androidx.compose.ui.graphics.Color(0xFF333333),
            showLogo = false
        ),
        SnapshotTemplate(
            id = "exclusive", displayName = "Exclusivo", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xD9000000), androidx.compose.ui.graphics.Color(0x66000000))), 
            contentColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            showLogo = false, showFooter = false, useLogoBackground = true,
            textAlignment = androidx.compose.ui.text.style.TextAlign.Start
        ),
        SnapshotTemplate(
            id = "clean", displayName = "Limpo",
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFFFFFFFF), androidx.compose.ui.graphics.Color(0xFFF0F0F0))), 
            contentColor = androidx.compose.ui.graphics.Color(0xFF333333),
            showLogo = false
        ),
        SnapshotTemplate(
            id = "note_solid_1", displayName = "Nota Solene", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(androidx.compose.ui.graphics.Color(0xFF232526), androidx.compose.ui.graphics.Color(0xFF414345))), 
            contentColor = androidx.compose.ui.graphics.Color(0xFFE0E0E0), 
            fontFamilyName = "Serif", textAlignment = androidx.compose.ui.text.style.TextAlign.Start, showLogo = false, logoAlpha = 0.3f
        )
    )

    private val _selectedTemplate = MutableStateFlow(templatesList.first())
    val selectedTemplate = _selectedTemplate.asStateFlow()

    private val _isNoteEditorOpen = MutableStateFlow(false)
    val isNoteEditorOpen = _isNoteEditorOpen.asStateFlow()

    private val _viewingNote = MutableStateFlow<Note?>(null)
    val viewingNote = _viewingNote.asStateFlow()

    private val _noteToDelete = MutableStateFlow<Note?>(null)
    val noteToDelete = _noteToDelete.asStateFlow()

    enum class ToastType { SUCCESS, ERROR, INFO }
    data class ToastState(val message: String, val type: ToastType = ToastType.SUCCESS)

    private val _toastState = MutableStateFlow<ToastState?>(null)
    val toastState = _toastState.asStateFlow()

    private val _tabRequest = MutableSharedFlow<AppTab>()
    val tabRequest = _tabRequest.asSharedFlow()

    private val _editingVerseRequest = MutableStateFlow<VerseRequest?>(null)
    val editingVerseRequest = _editingVerseRequest.asStateFlow()

    private val _editingNote = MutableStateFlow<Note?>(null)
    val editingNote = _editingNote.asStateFlow()

    private val MAX_SNAPSHOT_CHARS = 600
    private val MAX_SNAPSHOT_VERSES = 5

    val syncState = syncManager.syncState
    val isSyncAuthorized = syncManager.isAuthorized

    fun showToast(message: String, type: ToastType = ToastType.SUCCESS) {
        viewModelScope.launch {
            _toastState.value = ToastState(message, type)
            delay(3000)
            _toastState.value = null
        }
    }

    fun loginToDrive() {
        viewModelScope.launch {
            try {
                showToast("Iniciando autorização... Aguarde o navegador abrir.", ToastType.INFO)
                delay(300) // Garante que o toast apareça antes do navegador
                syncManager.authorize()
                showToast("Login realizado com sucesso!", ToastType.SUCCESS)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Erro desconhecido"
                showToast("Falha no login: $errorMsg", ToastType.ERROR)
                e.printStackTrace()
            }
        }
    }

    fun logoutDrive() {
        viewModelScope.launch {
            try {
                syncManager.signOut()
                showToast("Conta desconectada com sucesso.")
            } catch (e: Exception) {
                showToast("Erro ao desconectar: ${e.message}", ToastType.ERROR)
            }
        }
    }

    fun openNoteEditor(request: VerseRequest? = null, note: Note? = null) {
        _editingVerseRequest.value = request
        _editingNote.value = note
        _isNoteEditorOpen.value = true
        _viewingNote.value = null
    }

    fun closeNoteEditor() {
        _isNoteEditorOpen.value = false
        _editingVerseRequest.value = null
        _editingNote.value = null
    }

    fun openNoteViewer(note: Note) {
        _viewingNote.value = note
        _isNoteEditorOpen.value = false
    }

    fun closeNoteViewer() {
        _viewingNote.value = null
    }

    fun setTemplate(template: SnapshotTemplate) { _selectedTemplate.value = template }

    init {
        viewModelScope.launch {
            try {
                historyRepository.loadHistory()
                settingsRepository.loadSettings()
                notesRepository.loadNotes()
            } catch (e: Exception) {
                _errorState.value = UiError(Res.string.error_load_data, listOf(e.message ?: ""))
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

    fun setNoteFilter(filter: NoteFilter) { _noteFilter.value = filter }

    fun setSearchScope(scope: SearchScope) {
        _searchScope.value = scope
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
            _errorState.value = UiError(Res.string.error_search, listOf(e.message ?: ""))
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
                    _errorState.value = UiError(Res.string.error_select_verse, listOf(e.message ?: ""))
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
                val requests = withContext(dispatchers.io) { parser.processSelection(text, strict = isExternal) }
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
                    _tabRequest.emit(AppTab.VERSES)
                }
            } catch (e: Exception) {
                _errorState.value = UiError(Res.string.error_process_text, listOf(e.message ?: ""))
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
                _errorState.value = UiError(Res.string.error_select_verse, listOf(e.message ?: ""))
            }
        }
    }

    fun loadContext(direction: Int) {
        val currentList = _detectedVerses.value
        if (currentList.isEmpty()) return
        
        // Single book check: if we are strictly in one book, prevent crossing boundary
        val currentBook = currentList.first().first.book
        val isSingleBook = currentList.all { it.first.book == currentBook }
        
        viewModelScope.launch {
            try {
                val newVerses = currentList.toMutableList()
                if (direction < 0) {
                    val firstId = currentList.first().first.id
                    val prevId = firstId - 1
                    if (prevId > 0) {
                        val req = parser.repository.getVerseRequest(prevId)
                        if (req != null && (!isSingleBook || req.book == currentBook)) { // Stop if simple mode and book changes
                            val content = withContext(dispatchers.io) { database.getText(prevId) }
                            if (content != null) {
                                newVerses.add(0, req to content)
                            }
                        }
                    }
                } else {
                    val lastId = currentList.last().first.id
                    val nextId = lastId + 1
                    if (nextId < 32000) { 
                        val req = parser.repository.getVerseRequest(nextId)
                        if (req != null && (!isSingleBook || req.book == currentBook)) { // Stop if simple mode and book changes
                            val content = withContext(dispatchers.io) { database.getText(nextId) }
                            if (content != null) {
                                newVerses.add(req to content)
                            }
                        }
                    }
                }
                if (newVerses.size != currentList.size) { // Only update if changed
                    _detectedVerses.value = newVerses
                    currentOriginalVerses = newVerses
                    _isInternalUpdate.value = true
                }
            } catch (e: Exception) {
                _errorState.value = UiError(Res.string.error_load_context, listOf(e.message ?: ""))
            }
        }
    }

    fun focusOnBook(book: String) {
        pushToBackStack()
        val currentList = _detectedVerses.value
        // Filters only the verses of the selected book to create a unique context
        val newVerses = currentList.filter { it.first.book == book }
        if (newVerses.isNotEmpty()) {
            _detectedVerses.value = newVerses
            currentOriginalVerses = newVerses
            _isInternalUpdate.value = true
        }
    }

    fun removeContext(direction: Int) {
        tryRemoveContext(direction)
    }

    fun tryRemoveContext(direction: Int): Boolean {
        val currentList = _detectedVerses.value
        if (currentList.size <= 1) return false
        
        val itemToRemove = if (direction < 0) currentList.first() else currentList.last()
        val isOriginal = currentOriginalVerses.any { it.first.id == itemToRemove.first.id }
        
        if (isOriginal) return false

        val newVerses = currentList.toMutableList()
        if (direction < 0) newVerses.removeAt(0) else newVerses.removeAt(newVerses.lastIndex)
        _detectedVerses.value = newVerses
        _isInternalUpdate.value = true
        return true
    }
    
    fun refreshHistory() {
        viewModelScope.launch {
             try { historyRepository.loadHistory() } catch (e: Exception) { _errorState.value = UiError(Res.string.error_update_history, listOf(e.message ?: "")) }
        }
    }

    fun saveNote(content: String) {
        val verseIdFromRequest = _editingVerseRequest.value?.id
        val existingNote = _editingNote.value
        viewModelScope.launch {
            try {
                val finalVerseId = existingNote?.verseId ?: verseIdFromRequest
                val existing = finalVerseId?.let { notesRepository.getNoteForVerse(it) }
                val now = currentTimeMillis()
                val note = Note(
                    id = existing?.id ?: existingNote?.id ?: generateUuid(), 
                    verseId = finalVerseId, 
                    content = content, 
                    createdAt = existing?.createdAt ?: existingNote?.createdAt ?: now, 
                    updatedAt = now, 
                    syncStatus = SyncStatus.PENDING
                )
                notesRepository.saveNote(note)
            } catch (e: Exception) {
                _errorState.value = UiError(Res.string.error_save_note, listOf(e.message ?: ""))
            }
        }
    }

    fun confirmDeleteNote(note: Note) { _noteToDelete.value = note }
    fun cancelDeleteNote() { _noteToDelete.value = null }

    fun performDeleteNote() {
        val note = _noteToDelete.value ?: return
        viewModelScope.launch {
            try { 
                notesRepository.deleteNote(note.id) 
                _noteToDelete.value = null
                if (_viewingNote.value?.id == note.id) closeNoteViewer()
            } catch (e: Exception) { _errorState.value = UiError(Res.string.error_delete_note, listOf(e.message ?: "")) }
        }
    }

    fun getVerseReference(verseId: Int): String? {
        val req = parser.repository.getVerseRequest(verseId) ?: return null
        return "${req.book} ${req.chapter}:${req.verse}"
    }

    fun formatVersesForClipboard(verses: List<Pair<VerseRequest, String?>>): String {
        if (verses.isEmpty()) return ""
        val result = StringBuilder()
        var currentBook = ""
        var currentChapter = -1
        var currentVerse = -1
        var segmentStart: VerseRequest? = null
        var segmentVerses = mutableListOf<Pair<VerseRequest, String?>>()
        fun flushSegment() {
            if (segmentVerses.isEmpty()) return
            val segmentText = segmentVerses.joinToString(" ") { (_, content) -> content?.replace(Regex("<[^>]*>"), "")?.trim() ?: "" }
            val start = segmentStart!!
            val end = segmentVerses.last().first
            val reference = if (start.chapter == end.chapter && start.verse == end.verse) "(${start.book} ${start.chapter}:${start.verse} - ACF)"
            else if (start.chapter == end.chapter) "(${start.book} ${start.chapter}:${start.verse}-${end.verse} - ACF)"
            else "(${start.book} ${start.chapter}:${start.verse}-${end.chapter}:${end.verse} - ACF)"
            result.append(segmentText).append(" ").append(reference)
        }
        verses.forEach { (req, text) ->
            val needsBreak = req.book != currentBook || (currentChapter != -1 && req.chapter != currentChapter) || (currentVerse != -1 && req.verse != currentVerse + 1)
            if (needsBreak && segmentVerses.isNotEmpty()) {
                flushSegment()
                result.append("\n\n")
                segmentVerses.clear()
                segmentStart = req
            } else if (segmentStart == null) segmentStart = req
            segmentVerses.add(req to text)
            currentBook = req.book; currentChapter = req.chapter; currentVerse = req.verse
        }
        flushSegment()
        return result.toString().trim()
    }

    fun getConsolidatedReference(verses: List<Pair<VerseRequest, String?>>): String {
        if (verses.isEmpty()) return ""
        val groupedRefs = verses.map { it.first }.groupBy { it.book }
        return groupedRefs.entries.joinToString("; ") { (book, reqs) ->
            val chapters = reqs.groupBy { it.chapter }
            "$book " + chapters.entries.joinToString(", ") { (chap, vReqs) ->
                if (vReqs.size == 1) "$chap:${vReqs.first().verse}"
                else "$chap:${vReqs.first().verse}-${vReqs.last().verse}"
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            try { syncManager.performFullSync() } catch (e: Exception) { _errorState.value = UiError(Res.string.error_sync, listOf(e.message ?: "")) }
        }
    }

    fun updateFontSize(size: Int) { viewModelScope.launch { settingsRepository.updateFontSize(size) } }
    fun updateFontFamily(family: String) { viewModelScope.launch { settingsRepository.updateFontFamily(family) } }
    fun updateLineHeight(height: Float) { viewModelScope.launch { settingsRepository.updateLineHeight(height) } }
    fun updateShowFireAnimation(enabled: Boolean) { viewModelScope.launch { settingsRepository.updateShowFireAnimation(enabled) } }
    fun updateAnimatedWindow(enabled: Boolean) { viewModelScope.launch { settingsRepository.updateAnimatedWindow(enabled) } }
    fun updateSignature(text: String) { viewModelScope.launch { settingsRepository.updateSignature(text) } }
    fun updateShowSnapshotAction(enabled: Boolean) { viewModelScope.launch { settingsRepository.updateShowSnapshotAction(enabled) } }
    fun updateIsTransparent(enabled: Boolean) {
        viewModelScope.launch {
            // Valida suporte a transparência ANTES de qualquer ação
            if (enabled) {
                val isWine = System.getProperty("verse.isWine") == "true"
                val forceNoTransparent = System.getProperty("verse.noTransparent") == "true"
                
                // Bloqueia ativação em ambientes não suportados
                if (isWine || forceNoTransparent) {
                    showToast("Transparência não suportada neste ambiente. Configuração não foi salva.", ToastType.ERROR)
                    return@launch // Sai SEM salvar, SEM reiniciar
                }
            }
            
            // Salva a configuração (só chega aqui se validação passou ou se está desativando)
            settingsRepository.updateIsTransparent(enabled)
            
            // Tenta reiniciar o app para aplicar mudanças
            delay(500)
            restartApp()
        }
    }

    private fun restartApp() {
        try {
            val os = System.getProperty("os.name")?.lowercase() ?: ""
            if (os.contains("android")) return
            val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
            val jarFile = File(VerseViewModel::class.java.protectionDomain?.codeSource?.location?.toURI() ?: return)
            if (jarFile.extension == "jar") ProcessBuilder(javaBin, "-jar", jarFile.absolutePath).start()
            else { val cp = System.getProperty("java.class.path"); ProcessBuilder(javaBin, "-cp", cp, "br.com.irse.verse.MainKt").start() }
            System.exit(0)
        } catch (e: Exception) {
            // Falha no reinício automático - usuário pode fechar e abrir manualmente
            e.printStackTrace()
        }
    }

    fun captureSnapshot() {
        val currentVerses = _detectedVerses.value
        if (currentVerses.isEmpty()) return
        val totalLength = currentVerses.sumOf { it.second?.length ?: 0 }
        val verseCount = currentVerses.size
        if (totalLength > MAX_SNAPSHOT_CHARS || verseCount > MAX_SNAPSHOT_VERSES) {
            val fullText = currentVerses.joinToString("\n\n") { it.second?.replace(Regex("<[^>]*>"), "") ?: "" }
            val tempNote = Note(id = generateUuid(), verseId = currentVerses.first().first.id, content = fullText, createdAt = currentTimeMillis(), updatedAt = currentTimeMillis(), syncStatus = SyncStatus.PENDING)
            openNoteEditor(note = tempNote)
            if (verseCount > MAX_SNAPSHOT_VERSES) _errorState.value = UiError(Res.string.error_snapshot_too_many_verses, listOf(verseCount))
            else _errorState.value = UiError(Res.string.error_snapshot_too_long)
            return
        }
        val template = _selectedTemplate.value
        viewModelScope.launch {
            _isProcessing.value = true
            try { snapshotHandler.captureAndSave(currentVerses, template) } catch (e: Exception) { _errorState.value = UiError(Res.string.error_snapshot_capture, listOf(e.message ?: "")); e.printStackTrace() } finally { _isProcessing.value = false }
        }
    }

    fun captureNoteSnapshot(note: Note? = null, editorContent: String? = null) {
        val targetNote = note ?: _editingNote.value
        val content = editorContent ?: targetNote?.content ?: return
        if (content.length > MAX_SNAPSHOT_CHARS) {
            if (_isNoteEditorOpen.value) _errorState.value = UiError(Res.string.error_note_too_long_editor, listOf(content.length, MAX_SNAPSHOT_CHARS))
            else { openNoteEditor(note = targetNote); _errorState.value = UiError(Res.string.error_note_too_long_general) }
            return
        }
        val ref = targetNote?.verseId?.let { getVerseReference(it) } ?: _editingVerseRequest.value?.let { "${it.book} ${it.chapter}:${it.verse}" }
        val template = _selectedTemplate.value
        val sign = signature.value
        viewModelScope.launch {
            _isProcessing.value = true
            try { snapshotHandler.captureNoteAndSave(content, ref, sign, template) } catch (e: Exception) { _errorState.value = UiError(Res.string.error_note_capture, listOf(e.message ?: "")); e.printStackTrace() } finally { _isProcessing.value = false }
        }
    }
}
