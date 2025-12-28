package br.com.irse.verse.di

import br.com.irse.verse.core.BibleDatabase
import br.com.irse.verse.core.BibleParser
import br.com.irse.verse.core.BibleRepository
import br.com.irse.verse.core.VerseViewModel
import br.com.irse.verse.core.SnapshotHandler
import org.koin.dsl.module

import br.com.irse.verse.core.SettingsRepository
import br.com.irse.verse.core.SearchUseCase
import br.com.irse.verse.core.HistoryRepository
import br.com.irse.verse.core.CoroutineDispatchers
import br.com.irse.verse.core.NotesRepository
import br.com.irse.verse.core.LocalNotesRepository
import br.com.irse.verse.core.SyncManager
import br.com.irse.verse.core.CloudSyncProvider

val appModule = module {
    // Repository expects Map<String, BookMetaData> to be provided
    single { BibleRepository(get()) }
    
    // Parser expects BibleRepository
    single { BibleParser(get()) }
    
    single { SettingsRepository() }
    single { HistoryRepository() }
    single<NotesRepository> { LocalNotesRepository() }
    single { SearchUseCase(get(), get()) }
    single { CoroutineDispatchers() }
    
    // SyncManager - cloudProvider must be provided by platform
    single { SyncManager(get(), getOrNull(), get()) }
    
    // ViewModel expects Parser, Database, and SnapshotHandler
    // Database and SnapshotHandler must be provided by the platform module
    factory { 
        VerseViewModel(
            parser = get(),
            database = get(),
            snapshotHandler = get(),
            settingsRepository = get(),
            searchUseCase = get(),
            historyRepository = get(),
            notesRepository = get(),
            syncManager = get(),
            dispatchers = get()
        )
    }
}
