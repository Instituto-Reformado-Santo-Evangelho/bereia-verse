package br.com.irse.verse.core

import kotlinx.coroutines.flow.SharedFlow

interface DeepLinkHandler {
    val deepLinkFlow: SharedFlow<String>
    fun handleDeepLink(uri: String)
}
