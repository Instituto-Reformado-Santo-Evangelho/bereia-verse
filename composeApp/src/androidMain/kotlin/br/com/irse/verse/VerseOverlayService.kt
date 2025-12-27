package br.com.irse.verse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import br.com.irse.verse.core.BibleDatabase
import br.com.irse.verse.core.BibleParser
import br.com.irse.verse.core.BibleRepository
import br.com.irse.verse.core.BookMetaData
import br.com.irse.verse.core.VerseRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import writers.composeapp.generated.resources.Res
import java.io.File
import java.io.FileOutputStream

class VerseOverlayService : LifecycleService(), SavedStateRegistryOwner {

    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var composeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var parser: BibleParser? = null
    private var database: BibleDatabase? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        
        startForegroundService()
        initializeData()
        createOverlay()
    }

    private fun startForegroundService() {
        val channelId = "verse_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Verse Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bereia Verse Ativo")
            .setContentText("Toque no ícone flutuante para ler versículos")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun initializeData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load Mapping
                val mappingBytes = Res.readBytes("files/bible_mapping.json")
                val mappingJson = mappingBytes.decodeToString()
                val mapping = Json.decodeFromString<Map<String, BookMetaData>>(mappingJson)
                val repo = BibleRepository(mapping)
                parser = BibleParser(repo)

                // Load DB
                val dbBytes = Res.readBytes("files/bible.sqlite")
                val tempDbFile = File(cacheDir, "bible.sqlite")
                if (!tempDbFile.exists()) {
                    FileOutputStream(tempDbFile).use { it.write(dbBytes) }
                }
                database = BibleDatabase(tempDbFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createOverlay() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VerseOverlayService)
            setViewTreeSavedStateRegistryOwner(this@VerseOverlayService)
            
            val viewModelStore = ViewModelStore()
            val viewModelStoreOwner = object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = viewModelStore
            }
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)

            setContent {
                OverlayUI(
                    onClose = { 
                        // Minimize logic handled by state, or stop service?
                        // For closing entirely: stopSelf()
                        // For minimizing: handled in OverlayUI state
                         stopSelf()
                    },
                    onCheckClipboard = { checkForVerses() }
                )
            }
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        windowManager.addView(composeView, params)
    }

    private fun checkForVerses(): List<Pair<VerseRequest, String?>> {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text.toString()
            if (parser != null && database != null) {
                val requests = parser!!.processSelection(text)
                return requests.map { req ->
                    val content = database!!.getText(req.id)
                    req to content
                }
            }
        }
        return emptyList()
    }

    @Composable
    fun OverlayUI(onClose: () -> Unit, onCheckClipboard: () -> List<Pair<VerseRequest, String?>>) {
        var isExpanded by remember { mutableStateOf(false) }
        var detectedVerses by remember { mutableStateOf<List<Pair<VerseRequest, String?>>>(emptyList()) }
        
        LaunchedEffect(isExpanded) {
            if (params != null && composeView != null) {
                if (isExpanded) {
                    params?.flags = params!!.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                    params?.width = WindowManager.LayoutParams.MATCH_PARENT
                    params?.height = WindowManager.LayoutParams.WRAP_CONTENT
                } else {
                    params?.flags = params!!.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    params?.width = WindowManager.LayoutParams.WRAP_CONTENT
                    params?.height = WindowManager.LayoutParams.WRAP_CONTENT
                }
                windowManager.updateViewLayout(composeView, params)
            }
        }

        if (isExpanded) {
            AndroidOverlayExpanded(
                detectedVerses = detectedVerses,
                onClose = { isExpanded = false },
                onDismiss = { isExpanded = false }
            )
        } else {
            AndroidMiniBubble(
                onClick = {
                    val verses = onCheckClipboard()
                    if (verses.isNotEmpty()) {
                        detectedVerses = verses
                        isExpanded = true
                    } else {
                        // Optional: Show empty state or just expand
                        detectedVerses = emptyList()
                        isExpanded = true 
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (composeView != null) windowManager.removeView(composeView)
    }
}