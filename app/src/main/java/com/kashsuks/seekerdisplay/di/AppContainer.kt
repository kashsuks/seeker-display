package com.kashsuks.seekerdisplay.di

import android.content.Context
import com.kashsuks.seekerdisplay.data.settings.SettingsRepository

/** Hand-rolled dependency container, created once by [com.kashsuks.seekerdisplay.SeekerApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settings = SettingsRepository(appContext)
}
