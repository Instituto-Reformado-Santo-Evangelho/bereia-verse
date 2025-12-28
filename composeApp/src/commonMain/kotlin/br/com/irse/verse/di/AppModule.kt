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

val appModule = module {
    // Repository expects Map<String, BookMetaData> to be provided
    single { BibleRepository(get()) }
    
    // Parser expects BibleRepository
    single { BibleParser(get()) }
    
    single { SettingsRepository() }
    single { HistoryRepository() }
    single { SearchUseCase(get(), get()) }
    single { CoroutineDispatchers() }
    
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
            dispatchers = get()
        )
    }
}
