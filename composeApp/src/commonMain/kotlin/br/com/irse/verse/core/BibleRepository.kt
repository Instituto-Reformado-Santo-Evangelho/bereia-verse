package br.com.irse.verse.core

class BibleRepository(private val mapping: Map<String, BookMetaData>) {
    
    fun findBook(name: String): Book? {
        val searchName = name.lowercase().trim()
        
        // 1. Check mapping keys directly (case-insensitive)
        val directMatch = mapping.entries.find { it.key.lowercase() == searchName }
        if (directMatch != null) {
            return Book(name = directMatch.key, metaData = directMatch.value)
        }
        
        // 2. Check abbreviations
        val cleanName = searchName.replace(".", "")
        val fullName = BibleConstants.BOOK_ABBREVIATIONS[cleanName]
        if (fullName != null) {
            val meta = mapping[fullName]
            if (meta != null) {
                return Book(name = fullName, metaData = meta)
            }
        }
        
        return null
    }

    // Retorna todos os livros para busca global
    fun getAllBooks(): Map<String, BookMetaData> = mapping

    fun getVerseRequest(verseId: Int): VerseRequest? {
        // Find the book that contains this verseId
        // We iterate through all books to find the one where the verseId falls within its range.
        
        val bookEntry = mapping.entries.find { (_, meta) ->
            val totalVerses = meta.chapters.sum()
            verseId >= meta.start && verseId < (meta.start + totalVerses)
        } ?: return null

        val bookName = bookEntry.key
        val meta = bookEntry.value
        
        // Calculate relative index inside the book (0-based)
        var relativeIndex = verseId - meta.start
        
        var chapter = 1
        for (versesInChapter in meta.chapters) {
            if (relativeIndex < versesInChapter) {
                // Found the chapter
                return VerseRequest(
                    id = verseId,
                    book = bookName.replaceFirstChar { it.uppercase() },
                    chapter = chapter,
                    verse = relativeIndex + 1 // Verse is 1-based
                )
            }
            relativeIndex -= versesInChapter
            chapter++
        }
        
        return null
    }
}