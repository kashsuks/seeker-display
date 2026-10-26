package com.kashsuks.seekerdisplay

import android.app.Application
import com.kashsuks.seekerdisplay.di.AppContainer

class SeekerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
