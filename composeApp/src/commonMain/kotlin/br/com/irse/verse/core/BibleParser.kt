package br.com.irse.verse.core

open class BibleParser(val repository: BibleRepository) {

    // Regex parts
    private val suffixPattern = """(\d+)(?:\s*[:.,]\s*((?:[\d\s,]|[\u2013\u002d\u2014](?!\s*\d+\s*[:.,]))+))?(?:\s*[\u2013\u002d\u2014]\s*(\d+)(?:\s*[:.,]\s*((?:[\d\s,]|[\u2013\u002d\u2014](?!\s*\d+\s*[:.,]))+))?)?"""

    // Construída dinamicamente para garantir que apenas livros válidos sejam detectados
    // Usa Alternância para tratar separadamente "Com Livro" e "Sem Livro"
    // "Sem Livro" usa Lookahead Negativo para não engolir prefixos de livros (ex: "1" de "1 João")
    val refRegex by lazy {
        val allKeys = repository.getAllBooks().keys + BibleConstants.BOOK_ABBREVIATIONS.keys
        val triggers = allKeys
            .sortedByDescending { it.length }.joinToString("|") { key ->
                key.split(" ").joinToString("\\s*") { part -> Regex.escape(part) }
            }

        // Alt 1: Book + Suffix (Groups 1..5)
        // Alt 2: Lookahead(!Book) + Suffix (Groups 6..9 - Group 6 is the first capture in suffix, etc)
        // Note: Suffix has 4 capturing groups.
        // Structure:
        // (Book) (Cap) (Ver) (EndCap) (EndVer)  -> 5 groups
        // |
        // (?!) (Cap) (Ver) (EndCap) (EndVer)    -> 4 groups (start index 6)
        
        Regex(
            """(?:((?:$triggers)\.?\s*)$suffixPattern)|(?:(?!(?:$triggers))$suffixPattern)""",
            RegexOption.IGNORE_CASE
        )
    }

    open fun processSelection(text: String): List<VerseRequest> {
        if (text.length < 3) return emptyList()

        // Remove footnote markers like [1], [12]
        val cleanText = text.replace(Regex("""\[\d+]"""), "").replace(Regex("""\s+""" ), " ")
        val chunks = cleanText.split(";")
        
        var currentBook: Book? = null
        val allRequests = mutableListOf<VerseRequest>()

        for (rawChunk in chunks) {
            var chunk = rawChunk.trim()
            if (chunk.isEmpty()) continue

            chunk = chunk.replace(Regex("""^(cf\.|e\s|and\s|ver\s)""", RegexOption.IGNORE_CASE), "").trim()

            val matches = refRegex.findAll(chunk)
            
            for (match in matches) {
                if (match.value.isEmpty()) continue

                // Extraction Logic with Alternatives
                // Alt 1 (With Book): Groups 1, 2, 3, 4, 5
                // Alt 2 (No Book): Groups 6, 7, 8, 9
                
                // Group 1: Book
                val rawBook = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
                
                // Start Cap: Group 2 OR Group 6
                val startCapStr = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
                    ?: match.groupValues.getOrNull(6)
                
                // Start Verse: Group 3 OR Group 7
                var startVersePart = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }
                    ?: match.groupValues.getOrNull(7)?.takeIf { it.isNotBlank() } 
                    ?: "1-999"
                    
                // End Cap: Group 4 OR Group 8
                val endCapStr = match.groupValues.getOrNull(4)?.takeIf { it.isNotBlank() }
                    ?: match.groupValues.getOrNull(8)
                    
                // End Verse: Group 5 OR Group 9
                var endVersePart = match.groupValues.getOrNull(5)?.takeIf { it.isNotBlank() }
                    ?: match.groupValues.getOrNull(9)?.takeIf { it.isNotBlank() } 
                    ?: "1-999"

                if (startCapStr.isNullOrBlank()) continue
                val startCap = startCapStr.toIntOrNull() ?: continue
                val endCap = endCapStr?.takeIf { it.isNotBlank() }?.toIntOrNull()

                // Clean trailing dashes
                 startVersePart = startVersePart.replace(Regex("""[\s\u2013\u002d\u2014]+\$""" ), "")
                 endVersePart = endVersePart.replace(Regex("""[\s\u2013\u002d\u2014]+\$""" ), "")

                if (rawBook != null) {
                    val found = repository.findBook(rawBook.trim())
                    if (found != null) currentBook = found
                }

                if (currentBook != null) {
                    if (endCap != null && endCap > startCap) {
                        // Intervalo de capítulos
                        val startRange = getMinMaxVerses(startVersePart)
                        val endRange = getMinMaxVerses(endVersePart)
                        
                        // 1. Start Cap
                        val maxVerseStartCap = currentBook.metaData.chapters.getOrNull(startCap - 1)
                        if (maxVerseStartCap != null) {
                            val part = "${startRange.min}-$maxVerseStartCap"
                            allRequests.addAll(parseVerses(currentBook, startCap, part))
                        }

                        // 2. Middle Caps
                        for (c in (startCap + 1) until endCap) {
                            val maxV = currentBook.metaData.chapters.getOrNull(c - 1)
                            if (maxV != null) {
                                allRequests.addAll(parseVerses(currentBook, c, "1-$maxV"))
                            }
                        }

                        // 3. End Cap
                        val maxVerseEndCap = currentBook.metaData.chapters.getOrNull(endCap - 1)
                        if (maxVerseEndCap != null) {
                            var finalV = endRange.max
                            if (finalV > maxVerseEndCap) finalV = maxVerseEndCap
                            val part = "1-$finalV"
                            allRequests.addAll(parseVerses(currentBook, endCap, part))
                        }

                    } else {
                        // Só um capitulo
                        allRequests.addAll(parseVerses(currentBook, startCap, startVersePart))
                    }
                }
            }
        }

