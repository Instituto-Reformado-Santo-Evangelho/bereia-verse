package br.com.irse.verse.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.BufferedReader
import java.io.InputStreamReader

object ClipboardMonitor {
    fun textFlow(pollingIntervalMs: Long = 1000): Flow<String> = flow {
        var lastText = ""
        while (true) {
            try {
                val currentText = getClipboardText()
                if (currentText.isNotBlank() && currentText != lastText) {
                    lastText = currentText
                    emit(currentText)
                }
            } catch (e: Exception) {
                // Ignore errors to prevent crash loop affecting main thread
            }
            delay(pollingIntervalMs)
        }
    }

    private fun getClipboardText(): String {
        // 1. Try AWT (Standard Java) - Retry logic for Windows locks
        var attempts = 0
        while (attempts < 3) {
            try {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                    return clipboard.getData(DataFlavor.stringFlavor) as? String ?: ""
                }
                break // Success but no text
            } catch (_: Throwable) {
                // Ignore and retry
                attempts++
                try { Thread.sleep(50) } catch (_: Exception) {}
            }
        }

        // 2. Try Linux Native (Wayland/X11) as fallback
        if (System.getProperty("os.name")?.lowercase()?.contains("linux") == true) {
            return getLinuxClipboard()
        }

        return ""
    }

    private fun getLinuxClipboard(): String {
        // Try wl-paste (Wayland)
        try {
            val p = Runtime.getRuntime().exec(arrayOf("wl-paste", "--no-newline"))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val text = reader.readText()
            if (p.waitFor() == 0 && text.isNotBlank()) return text
        } catch (_: Exception) { }

        // Try xclip (X11)
        try {
            val p = Runtime.getRuntime().exec(arrayOf("xclip", "-o", "-selection", "clipboard"))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val text = reader.readText()
            if (p.waitFor() == 0 && text.isNotBlank()) return text
        } catch (_: Exception) { }

        // Try xsel (X11)
        try {
            val p = Runtime.getRuntime().exec(arrayOf("xsel", "--clipboard", "--output"))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val text = reader.readText()
            if (p.waitFor() == 0 && text.isNotBlank()) return text
        } catch (_: Exception) { }

        return ""
    }
}
