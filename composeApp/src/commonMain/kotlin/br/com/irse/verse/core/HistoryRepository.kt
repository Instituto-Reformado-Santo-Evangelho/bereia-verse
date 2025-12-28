package br.com.irse.verse.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class HistoryRepository {
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history = _history.asStateFlow()

    open suspend fun loadHistory() {
        _history.value = HistoryManager.getHistory()
    }

    open suspend fun saveEntry(query: String) {
        HistoryManager.saveEntry(query)
        loadHistory()
    }

    open suspend fun clear() {
        HistoryManager.clearHistory()
        _history.value = emptyList()
    }
}
