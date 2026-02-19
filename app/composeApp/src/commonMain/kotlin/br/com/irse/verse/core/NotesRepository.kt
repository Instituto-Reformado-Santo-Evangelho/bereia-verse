package br.com.irse.verse.core

import kotlinx.coroutines.flow.StateFlow

interface NotesRepository {
    val notes: StateFlow<List<Note>>
    suspend fun loadNotes()
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(noteId: String)
    suspend fun hardDeleteNote(noteId: String)
    suspend fun getNotesForSync(): List<Note>
    fun getNoteForVerse(verseId: Int): Note?
    fun searchNotes(query: String): List<Note>
}
