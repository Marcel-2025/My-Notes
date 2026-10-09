package dev.flondy.mynotes.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.flondy.mynotes.R

/**
 * Alternative App-Icons über <activity-alias> (siehe AndroidManifest.xml).
 *
 * Der Standard-Alias heißt weiterhin `dev.flondy.mynotes.MainActivity`, damit bestehende
 * Startbildschirm-Verknüpfungen nach dem Update erhalten bleiben.
 * Es ist immer genau ein Launcher-Alias aktiv → keine doppelten Icons.
 */
enum class AppIcon(
    val aliasClass: String,
    @StringRes val labelRes: Int,
    @DrawableRes val previewRes: Int,
) {
    DEFAULT("dev.flondy.mynotes.MainActivity", R.string.icon_default, R.mipmap.ic_launcher),
    DARK("dev.flondy.mynotes.IconDark", R.string.icon_dark, R.mipmap.ic_launcher_dark),
    MINIMAL("dev.flondy.mynotes.IconMinimal", R.string.icon_minimal, R.mipmap.ic_launcher_minimal),
    NEON("dev.flondy.mynotes.IconNeon", R.string.icon_neon, R.mipmap.ic_launcher_neon),
    GOLD("dev.flondy.mynotes.IconGold", R.string.icon_gold, R.mipmap.ic_launcher_gold);

    companion object {
        fun fromName(name: String?): AppIcon = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

object AppIconManager {
    private const val TAG = "AppIconManager"

    /** Alternative Icons nutzen Adaptive Icons (Android 8+). */
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    fun currentIcon(context: Context): AppIcon {
        val pm = context.packageManager
        return AppIcon.entries.firstOrNull { icon ->
            val state = pm.getComponentEnabledSetting(ComponentName(context.packageName, icon.aliasClass))
            when (state) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.DEFAULT
                else -> false
            }
        } ?: AppIcon.DEFAULT
    }

    /**
     * Wechselt den aktiven Launcher-Alias. Zuerst wird das neue Icon aktiviert, danach werden
     * die übrigen deaktiviert – so existiert zu keinem Zeitpunkt kein Launcher-Eintrag.
     * Wird beim Verlassen der App (onStop) aufgerufen, weil manche Launcher die App beim Wechsel schließen.
     */
    fun apply(context: Context, target: AppIcon): Boolean {
        if (!isSupported) return false
        if (currentIcon(context) == target) return false
        return try {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(
                ComponentName(context.packageName, target.aliasClass),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            AppIcon.entries.filter { it != target }.forEach { other ->
                pm.setComponentEnabledSetting(
                    ComponentName(context.packageName, other.aliasClass),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Icon-Wechsel fehlgeschlagen", e)
            false
        }
    }
}
