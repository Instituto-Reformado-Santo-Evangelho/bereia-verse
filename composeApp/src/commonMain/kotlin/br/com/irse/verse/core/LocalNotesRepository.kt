package br.com.irse.verse.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.io.File

class LocalNotesRepository : NotesRepository {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    override val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val notesDir: File by lazy {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        val baseDir = if (os.contains("win")) {
            File(System.getenv("APPDATA"), "BereiaVerse")
        } else {
            File(System.getProperty("user.home") ?: "", ".local/share/bereia-verse")
        }
        val dir = File(baseDir, "notes")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    override suspend fun loadNotes() = withContext(Dispatchers.IO) {
        try {
            val noteFiles = notesDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
            val loadedNotes = noteFiles.mapNotNull { file ->
                try {
                    json.decodeFromString<Note>(file.readText())
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.updatedAt }
            
            _notes.value = loadedNotes
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun saveNote(note: Note) = withContext(Dispatchers.IO) {
        try {
            val file = File(notesDir, "${note.id}.json")
            val content = json.encodeToString(Note.serializer(), note)
            file.writeText(content)
            
            // Atualiza estado local
            val currentList = _notes.value.toMutableList()
            currentList.removeAll { it.id == note.id }
            currentList.add(0, note)
            _notes.value = currentList.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(notesDir, "$noteId.json")
            if (file.exists()) file.delete()
            
            _notes.value = _notes.value.filterNot { it.id == noteId }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getNoteForVerse(verseId: Int): Note? {
        return _notes.value.find { it.verseId == verseId }
    }

    override fun searchNotes(query: String): List<Note> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()
        return _notes.value.filter { note ->
            note.content.lowercase().contains(lowerQuery)
        }
    }
}
