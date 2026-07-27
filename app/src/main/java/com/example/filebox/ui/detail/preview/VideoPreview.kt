package com.example.filebox.ui.detail.preview

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.filebox.R
import java.io.File
import kotlinx.coroutines.delay

private const val SEEK_STEP_MS = 10_000L
private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private val RESIZE_MODES = listOf(
    AspectRatioFrameLayout.RESIZE_MODE_FIT,
    AspectRatioFrameLayout.RESIZE_MODE_FILL,
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
)

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Suppress("DEPRECATION")
private fun Context.realDisplaySize(): Pair<Int, Int>? {
    val wm = getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager ?: return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = wm.maximumWindowMetrics.bounds
        bounds.width() to bounds.height()
    } else {
        val point = android.graphics.Point()
        wm.defaultDisplay.getRealSize(point)
        point.x to point.y
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPreview(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            prepare()
            playWhenReady = false
        }
    }

    var looping by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(player.volume) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var fullscreen by remember { mutableStateOf(false) }

    DisposableEffect(looping) {
        player.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        onDispose { }
    }
    DisposableEffect(volume) {
        player.volume = volume
        onDispose { }
    }
    DisposableEffect(speed) {
        player.setPlaybackSpeed(speed)
        onDispose { }
    }
    DisposableEffect(file) {
        onDispose { player.release() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        if (!fullscreen) {
            VideoSurface(
                player = player,
                resizeMode = resizeMode,
                looping = looping,
                onToggleLoop = { looping = !looping },
                volume = volume,
                onVolumeChange = { volume = it },
                speed = speed,
                onSpeedChange = { speed = it },
                onCycleResize = {
                    val idx = RESIZE_MODES.indexOf(resizeMode)
                    resizeMode = RESIZE_MODES[(idx + 1) % RESIZE_MODES.size]
                },
                fullscreen = false,
                onToggleFullscreen = { fullscreen = true },
                enableBrightnessGesture = false,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (fullscreen) {
        FullscreenVideo(
            player = player,
            resizeMode = resizeMode,
            looping = looping,
            onToggleLoop = { looping = !looping },
            volume = volume,
            onVolumeChange = { volume = it },
            speed = speed,
            onSpeedChange = { speed = it },
            onCycleResize = {
                val idx = RESIZE_MODES.indexOf(resizeMode)
                resizeMode = RESIZE_MODES[(idx + 1) % RESIZE_MODES.size]
            },
            onExit = { fullscreen = false }
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FullscreenVideo(
    player: ExoPlayer,
    resizeMode: Int,
    looping: Boolean,
    onToggleLoop: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onCycleResize: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(Unit) {
        val original = activity?.requestedOrientation
        val current = player.videoSize
        val initial = when {
            current.width > 0 && current.height > 0 && current.width >= current.height ->
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            current.width > 0 && current.height > 0 ->
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> null
        }
        if (initial != null) activity?.requestedOrientation = initial
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width <= 0 || videoSize.height <= 0) return
                activity?.requestedOrientation = if (videoSize.width >= videoSize.height) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            activity?.requestedOrientation =
                original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        val configuration = LocalConfiguration.current
        LaunchedEffect(configuration.orientation) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window ?: return@LaunchedEffect
            fun applyLayout() {
                val size = context.realDisplaySize()
                if (size != null) {
                    window.setLayout(size.first, size.second)
                } else {
                    window.setLayout(MATCH_PARENT, MATCH_PARENT)
                }
            }
            applyLayout()
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))
            window.statusBarColor = AndroidColor.TRANSPARENT
            window.navigationBarColor = AndroidColor.TRANSPARENT
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        } else {
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                }
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, dialogView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            @Suppress("DEPRECATION")
            dialogView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            repeat(5) {
                delay(150)
                applyLayout()
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            val controller = window?.let { WindowInsetsControllerCompat(it, dialogView) }
            fun applyLayout() {
                if (window == null) return
                val size = context.realDisplaySize()
                if (size != null) {
                    window.setLayout(size.first, size.second)
                } else {
                    window.setLayout(MATCH_PARENT, MATCH_PARENT)
                }
            }
            fun reHide() {
                applyLayout()
                controller?.hide(WindowInsetsCompat.Type.systemBars())
                controller?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                @Suppress("DEPRECATION")
                dialogView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
            }
            ViewCompat.setOnApplyWindowInsetsListener(dialogView) { _, insets ->
                if (insets.isVisible(WindowInsetsCompat.Type.systemBars())) reHide()
                WindowInsetsCompat.Builder().build()
            }
            val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                if (hasFocus) reHide()
            }
            dialogView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
            val layoutListener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
                val size = context.realDisplaySize() ?: return@OnGlobalLayoutListener
                if (dialogView.width < size.first || dialogView.height < size.second) {
                    reHide()
                }
            }
            dialogView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
            onDispose {
                ViewCompat.setOnApplyWindowInsetsListener(dialogView, null)
                dialogView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
                dialogView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            }
        }

        VideoSurface(
            player = player,
            resizeMode = resizeMode,
            looping = looping,
            onToggleLoop = onToggleLoop,
            volume = volume,
            onVolumeChange = onVolumeChange,
            speed = speed,
            onSpeedChange = onSpeedChange,
            onCycleResize = onCycleResize,
            fullscreen = true,
            onToggleFullscreen = onExit,
            enableBrightnessGesture = true,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(
    player: ExoPlayer,
    resizeMode: Int,
    looping: Boolean,
    onToggleLoop: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onCycleResize: () -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    enableBrightnessGesture: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var playbackState by remember { mutableIntStateOf(player.playbackState) }
    var hasError by remember { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }

    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubTarget by remember { mutableLongStateOf(0L) }
    var hud by remember { mutableStateOf<String?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) duration = player.duration.coerceAtLeast(0L)
            }
            override fun onPlayerError(error: PlaybackException) { hasError = true }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(scrubbing) {
        while (!scrubbing) {
            position = player.currentPosition.coerceAtLeast(0L)
            if (duration <= 0L) duration = player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, locked) {
        if (controlsVisible && isPlaying && !locked) {
            delay(3500)
            controlsVisible = false
        }
    }

    LaunchedEffect(hud) {
        if (hud != null) {
            delay(700)
            hud = null
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    this.resizeMode = resizeMode
                    ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ ->
                        WindowInsetsCompat.Builder().build()
                    }
                }
            },
            update = {
                it.player = player
                it.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(locked) {
                    if (locked) {
                        detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                    } else {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                val target = if (offset.x < size.width / 2) {
                                    (player.currentPosition - SEEK_STEP_MS).coerceAtLeast(0L)
                                } else {
                                    (player.currentPosition + SEEK_STEP_MS)
                                        .coerceAtMost(player.duration.coerceAtLeast(0L))
                                }
                                player.seekTo(target)
                                position = target
                            }
                        )
                    }
                }
                .pointerInput(locked, enableBrightnessGesture) {
                    if (locked) return@pointerInput
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val leftHalf = change.position.x < size.width / 2
                        val delta = -dragAmount / size.height * 1.5f
                        if (leftHalf && enableBrightnessGesture && activity != null) {
                            val lp = activity.window.attributes
                            val current = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                            val next = (current + delta).coerceIn(0.01f, 1f)
                            lp.screenBrightness = next
                            activity.window.attributes = lp
                            hud = "${context.getString(R.string.video_brightness)} ${(next * 100).toInt()}%"
                        } else {
                            val next = (volume + delta).coerceIn(0f, 1f)
                            onVolumeChange(next)
                            hud = "${context.getString(R.string.video_volume)} ${(next * 100).toInt()}%"
                        }
                    }
                }
        )

        if (playbackState == Player.STATE_BUFFERING && !hasError) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (hasError) {
            ErrorOverlay(
                onRetry = {
                    hasError = false
                    player.prepare()
                    player.play()
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        hud?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(it, color = Color.White, fontSize = 14.sp)
            }
        }

        if (locked) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                IconButton(
                    onClick = { locked = false },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.video_unlock),
                        tint = Color.White
                    )
                }
            }
        } else {
            AnimatedVisibility(
                visible = controlsVisible && !hasError,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                ControlsScrim(
                    isPlaying = isPlaying,
                    ended = playbackState == Player.STATE_ENDED,
                    onPlayPause = {
                        if (playbackState == Player.STATE_ENDED) {
                            player.seekTo(0L)
                            player.play()
                        } else if (isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                        controlsVisible = true
                    },
                    duration = duration,
                    position = if (scrubbing) scrubTarget else position,
                    onScrubStart = { scrubbing = true },
                    onScrub = { scrubTarget = it },
                    onScrubEnd = {
                        player.seekTo(scrubTarget)
                        position = scrubTarget
                        scrubbing = false
                    },
                    volume = volume,
                    onVolumeChange = onVolumeChange,
                    looping = looping,
                    onToggleLoop = onToggleLoop,
                    speed = speed,
                    onSpeedChange = onSpeedChange,
                    onCycleResize = onCycleResize,
                    fullscreen = fullscreen,
                    onToggleFullscreen = onToggleFullscreen,
                    onLock = { locked = true; controlsVisible = true },
                    onRotate = {
                        activity?.let { act ->
                            act.requestedOrientation =
                                if (act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ErrorOverlay(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.video_error), color = Color.White)
        IconButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.video_retry), tint = Color.White)
        }
    }
}

@Composable
private fun ControlsScrim(
    isPlaying: Boolean,
    ended: Boolean,
    onPlayPause: () -> Unit,
    duration: Long,
    position: Long,
    onScrubStart: () -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    looping: Boolean,
    onToggleLoop: () -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onCycleResize: () -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onLock: () -> Unit,
    onRotate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpeedMenu(speed = speed, onSpeedChange = onSpeedChange)
            IconButton(onClick = onCycleResize) {
                Icon(Icons.Filled.AspectRatio, contentDescription = stringResource(R.string.video_resize), tint = Color.White)
            }
            IconButton(onClick = onLock) {
                Icon(Icons.Filled.LockOpen, contentDescription = stringResource(R.string.video_lock), tint = Color.White)
            }
        }

        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
        ) {
            Icon(
                imageVector = when {
                    ended -> Icons.Filled.Replay
                    isPlaying -> Icons.Filled.Pause
                    else -> Icons.Filled.PlayArrow
                },
                contentDescription = stringResource(
                    when {
                        ended -> R.string.video_replay
                        isPlaying -> R.string.video_pause
                        else -> R.string.video_play
                    }
                ),
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(position), color = Color.White, fontSize = 12.sp)
                Slider(
                    value = position.toFloat(),
                    onValueChange = {
                        onScrubStart()
                        onScrub(it.toLong())
                    },
                    onValueChangeFinished = onScrubEnd,
                    valueRange = 0f..(duration.coerceAtLeast(1L)).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Text(formatTime(duration), color = Color.White, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleLoop) {
                    Icon(
                        if (looping) Icons.Filled.RepeatOn else Icons.Filled.Repeat,
                        contentDescription = stringResource(if (looping) R.string.video_loop_on else R.string.video_loop_off),
                        tint = if (looping) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                if (fullscreen) {
                    Icon(
                        if (volume <= 0f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.video_volume),
                        tint = Color.White
                    )
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White
                        ),
                        modifier = Modifier
                            .width(120.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (fullscreen) {
                    IconButton(onClick = onRotate) {
                        Icon(Icons.Filled.ScreenRotation, contentDescription = stringResource(R.string.video_orientation_landscape), tint = Color.White)
                    }
                }
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = stringResource(
                            if (fullscreen) R.string.video_fullscreen_exit else R.string.video_fullscreen_enter
                        ),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedMenu(speed: Float, onSpeedChange: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Speed, contentDescription = stringResource(R.string.video_speed), tint = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SPEED_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.video_speed_value, option.toString()),
                            fontWeight = if (option == speed) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSpeedChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
