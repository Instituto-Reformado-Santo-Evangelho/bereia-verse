package br.com.irse.verse.core

open class BibleParser(val repository: BibleRepository) {

    // Regex parts
    private val suffixPattern = """(\d+)(?:\s*[:.,]\s*((?:[\d\s,]|[\u2013\u002d\u2014](?!\s*\d+\s*[:.,]))+))?(?:\s*[\u2013\u002d\u2014]\s*(\d+)(?:\s*[:.,]\s*((?:[\d\s,]|[\u2013\u002d\u2014](?!\s*\d+\s*[:.,]))+))?)?"""

    // Construída dinamicamente para garantir que apenas livros válidos sejam detectados
    val refRegex by lazy {
        val allKeys = repository.getAllBooks().keys + BibleConstants.BOOK_ABBREVIATIONS.keys
        val triggers = allKeys
            .sortedByDescending { it.length }.joinToString("|") { key ->
                key.split(" ").joinToString("\\s*") { part -> Regex.escape(part) }
            }

        // Agora EXIGE o livro (removida a alternativa sem livro para evitar falsos positivos no clipboard)
        Regex(
            """((?:$triggers)\.?\s*)$suffixPattern""",
            RegexOption.IGNORE_CASE
        )
    }

    /**
     * @param text O texto a ser processado
     * @param strict Se true, o texto deve conter APENAS referências válidas (e pontuação básica), 
     *               rejeitando se houver palavras extras ou se for um texto longo.
     */
    open fun processSelection(text: String, strict: Boolean = false): List<VerseRequest> {
        val trimmed = text.trim()
        if (trimmed.length < 3) return emptyList()
        
        // Se estiver no modo estrito (vindo do clipboard), rejeita textos muito longos
        // Uma referência bíblica raramente passa de 100 caracteres
        if (strict && trimmed.length > 150) return emptyList()

        // Remove footnote markers like [1], [12]
        val cleanText = trimmed.replace(Regex("""\[\d+]"""), "").replace(Regex("""\s+""" ), " ")
        val chunks = cleanText.split(";")
        
        var currentBook: Book? = null
        val allRequests = mutableListOf<VerseRequest>()
        
        // No modo estrito, vamos validar se o que foi detectado "cobre" o essencial do texto
        var totalMatchedLength = 0

        for (rawChunk in chunks) {
            var chunk = rawChunk.trim()
            if (chunk.isEmpty()) continue

            // Remove prefixos comuns de citação
            val prefixRegex = Regex("""^(cf\.|e\s|and\s|ver\s)""", RegexOption.IGNORE_CASE)
            val prefixMatch = prefixRegex.find(chunk)
            if (prefixMatch != null) {
                if (strict) return emptyList() // No modo estrito, não aceitamos nem prefixos "cf." etc se for puro
                chunk = chunk.substring(prefixMatch.range.last + 1).trim()
            }

            val matches = refRegex.findAll(chunk).toList()
            
            // Se modo estrito e não achou nada no chunk, ou achou texto extra
            if (strict && matches.isEmpty()) return emptyList()

            for (match in matches) {
                if (match.value.isEmpty()) continue
                
                totalMatchedLength += match.value.length

                // Extraction Logic (Agora simplificada pois o livro é obrigatório)
                val rawBook = match.groupValues[1].takeIf { it.isNotBlank() }
                val startCapStr = match.groupValues[2].takeIf { it.isNotBlank() }
                var startVersePart = match.groupValues[3].takeIf { it.isNotBlank() } ?: "1-999"
                val endCapStr = match.groupValues[4].takeIf { it.isNotBlank() }
                var endVersePart = match.groupValues[5].takeIf { it.isNotBlank() } ?: "1-999"

                if (startCapStr.isNullOrBlank()) continue
                val startCap = startCapStr.toIntOrNull() ?: continue
                val endCap = endCapStr?.takeIf { it.isNotBlank() }?.toIntOrNull()

                // Clean trailing dashes
                 startVersePart = startVersePart.replace(Regex("""[\s\u2013\u002d\u2014]+$""" ), "")
                 endVersePart = endVersePart.replace(Regex("""[\s\u2013\u002d\u2014]+$""" ), "")

                if (rawBook != null) {
                    val found = repository.findBook(rawBook.trim())
                    if (found != null) currentBook = found
                }

                if (currentBook != null) {
                    if (endCap != null && endCap > startCap) {
                        // Intervalo de capítulos
                        val startRange = getMinMaxVerses(startVersePart)
                        val endRange = getMinMaxVerses(endVersePart)
                        
                        val maxVerseStartCap = currentBook.metaData.chapters.getOrNull(startCap - 1)
                        if (maxVerseStartCap != null) {
                            val part = "${startRange.min}-$maxVerseStartCap"
                            allRequests.addAll(parseVerses(currentBook, startCap, part))
                        }

                        for (c in (startCap + 1) until endCap) {
                            val maxV = currentBook.metaData.chapters.getOrNull(c - 1)
                            if (maxV != null) {
                                allRequests.addAll(parseVerses(currentBook, c, "1-$maxV"))
                            }
                        }

                        val maxVerseEndCap = currentBook.metaData.chapters.getOrNull(endCap - 1)
                        if (maxVerseEndCap != null) {
                            var finalV = endRange.max
                            if (finalV > maxVerseEndCap) finalV = maxVerseEndCap
                            val part = "1-$finalV"
                            allRequests.addAll(parseVerses(currentBook, endCap, part))
                        }

                    } else {
                        allRequests.addAll(parseVerses(currentBook, startCap, startVersePart))
                    }
                }
            }
        }
        
        // Validação final de "Pureza" no modo estrito:
        // O comprimento dos matches + separadores (;) deve ser próximo ao comprimento total
        if (strict) {
            val ratio = totalMatchedLength.toDouble() / cleanText.length.toDouble()
            // Se os matches cobrirem menos de 80% do texto (descontando espaços/pontuação), provavelmente há lixo
            if (ratio < 0.75) return emptyList()
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
         
         // Validação: capítulo deve existir
         if (chapter < 1 || chapter > book.metaData.chapters.size) {
             return emptyList()
         }
         
         // Calculate startId manually since Repository doesn't expose it anymore
         var startId = book.metaData.start
         for (i in 0 until chapter - 1) {
             startId += book.metaData.chapters.getOrElse(i) { 0 }
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
