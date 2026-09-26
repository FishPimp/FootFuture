package se.storleksprognosen.app.ui.foot

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Skärmens uppgivna täthet längs höjden, i pixlar per millimeter. */
@Composable
fun reportedPxPerMm(): Float =
    LocalContext.current.resources.displayMetrics.ydpi / FootViewModel.MM_PER_INCH

/**
 * Låser porträttläge och håller skärmen tänd medan en mätvy visas.
 * Med [immersive] döljs dessutom system- och navigeringsfälten så att ritytan
 * börjar exakt vid skärmens nederkant.
 */
@Composable
fun MeasurementWindowMode(immersive: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(immersive) {
        val activity = context.findActivity()
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        view.keepScreenOn = true

        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        if (immersive && controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (immersive) controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
            if (activity != null && previousOrientation != null) activity.requestedOrientation = previousOrientation
        }
    }
}
