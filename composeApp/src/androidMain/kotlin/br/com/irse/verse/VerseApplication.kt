package br.com.irse.verse

import android.app.Application
import android.content.Context
import br.com.irse.verse.di.appModule
import br.com.irse.verse.di.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VerseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this

        startKoin {
            androidContext(this@VerseApplication)
            modules(appModule, androidModule)
        }
    }

    companion object {
        lateinit var appContext: Context
    }
}
