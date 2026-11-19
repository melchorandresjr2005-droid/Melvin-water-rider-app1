package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WaterRiderTheme
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun BigCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        content = content
    )
}

@Composable
fun WaterDropletRiderBadge(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BadgePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BadgeScale"
    )

    val themeColors = WaterRiderTheme.colors
    val badgeIconColor = if (themeColors.primaryAccent.luminance() > 0.45f) {
        Color(0xFF090D16)
    } else {
        Color.White
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(themeColors.primaryAccent, themeColors.secondaryAccent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.WaterDrop,
            contentDescription = "Water Droplet",
            tint = badgeIconColor,
            modifier = Modifier
                .size(size * 0.62f)
                .offset(x = (-2).dp, y = (-1).dp)
        )
        Icon(
            imageVector = Icons.Default.TwoWheeler,
            contentDescription = "Delivery Rider Badge",
            tint = badgeIconColor.copy(alpha = 0.9f),
            modifier = Modifier
                .size(size * 0.42f)
                .align(Alignment.BottomEnd)
                .padding(end = 3.dp, bottom = 3.dp)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedRollingAmount(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(animationSpec = tween(260)) { height -> height / 2 } + fadeIn(tween(220)))
                .togetherWith(
                    slideOutVertically(animationSpec = tween(220)) { height -> -height / 2 } + fadeOut(tween(180))
                )
        },
        modifier = modifier,
        label = "RollingNumberCounter"
    ) { targetText ->
        Text(
            text = targetText,
            style = style,
            color = color,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun SpringBounceStepperButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    buttonSize: Dp = 48.dp,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "StepperBounce"
    )

    FilledTonalIconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = Modifier
            .size(buttonSize)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(buttonSize * 0.5f)
        )
    }
}

@Composable
fun SpringBounceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ),
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PrimaryButtonBounce"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        colors = colors,
        shape = shape,
        modifier = modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        content = content
    )
}

@Composable
fun SpringPriceChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 0.98f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PriceChipScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180),
        label = "PriceChipBg"
    )
    val selectedTextColor = if (accentColor.luminance() > 0.45f) Color(0xFF090D16) else Color.White
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedTextColor else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(180),
        label = "PriceChipText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else accentColor.copy(alpha = 0.55f),
        animationSpec = tween(180),
        label = "PriceChipBorder"
    )

    androidx.compose.material3.Surface(
        color = bgColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, borderColor),
        shadowElevation = if (selected) 4.dp else 0.dp,
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun WaterWaveBackground(
    modifier: Modifier = Modifier,
    waveColorPrimary: Color = WaterRiderTheme.colors.primaryAccent.copy(alpha = 0.12f),
    waveColorSecondary: Color = WaterRiderTheme.colors.secondaryAccent.copy(alpha = 0.12f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaterWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val baseY = height * 0.65f
        val amplitude = height * 0.12f

        val path1 = Path().apply {
            moveTo(0f, height)
            lineTo(0f, baseY)
            var x = 0f
            while (x <= width) {
                val y = baseY + amplitude * sin((x / width) * 2 * PI + phase).toFloat()
                lineTo(x, y)
                x += 12f
            }
            lineTo(width, height)
            close()
        }

        val path2 = Path().apply {
            moveTo(0f, height)
            lineTo(0f, baseY + 10f)
            var x = 0f
            while (x <= width) {
                val y = (baseY + 8f) + (amplitude * 0.8f) * sin((x / width) * 2.5 * PI - phase).toFloat()
                lineTo(x, y)
                x += 12f
            }
            lineTo(width, height)
            close()
        }

        drawPath(path = path1, color = waveColorPrimary)
        drawPath(path = path2, color = waveColorSecondary)
    }
}
