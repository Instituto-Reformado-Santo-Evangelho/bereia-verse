package br.com.irse.verse.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class UserSettings(
    val fontSize: Int = 16,
    val fontFamily: String = "sans-serif",
    val lineHeight: Float = 1.4f
)

object SettingsManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val settingsFile: File by lazy {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        val dir = if (os.contains("win")) {
            File(System.getenv("APPDATA"), "BereiaVerse")
        } else {
            File(System.getProperty("user.home") ?: "", ".local/share/bereia-verse")
        }
        if (!dir.exists()) dir.mkdirs()
        File(dir, "settings.json")
    }

    suspend fun saveSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        try {
            settingsFile.writeText(json.encodeToString(settings))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getSettings(): UserSettings = withContext(Dispatchers.IO) {
        try {
            if (!settingsFile.exists()) return@withContext UserSettings()
            json.decodeFromString<UserSettings>(settingsFile.readText())
        } catch (e: Exception) {
            UserSettings()
        }
    }
}