package com.example.filebox.ui.tools

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.DisposableEffect
import com.example.filebox.R

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private val TextColors = listOf(
    Color.White, Color.Red, Color(0xFF00E676), Color.Yellow,
    Color(0xFF29B6F6), Color(0xFFFF4081), Color(0xFFFFA726), Color.Black,
    Color(0xFF00BCD4), Color(0xFF7C4DFF), Color(0xFFEC407A), Color(0xFFCDDC39),
    Color(0xFFFF7043), Color(0xFF9CCC65), Color(0xFFBA68C8), Color(0xFF80D8FF)
)

private val BgColors = listOf(
    Color.Black, Color(0xFF102027), Color(0xFF1A237E), Color(0xFF4A148C),
    Color(0xFFB71C1C), Color(0xFF1B5E20), Color.White,
    Color(0xFF263238), Color(0xFF006064), Color(0xFF3E2723), Color(0xFF880E4F),
    Color(0xFF827717), Color(0xFFE65100), Color(0xFF01579B), Color(0xFF311B92)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarqueeToolScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(TextColors.first()) }
    var bgColor by remember { mutableStateOf(BgColors.first()) }
    var fontSize by remember { mutableFloatStateOf(120f) }
    var speed by remember { mutableFloatStateOf(120f) }
    var running by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.marquee_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.marquee_text_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.marquee_preview), style = MaterialTheme.typography.titleSmall)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = text.ifBlank { stringResource(R.string.marquee_empty_hint) },
                    color = textColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            ColorPickerRow(
                label = stringResource(R.string.marquee_text_color),
                colors = TextColors,
                selected = textColor,
                onSelect = { textColor = it }
            )
            ColorPickerRow(
                label = stringResource(R.string.marquee_bg_color),
                colors = BgColors,
                selected = bgColor,
                onSelect = { bgColor = it }
            )

            SliderRow(
                label = stringResource(R.string.marquee_font_size),
                value = fontSize,
                valueRange = 40f..320f,
                display = "${fontSize.toInt()}sp",
                onChange = { fontSize = it }
            )
            SliderRow(
                label = stringResource(R.string.marquee_speed),
                value = speed,
                valueRange = 40f..400f,
                display = "${speed.toInt()}",
                onChange = { speed = it }
            )

            Button(
                onClick = { running = true },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.marquee_start))
            }
        }
    }

    if (running) {
        MarqueeFullscreen(
            text = text,
            textColor = textColor,
            bgColor = bgColor,
            fontSizeSp = fontSize,
            speed = speed,
            onExit = { running = false }
        )
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    colors: List<Color>,
    selected: Color,
    onSelect: (Color) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            colors.forEach { color ->
                val isSelected = color == selected
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape
                        )
                        .clickable { onSelect(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = if (color.approxLuminance() > 0.5f) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(display, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = valueRange)
    }
}

@Composable
private fun MarqueeFullscreen(
    text: String,
    textColor: Color,
    bgColor: Color,
    fontSizeSp: Float,
    speed: Float,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.requestedOrientation =
                originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

        var textWidthPx by remember { mutableFloatStateOf(0f) }

        val distance = screenWidthPx + textWidthPx
        val durationMs = if (distance > 0f) {
            ((distance / with(density) { 1.dp.toPx() }) / speed * 1000f).toInt().coerceAtLeast(1)
        } else {
            1
        }

        val transition = rememberInfiniteTransition(label = "marquee")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs, easing = LinearEasing)
            ),
            label = "offset"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .clickable(onClick = onExit),
            contentAlignment = Alignment.CenterStart
        ) {
            val offsetX = screenWidthPx - progress * distance
            Text(
                text = text,
                color = textColor,
                fontSize = fontSizeSp.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .wrapContentSize(unbounded = true, align = Alignment.CenterStart)
                    .onSizeChanged { textWidthPx = it.width.toFloat() }
                    .offset { IntOffset(offsetX.toInt(), 0) }
            )

            IconButton(
                onClick = onExit,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.marquee_exit),
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun Color.approxLuminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
