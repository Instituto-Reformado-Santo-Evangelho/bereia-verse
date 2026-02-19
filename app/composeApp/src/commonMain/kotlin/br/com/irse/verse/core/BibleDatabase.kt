package br.com.irse.verse.core

data class SearchResult(
    val id: Int,
    val content: String,
    val book: String = "",
    val chapter: Int = 0,
    val verse: Int = 0
)

expect class BibleDatabase(dbPath: String) {
    fun getText(verseId: Int): String?
    fun searchVerses(text: String, limit: Int = 20): List<SearchResult>
    fun close()
}