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
        // Premium Plus enthält Werbefreiheit, solange das Abo läuft (remove_ads bleibt davon unberührt)
        val withPlus = if (state.plusActive) state.copy(adFree = true) else state
        if (BuildConfig.DEBUG && s.debugForceAdFree) withPlus.copy(adFree = true, initialized = true) else withPlus
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
    // 2.4: Design & Papier (Premium nur, wenn freigeschaltet – sonst bleibt es bei der Vorschau)
    fun setSkin(skin: dev.flondy.mynotes.ui.theme.Skin) = viewModelScope.launch {
        if (dev.flondy.mynotes.ui.theme.Entitlements.skinUnlocked(skin, settings.value.unlocked)) store.setSkin(skin.name)
    }
    fun setPaperPattern(p: dev.flondy.mynotes.ui.theme.PaperPattern) = viewModelScope.launch {
        if (dev.flondy.mynotes.ui.theme.Entitlements.patternUnlocked(p, settings.value.unlocked)) store.setPaperPattern(p.name)
    }
    // 2.6: Schriftgröße im Editor und eigene Vorlagen (3 kostenlos, unbegrenzt mit Premium Plus)
    fun setEditorFontScale(value: Float) = viewModelScope.launch { store.setEditorFontScale(value) }
    /** false = Grenze erreicht (dann im Shop Premium Plus anbieten). */
    fun addCustomTemplate(t: dev.flondy.mynotes.data.NoteTemplate): Boolean {
        val list = dev.flondy.mynotes.data.CustomTemplates.decode(settings.value.customTemplates)
        if (!dev.flondy.mynotes.ui.theme.Entitlements.canSaveTemplate(list.size, settings.value.unlocked)) return false
        viewModelScope.launch { store.setCustomTemplates(dev.flondy.mynotes.data.CustomTemplates.encode(listOf(t) + list)) }
        return true
    }
    fun deleteCustomTemplate(id: String) = viewModelScope.launch {
        val list = dev.flondy.mynotes.data.CustomTemplates.decode(settings.value.customTemplates)
        store.setCustomTemplates(dev.flondy.mynotes.data.CustomTemplates.encode(list.filterNot { it.id == id }))
    }
    fun setDebugUnlockAll(value: Boolean) = viewModelScope.launch { if (BuildConfig.DEBUG) store.setDebugUnlockAll(value) }

    fun purchaseAdFree(activity: Activity) = billing.launchPurchase(activity)
    fun purchaseDesignPack(activity: Activity) = billing.launchShopPurchase(activity, BillingManager.PRODUCT_DESIGN_PACK)
    fun purchasePlus(activity: Activity, yearly: Boolean) = billing.launchShopPurchase(
        activity, BillingManager.PRODUCT_PREMIUM_PLUS, if (yearly) BillingManager.BASE_PLAN_YEARLY else BillingManager.BASE_PLAN_MONTHLY)
    fun restorePurchases() = billing.refreshPurchases(userInitiated = true)
    fun refreshBilling() = billing.start()

    fun startAdsIfNeeded(activity: Activity) = ads.gatherConsentAndInit(activity)
    fun showPrivacyOptions(activity: Activity, onDone: (Boolean) -> Unit) = ads.showPrivacyOptions(activity, onDone)
}
