package dev.flondy.mynotes.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import dev.flondy.mynotes.BuildConfig

/**
 * Adaptiver Anker-Banner. Wird nur einmal pro Breite erzeugt (kein Neuladen bei Recomposition),
 * folgt dem Lifecycle (pause/resume/destroy) und blendet sich bei Ladefehlern sauber aus.
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        var failed by remember(widthDp) { mutableStateOf(false) }
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        val adView = remember(widthDp) {
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = BuildConfig.BANNER_AD_UNIT_ID
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) { failed = true }
                    override fun onAdLoaded() { failed = false }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(adView, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> adView.pause()
                    Lifecycle.Event.ON_RESUME -> adView.resume()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adView.destroy()
            }
        }
        if (!failed) {
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth().height(adSize.height.dp),
            )
        }
    }
}
