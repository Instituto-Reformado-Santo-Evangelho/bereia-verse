package br.com.irse.verse.platform

import br.com.irse.verse.core.CloudSyncProvider
import br.com.irse.verse.core.CloudSyncState
import br.com.irse.verse.core.Note
import br.com.irse.verse.core.SyncStatus
import br.com.irse.verse.core.SettingsManager
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStreamReader
import verse.composeapp.generated.resources.Res

class JvmGoogleDriveProvider : CloudSyncProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val APPLICATION_NAME = "Bereia Versículos"
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private val SCOPES = listOf(DriveScopes.DRIVE_APPDATA)
    
    private val _syncState = MutableStateFlow(CloudSyncState.IDLE)
    override val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private var driveService: Drive? = null

    // Sincronizado com o diretório central do app
    private val appConfigDir: File get() = SettingsManager.dataDir

    private val dataStoreDir by lazy { File(appConfigDir, "tokens") }

    private val _manualCodeFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    override fun onManualCodeEntered(code: String) {
        _manualCodeFlow.tryEmit(code)
    }

    init {
        // Tenta re-autorizar silenciosamente se já tiver token
        CoroutineScope(Dispatchers.IO).launch {
            try {
                 if (dataStoreDir.exists() && dataStoreDir.listFiles()?.isNotEmpty() == true) {
                     restoreSession()
                 }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getGoogleClientSecrets(): GoogleClientSecrets {
        val details = GoogleClientSecrets.Details().apply {
            clientId = "221997659982-kuaq72a18co34kkb88rnamdlt0ablkaj.apps.googleusercontent.com"
            clientSecret = "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"
            authUri = "https://accounts.google.com/o/oauth2/auth"
            tokenUri = "https://oauth2.googleapis.com/token"
        }
        return GoogleClientSecrets().setInstalled(details)
    }

    private suspend fun restoreSession() {
        logError("Restoring session...")
        try {
            val clientSecrets = getGoogleClientSecrets()
            val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()

            val flow = GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
            )
            .setDataStoreFactory(FileDataStoreFactory(dataStoreDir))
            .setAccessType("offline")
            .build()

            logError("Flow built for restoration. Checking for stored credentials...")
            val credential = flow.loadCredential("user")
            
            if (credential != null && (credential.refreshToken != null || credential.expiresInSeconds == null || credential.expiresInSeconds > 60)) {
                logError("Stored credential found and valid.")
                driveService = Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build()
                _isAuthorized.value = true
                logError("Session restored successfully.")
            } else {
                logError("No valid stored credential found.")
            }
        } catch (e: Exception) {
            logError("Failed to restore session: ${e.message}", e)
            _isAuthorized.value = false
        }
    }

    private fun logError(message: String, error: Throwable? = null) {
        val timestamp = java.time.LocalDateTime.now()
        val logMessage = "[$timestamp] DRIVE_LOG: $message"
        println(logMessage)
        error?.printStackTrace()
    }

    override suspend fun authorize() = withContext(Dispatchers.IO) {
        logError("Starting manual authorization (Server-Side redirect)...")
        try {
            val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
            val clientSecrets = getGoogleClientSecrets()
            
            val flow = GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
            )
            .setDataStoreFactory(FileDataStoreFactory(dataStoreDir))
            .setAccessType("offline")
            .build()

            logError("Flow created. Redirecting to server for code capture...")
            
            // Redirect URI agora é o seu servidor Nuxt
            val redirectUri = "https://tech.santoevangelho.com.br/auth"
            
            val credential = try {
                val storedCredential = try { 
                    flow.loadCredential("user") 
                } catch (e: Exception) {
                    if (dataStoreDir.exists()) dataStoreDir.deleteRecursively()
                    null
                }

                if (storedCredential != null && storedCredential.refreshToken != null) {
                    logError("Using existing credential.")
                    storedCredential
                } else {
                    val state = java.util.UUID.randomUUID().toString()
                    val secureRandom = java.security.SecureRandom()
                    val verifierBytes = ByteArray(32)
                    secureRandom.nextBytes(verifierBytes)
                    val verifier = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes)
                    
                    val messageDigest = java.security.MessageDigest.getInstance("SHA-256")
                    val hash = messageDigest.digest(verifier.toByteArray(Charsets.US_ASCII))
                    val challenge = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash)

                    val authorizationUrl = com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl(
                        clientSecrets.details.authUri,
                        clientSecrets.details.clientId,
                        redirectUri,
                        SCOPES
                    ).apply {
                        this.state = state
                        set("code_challenge", challenge)
                        set("code_challenge_method", "S256")
                    }.build()
                    
                    logError("Authorization URL: $authorizationUrl")
                    
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().browse(java.net.URI(authorizationUrl))
                        } else {
                            ProcessBuilder("cmd", "/c", "start", authorizationUrl.replace("&", "^&")).start()
                        }
                    } catch (e: Exception) {
                        logError("Browser open failed", e)
                    }

                    logError("Waiting for manual code entry from UI...")
                    
                    // Aguarda o código ser injetado via onManualCodeEntered
                    val code = _manualCodeFlow.first()
                    
                    logError("Code received from UI! Exchanging...")
                    val tokenResponse = com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest(
                        HTTP_TRANSPORT,
                        JSON_FACTORY,
                        clientSecrets.details.tokenUri,
                        clientSecrets.details.clientId,
                        clientSecrets.details.clientSecret,
                        code,
                        redirectUri
                    ).apply {
                        set("code_verifier", verifier)
                    }.execute()
                    
                    flow.createAndStoreCredential(tokenResponse, "user")
                }
            } catch (e: Exception) {
                logError("Auth failed", e)
                throw e
            }
            
            driveService = Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
            
            _isAuthorized.value = true
            logError("AUTHORIZATION SUCCESSFUL.")
        } catch (e: Exception) {
            logError("Authorization failed", e)
            _isAuthorized.value = false
            throw e
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
        
        val isAuthError = (e is TokenResponseException && e.statusCode == 401) ||
                         (e is GoogleJsonResponseException && e.statusCode == 401) ||
                         (e.cause is TokenResponseException && (e.cause as TokenResponseException).statusCode == 401)

        if (isAuthError) {
            logError("Authentication failed (401). Resetting session.", e)
            _isAuthorized.value = false
            driveService = null
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
