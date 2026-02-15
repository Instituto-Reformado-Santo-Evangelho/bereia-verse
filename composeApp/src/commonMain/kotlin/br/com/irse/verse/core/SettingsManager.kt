package br.com.irse.verse.core

import ca.gosyer.appdirs.AppDirs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class UserSettings(
    val fontSize: Int = 16,
    val fontFamily: String = "sans-serif",
    val lineHeight: Float = 1.4f,
    val showFireAnimation: Boolean = false,

    val signature: String = "",
    val showSnapshotAction: Boolean = false,
    val isTransparent: Boolean = false, // Padrão desabilitado no Windows
    val isTransparencySupported: Boolean = true, // Detected at runtime
    val windowWidth: Int = 400,
    val windowHeight: Int = 400,
    val windowX: Int? = null,  // Posição X salva (null = usar padrão)
    val windowY: Int? = null   // Posição Y salva (null = usar padrão)
)

object SettingsManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    private val appDirs = AppDirs(appName = "BereiaVerse", appAuthor = "IRSE")

    private val settingsFile: File by lazy { File(dataDir, "settings.json") }
    val lockFile: File by lazy { File(dataDir, "launching.lock") }

    fun getSettingsSync(): UserSettings {
        return try {
            if (!settingsFile.exists()) UserSettings()
            else json.decodeFromString<UserSettings>(settingsFile.readText())
        } catch (e: Exception) {
            UserSettings()
        }
    }

    fun saveSettingsSync(settings: UserSettings) {
        try {
            settingsFile.writeText(json.encodeToString(settings))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        saveSettingsSync(settings)
    }

    suspend fun getSettings(): UserSettings = withContext(Dispatchers.IO) {
        getSettingsSync()
    }
}