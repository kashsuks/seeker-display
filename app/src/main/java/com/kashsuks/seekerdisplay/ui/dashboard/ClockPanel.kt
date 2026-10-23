package com.kashsuks.seekerdisplay.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.kashsuks.seekerdisplay.core.TimeFormat
import com.kashsuks.seekerdisplay.ui.theme.DashColors
import com.kashsuks.seekerdisplay.ui.theme.DashText
import java.time.ZonedDateTime

@Composable
fun ClockPanel(
    now: ZonedDateTime,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(TimeFormat.weekday(now), style = DashText.Weekday, color = DashColors.Red)
        Text(
            text = TimeFormat.time(now),
            style = DashText.Time,
            color = DashColors.Time,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTimeClick,
            ),
        )
        Text(TimeFormat.date(now), style = DashText.Date, color = DashColors.Secondary)
    }
}
