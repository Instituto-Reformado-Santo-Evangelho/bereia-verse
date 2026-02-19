package br.com.irse.verse.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class HistoryEntry(
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

object HistoryManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val historyFile: File by lazy {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        val dir = if (os.contains("win")) {
            File(System.getenv("APPDATA"), "BereiaVerse")
        } else {
            File(System.getProperty("user.home") ?: "", ".local/share/bereia-verse")
        }
        if (!dir.exists()) dir.mkdirs()
        File(dir, "history.json")
    }

    suspend fun saveEntry(query: String) = withContext(Dispatchers.IO) {
        try {
            val currentHistory = getHistory().toMutableList()
            
            // Remove a entrada se já existir em qualquer lugar para evitar duplicatas
            currentHistory.removeAll { it.query.equals(query, ignoreCase = true) }
            
            // Adiciona no topo
            currentHistory.add(0, HistoryEntry(query))
            
            val limited = currentHistory.take(50)
            historyFile.writeText(json.encodeToString(limited))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getHistory(): List<HistoryEntry> = withContext(Dispatchers.IO) {
        try {
            if (!historyFile.exists()) return@withContext emptyList()
            json.decodeFromString<List<HistoryEntry>>(historyFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }
    
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        if (historyFile.exists()) historyFile.delete()
    }
}