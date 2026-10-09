package dev.flondy.mynotes.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Einwilligung (Google UMP, DSGVO/EWR) und Initialisierung des Mobile Ads SDK.
 *
 * Reihenfolge laut Google: Einwilligungsstatus aktualisieren → ggf. Formular zeigen →
 * erst wenn canRequestAds() true ist, das SDK initialisieren und Anzeigen laden.
 * Für werbefreie Nutzer wird nichts davon ausgeführt.
 */
class AdsManager(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(appContext)
    private val sdkInitStarted = AtomicBoolean(false)
    private val consentRequested = AtomicBoolean(false)

    private val _adsReady = MutableStateFlow(false)
    /** true, wenn das SDK initialisiert ist und Anzeigen angefragt werden dürfen. */
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun gatherConsentAndInit(activity: Activity) {
        // Einwilligung aus einer früheren Sitzung → sofort initialisieren
        if (consentInformation.canRequestAds()) initializeSdk()
        if (!consentRequested.compareAndSet(false, true)) return

        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Log.w(TAG, "Consent-Formular: ${formError.errorCode} ${formError.message}")
                    updatePrivacyState()
                    if (consentInformation.canRequestAds()) initializeSdk()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent-Update fehlgeschlagen: ${requestError.errorCode} ${requestError.message}")
                consentRequested.set(false)
                updatePrivacyState()
                if (consentInformation.canRequestAds()) initializeSdk()
            }
        )
    }

    /** „Werbeeinwilligung verwalten“ in den Einstellungen. */
    fun showPrivacyOptions(activity: Activity, onDone: (success: Boolean) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            updatePrivacyState()
            onDone(formError == null)
        }
    }

    private fun updatePrivacyState() {
        _privacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initializeSdk() {
        if (!sdkInitStarted.compareAndSet(false, true)) return
        // Initialisierung im Hintergrund, um den Start der App nicht zu verzögern
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(appContext) { _adsReady.value = true }
        }
    }

    private companion object {
        const val TAG = "AdsManager"
    }
}
