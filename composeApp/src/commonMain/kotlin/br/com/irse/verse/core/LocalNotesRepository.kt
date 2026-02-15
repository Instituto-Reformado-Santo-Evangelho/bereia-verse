package br.com.irse.verse.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.io.File

class LocalNotesRepository(private val baseDir: File) : NotesRepository {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    override val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val notesDir: File by lazy {
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
            }.filter { !it.isDeleted }.sortedByDescending { it.updatedAt }
            
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
            
            // Atualiza estado local (só se não estiver deletada)
            val currentList = _notes.value.toMutableList()
            currentList.removeAll { it.id == note.id }
            if (!note.isDeleted) {
                currentList.add(0, note)
            }
            _notes.value = currentList.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        try {
            // Buscamos em TODAS as notas físicas para garantir que podemos marcar como deletada
            val file = File(notesDir, "$noteId.json")
            if (!file.exists()) return@withContext
            
            val note = json.decodeFromString<Note>(file.readText())
            saveNote(note.copy(isDeleted = true, syncStatus = SyncStatus.PENDING, updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun hardDeleteNote(noteId: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(notesDir, "$noteId.json")
            if (file.exists()) file.delete()
            
            _notes.value = _notes.value.filterNot { it.id == noteId }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getNotesForSync(): List<Note> = withContext(Dispatchers.IO) {
        try {
            val noteFiles = notesDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
            noteFiles.mapNotNull { file ->
                try {
                    json.decodeFromString<Note>(file.readText())
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
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