package br.com.irse.verse.core

class BibleParser(val repository: BibleRepository) {

    // Regex adaptada para Kotlin usando Raw String para evitar escapes duplos
    private val refRegex = Regex(
        """((?:[1-3]\s*)?[A-Za-zá-úÁ-Úçã]{2,}\.?\s*)?(\d+)(?:\s*[:.,]\s*((?:[\d\s,]|[\u2013\u002d\u2014](?!\s*\d+\s*[:.,]))+))?(?:\s*[\u2013\u002d\u2014]\s*(\d+)(?:\s*[:.,]\s*((?:[\d\s,]|[\u2013\u002d\u2014](?!\s*\d+\s*[:.,]))+))?)?"""
    )

    fun processSelection(text: String): List<VerseRequest> {
        if (text.length < 3) return emptyList()

        // Remove footnote markers like [1], [12]
        val cleanText = text.replace(Regex("""\[\d+]"""), "").replace(Regex("""\s+"""), " ")
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

                val rawBook = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
                val startCapStr = match.groupValues.getOrNull(2)
                var startVersePart = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() } ?: "1-999"
                val endCapStr = match.groupValues.getOrNull(4)
                var endVersePart = match.groupValues.getOrNull(5)?.takeIf { it.isNotBlank() } ?: "1-999"

                if (startCapStr.isNullOrBlank()) continue
                val startCap = startCapStr.toIntOrNull() ?: continue
                val endCap = endCapStr?.takeIf { it.isNotBlank() }?.toIntOrNull()

                // Clean trailing dashes
                 startVersePart = startVersePart.replace(Regex("""[\s\u2013\u002d\u2014]+$"""), "")
                 endVersePart = endVersePart.replace(Regex("""[\s\u2013\u002d\u2014]+$"""), "")

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
        val cleanPart = versePart.replace(Regex("""[\u2013\u002d\u2014]"""), "-").replace(Regex("""\s+"""), "")
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
         val cleanPart = versePart.replace(Regex("""[\u2013\u002d\u2014]"""), "-").replace(Regex("""\s+"""), "")
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