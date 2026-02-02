package br.com.irse.verse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.*
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
import br.com.irse.verse.core.*
import org.koin.android.ext.android.get
import org.koin.core.component.KoinComponent

class VerseOverlayService : LifecycleService(), SavedStateRegistryOwner, KoinComponent {

    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    
    private var composeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var viewModel: VerseViewModel
    
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboard(autoExpand = false)
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        
        try {
            viewModel = get()
        } catch (e: Exception) {
            android.util.Log.e("VerseOverlayService", "Falha ao injetar ViewModel", e)
            stopSelf()
            return
        }

        startForegroundService()
        clipboardManager.addPrimaryClipChangedListener(clipListener)
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                viewModel.processQuery(sharedText, isExternal = true)
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "verse_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Monitor", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Bereia Verse")
            .setContentText("Clique na bolha para ler versículos")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun checkClipboard(autoExpand: Boolean = true) {
        // No Android 10, o clipboard exige foco.
        // Já configuramos o LayoutParams no onClick para ganhar foco.
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank()) {
                    android.util.Log.d("VerseOverlayService", "Texto capturado: $text")
                    viewModel.processQuery(text, isExternal = true)
                } else if (autoExpand) {
                    Toast.makeText(this, "Área de transferência vazia", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VerseOverlayService", "Erro ao acessar clipboard", e)
        }
    }

    private fun createOverlay() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VerseOverlayService)
            setViewTreeSavedStateRegistryOwner(this@VerseOverlayService)
            
            val viewModelStore = ViewModelStore()
            val viewModelStoreOwner = object : ViewModelStoreOwner { override val viewModelStore: ViewModelStore = viewModelStore }
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)

            setContent {
                val detectedVerses by viewModel.detectedVerses.collectAsState()
                var isExpanded by remember { mutableStateOf(false) }
                
                LaunchedEffect(detectedVerses) {
                    if (detectedVerses.isNotEmpty()) {
                        isExpanded = true
                    }
                }

                LaunchedEffect(isExpanded) {
                    updateLayoutParams(isExpanded)
                }

                if (isExpanded) {
                    AndroidOverlayExpanded(
                        viewModel = viewModel,
                        onClose = { isExpanded = false }
                    )
                } else {
                    AndroidMiniBubble(
                        onClick = { 
                            // 1. Ganha foco temporário para ler o clipboard
                            requestTemporaryFocus {
                                checkClipboard(autoExpand = true)
                            }
                        }
                    )
                }
            }
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = 300
        }

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            android.util.Log.e("VerseOverlayService", "Erro ao adicionar overlay", e)
        }
    }

    private fun requestTemporaryFocus(onFocused: () -> Unit) {
        val p = params ?: return
        // Remove a flag que impede foco
        p.flags = p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        try {
            windowManager.updateViewLayout(composeView, p)
            // Agora que tem foco, executa a ação (leitura de clipboard)
            onFocused()
            // Retorna ao estado normal (sem foco) após um pequeno delay para a bolha não "travar" o teclado do usuário
            composeView?.postDelayed({
                p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(composeView, p)
            }, 100)
        } catch (e: Exception) {
            onFocused() // Tenta ler mesmo se falhar o foco
        }
    }

    private fun updateLayoutParams(isExpanded: Boolean) {
        val p = params ?: return
        if (isExpanded) {
            p.flags = p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            p.width = WindowManager.LayoutParams.MATCH_PARENT
            p.height = WindowManager.LayoutParams.MATCH_PARENT
        } else {
            p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            p.width = WindowManager.LayoutParams.WRAP_CONTENT
            p.height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        try {
            windowManager.updateViewLayout(composeView, p)
        } catch (e: Exception) {
            android.util.Log.e("VerseOverlayService", "Erro ao atualizar layout", e)
        }
    }

    override fun onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(clipListener)
        if (composeView != null) {
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        super.onDestroy()
    }
}