        return allRequests.distinctBy { it.id }
    }

    private fun getMinMaxVerses(versePart: String): VerseRange {
        val cleanPart = versePart.replace(Regex("""[\u2013\u002d\u2014]""" ), "-").replace(Regex("""\s+""" ), "")
        val groups = cleanPart.split(",")
        var min = 9999
        var max = -1

        for (group in groups) {
            if (group.contains("-")) {
                val parts = group.split("-")
                val s = parts.getOrNull(0)?.toIntOrNull()
                val e = parts.getOrNull(1)?.toIntOrNull()
                if (s != null && s < min) min = s
                if (e != null && e > max) max = e
            } else {
                val v = group.toIntOrNull()
                if (v != null) {
                    if (v < min) min = v
                    if (v > max) max = v
                }
            }
        }
        return VerseRange(if (min == 9999) 1 else min, if (max == -1) 1 else max)
    }

    private fun parseVerses(book: Book, chapter: Int, versePart: String): List<VerseRequest> {
         val cleanPart = versePart.replace(Regex("""[\u2013\u002d\u2014]""" ), "-").replace(Regex("""\s+""" ), "")
         val ids = mutableListOf<VerseRequest>()
         
         // Calculate startId manually since Repository doesn't expose it anymore
         var startId = book.metaData.start
         for (i in 0 until chapter - 1) {
             startId += book.metaData.chapters[i]
         }
         
         val maxVerse = book.metaData.chapters.getOrNull(chapter - 1) ?: return emptyList()

         val groups = cleanPart.split(",")
         for (group in groups) {
             if (group.contains("-")) {
                 val parts = group.split("-")
                 if (parts.size >= 2) {
                     val startV = parts[0].toIntOrNull()
                     val endV = parts[1].toIntOrNull()
                     
                     if (startV != null && endV != null) {
                         for (v in startV..endV) {
                             if (v in 1..maxVerse) {
                                 ids.add(VerseRequest(startId + v - 1, book.name, chapter, v))
                             }
                         }
                     }
                 }
             } else {
                 val v = group.toIntOrNull()
                 if (v != null && v in 1..maxVerse) {
                      ids.add(VerseRequest(startId + v - 1, book.name, chapter, v))
                 }
             }
         }
         return ids
    }
}
