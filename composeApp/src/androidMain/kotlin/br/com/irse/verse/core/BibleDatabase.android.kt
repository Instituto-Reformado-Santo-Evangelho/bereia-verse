package br.com.irse.verse.core

import android.database.sqlite.SQLiteDatabase

actual class BibleDatabase actual constructor(dbPath: String) {
    
    private var database: SQLiteDatabase? = null

    init {
        try {
            database = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun getText(verseId: Int): String? {
        if (database == null) return null
        val cursor = database!!.rawQuery("SELECT content FROM verses WHERE id = ?", arrayOf(verseId.toString()))
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    actual fun searchVerses(text: String, limit: Int): List<SearchResult> {
        if (database == null || text.isBlank()) return emptyList()
        val results = mutableListOf<SearchResult>()
        val cursor = database!!.rawQuery(
            "SELECT id, content FROM verses WHERE content LIKE ? LIMIT ?", 
            arrayOf("%$text%", limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                results.add(SearchResult(
                    id = it.getInt(0),
                    content = it.getString(1)
                ))
            }
        }
        return results
    }
    
    actual fun close() {
        database?.close()
    }
}
