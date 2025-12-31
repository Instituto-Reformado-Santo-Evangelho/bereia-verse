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
    
    @Test
    fun `should NOT match non-biblical text`() {
        val invalidCases = listOf(
            "Olá mundo",
            "Email: user@example.com",
            "Telefone: 11 98765-4321",
            "Preço: R$ 19,90",
            "Data: 31/12/2024",
            "Hora: 15:30",
            "Versão 1.0.1",
            "Artigo 5º, inciso III",
            "Capítulo 3, página 16",
            "Item 2.1.3",
            "Processo nº 123456/2024",
            "CEP: 12345-678",
            "João da Silva mora aqui",
            "Maria Gomes trabalha",
            "Paulo Santos estuda",
            "Pedro Oliveira joga",
            "123456789",
            "abc123",
            "test@test.com",
            "http://example.com",
            "C# 3:16",
            "Python 3.11",
            "Java 17",
            "Node.js 18",
            "Angular 16",
            "iOS 17",
            "Android 14",
            "Windows 11",
            "Ubuntu 22.04",
            "Documento 3:16",
            "Seção 2:5",
            "Parágrafo 1:1",
            "IP 192.168.1.1",
            "Versão 2.3.16",
            "Código 404:500",
            "Bug #3:16",
            "Issue 1:1"
        )
        
        for (text in invalidCases) {
            val result = parser.processSelection(text)
            assertTrue(
                result.isEmpty(),
                "Expected '$text' to NOT match biblical reference but got: ${result.map { "${it.book} ${it.chapter}:${it.verse}" }}"
            )
        }
    }
    
    @Test
    fun `should handle mixed content correctly`() {
        val text1 = "Leia João 3:16 para mais detalhes"
        val result1 = parser.processSelection(text1)
        assertEquals(1, result1.size, "Should find exactly 1 reference")
        
        val text2 = "Email: test@test.com, mas leia João 1:1"
        val result2 = parser.processSelection(text2)
        assertEquals(1, result2.size, "Should ignore email and find only verse")
        
        val text3 = "Data: 31/12/2024 - Não confundir com João 3:16"
        val result3 = parser.processSelection(text3)
        assertEquals(1, result3.size, "Should ignore date and find verse")
    }
    
    @Test
    fun `should handle common names without false positives`() {
        val namesNotToMatch = listOf(
            "João está aqui",
            "João trabalha",
            "João comprou",
            "João disse",
            "O João",
            "para João",
            "com João"
        )
        
        for (text in namesNotToMatch) {
            val result = parser.processSelection(text)
            assertTrue(
                result.isEmpty(),
                "Expected '$text' (common name usage) to NOT match but got: ${result.map { "${it.book} ${it.chapter}:${it.verse}" }}"
            )
        }
    }
    
    @Test
    fun `should handle edge cases`() {
        assertEquals(0, parser.processSelection("").size, "Empty string should not match")
        assertEquals(0, parser.processSelection("   ").size, "Spaces only should not match")
        assertEquals(0, parser.processSelection("João").size, "Book name only should not match")
        assertEquals(0, parser.processSelection("3:16").size, "Numbers without book should not match")
        assertEquals(0, parser.processSelection("João:16").size, "Missing chapter should not match")
        // Note: Some parsers might interpret "João 3:" as "João 3" (whole chapter)
        // This is acceptable behavior, so we don't test it as strictly invalid
    }
}