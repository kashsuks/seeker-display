package com.kashsuks.seekerdisplay.ui.focusclock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kashsuks.seekerdisplay.core.TimeFormat
import com.kashsuks.seekerdisplay.core.burnInShift
import com.kashsuks.seekerdisplay.ui.theme.DashColors
import com.kashsuks.seekerdisplay.ui.theme.DashText
import java.time.ZonedDateTime

/** Fullscreen split-flap clock. Tap anywhere to go back. */
@Composable
fun FocusClockScreen(
    now: ZonedDateTime,
    showSeconds: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(DashColors.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val groups = if (showSeconds) 3 else 2
        val digitCount = groups * 2
        // Card width is 0.62 of height; gaps are 4% of height inside a pair, 18% between groups.
        val widthUnits = digitCount * 0.62f + groups * 0.04f + (groups - 1) * 0.18f
        val cardHeight: Dp = minOf(maxHeight * 0.62f, maxWidth * 0.94f / widthUnits)
        val cardWidth = cardHeight * 0.62f

        Column(Modifier.burnInShift(), horizontalAlignment = Alignment.CenterHorizontally) {
            val digits = buildString {
                append("%02d".format(now.hour))
                append("%02d".format(now.minute))
                if (showSeconds) append("%02d".format(now.second))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(cardHeight * 0.18f)) {
                digits.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(cardHeight * 0.04f)) {
                        pair.forEach { FlipDigit(it, cardWidth, cardHeight) }
                    }
                }
            }
            Text(
                text = "${TimeFormat.weekday(now)}  ·  ${TimeFormat.date(now)}",
                style = DashText.Date,
                color = DashColors.Secondary,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}
