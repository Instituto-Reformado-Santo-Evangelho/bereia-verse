package br.com.irse.verse.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VerseViewModelTest {
    
    // Fakes
    class FakeRepository : BibleRepository(emptyMap()) {
        override fun getVerseRequest(verseId: Int): VerseRequest? = null
    }

    class FakeParser(repo: BibleRepository) : BibleParser(repo) {
        override fun processSelection(text: String): List<VerseRequest> {
             if (text == "Error") throw Exception("Parser Failure")
             return emptyList()
        }
    }

    class FakeDatabase : BibleDatabase("dummy") {
        override fun getText(verseId: Int): String? = "Verse Content"
        override fun searchVerses(text: String, limit: Int): List<SearchResult> {
            if (text == "Error") throw Exception("DB Failure")
            return emptyList()
        }
    }

    class FakeSnapshotHandler : SnapshotHandler {
        override suspend fun captureAndSave(
            verses: List<Pair<VerseRequest, String?>>,
            template: VerseViewModel.SnapshotTemplate
        ) {}
        
        override suspend fun captureNoteAndSave(
            content: String,
            reference: String?,
            signature: String?,
            template: VerseViewModel.SnapshotTemplate
        ) {}
    }
    
    class FakeSettingsRepository : SettingsRepository() {
         override suspend fun loadSettings() {}
    }
    
    class FakeSearchUseCase(parser: BibleParser, database: BibleDatabase) : SearchUseCase(parser, database) {
        override suspend fun execute(query: String): List<SearchResult> {
            if (query == "Error") throw Exception("Search Failure")
            return emptyList()
        }
    }

    class FakeHistoryRepository : HistoryRepository() {
        override suspend fun loadHistory() {}
        override suspend fun saveEntry(query: String) {}
    }
    
    class FakeNotesRepository : NotesRepository {
        override val notes = MutableStateFlow<List<Note>>(emptyList())
        override suspend fun loadNotes() {}
        override suspend fun saveNote(note: Note) {}
        override suspend fun deleteNote(noteId: String) {}
        override fun getNoteForVerse(verseId: Int): Note? = null
        override fun searchNotes(query: String): List<Note> = emptyList()
    }

    private lateinit var viewModel: VerseViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testDispatchers = CoroutineDispatchers(
        main = testDispatcher,
        io = testDispatcher,
        default = testDispatcher
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        val repo = FakeRepository()
        val parser = FakeParser(repo)
        val database = FakeDatabase()
        val notesRepo = FakeNotesRepository()
        val syncManager = SyncManager(notesRepo, null, testDispatchers)
        
        viewModel = VerseViewModel(
            parser = parser,
            database = database,
            snapshotHandler = FakeSnapshotHandler(),
            settingsRepository = FakeSettingsRepository(),
            searchUseCase = FakeSearchUseCase(parser, database),
            historyRepository = FakeHistoryRepository(),
            notesRepository = notesRepo,
            syncManager = syncManager,
            dispatchers = testDispatchers
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when search fails, error state should be updated`() = runTest {
        viewModel.onSearchQueryChanged("Error")
        
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.runCurrent()
        
        val error = viewModel.errorState.value
        assertNotNull(error, "Error state should not be null after search failure")
        assertTrue(error.contains("Search Failure"), "Error message '$error' should contain 'Search Failure'")
    }

    @Test
    fun `when processQuery fails, error state should be updated`() = runTest {
        viewModel.processQuery("Error")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val error = viewModel.errorState.value
        assertNotNull(error, "Error state should not be null after processQuery failure")
        assertTrue(error.contains("Parser Failure"), "Error message '$error' should contain 'Parser Failure'")
    }
}