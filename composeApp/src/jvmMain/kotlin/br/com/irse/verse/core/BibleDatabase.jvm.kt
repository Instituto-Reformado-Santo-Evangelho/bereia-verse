package br.com.irse.verse.core

import java.sql.DriverManager
import java.sql.Connection

actual class BibleDatabase actual constructor(dbPath: String) {
    
    private var connection: Connection? = null

    init {
        try {
            Class.forName("org.sqlite.JDBC")
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun getText(verseId: Int): String? {
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

    actual fun searchVerses(text: String, limit: Int): List<SearchResult> {
        if (connection == null || text.isBlank()) return emptyList()
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
    
    actual fun close() {
        connection?.close()
    }
}
