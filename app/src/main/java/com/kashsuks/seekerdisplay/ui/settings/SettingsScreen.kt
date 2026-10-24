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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kashsuks.seekerdisplay.data.calendar.DeviceCalendar
import com.kashsuks.seekerdisplay.ui.theme.DashColors
import com.kashsuks.seekerdisplay.ui.theme.DashText

data class SettingsState(
    val locationGranted: Boolean,
    val calendarGranted: Boolean,
    val notificationAccessGranted: Boolean,
    val calendars: List<DeviceCalendar>,
    val hiddenCalendarIds: Set<Long>,
)

class SettingsActions(
    val onGrantLocation: () -> Unit,
    val onGrantCalendar: () -> Unit,
    val onGrantNotificationAccess: () -> Unit,
    val onToggleCalendar: (calendarId: Long, visible: Boolean) -> Unit,
    val onOpenAppSettings: () -> Unit,
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
