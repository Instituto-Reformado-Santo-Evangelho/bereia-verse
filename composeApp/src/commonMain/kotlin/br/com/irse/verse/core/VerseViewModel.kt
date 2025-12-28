package br.com.irse.verse.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
class VerseViewModel(
    private val parser: BibleParser,
    private val database: BibleDatabase
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // UI State
    private val _detectedVerses = MutableStateFlow<List<Pair<VerseRequest, String?>>>(emptyList())
    val detectedVerses = _detectedVerses.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history = _history.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Flag para controle de interface (Desktop)
    private val _isInternalUpdate = MutableStateFlow(false)
    val isInternalUpdate = _isInternalUpdate.asStateFlow()

    // --- Session Navigation State ---
    private val backStack = java.util.Stack<List<Pair<VerseRequest, String?>>>()
    private val forwardStack = java.util.Stack<List<Pair<VerseRequest, String?>>>()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward = _canGoForward.asStateFlow()

    // Settings State
    private val _fontSize = MutableStateFlow(16)
    val fontSize = _fontSize.asStateFlow()

    private val _fontFamily = MutableStateFlow("sans-serif")
    val fontFamily = _fontFamily.asStateFlow()

    private val _lineHeight = MutableStateFlow(1.4f)
    val lineHeight = _lineHeight.asStateFlow()

    private val _showFireAnimation = MutableStateFlow(true)
    val showFireAnimation = _showFireAnimation.asStateFlow()

    // App Theme Colors (Defaults, can be customized later)
    private val _textColor = MutableStateFlow(androidx.compose.ui.graphics.Color(0xFF333333))
    val textColor = _textColor.asStateFlow()

    private val _borderColor = MutableStateFlow(androidx.compose.ui.graphics.Color.LightGray)
    val borderColor = _borderColor.asStateFlow()

    private val _backgroundColor = MutableStateFlow(androidx.compose.ui.graphics.Color.White)
    val backgroundColor = _backgroundColor.asStateFlow()

    // Snapshot Templates
    data class SnapshotTemplate(
        val id: String, 
        val displayName: String, 
        val backgroundBrush: androidx.compose.ui.graphics.Brush, 
        val contentColor: androidx.compose.ui.graphics.Color,
        val fontFamilyName: String = "SansSerif", // Permite que o template dite a fonte
        val borderColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent
    )

    // Lista de Templates Disponíveis
    val templatesList = listOf(
        SnapshotTemplate(
            id = "classic", 
            displayName = "Clássico", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(androidx.compose.ui.graphics.Color(0xFFFFC107), androidx.compose.ui.graphics.Color(0xFFFFD54F))
            ), 
            contentColor = androidx.compose.ui.graphics.Color(0xFF333333)
        ),
        SnapshotTemplate(
            id = "dark_modern", 
            displayName = "Dark", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF1E1E1E), androidx.compose.ui.graphics.Color(0xFF2C2C2C))
            ), 
            contentColor = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
        ),
        SnapshotTemplate(
            id = "sunset", 
            displayName = "Pôr do Sol", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(androidx.compose.ui.graphics.Color(0xFFff512f), androidx.compose.ui.graphics.Color(0xFFdd2476))
            ), 
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        SnapshotTemplate(
            id = "ocean", 
            displayName = "Oceano", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF2193b0), androidx.compose.ui.graphics.Color(0xFF6dd5ed))
            ), 
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        SnapshotTemplate(
            id = "elegant", 
            displayName = "Elegante", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(androidx.compose.ui.graphics.Color(0xFFF5F5DC), androidx.compose.ui.graphics.Color(0xFFE8E8C8))
            ), 
            contentColor = androidx.compose.ui.graphics.Color(0xFF2C1B18),
            fontFamilyName = "Serif"
        ),
        SnapshotTemplate(
            id = "night", 
            displayName = "Noite", 
            backgroundBrush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF1A237E), androidx.compose.ui.graphics.Color(0xFF000000))
            ), 
            contentColor = androidx.compose.ui.graphics.Color.White
        )
    )

    private val _selectedTemplate = MutableStateFlow(templatesList.first())
    val selectedTemplate = _selectedTemplate.asStateFlow()

    fun setTemplate(template: SnapshotTemplate) {
        _selectedTemplate.value = template
    }

    init {
        // Inicializa histórico e configurações
        viewModelScope.launch {
            _history.value = HistoryManager.getHistory()
            val savedSettings = SettingsManager.getSettings()
            _fontSize.value = savedSettings.fontSize
            _fontFamily.value = savedSettings.fontFamily
            _lineHeight.value = savedSettings.lineHeight
            _showFireAnimation.value = savedSettings.showFireAnimation
        }

        // Setup Search Debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    // --- Navigation Methods ---

    private fun updateNavigationState() {
        _canGoBack.value = !backStack.isEmpty()
        _canGoForward.value = !forwardStack.isEmpty()
    }

    private fun pushToBackStack() {
        if (_detectedVerses.value.isNotEmpty()) {
            backStack.push(_detectedVerses.value)
            forwardStack.clear() // Nova navegação limpa o futuro
            updateNavigationState()
        }
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            val current = _detectedVerses.value
            if (current.isNotEmpty()) {
                forwardStack.push(current)
            }
            
            val previous = backStack.pop()
            _detectedVerses.value = previous
            updateNavigationState()
            _isInternalUpdate.value = true
        }
    }

    fun navigateForward() {
        if (forwardStack.isNotEmpty()) {
            val current = _detectedVerses.value
            if (current.isNotEmpty()) {
                backStack.push(current)
            }

            val next = forwardStack.pop()
            _detectedVerses.value = next
            updateNavigationState()
            _isInternalUpdate.value = true
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.length < 2) {
            _searchResults.value = emptyList()
        }
    }

    private suspend fun performSearch(query: String) {
        val hybridResults = mutableListOf<SearchResult>()
        
        // 1. Tenta tratar como referência
        val refs = withContext(Dispatchers.IO) { parser.processSelection(query) }
        refs.forEach { ref ->
            val content = withContext(Dispatchers.IO) { database.getText(ref.id) }
            if (content != null) {
                hybridResults.add(SearchResult(ref.id, content, ref.book, ref.chapter, ref.verse))
            }
        }
        
        // 2. Busca por texto
        val textResults = withContext(Dispatchers.IO) { database.searchVerses(query, limit = 20) }
        textResults.forEach { res ->
            if (hybridResults.none { it.id == res.id }) {
                // Enriquecer com metadados do repositório
                val ref = parser.repository.getVerseRequest(res.id)
                val enrichedRes = if (ref != null) {
                    res.copy(book = ref.book, chapter = ref.chapter, verse = ref.verse)
                } else res
                
                hybridResults.add(enrichedRes)
            }
        }
        
        _searchResults.value = hybridResults
    }
    
    fun selectVerse(verseId: Int) {
        // Salva estado anterior antes de mudar
        pushToBackStack()
        
        _isInternalUpdate.value = true
        val req = parser.repository.getVerseRequest(verseId)
        if (req != null) {
            // Chamada direta para não duplicar o pushToBackStack
            viewModelScope.launch {
                val content = withContext(Dispatchers.IO) { database.getText(req.id) }
                _detectedVerses.value = listOf(req to content)
            }
        }
    }

    fun processQuery(text: String, addToHistory: Boolean = true, isExternal: Boolean = false) {
        val wasInternal = !isExternal
        _isInternalUpdate.value = wasInternal
        
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val requests = withContext(Dispatchers.IO) { parser.processSelection(text) }
                if (requests.isNotEmpty()) {
                    // Só salva na pilha se não for o mesmo conteúdo (evita duplicação)
                    val currentFirst = _detectedVerses.value.firstOrNull()?.first
                    val newFirst = requests.firstOrNull()
                    
                    // Lógica simplificada de igualdade para evitar spam na pilha
                    if (currentFirst?.id != newFirst?.id) {
                         pushToBackStack()
                    }

                    val results = withContext(Dispatchers.IO) {
                        requests.map { req -> database.getText(req.id) to req }.map { it.second to it.first }
                    }
                    _detectedVerses.value = results
                    
                    if (addToHistory) {
                        val historyLabel = formatHistoryLabel(requests)
                        HistoryManager.saveEntry(historyLabel)
                        _history.value = HistoryManager.getHistory() // Atualiza UI
                    }
                }
            } catch (e: Exception) {
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
        
        return if (requests.size == 1) {
            "${first.book} ${first.chapter}:${first.verse}"
        } else if (first.book == last.book && first.chapter == last.chapter) {
            "${first.book} ${first.chapter}:${first.verse}-${last.verse}"
        } else if (first.book == last.book) {
            "${first.book} ${first.chapter}:${first.verse} - ${last.chapter}:${last.verse}"
        } else {
            // Múltiplos livros detectados
            val uniqueBooks = requests.map { it.book }.distinct()
            if (uniqueBooks.size > 1) {
                "${first.book} ${first.chapter}:${first.verse} (+${uniqueBooks.size - 1} livros)"
            } else {
                "${first.book} ${first.chapter}:${first.verse}..."
            }
        }
    }

    fun selectVerse(request: VerseRequest) {
        pushToBackStack()
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) { database.getText(request.id) }
            _detectedVerses.value = listOf(request to content)
        }
    }

    fun loadContext(direction: Int) {
        val currentList = _detectedVerses.value
        if (currentList.isEmpty()) return

        // NÃO fazemos pushToBackStack aqui, pois queremos que Voltar/Avançar
        // naveguem apenas entre as seleções principais (Clipboard/Busca),
        // tratando a expansão de contexto como uma visualização temporária.

        viewModelScope.launch {
            val newVerses = currentList.toMutableList()
            
            if (direction < 0) {
                // Carregar Anterior
                val firstId = currentList.first().first.id
                val prevId = firstId - 1
                if (prevId > 0) {
                    val content = withContext(Dispatchers.IO) { database.getText(prevId) }
                    if (content != null) {
                        val req = parser.repository.getVerseRequest(prevId)
                        if (req != null) {
                            newVerses.add(0, req to content)
                        }
                    }
                }
            } else {
                // Carregar Próximo
                val lastId = currentList.last().first.id
                val nextId = lastId + 1
                // Assumindo um limite razoável (ex: 31102 versículos na bíblia)
                if (nextId < 32000) { 
                    val content = withContext(Dispatchers.IO) { database.getText(nextId) }
                    if (content != null) {
                        val req = parser.repository.getVerseRequest(nextId)
                        if (req != null) {
                            newVerses.add(req to content)
                        }
                    }
                }
            }
            
            _detectedVerses.value = newVerses
            _isInternalUpdate.value = true
        }
    }

    fun removeContext(direction: Int) {
        val currentList = _detectedVerses.value
        if (currentList.size <= 1) return // Mantém pelo menos um versículo

        val newVerses = currentList.toMutableList()
        
        if (direction < 0) {
            // Remover Primeiro (Topo)
            newVerses.removeAt(0)
        } else {
            // Remover Último (Fundo)
            newVerses.removeAt(newVerses.lastIndex)
        }

        _detectedVerses.value = newVerses
        _isInternalUpdate.value = true
    }
    
    fun refreshHistory() {
        viewModelScope.launch {
             _history.value = HistoryManager.getHistory()
        }
    }

    fun updateFontSize(size: Int) {
        val newSize = size.coerceIn(12, 32)
        _fontSize.value = newSize
        viewModelScope.launch {
            SettingsManager.saveSettings(UserSettings(fontSize = newSize, fontFamily = _fontFamily.value, lineHeight = _lineHeight.value))
        }
    }

    fun updateFontFamily(family: String) {
        _fontFamily.value = family
        viewModelScope.launch {
            SettingsManager.saveSettings(UserSettings(fontSize = _fontSize.value, fontFamily = family, lineHeight = _lineHeight.value))
        }
    }

    fun updateLineHeight(height: Float) {
        _lineHeight.value = height
        viewModelScope.launch {
            SettingsManager.saveSettings(UserSettings(fontSize = _fontSize.value, fontFamily = _fontFamily.value, lineHeight = height, showFireAnimation = _showFireAnimation.value))
        }
    }

    fun updateShowFireAnimation(enabled: Boolean) {
        _showFireAnimation.value = enabled
        viewModelScope.launch {
            SettingsManager.saveSettings(UserSettings(fontSize = _fontSize.value, fontFamily = _fontFamily.value, lineHeight = _lineHeight.value, showFireAnimation = enabled))
        }
    }
}