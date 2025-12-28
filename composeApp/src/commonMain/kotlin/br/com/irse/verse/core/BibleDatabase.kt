package br.com.irse.verse.core

import java.sql.DriverManager
import java.sql.Connection

data class SearchResult(
    val id: Int,
    val content: String,
    val book: String = "",
    val chapter: Int = 0,
    val verse: Int = 0
)

open class BibleDatabase(private val dbPath: String) {
    
    private var connection: Connection? = null

    init {
        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
            println("Database connected at $dbPath")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to connect to database: ${e.message}")
        }
    }

    open fun getText(verseId: Int): String? {
        if (connection == null) return null
        val query = "SELECT content FROM verses WHERE id = ?"
        return try {
            val stmt = connection!!.prepareStatement(query)
            stmt.setInt(1, verseId)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getString("content") else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Busca versículos por texto em tempo real
    open fun searchVerses(text: String, limit: Int = 20): List<SearchResult> {
        if (connection == null || text.isBlank()) return emptyList()
        
        // Busca ignorando case e acentos (dependendo do suporte do SQLite local)
        // Por padrão, usaremos LIKE.
        val query = "SELECT id, content FROM verses WHERE content LIKE ? LIMIT ?"
        
        return try {
            val stmt = connection!!.prepareStatement(query)
            stmt.setString(1, "%$text%")
            stmt.setInt(2, limit)
            val rs = stmt.executeQuery()
            val results = mutableListOf<SearchResult>()
            while (rs.next()) {
                results.add(SearchResult(
                    id = rs.getInt("id"),
                    content = rs.getString("content")
                ))
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun close() {
        connection?.close()
    }
}
