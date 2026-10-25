package com.kashsuks.seekerdisplay.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kashsuks.seekerdisplay.core.NightWindow
import com.kashsuks.seekerdisplay.data.calendar.DeviceCalendar
import com.kashsuks.seekerdisplay.data.settings.DisplaySettings
import com.kashsuks.seekerdisplay.ui.theme.DashColors
import com.kashsuks.seekerdisplay.ui.theme.DashText

data class SettingsState(
    val locationGranted: Boolean,
    val calendarGranted: Boolean,
    val notificationAccessGranted: Boolean,
    val calendars: List<DeviceCalendar>,
    val hiddenCalendarIds: Set<Long>,
    val bridgeAddress: String,
    val bridgeToken: String,
    val bridgeTestResult: String?,
    val display: DisplaySettings,
)

class SettingsActions(
    val onGrantLocation: () -> Unit,
    val onGrantCalendar: () -> Unit,
    val onGrantNotificationAccess: () -> Unit,
    val onToggleCalendar: (calendarId: Long, visible: Boolean) -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onBridgeAddressChange: (String) -> Unit,
    val onBridgeTokenChange: (String) -> Unit,
    val onTestBridge: () -> Unit,
    val onShowSecondsChange: (Boolean) -> Unit,
    val onNightDimChange: (Boolean) -> Unit,
    val onNightWindowChange: (startMinute: Int, endMinute: Int) -> Unit,
)

@Composable
fun SettingsScreen(
    state: SettingsState,
    actions: SettingsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(DashColors.Black).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                Row(Modifier.padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = DashColors.Primary)
                    }
                    Text("Settings", style = DashText.EventHero, color = DashColors.Primary)
                }
            }

            item { SectionHeader("DISPLAY") }
            item { SwitchRow("Show seconds on the focus clock", state.display.showSeconds, actions.onShowSecondsChange) }
            item { SwitchRow("Dim the screen at night", state.display.nightDimEnabled, actions.onNightDimChange) }
            if (state.display.nightDimEnabled) {
                item {
                    HourStepper("Dim from", state.display.nightStartMinute) { actions.onNightWindowChange(it, state.display.nightEndMinute) }
                }
                item {
                    HourStepper("Back to normal at", state.display.nightEndMinute) { actions.onNightWindowChange(state.display.nightStartMinute, it) }
                }
            }

            item { SectionHeader("PERMISSIONS") }
            item { PermissionRow("Location", "Local weather", state.locationGranted, actions.onGrantLocation) }
            item { PermissionRow("Calendar", "Up Next events", state.calendarGranted, actions.onGrantCalendar) }
            item {
                PermissionRow(
                    "Notification access",
                    "Lets the app see and control Spotify on this phone",
                    state.notificationAccessGranted,
                    actions.onGrantNotificationAccess,
                )
            }
            item {
                TextButton(onClick = actions.onOpenAppSettings) {
                    Text("Open app settings", style = DashText.EventSub, color = DashColors.Blue)
                }
            }

            item { SectionHeader("MAC BRIDGE (SPOTIFY ON YOUR LAPTOP)") }
            item {
                Hint("Run companion/spotify_bridge.py on your Mac, then paste its access token here.")
            }
            item {
                SettingsField("Token", state.bridgeToken, actions.onBridgeTokenChange, placeholder = "From the bridge's output")
            }
            item {
                SettingsField("Address (optional)", state.bridgeAddress, actions.onBridgeAddressChange, placeholder = "Leave empty to auto-discover, or 192.168.1.5:8765")
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = actions.onTestBridge) { Text("Test connection", style = DashText.EventRow, color = DashColors.Blue) }
                    state.bridgeTestResult?.let { Text(it, style = DashText.EventSub, color = DashColors.Secondary, modifier = Modifier.padding(start = 8.dp)) }
                }
            }

            item { SectionHeader("CALENDARS") }
            if (!state.calendarGranted) {
                item { Hint("Grant calendar access to choose which calendars appear.") }
            } else if (state.calendars.isEmpty()) {
                item { Hint("No calendars found. Add a Google account in Android Settings and turn on Calendar sync.") }
            } else {
                items(state.calendars, key = { it.id }) { calendar ->
                    val visible = calendar.id !in state.hiddenCalendarIds
                    CalendarRow(calendar, visible) { actions.onToggleCalendar(calendar.id, it) }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = DashText.EventRow, color = DashColors.Primary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun HourStepper(title: String, minute: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = DashText.EventRow, color = DashColors.Secondary, modifier = Modifier.weight(1f))
        TextButton(onClick = { onChange(NightWindow.shiftHours(minute, -1)) }) { Text("−", style = DashText.EventHero, color = DashColors.Blue) }
        Text(NightWindow.format(minute), style = DashText.EventRow, color = DashColors.Primary)
        TextButton(onClick = { onChange(NightWindow.shiftHours(minute, 1)) }) { Text("+", style = DashText.EventHero, color = DashColors.Blue) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = DashText.City, color = DashColors.Red, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, style = DashText.EventSub, color = DashColors.Secondary, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun PermissionRow(title: String, subtitle: String, granted: Boolean, onGrant: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DashText.EventRow, color = DashColors.Primary)
            Text(subtitle, style = DashText.Small, color = DashColors.Secondary)
        }
        if (granted) {
            Text("Granted", style = DashText.EventSub, color = DashColors.Secondary)
        } else {
            TextButton(onClick = onGrant) { Text("Grant", style = DashText.EventRow, color = DashColors.Blue) }
        }
    }
}

@Composable
private fun CalendarRow(calendar: DeviceCalendar, visible: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle(!visible) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(calendar.color)))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(calendar.name, style = DashText.EventRow, color = DashColors.Primary)
            Text(calendar.account, style = DashText.Small, color = DashColors.Secondary)
        }
        Switch(checked = visible, onCheckedChange = onToggle)
    }
}

/**
 * Keeps its own text so typing never waits on storage. The stored value is only adopted until the
 * user starts typing, which covers the first load from DataStore.
 */
@Composable
private fun SettingsField(label: String, value: String, onChange: (String) -> Unit, placeholder: String) {
    var text by remember { mutableStateOf(value) }
    var edited by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!edited) text = value
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            edited = true
            text = it
            onChange(it)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}
