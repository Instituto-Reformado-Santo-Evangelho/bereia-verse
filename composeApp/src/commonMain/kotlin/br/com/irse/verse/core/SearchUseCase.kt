package br.com.irse.verse.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class SearchUseCase(
    private val parser: BibleParser,
    private val database: BibleDatabase
) {
    open suspend fun execute(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val hybridResults = mutableListOf<SearchResult>()
        
        // 1. Tenta tratar como referência
        val refs = parser.processSelection(query)
        refs.forEach { ref ->
            val content = database.getText(ref.id)
            if (content != null) {
                hybridResults.add(SearchResult(ref.id, content, ref.book, ref.chapter, ref.verse))
            }
        }
        
        // 2. Busca por texto
        val textResults = database.searchVerses(query, limit = 20)
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
        
        hybridResults
    }
}
