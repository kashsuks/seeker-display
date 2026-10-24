package com.kashsuks.seekerdisplay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kashsuks.seekerdisplay.data.weather.WeatherData
import com.kashsuks.seekerdisplay.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(private val container: AppContainer) : ViewModel() {
    val weather: StateFlow<WeatherData?> = container.weather.weather
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Call after runtime permissions change so sources can retry straight away. */
    fun onPermissionsChanged() {
        container.weather.refreshNow()
    }
}
