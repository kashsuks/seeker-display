package com.kashsuks.seekerdisplay.core

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Nudges content by a few dp in a random direction every few minutes, so an always-on OLED
 * doesn't wear the same pixels. The move is slow enough to be unnoticeable.
 */
@Composable
fun Modifier.burnInShift(maxShift: Dp = 6.dp, intervalMs: Long = 3 * 60_000L): Modifier {
    val maxPx = with(LocalDensity.current) { maxShift.roundToPx() }
    var targetX by remember { mutableIntStateOf(0) }
    var targetY by remember { mutableIntStateOf(0) }
    LaunchedEffect(maxPx, intervalMs) {
        while (true) {
            delay(intervalMs)
            targetX = Random.nextInt(-maxPx, maxPx + 1)
            targetY = Random.nextInt(-maxPx, maxPx + 1)
        }
    }
    val x by animateIntAsState(targetX, tween(2_000), label = "burnInX")
    val y by animateIntAsState(targetY, tween(2_000), label = "burnInY")
    return this.offset { IntOffset(x, y) }
}
