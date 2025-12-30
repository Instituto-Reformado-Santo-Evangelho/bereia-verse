package br.com.irse.verse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
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
import verse.composeapp.generated.resources.Res
import java.io.File
import java.io.FileOutputStream

class VerseOverlayService : LifecycleService(), SavedStateRegistryOwner {

    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var composeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var parser: BibleParser? = null
    private var database: BibleDatabase? = null

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        
        startForegroundService()
        initializeData()
        createOverlay()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun startForegroundService() {
        val channelId = "verse_overlay_channel"
        val channel = NotificationChannel(
            channelId,
            "Verse Overlay Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IRSE | Bereia Verse Ativo")
            .setContentText("Monitorando área de transferência...")
            .setSmallIcon(R.mipmap.ic_launcher)

        startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
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
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
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
                    isExpanded = if (verses.isNotEmpty()) {
                        true
                    } else {
                        // Optional: Show empty state or just expand
                        true
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