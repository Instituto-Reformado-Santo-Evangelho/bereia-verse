package br.com.irse.verse.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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

    init {
        // Inicializa histórico
        viewModelScope.launch {
            _history.value = HistoryManager.getHistory()
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
        val req = parser.repository.getVerseRequest(verseId)
        if (req != null) {
            selectVerse(req)
        }
    }

    fun processQuery(text: String, addToHistory: Boolean = true) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val requests = withContext(Dispatchers.IO) { parser.processSelection(text) }
                if (requests.isNotEmpty()) {
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
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) { database.getText(request.id) }
            _detectedVerses.value = listOf(request to content)
        }
    }
    
    fun refreshHistory() {
        viewModelScope.launch {
             _history.value = HistoryManager.getHistory()
        }
    }
}