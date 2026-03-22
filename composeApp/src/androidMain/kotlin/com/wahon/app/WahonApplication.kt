package com.wahon.app

import android.app.Application
import com.wahon.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class WahonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@WahonApplication)
        }
    }
}
