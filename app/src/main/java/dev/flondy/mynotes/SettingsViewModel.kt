package dev.flondy.mynotes

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.flondy.mynotes.billing.BillingManager
import dev.flondy.mynotes.icons.AppIcon
import dev.flondy.mynotes.settings.AccentColor
import dev.flondy.mynotes.settings.AppSettings
import dev.flondy.mynotes.settings.CardDensity
import dev.flondy.mynotes.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Darstellung, Einstellungen und Premium-Status (ersetzt den früheren ThemeViewModel). */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.app
    private val store = app.settings
    private val billing = app.billing
    private val ads = app.ads

    val settings: StateFlow<AppSettings> = store.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Werbefrei = gültiger Kauf bei Google Play (oder Debug-Simulation, nur in Debug-Builds). */
    val premium: StateFlow<BillingManager.State> = combine(billing.state, store.settingsFlow) { state, s ->
        if (BuildConfig.DEBUG && s.debugForceAdFree) state.copy(adFree = true, initialized = true) else state
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BillingManager.State())

    val billingEvents = billing.events
    val adsReady: StateFlow<Boolean> = ads.adsReady
    val privacyOptionsRequired: StateFlow<Boolean> = ads.privacyOptionsRequired

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }

    /** Schnellumschalter in der Titelleiste (bisheriges Verhalten). */
    fun toggleTheme(currentlyDark: Boolean) = setThemeMode(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)

    fun setAccent(accent: AccentColor) = viewModelScope.launch { store.setAccent(accent) }
    fun setCardDensity(value: CardDensity) = viewModelScope.launch { store.setCardDensity(value) }
    fun setAnimationsEnabled(value: Boolean) = viewModelScope.launch { store.setAnimationsEnabled(value) }
    fun setImmersiveMode(value: Boolean) = viewModelScope.launch { store.setImmersiveMode(value) }
    fun setHideCheckedItems(value: Boolean) = viewModelScope.launch { store.setHideCheckedItems(value) }
    fun setShowAdFreeBadge(value: Boolean) = viewModelScope.launch { store.setShowAdFreeBadge(value) }
    fun setAppIcon(icon: AppIcon) = viewModelScope.launch { store.setAppIcon(icon.name) }
    fun setDebugForceAdFree(value: Boolean) = viewModelScope.launch { if (BuildConfig.DEBUG) store.setDebugForceAdFree(value) }

    fun purchaseAdFree(activity: Activity) = billing.launchPurchase(activity)
    fun restorePurchases() = billing.refreshPurchases(userInitiated = true)
    fun refreshBilling() = billing.start()

    fun startAdsIfNeeded(activity: Activity) = ads.gatherConsentAndInit(activity)
    fun showPrivacyOptions(activity: Activity, onDone: (Boolean) -> Unit) = ads.showPrivacyOptions(activity, onDone)
}
