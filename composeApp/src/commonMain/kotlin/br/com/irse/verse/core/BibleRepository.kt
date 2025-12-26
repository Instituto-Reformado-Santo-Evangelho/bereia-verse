package br.com.irse.verse.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BibleRepository(private val mapping: Map<String, BookMetaData>) {
    
    private val bookCache = mutableMapOf<String, Book?>()
    private val chapterStartCache = mutableMapOf<String, Int>()
    private val mutex = Mutex()

    suspend fun getBookData(rawBookName: String): Book? {
        if (rawBookName.isBlank()) return null
        
        mutex.withLock {
            if (bookCache.containsKey(rawBookName)) {
                return bookCache[rawBookName]
            }
        }

        val cleanName = rawBookName.lowercase().replace(".", "").trim()
        var fullName = BibleConstants.BOOK_ABBREVIATIONS[cleanName]

        if (fullName == null) {
            fullName = mapping.keys.find { it.lowercase() == cleanName }
        }

        if (fullName == null && cleanName == "jo") fullName = "João"
        if (fullName == null && cleanName == "jó") fullName = "Jó"

        mutex.withLock {
            if (fullName == null) {
                bookCache[rawBookName] = null
                return null
            }

            val metaData = mapping[fullName]
            if (metaData == null) {
                 bookCache[rawBookName] = null
                 return null
            }

            val result = Book(fullName, metaData)
            bookCache[rawBookName] = result
            return result
        }
    }

    suspend fun getChapterStartId(bookData: BookMetaData, chapter: Int): Int? {
        if (chapter < 1 || chapter > bookData.chapters.size) return null

        val cacheKey = "${bookData.start}-$chapter"
        
        mutex.withLock {
            if (chapterStartCache.containsKey(cacheKey)) {
                return chapterStartCache[cacheKey]
            }
        }

        var id = bookData.start
        for (i in 0 until chapter - 1) {
            id += bookData.chapters[i]
        }

        mutex.withLock {
            chapterStartCache[cacheKey] = id
        }
        return id
    }
}
