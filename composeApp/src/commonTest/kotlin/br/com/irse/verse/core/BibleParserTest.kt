package br.com.irse.verse.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BibleParserTest {

    // Setup de um repositório com dados reais parciais para teste
    private val mockMapping = mapOf(
        "Gênesis" to BookMetaData(1, listOf(31, 25, 24)), // Caps 1-3
        "João" to BookMetaData(100, listOf(51, 25, 36)),
        "1 João" to BookMetaData(200, listOf(10, 29)),
        "Jó" to BookMetaData(300, listOf(22, 13))
    )
    
    // Instanciação direta
    private val repository = BibleRepository(mockMapping)
    private val parser = BibleParser(repository)

    @Test
    fun `test simple reference detection`() {
        val text = "Texto aleatório João 3:16 mais texto"
        val result = parser.processSelection(text)
        
        assertEquals(1, result.size)
        assertEquals("João", result[0].book)
        assertEquals(3, result[0].chapter)
        assertEquals(16, result[0].verse)
        
        // ID Calc check:
        // Start(100) + Offset(Chap 1(51) + Chap 2(25) = 76) + Verse(16-1=15) = 191
        assertEquals(191, result[0].id)
    }

    @Test
    fun `test short abbreviation Jo`() {
        val text = "Lendo Jo 1:1 aqui"
        val result = parser.processSelection(text)
        
        // Se BibleConstants mapeia "jo" -> "João", deve retornar "João"
        assertTrue(result.isNotEmpty(), "Deveria detectar 'Jo 1:1'")
        assertEquals("João", result[0].book)
        assertEquals(1, result[0].chapter)
        assertEquals(1, result[0].verse)
    }

    @Test
    fun `test numbered book 1 Jo`() {
        val text = "Lendo 1 Jo 1:1 agora"
        val result = parser.processSelection(text)
        
        assertTrue(result.isNotEmpty(), "Deveria detectar '1 Jo 1:1'")
        assertEquals("1 João", result[0].book, "Livro incorreto. Retornou: '${result[0].book}'")
        assertEquals(1, result[0].chapter)
        assertEquals(1, result[0].verse)
    }

    @Test
    fun `test accent handling Jo vs Job`() {
        val text1 = "Livro de Jó 1:1"
        val res1 = parser.processSelection(text1)
        assertEquals("Jó", res1.firstOrNull()?.book)

        val text2 = "Livro de Jo 1:1" 
        val res2 = parser.processSelection(text2)
        assertEquals("João", res2.firstOrNull()?.book) 
    }

    @Test
    fun `test format variation dot and comma`() {
        val text = "Gn 1.1 e Gn 2,2"
        val result = parser.processSelection(text)
        
        assertEquals(2, result.size)
        assertEquals("Gênesis", result[0].book)
        assertEquals(1, result[0].verse)
        
        assertEquals("Gênesis", result[1].book)
        assertEquals(2, result[1].verse)
    }
    
    @Test
    fun `test range detection`() {
        val text = "João 1:1-3"
        val result = parser.processSelection(text)
        
        assertEquals(3, result.size)
        assertEquals(1, result[0].verse)
        assertEquals(2, result[1].verse)
        assertEquals(3, result[2].verse)
    }
    
    @Test
    fun `test complex range across chapters`() {
        val text = "João 1:50-2:2"
        val result = parser.processSelection(text)
        
        assertEquals(4, result.size) // 1:50, 1:51, 2:1, 2:2
        
        assertEquals(50, result[0].verse)
        assertEquals(1, result[0].chapter)
        
        assertEquals(1, result[2].verse)
        assertEquals(2, result[2].chapter)
        
        assertEquals(2, result[3].verse)
        assertEquals(2, result[3].chapter)
    }
}