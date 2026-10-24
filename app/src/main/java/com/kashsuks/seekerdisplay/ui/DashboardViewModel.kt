package com.kashsuks.seekerdisplay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kashsuks.seekerdisplay.data.calendar.CalendarEvent
import com.kashsuks.seekerdisplay.data.weather.WeatherData
import com.kashsuks.seekerdisplay.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(private val container: AppContainer) : ViewModel() {
    val weather: StateFlow<WeatherData?> = container.weather.weather
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val events: StateFlow<List<CalendarEvent>> = container.calendar.upcoming
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _calendarPermission = MutableStateFlow(container.calendar.hasPermission())
    val calendarPermission: StateFlow<Boolean> = _calendarPermission.asStateFlow()

    /** Call after runtime permissions change so sources can retry straight away. */
    fun onPermissionsChanged() {
        _calendarPermission.value = container.calendar.hasPermission()
        container.weather.refreshNow()
        container.calendar.refreshNow()
    }
}
