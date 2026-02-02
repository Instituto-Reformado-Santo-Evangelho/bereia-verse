package br.com.irse.verse.platform

import br.com.irse.verse.core.CloudSyncProvider
import br.com.irse.verse.core.CloudSyncState
import br.com.irse.verse.core.Note
import br.com.irse.verse.core.SyncStatus
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
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

    // Diretório de configuração padrão do app
    private val appConfigDir: File by lazy {
        val os = System.getProperty("os.name").lowercase()
        val baseDir = if (os.contains("win")) {
            File(System.getenv("APPDATA"), "BereiaVerse")
        } else {
            File(System.getProperty("user.home"), ".local/share/bereia-verse")
        }
        if (!baseDir.exists()) baseDir.mkdirs()
        baseDir
    }

    private val dataStoreDir by lazy { File(appConfigDir, "tokens") }

    init {
        // Tenta re-autorizar silenciosamente se já tiver token
        try {
             if (dataStoreDir.exists() && dataStoreDir.listFiles()?.isNotEmpty() == true) {
                 // Inicializa o serviço em background se houver tokens
                 // Não podemos chamar authorize() aqui pois ele abre o browser se o token estiver inválido/expirado
                 // Mas podemos tentar reconstruir o serviço
                 restoreSession()
             }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restoreSession() {
        try {
            val secretsStream = getClientSecretsStream() ?: return
            val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
            val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(secretsStream))

            val flow = GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
            )
            .setDataStoreFactory(FileDataStoreFactory(dataStoreDir))
            .setAccessType("offline")
            .build()

            // Carrega credencial armazenada (userId="user")
            val credential = flow.loadCredential("user")
            
            if (credential != null && (credential.refreshToken != null || credential.expiresInSeconds == null || credential.expiresInSeconds > 60)) {
                // Se tiver refresh token, ele atualiza automaticamente quando precisar
                driveService = Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build()
                _isAuthorized.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isAuthorized.value = false
        }
    }
    
    private fun getClientSecretsStream(): java.io.InputStream? {
        // 1. Tenta carregar do recurso embutido (JAR) - Ideal para Produção
        val resourceStream = object {}.javaClass.getResourceAsStream("/client_secrets.json")
        if (resourceStream != null) return resourceStream

        // 2. Fallback para arquivos externos (Desenvolvimento/Override)
        val possibilities = listOf(
            File(appConfigDir, "client_secrets.json"),
            File(System.getProperty("user.dir"), "client_secrets.json"),
            File("client_secrets.json")
        )
        
        return possibilities.find { it.exists() }?.inputStream()
    }

    private fun logError(message: String, error: Throwable? = null) {
        try {
            val logFile = File(appConfigDir, "auth_debug.txt")
            val timestamp = java.time.LocalDateTime.now()
            val stackTrace = error?.stackTraceToString() ?: ""
            logFile.appendText("[$timestamp] $message\n$stackTrace\n\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun authorize() = withContext(Dispatchers.IO) {
        try {
            logError("Starting authorization process...")
            val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
            
            val secretsStream = getClientSecretsStream()
            if (secretsStream == null) {
                val msg = "client_secrets.json NOT FOUND. Searched in: Resources, ${appConfigDir.absolutePath}, ${System.getProperty("user.dir")}"
                logError(msg)
                throw Exception("Arquivo de credenciais (client_secrets.json) não encontrado.\nVerifique o arquivo auth_debug.txt na pasta do aplicativo.")
            }

            logError("Secrets file found. Loading...")
            val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(secretsStream))
            
            val flow = GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
            )
            .setDataStoreFactory(FileDataStoreFactory(dataStoreDir))
            .setAccessType("offline")
            .build()

            logError("Flow built. Attempting to authorize...")
            
            // Custom receiver that logs the URL if browser fails
            val receiver = LocalServerReceiver.Builder().setPort(8888).build()
            
            val authApp = AuthorizationCodeInstalledApp(flow, receiver)
            
            val credential = try {
                // Tenta abrir o navegador padrão de forma customizada se necessário
                authApp.authorize("user")
            } catch (e: Exception) {
                logError("AuthorizationCodeInstalledApp failed. Try manual fallback.", e)
                // Se o erro for relacionado à abertura do browser, tentamos notificar melhor
                throw Exception("Não foi possível abrir o seu navegador para o login.\nVerifique se você tem um navegador padrão configurado ou tente novamente.")
            }
            
            logError("Authorization successful. Building Drive service...")
            driveService = Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
            
            _isAuthorized.value = true
            logError("Service ready.")
        } catch (e: Exception) {
            logError("CRITICAL AUTHORIZATION ERROR", e)
            _isAuthorized.value = false
            
            val userMsg = when {
                e.message?.contains("browse") == true -> "Falha ao abrir navegador. Certifique-se de que há um navegador padrão configurado."
                e.message?.contains("access_denied") == true -> "Acesso negado. Você precisa autorizar o aplicativo para sincronizar."
                else -> "Erro na autenticação: ${e.localizedMessage ?: "Falha desconhecida"}"
            }
            throw Exception(userMsg)
        }
    }

    override suspend fun signOut() {
        if (dataStoreDir.exists()) dataStoreDir.deleteRecursively()
        driveService = null
        _isAuthorized.value = false
    }

    private fun handleSyncException(e: Exception) {
        e.printStackTrace()
        _syncState.value = CloudSyncState.ERROR
        
        // Verifica se é um erro de autenticação (Token revogado ou expirado sem refresh)
        val isAuthError = (e is TokenResponseException && e.statusCode == 401) ||
                         (e is GoogleJsonResponseException && e.statusCode == 401) ||
                         (e.cause is TokenResponseException && (e.cause as TokenResponseException).statusCode == 401)

        if (isAuthError) {
            logError("Authentication failed (401). Resetting session.", e)
            _isAuthorized.value = false
            driveService = null
            // Limpa tokens locais para forçar novo login no próximo authorize()
            if (dataStoreDir.exists()) dataStoreDir.deleteRecursively()
        }
    }

    override suspend fun uploadNote(note: Note) = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext
        try {
            _syncState.value = CloudSyncState.SYNCING
            
            val fileMetadata = com.google.api.services.drive.model.File()
            fileMetadata.name = "${note.id}.json"
            fileMetadata.parents = listOf("appDataFolder")

            val tempFile = File.createTempFile("note_sync", ".json")
            tempFile.writeText(Json.encodeToString(Note.serializer(), note))
            
            val mediaContent = FileContent("application/json", tempFile)
            
            // Verifica se o arquivo já existe para atualizar (update) ou criar (insert)
            val existingFiles = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '${note.id}.json' and trashed = false")
                .execute()

            if (existingFiles.files.isEmpty()) {
                service.files().create(fileMetadata, mediaContent).execute()
            } else {
                service.files().update(existingFiles.files[0].id, null, mediaContent).execute()
            }
            tempFile.delete()
            
            _syncState.value = CloudSyncState.SUCCESS
        } catch (e: Exception) {
            handleSyncException(e)
        }
    }

    override suspend fun downloadNotes(): List<Note> = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext emptyList()
        try {
            _syncState.value = CloudSyncState.SYNCING
            
            val result = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("trashed = false and name contains '.json'")
                .setFields("files(id, name)")
                .execute()

            val notes = result.files.mapNotNull { file ->
                val outputStream = java.io.ByteArrayOutputStream()
                service.files().get(file.id).executeMediaAndDownloadTo(outputStream)
                try {
                    json.decodeFromString<Note>(outputStream.toString())
                } catch (e: Exception) {
                    null
                }
            }
            
            _syncState.value = CloudSyncState.SUCCESS
            notes
        } catch (e: Exception) {
            handleSyncException(e)
            emptyList()
        }
    }

    override suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        val service = driveService ?: return@withContext
        try {
            _syncState.value = CloudSyncState.SYNCING
            
            val existingFiles = service.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$noteId.json' and trashed = false")
                .execute()

            existingFiles.files.forEach { file ->
                service.files().delete(file.id).execute()
            }
            
            _syncState.value = CloudSyncState.SUCCESS
        } catch (e: Exception) {
            handleSyncException(e)
        }
    }
}
