package br.com.irse.verse.platform

import br.com.irse.verse.core.CloudSyncProvider
import br.com.irse.verse.core.CloudSyncState
import br.com.irse.verse.core.Note
import br.com.irse.verse.core.SyncStatus
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStreamReader

class JvmGoogleDriveProvider : CloudSyncProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val APPLICATION_NAME = "Bereia Verse"
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private val SCOPES = listOf(DriveScopes.DRIVE_APPDATA)
    
    private val _syncState = MutableStateFlow(CloudSyncState.IDLE)
    override val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private var driveService: Drive? = null

    private val dataStoreDir = File(System.getProperty("user.home"), ".bereia-verse/tokens")

    init {
        // Tenta re-autorizar silenciosamente se já tiver token
        if (dataStoreDir.exists() && dataStoreDir.listFiles()?.isNotEmpty() == true) {
            try {
                // Em um app real, faríamos a inicialização do serviço aqui
                // _isAuthorized.value = true
            } catch (e: Exception) {}
        }
    }

    override suspend fun authorize() = withContext(Dispatchers.IO) {
        try {
            val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
            
            // Carrega credenciais (Placeholder ou Arquivo)
            // Para o usuário: Ele deve colocar o client_secrets.json na pasta de recursos
            val secretsFile = File("client_secrets.json")
            if (!secretsFile.exists()) {
                throw Exception("Arquivo client_secrets.json não encontrado. Configure no Google Cloud Console.")
            }

            val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, secretsFile.reader())
            
            val flow = GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
            )
            .setDataStoreFactory(FileDataStoreFactory(dataStoreDir))
            .setAccessType("offline")
            .build()

            val credential = AuthorizationCodeInstalledApp(flow, LocalServerReceiver()).authorize("user")
            
            driveService = Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
            
            _isAuthorized.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            _isAuthorized.value = false
        }
    }

    override suspend fun signOut() {
        if (dataStoreDir.exists()) dataStoreDir.deleteRecursively()
        driveService = null
        _isAuthorized.value = false
    }

    override suspend fun uploadNote(note: Note) = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext
        try {
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = "${note.id}.json"
            fileMetadata.parents = listOf("appDataFolder")

            val tempFile = File.createTempFile("note_sync", ".json")
            tempFile.writeText(Json.encodeToString(Note.serializer(), note))
            
            val mediaContent = FileContent("application/json", tempFile)
            
            // Verifica se o arquivo já existe para atualizar (update) ou criar (insert)
            val existingFiles = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '${note.id}.json'")
                .execute()

            if (existingFiles.files.isEmpty()) {
                service.files().create(fileMetadata, mediaContent).execute()
            } else {
                service.files().update(existingFiles.files[0].id, null, mediaContent).execute()
            }
            tempFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun downloadNotes(): List<Note> = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext emptyList()
        try {
            val result = service.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute()

            result.files.mapNotNull { file ->
                val outputStream = java.io.ByteArrayOutputStream()
                service.files().get(file.id).executeMediaAndDownloadTo(outputStream)
                try {
                    json.decodeFromString<Note>(outputStream.toString())
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext
        try {
            val existingFiles = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$noteId.json'")
                .execute()

            existingFiles.files.forEach { file ->
                service.files().delete(file.id).execute()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
