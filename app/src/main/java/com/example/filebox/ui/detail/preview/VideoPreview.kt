package com.example.filebox.ui.detail.preview

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.filebox.R
import java.io.File

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPreview(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            prepare()
            playWhenReady = false
        }
    }

    var looping by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(player.volume) }
    var fullscreen by remember { mutableStateOf(false) }

    DisposableEffect(looping) {
        player.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        onDispose { }
    }
    DisposableEffect(volume) {
        player.volume = volume
        onDispose { }
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!fullscreen) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                update = { it.player = player },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        VideoControls(
            looping = looping,
            onToggleLoop = { looping = !looping },
            volume = volume,
            onVolumeChange = { volume = it },
            fullscreen = false,
            onToggleFullscreen = { fullscreen = true }
        )
    }

    if (fullscreen) {
        FullscreenVideo(
            player = player,
            looping = looping,
            onToggleLoop = { looping = !looping },
            volume = volume,
            onVolumeChange = { volume = it },
            onExit = { fullscreen = false }
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FullscreenVideo(
    player: ExoPlayer,
    looping: Boolean,
    onToggleLoop: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(Unit) {
        val original = activity?.requestedOrientation
        onDispose {
            activity?.requestedOrientation =
                original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogView = LocalView.current
        val configuration = LocalConfiguration.current
        LaunchedEffect(configuration.orientation) {
            val window = (dialogView.parent as? DialogWindowProvider)?.window ?: return@LaunchedEffect
            window.setLayout(MATCH_PARENT, MATCH_PARENT)
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, dialogView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    activity?.let { act ->
                        act.requestedOrientation =
                            if (act.requestedOrientation ==
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            ) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                    }
                }) {
                    Icon(
                        Icons.Filled.ScreenRotation,
                        contentDescription = stringResource(R.string.video_orientation_landscape),
                        tint = Color.White
                    )
                }
                IconButton(onClick = onToggleLoop) {
                    Icon(
                        if (looping) Icons.Filled.RepeatOn else Icons.Filled.Repeat,
                        contentDescription = stringResource(
                            if (looping) R.string.video_loop_on else R.string.video_loop_off
                        ),
                        tint = Color.White
                    )
                }
                IconButton(onClick = onExit) {
                    Icon(
                        Icons.Filled.FullscreenExit,
                        contentDescription = stringResource(R.string.video_fullscreen_exit),
                        tint = Color.White
                    )
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (volume <= 0f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.video_volume),
                    tint = Color.White
                )
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .width(220.dp)
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun VideoControls(
    looping: Boolean,
    onToggleLoop: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleLoop) {
            Icon(
                if (looping) Icons.Filled.RepeatOn else Icons.Filled.Repeat,
                contentDescription = stringResource(
                    if (looping) R.string.video_loop_on else R.string.video_loop_off
                ),
                tint = if (looping) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Icon(
            if (volume <= 0f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
            contentDescription = stringResource(R.string.video_volume),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        IconButton(onClick = onToggleFullscreen) {
            Icon(
                if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = stringResource(
                    if (fullscreen) R.string.video_fullscreen_exit
                    else R.string.video_fullscreen_enter
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
