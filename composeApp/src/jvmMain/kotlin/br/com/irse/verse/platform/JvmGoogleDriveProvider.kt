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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
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
        CoroutineScope(Dispatchers.IO).launch {
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
    }

    private fun restoreSession() {
        try {
            val secretsStream = getClientSecretsStream() ?: return
            val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
            val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(secretsStream, Charsets.UTF_8))

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
        // 1. Tenta carregar da variável de ambiente GOOGLE_CLIENT_SECRETS
        val envSecrets = System.getenv("GOOGLE_CLIENT_SECRETS")
        if (!envSecrets.isNullOrBlank()) {
            logError("Loading credentials from GOOGLE_CLIENT_SECRETS environment variable")
            return envSecrets.byteInputStream(Charsets.UTF_8)
        }
        
        // 2. Tenta carregar do recurso embutido - PRIORIDADE (composeResources/files/)
        val resourcePaths = listOf(
            "/composeResources/verse.composeapp.generated.resources/files/client_secrets.json",
            "/files/client_secrets.json",  // Compose Resources
            "/client_secrets.json"          // Resources raiz (fallback)
        )
        
        for (path in resourcePaths) {
            val resourceStream = object {}.javaClass.getResourceAsStream(path)
            if (resourceStream != null) {
                logError("Loading credentials from embedded resources: $path")
                return resourceStream
            }
        }
        
        // 3. Tenta carregar do arquivo especificado em GOOGLE_CLIENT_SECRETS_PATH
        val envPath = System.getenv("GOOGLE_CLIENT_SECRETS_PATH")
        if (!envPath.isNullOrBlank()) {
            val envFile = File(envPath)
            if (envFile.exists()) {
                logError("Loading credentials from GOOGLE_CLIENT_SECRETS_PATH: ${envFile.absolutePath}")
                return envFile.inputStream()
            }
        }

        // 4. Fallback para arquivos externos (Desenvolvimento/Override)
        val possibilities = listOf(
            File(appConfigDir, "client_secrets.json"),
            File(System.getProperty("user.dir"), "client_secrets.json"),
            File("client_secrets.json")
        )
        
        val foundFile = possibilities.find { it.exists() }
        if (foundFile != null) {
            logError("Loading credentials from file: ${foundFile.absolutePath}")
        }
        return foundFile?.inputStream()
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
            val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(secretsStream, Charsets.UTF_8))
            
            val flow = GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
            )
            .setDataStoreFactory(FileDataStoreFactory(dataStoreDir))
            .setAccessType("offline")
            .build()

            logError("Flow built. Starting local server...")
            // Usa porta dinâmica padrão para evitar conflitos
            val receiver = LocalServerReceiver.Builder().build() 
            
            val credential = try {
                // Verifica se já existe credencial válida
                val storedCredential = try { 
                    flow.loadCredential("user") 
                } catch (e: Exception) {
                    logError("Error loading stored credential, clearing dataStoreDir", e)
                    if (dataStoreDir.exists()) dataStoreDir.deleteRecursively()
                    null
                }

                if (storedCredential != null && storedCredential.refreshToken != null) {
                    logError("Found existing credential, using it")
                    storedCredential
                } else {
                    // Precisa autorizar - gera URL e FORÇA abertura do navegador
                    val redirectUri = receiver.redirectUri
                    
                    // 1. Gerar State para prevenir CSRF (Cross-Site Request Forgery)
                    val state = java.util.UUID.randomUUID().toString()
                    
                    // 2. Gerar PKCE (Proof Key for Code Exchange)
                    // Verifier: Segredo aleatório de alta entropia
                    val secureRandom = java.security.SecureRandom()
                    val verifierBytes = ByteArray(32)
                    secureRandom.nextBytes(verifierBytes)
                    val verifier = com.google.api.client.util.Base64.encodeBase64URLSafeString(verifierBytes)
                    
                    // Challenge: Hash SHA-256 do verifier
                    val messageDigest = java.security.MessageDigest.getInstance("SHA-256")
                    val hash = messageDigest.digest(verifier.toByteArray(Charsets.US_ASCII))
                    val challenge = com.google.api.client.util.Base64.encodeBase64URLSafeString(hash)

                    val authorizationUrl = com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl(
                        clientSecrets.details.authUri,
                        clientSecrets.details.clientId,
                        redirectUri,
                        SCOPES
                    ).apply {
                        this.state = state // Usa 'this' para diferenciar da val local
                        set("code_challenge", challenge)
                        set("code_challenge_method", "S256")
                    }.build()
                    
                    logError("Authorization URL generated with PKCE and State")
                    logError("Attempting to open browser...")
                    
                    // Tenta abrir navegador com múltiplos métodos
                    var browserOpened = false
                    
                    // Método 1: Java Desktop API (Recomendado - Resolve problemas de aspas/espaços)
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            val desktop = java.awt.Desktop.getDesktop()
                            if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                                logError("Trying Desktop.browse()...")
                                desktop.browse(java.net.URI(authorizationUrl))
                                browserOpened = true
                                logError("Browser opened via Desktop.browse()")
                            }
                        }
                    } catch (e: Exception) {
                        logError("Desktop.browse() failed: ${e.message}")
                    }
                    
                    // Método 2: Runtime.exec com comandos específicos de OS (Fallback)
                    if (!browserOpened) {
                        try {
                            val os = System.getProperty("os.name").lowercase()
                            if (os.contains("win")) {
                                logError("Trying Windows cmd /c start...")
                                // Tenta primeiro cmd /c start que resolve melhor URLs no Win
                                try {
                                    val escapedUrl = authorizationUrl.replace("&", "^&")
                                    Runtime.getRuntime().exec("cmd /c start $escapedUrl")
                                    browserOpened = true
                                    logError("Browser attempted via cmd /c start")
                                } catch (e: Exception) {
                                    logError("cmd /c start failed, trying rundll32...")
                                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $authorizationUrl")
                                    browserOpened = true
                                    logError("Browser attempted via rundll32")
                                }
                            } else if (os.contains("mac")) {
                                logError("Trying macOS open...")
                                Runtime.getRuntime().exec(arrayOf("open", authorizationUrl))
                                browserOpened = true
                                logError("Browser opened via open")
                            } else {
                                logError("Trying Linux xdg-open/alternatives...")
                                val commands = listOf("xdg-open", "gnome-open", "kde-open")
                                for (cmd in commands) {
                                    try {
                                        Runtime.getRuntime().exec(arrayOf(cmd, authorizationUrl))
                                        browserOpened = true
                                        logError("Browser opened via $cmd")
                                        break
                                    } catch (_: Exception) { }
                                }
                            }
                        } catch (e: Exception) {
                            logError("Runtime.exec failed: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                    
                    if (!browserOpened) {
                        logError("ALL BROWSER OPENING METHODS FAILED!")
                        logError("User MUST manually open: $authorizationUrl")
                        throw Exception("Não foi possível abrir o navegador automaticamente.\n\nCopie e abra esta URL manualmente no seu navegador:\n\n$authorizationUrl\n\nDepois volte ao aplicativo e aguarde.")
                    }
                    
                    // Aguarda callback do navegador
                    logError("Waiting for user authorization...")
                    val code = receiver.waitForCode()
                    logError("Authorization code received")
                    
                    // Troca código por token, enviando o Verifier do PKCE
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
                    
                    logError("Token obtained successfully with PKCE verification")
                    
                    flow.createAndStoreCredential(tokenResponse, "user")
                }
            } catch (e: Exception) {
                receiver.stop()
                logError("Authorization failed", e)
                throw e
            } finally {
                receiver.stop()
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
