package dev.flondy.mynotes.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dev.flondy.mynotes.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Google Play Billing (Library 8) für den bestehenden Einmalkauf [PRODUCT_REMOVE_ADS].
 *
 * Quelle der Wahrheit ist Google Play: Bei jedem Start/Verbindungsaufbau werden die Käufe neu abgefragt.
 * Der lokale Cache verhindert nur ein kurzes Aufblitzen von Werbung und wird bei Rückerstattung widerrufen.
 * Hinweis: Eine serverseitige Prüfung der Kauf-Tokens wäre noch sicherer (siehe Roadmap).
 */
class BillingManager(
    context: Context,
    private val settings: SettingsDataStore,
    private val scope: CoroutineScope,
) {
    companion object {
        /** Bestehende Produkt-ID – niemals ändern, sonst verlieren Käufer ihre Werbefreiheit. */
        const val PRODUCT_REMOVE_ADS = "remove_ads"
        private const val TAG = "BillingManager"
    }

    data class State(
        /** true, sobald der Cache gelesen wurde – vorher wird keine Werbung angezeigt. */
        val initialized: Boolean = false,
        val adFree: Boolean = false,
        val purchasePending: Boolean = false,
        val connected: Boolean = false,
        val formattedPrice: String? = null,
        val productAvailable: Boolean = false,
    )

    sealed interface Event {
        data object PurchaseSuccess : Event
        data object PurchasePending : Event
        data object PurchaseCanceled : Event
        data object AlreadyOwned : Event
        data class RestoreResult(val restored: Boolean) : Event
        data class Error(val responseCode: Int) : Event
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var productDetails: ProductDetails? = null
    private var pendingRestoreFeedback = false
    @Volatile private var playQueried = false

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingResponseCode.OK -> handlePurchases(purchases.orEmpty(), fromQuery = false)
            BillingResponseCode.USER_CANCELED -> _events.trySend(Event.PurchaseCanceled)
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _events.trySend(Event.AlreadyOwned)
                refreshPurchases()
            }
            else -> {
                Log.w(TAG, "Kauf fehlgeschlagen: ${result.responseCode} ${result.debugMessage}")
                _events.trySend(Event.Error(result.responseCode))
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        scope.launch {
            val cached = settings.adFreeCacheFlow.first()
            // Hat Google Play bereits geantwortet, gilt dessen Ergebnis – nicht der Cache.
            _state.update { it.copy(initialized = true, adFree = if (playQueried) it.adFree else cached) }
        }
    }

    fun start() {
        if (billingClient.isReady) {
            refreshPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingResponseCode.OK
                _state.update { it.copy(connected = ok) }
                if (ok) {
                    refreshPurchases()
                    loadProductDetails()
                } else {
                    Log.w(TAG, "Billing-Setup fehlgeschlagen: ${result.responseCode} ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() verbindet beim nächsten Aufruf automatisch neu.
                _state.update { it.copy(connected = false) }
            }
        })
    }

    /** Fragt bestehende Käufe bei Google Play ab („Käufe wiederherstellen“). */
    fun refreshPurchases(userInitiated: Boolean = false) {
        if (userInitiated) pendingRestoreFeedback = true
        if (!billingClient.isReady) {
            start()
            return
        }
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) {
                handlePurchases(purchases, fromQuery = true)
            } else {
                Log.w(TAG, "Kaufabfrage fehlgeschlagen: ${result.responseCode}")
                if (pendingRestoreFeedback) {
                    pendingRestoreFeedback = false
                    _events.trySend(Event.Error(result.responseCode))
                }
            }
        }
    }

    private fun loadProductDetails(onLoaded: ((ProductDetails?) -> Unit)? = null) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_REMOVE_ADS)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()
        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            val details = if (result.responseCode == BillingResponseCode.OK) {
                queryResult.productDetailsList.firstOrNull { it.productId == PRODUCT_REMOVE_ADS }
            } else null
            productDetails = details
            _state.update {
                it.copy(
                    productAvailable = details != null,
                    formattedPrice = details?.oneTimePurchaseOfferDetails?.formattedPrice,
                )
            }
            onLoaded?.invoke(details)
        }
    }

    fun launchPurchase(activity: Activity) {
        val launch: (ProductDetails?) -> Unit = { details ->
            if (details == null) {
                _events.trySend(Event.Error(BillingResponseCode.ITEM_UNAVAILABLE))
            } else {
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
                    ).build()
                activity.runOnUiThread {
                    val r = billingClient.launchBillingFlow(activity, flowParams)
                    if (r.responseCode != BillingResponseCode.OK) _events.trySend(Event.Error(r.responseCode))
                }
            }
        }
        val cached = productDetails
        when {
            cached != null -> launch(cached)
            billingClient.isReady -> loadProductDetails(launch)
            else -> {
                start()
                _events.trySend(Event.Error(BillingResponseCode.SERVICE_UNAVAILABLE))
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>, fromQuery: Boolean) {
        val relevant = purchases.filter { PRODUCT_REMOVE_ADS in it.products }
        val owned = relevant.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = !owned && relevant.any { it.purchaseState == Purchase.PurchaseState.PENDING }

        relevant.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { acknowledge(it) }

        val wasAdFree = _state.value.adFree
        if (fromQuery) {
            playQueried = true
            // Eine erfolgreiche Abfrage ist verbindlich: Besitz bestätigen ODER widerrufen (z. B. Rückerstattung).
            _state.update { it.copy(initialized = true, adFree = owned, purchasePending = pending) }
            scope.launch { settings.setAdFreeCache(owned) }
            if (pendingRestoreFeedback) {
                pendingRestoreFeedback = false
                _events.trySend(Event.RestoreResult(owned))
            }
        } else {
            if (owned) {
                _state.update { it.copy(adFree = true, purchasePending = false) }
                scope.launch { settings.setAdFreeCache(true) }
                if (!wasAdFree) _events.trySend(Event.PurchaseSuccess)
            } else if (pending) {
                _state.update { it.copy(purchasePending = true) }
                _events.trySend(Event.PurchasePending)
            }
        }
    }

    /** Käufe müssen innerhalb von 3 Tagen bestätigt werden, sonst erstattet Google Play automatisch. */
    private fun acknowledge(purchase: Purchase, attempt: Int = 1) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingResponseCode.OK && attempt < 3) {
                Log.w(TAG, "Acknowledge fehlgeschlagen (${result.responseCode}), neuer Versuch")
                acknowledge(purchase, attempt + 1)
            }
        }
    }
}
