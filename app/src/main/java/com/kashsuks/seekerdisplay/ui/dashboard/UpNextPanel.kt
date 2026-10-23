package com.kashsuks.seekerdisplay.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kashsuks.seekerdisplay.data.calendar.CalendarEvent
import com.kashsuks.seekerdisplay.data.calendar.EventLabels
import com.kashsuks.seekerdisplay.ui.theme.DashColors
import com.kashsuks.seekerdisplay.ui.theme.DashText
import java.time.ZonedDateTime

private const val MAX_COMPACT_ROWS = 4

@Composable
fun UpNextPanel(
    events: List<CalendarEvent>,
    now: ZonedDateTime,
    hasPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text("UP NEXT", style = DashText.City, color = DashColors.Red)
        Spacer(Modifier.height(8.dp))
        when {
            !hasPermission -> Text("Calendar access needed. Long-press for settings.", style = DashText.EventSub, color = DashColors.Secondary)
            events.isEmpty() -> Text("Nothing coming up", style = DashText.EventSub, color = DashColors.Secondary)
            else -> {
                val first = events.first()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorBar(first.color, Modifier.padding(end = 10.dp).width(5.dp).height(46.dp))
                    Column {
                        Text(first.title, style = DashText.EventHero, color = DashColors.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(EventLabels.subtitle(first, now), style = DashText.EventSub, color = DashColors.Secondary)
                    }
                }
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    events.drop(1).take(MAX_COMPACT_ROWS).forEach { event ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ColorBar(event.color, Modifier.padding(end = 10.dp).width(4.dp).height(18.dp))
                            Text(
                                event.title,
                                style = DashText.EventRow,
                                color = DashColors.Primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                EventLabels.dayLabel(event, now),
                                style = DashText.EventRow,
                                color = DashColors.Secondary,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorBar(color: Int, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(Color(color)))
}
