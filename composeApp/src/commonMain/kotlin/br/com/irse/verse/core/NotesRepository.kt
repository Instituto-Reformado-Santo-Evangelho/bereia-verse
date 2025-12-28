package br.com.irse.verse.core

import kotlinx.coroutines.flow.StateFlow

interface NotesRepository {
    val notes: StateFlow<List<Note>>
    suspend fun loadNotes()
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(noteId: String)
    fun getNoteForVerse(verseId: Int): Note?
}
