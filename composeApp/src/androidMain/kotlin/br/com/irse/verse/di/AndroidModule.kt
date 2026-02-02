package br.com.irse.verse.di

import android.content.Context
import android.util.Log
import br.com.irse.verse.core.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import verse.composeapp.generated.resources.Res
import java.io.File
import java.io.FileOutputStream

val androidModule = module {
    // 1. Fornece o Mapeamento da Bíblia
    single<Map<String, BookMetaData>> {
        try {
            val mappingBytes = kotlinx.coroutines.runBlocking { Res.readBytes("files/bible_mapping.json") }
            Json.decodeFromString(mappingBytes.decodeToString())
        } catch (e: Exception) {
            Log.e("AndroidModule", "Erro ao carregar mapeamento", e)
            emptyMap()
        }
    }

    // 2. Fornece o Banco de Dados
    single<BibleDatabase> {
        val context: Context = get()
        val dbFile = File(context.filesDir, "bible.sqlite")
        
        // Sempre verifica o tamanho para garantir que não foi interrompido
        if (!dbFile.exists() || dbFile.length() < 1000000) { 
            Log.d("AndroidModule", "Copiando banco de dados para os arquivos locais...")
            try {
                val dbBytes = kotlinx.coroutines.runBlocking { Res.readBytes("files/bible.sqlite") }
                dbFile.writeBytes(dbBytes)
                Log.d("AndroidModule", "Banco de dados copiado com sucesso (${dbFile.length()} bytes)")
            } catch (e: Exception) {
                Log.e("AndroidModule", "Erro ao copiar banco de dados", e)
            }
        }
        
        BibleDatabase(dbFile.absolutePath)
    }

    // 2.5 Fornece o diretório base para o NotesRepository
    single<File> { 
        val context: Context = get()
        context.filesDir 
    }

    // 3. Fornece um SnapshotHandler fake
    single<SnapshotHandler> {
        object : SnapshotHandler {
            override suspend fun captureAndSave(verses: List<Pair<VerseRequest, String?>>, template: VerseViewModel.SnapshotTemplate) {}
            override suspend fun captureNoteAndSave(content: String, reference: String?, signature: String?, template: VerseViewModel.SnapshotTemplate) {}
        }
    }
}