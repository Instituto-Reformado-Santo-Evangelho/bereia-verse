package br.com.irse.verse

import android.app.Application
import android.content.Context

class VerseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    companion object {
        lateinit var appContext: Context
    }
}
