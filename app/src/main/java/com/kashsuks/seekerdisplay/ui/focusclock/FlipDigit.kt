package com.kashsuks.seekerdisplay.ui.focusclock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kashsuks.seekerdisplay.ui.theme.DashColors
import com.kashsuks.seekerdisplay.ui.theme.DisplayFont
import androidx.compose.ui.text.font.FontWeight

private val CardColor = Color(0xFF1C1C1E)
private const val FirstHalfMs = 240
private const val SecondHalfMs = 360

/** Which way the flaps tilt. If the perspective looks inverted on a device, flip this to -1f. */
private const val FlipDirection = -1f

/**
 * A single split-flap digit. When [digit] changes the old top half folds down over the hinge,
 * then the new bottom half falls into place.
 */
@Composable
fun FlipDigit(
    digit: Char,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    var current by remember { mutableStateOf(digit) }
    var previous by remember { mutableStateOf(digit) }
    // 0 = old digit fully showing, 0.5 = flap edge-on at the hinge, 1 = new digit fully showing.
    val progress = remember { Animatable(1f) }

    LaunchedEffect(digit) {
        if (digit != current) {
            previous = current
            current = digit
            progress.snapTo(0f)
            progress.animateTo(0.5f, tween(FirstHalfMs, easing = FastOutLinearInEasing))
            progress.animateTo(1f, tween(SecondHalfMs, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
        }
    }

    val density = LocalDensity.current
    val fontSize = with(density) { (height.toPx() * 0.82f).toSp() }
    val style = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        color = DashColors.Time,
        textAlign = TextAlign.Center,
    )
    val corner = height * 0.07f
    val p = progress.value
    val animating = p < 1f

    Box(modifier.size(width, height)) {
        // New top half sits underneath the folding flap.
        FlapHalf(current, HalfSide.Top, width, height, style, corner, Modifier.align(Alignment.TopCenter))
        // Old bottom half stays until the new flap lands on it.
        FlapHalf(if (animating) previous else current, HalfSide.Bottom, width, height, style, corner, Modifier.align(Alignment.BottomCenter))

        if (animating && p < 0.5f) {
            val angle = (p / 0.5f) * 90f
            FlapHalf(
                previous, HalfSide.Top, width, height, style, corner,
                Modifier
                    .align(Alignment.TopCenter)
                    .flap(rotation = FlipDirection * angle, origin = TransformOrigin(0.5f, 1f), shade = angle / 90f),
            )
        } else if (animating) {
            val angle = ((1f - p) / 0.5f) * 90f
            FlapHalf(
                current, HalfSide.Bottom, width, height, style, corner,
                Modifier
                    .align(Alignment.BottomCenter)
                    .flap(rotation = -FlipDirection * angle, origin = TransformOrigin(0.5f, 0f), shade = angle / 90f),
            )
        }

        // Hinge line.
        Box(Modifier.align(Alignment.Center).size(width, 2.dp).background(Color.Black))
    }
}

private enum class HalfSide { Top, Bottom }

private fun Modifier.flap(rotation: Float, origin: TransformOrigin, shade: Float): Modifier =
    graphicsLayer {
        rotationX = rotation
        transformOrigin = origin
        cameraDistance = 16f * density
    }.drawWithContent {
        drawContent()
        drawRect(Color.Black.copy(alpha = (shade * 0.65f).coerceIn(0f, 1f)))
    }

@Composable
private fun FlapHalf(
    digit: Char,
    half: HalfSide,
    width: Dp,
    height: Dp,
    style: TextStyle,
    corner: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = when (half) {
        HalfSide.Top -> RoundedCornerShape(topStart = corner, topEnd = corner)
        HalfSide.Bottom -> RoundedCornerShape(bottomStart = corner, bottomEnd = corner)
    }
    Box(
        modifier
            .size(width, height / 2)
            .clip(shape)
            .background(CardColor)
            .clipToBounds(),
    ) {
        // Measure the digit at full card size, but report only half the height and shift it so
        // that the top or bottom half is what shows. (An oversized child would be centred instead.)
        Box(
            Modifier.layout { measurable, _ ->
                val fullWidth = width.roundToPx()
                val fullHeight = height.roundToPx()
                val placeable = measurable.measure(Constraints.fixed(fullWidth, fullHeight))
                layout(fullWidth, fullHeight / 2) {
                    placeable.place(0, if (half == HalfSide.Top) 0 else -(fullHeight / 2))
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Text(digit.toString(), style = style)
        }
    }
}
