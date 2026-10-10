package com.example.filebox.ui.detail.preview

import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.exoplayer.ExoPlayer

data class FullscreenRequest(
    val player: ExoPlayer,
    val looping: MutableState<Boolean>,
    val volume: MutableState<Float>,
    val speed: MutableState<Float>,
    val resizeMode: MutableState<Int>,
    val onExit: () -> Unit
)

class VideoFullscreenController(
    val state: MutableState<FullscreenRequest?>
) {
    fun enter(request: FullscreenRequest) {
        state.value = request
    }

    fun exit() {
        state.value = null
    }
}

val LocalVideoFullscreenController =
    staticCompositionLocalOf<VideoFullscreenController?> { null }

@Composable
fun VideoFullscreenHost(content: @Composable () -> Unit) {
    val requestState = remember { mutableStateOf<FullscreenRequest?>(null) }
    val controller = remember { VideoFullscreenController(requestState) }
    val request = requestState.value

    CompositionLocalProvider(LocalVideoFullscreenController provides controller) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            if (request != null) {
                FullscreenOverlay(request = request)
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun FullscreenOverlay(
    request: FullscreenRequest
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    BackHandler { request.onExit() }

    DisposableEffect(activity) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes = window.attributes.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
            }
        }
        val controller0 = WindowInsetsControllerCompat(window, window.decorView)
        controller0.hide(WindowInsetsCompat.Type.systemBars())
        controller0.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        onDispose {
            controller0.show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        FullscreenPlayerContent(
            player = request.player,
            resizeMode = request.resizeMode.value,
            looping = request.looping.value,
            onToggleLoop = { request.looping.value = !request.looping.value },
            volume = request.volume.value,
            onVolumeChange = { request.volume.value = it },
            speed = request.speed.value,
            onSpeedChange = { request.speed.value = it },
            onCycleResize = {
                val idx = RESIZE_MODES.indexOf(request.resizeMode.value)
                request.resizeMode.value =
                    RESIZE_MODES[(idx + 1) % RESIZE_MODES.size]
            },
            onExit = request.onExit
        )
    }
}
