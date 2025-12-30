package br.com.irse.verse.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

open class SettingsRepository {
    // We could inject a Persistence layer here later
    
    private val _settings = MutableStateFlow(UserSettings())
    val settings = _settings.asStateFlow()

    open suspend fun loadSettings() {
        // Wrapper for legacy SettingsManager
        val loaded = SettingsManager.getSettings()
        _settings.value = loaded
    }

    open suspend fun updateFontSize(size: Int) {
        val current = _settings.value
        val newSettings = current.copy(fontSize = size.coerceIn(12, 32))
        save(newSettings)
    }

    open suspend fun updateFontFamily(family: String) {
        val current = _settings.value
        val newSettings = current.copy(fontFamily = family)
        save(newSettings)
    }

    open suspend fun updateLineHeight(height: Float) {
        val current = _settings.value
        val newSettings = current.copy(lineHeight = height)
        save(newSettings)
    }

    open suspend fun updateShowFireAnimation(enabled: Boolean) {
        val current = _settings.value
        val newSettings = current.copy(showFireAnimation = enabled)
        save(newSettings)
    }

    open suspend fun updateAnimatedWindow(enabled: Boolean) {
        val current = _settings.value
        val newSettings = current.copy(animatedWindow = enabled)
        save(newSettings)
    }

    open suspend fun updateSignature(signature: String) {
        val current = _settings.value
        val newSettings = current.copy(signature = signature)
        save(newSettings)
    }

    open suspend fun updateShowSnapshotAction(enabled: Boolean) {
        val current = _settings.value
        val newSettings = current.copy(showSnapshotAction = enabled)
        save(newSettings)
    }

    private suspend fun save(newSettings: UserSettings) {
        SettingsManager.saveSettings(newSettings)
        _settings.value = newSettings
    }
}
