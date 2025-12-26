package br.com.irse.verse.core

import java.sql.DriverManager
import java.sql.Connection
import java.io.File

class BibleDatabase(private val dbPath: String) {
    
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

    fun getText(verseId: Int): String? {
        if (connection == null) return null
        
        // Assuming table 'bible_verse' has 'id' and 'text' columns based on context
        // We will adjust if the schema is different
        val query = "SELECT content FROM verses WHERE id = ?"
        
        return try {
            val stmt = connection!!.prepareStatement(query)
            stmt.setInt(1, verseId)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                rs.getString("content")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun close() {
        connection?.close()
    }
}
