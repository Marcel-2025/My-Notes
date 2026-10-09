package dev.flondy.mynotes

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.flondy.mynotes.icons.AppIcon
import dev.flondy.mynotes.icons.AppIconManager
import dev.flondy.mynotes.ui.AppRoot
import dev.flondy.mynotes.ui.theme.MyNotesTheme
import dev.flondy.mynotes.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Einzige Activity der App.
 *
 * Wird über <activity-alias> gestartet – der Standard-Alias heißt weiterhin
 * `dev.flondy.mynotes.MainActivity`, damit Startbildschirm-Verknüpfungen erhalten bleiben.
 */
class NotesActivity : ComponentActivity() {
    private val notesViewModel: NotesViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application.app
        app.billing.start()

        // Werbung erst nach Kaufstatus-Prüfung und Einwilligung – nie für werbefreie Nutzer.
        lifecycleScope.launch {
            val state = settingsViewModel.premium.filter { it.initialized }.first()
            if (!state.adFree) settingsViewModel.startAdsIfNeeded(this@NotesActivity)
        }

        // Vollbildmodus (bestehende Einstellung, jetzt dauerhaft gespeichert)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.settings.map { it.immersiveMode }.distinctUntilChanged().collect { immersive ->
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    if (immersive) {
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        controller.show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
        }

        setContent {
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val dark = isAppInDarkTheme(settings.themeMode)

            // Statusleisten-Icons passend zum App-Theme (nicht zum System-Theme)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }

            MyNotesTheme(
                themeMode = settings.themeMode,
                accentColor = settings.accent,
                animationsEnabled = settings.animationsEnabled,
            ) {
                AppRoot(notesVm = notesViewModel, settingsVm = settingsViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Käufe, die außerhalb der App abgeschlossen wurden (z. B. ausstehende Zahlungen), übernehmen
        application.app.billing.refreshPurchases()
    }

    override fun onStop() {
        super.onStop()
        // Icon-Wechsel erst beim Verlassen anwenden – manche Launcher schließen die App dabei.
        if (!isChangingConfigurations) {
            val desired = AppIcon.fromName(settingsViewModel.settings.value.appIcon)
            AppIconManager.apply(this, desired)
        }
    }
}
