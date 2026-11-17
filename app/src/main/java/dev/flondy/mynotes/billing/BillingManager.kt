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
        /** 2.5: Einmalkauf „Design-Paket“ – alle Premium-Designs und Papiermuster dauerhaft. */
        const val PRODUCT_DESIGN_PACK = "personalization_pack"
        /** 2.5: Abo „Premium Plus“ – Design-Paket-Inhalte + werbefrei, solange das Abo läuft. */
        const val PRODUCT_PREMIUM_PLUS = "premium_plus"
        const val BASE_PLAN_MONTHLY = "monthly"
        const val BASE_PLAN_YEARLY = "yearly"
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
        /** true, sobald Google Play auf die Produktabfrage geantwortet hat (auch wenn kein Produkt kam). */
        val productQueried: Boolean = false,
        // 2.5: Shop-Produkte (Preise immer aus Google Play)
        val designPack: Boolean = false,
        val plusActive: Boolean = false,
        val designPackPrice: String? = null,
        val plusMonthlyPrice: String? = null,
        val plusYearlyPrice: String? = null,
        /** Länge der kostenlosen Testphase in Tagen, wenn Play dem Nutzer ein Probe-Angebot anbietet. */
        val plusTrialDays: Int? = null,
        val shopQueried: Boolean = false,
    ) {
        val designPackAvailable: Boolean get() = designPackPrice != null
        val plusMonthlyAvailable: Boolean get() = plusMonthlyPrice != null
        val plusYearlyAvailable: Boolean get() = plusYearlyPrice != null
        /** Premium-Designs freigeschaltet (Design-Paket oder aktives Premium Plus). */
        val designsUnlocked: Boolean get() = designPack || plusActive
    }

    sealed interface Event {
        data object PurchaseSuccess : Event
        data object ShopPurchaseSuccess : Event
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
    private var designPackDetails: ProductDetails? = null
    private var plusDetails: ProductDetails? = null
    private var pendingRestoreFeedback = false
    @Volatile private var playQueried = false
    @Volatile private var packQueried = false
    @Volatile private var subsQueried = false

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                handlePurchases(purchases.orEmpty(), fromQuery = false)
                handleShopPurchases(purchases.orEmpty(), type = null)
            }
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
        scope.launch {
            val (pack, plus) = settings.premiumCacheFlow.first()
            _state.update { it.copy(designPack = if (packQueried) it.designPack else pack, plusActive = if (subsQueried) it.plusActive else plus) }
        }
    }

    fun start() {
        if (billingClient.isReady) {
            refreshPurchases()
            if (productDetails == null) loadProductDetails()
            if (designPackDetails == null || plusDetails == null) loadShopDetails()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingResponseCode.OK
                _state.update { it.copy(connected = ok) }
                if (ok) {
                    refreshPurchases()
                    loadProductDetails()
                    loadShopDetails()
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
        querySubscriptions()
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) {
                handleShopPurchases(purchases, type = BillingClient.ProductType.INAPP)
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

    private fun querySubscriptions() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) handleShopPurchases(purchases, type = BillingClient.ProductType.SUBS)
            else Log.w(TAG, "Abo-Abfrage fehlgeschlagen: ${result.responseCode}")
        }
    }

    /** Preise und Angebote für Design-Paket und Premium Plus (nur, was in der Play Console angelegt ist). */
    private fun loadShopDetails() {
        fun query(id: String, type: String, done: (ProductDetails?) -> Unit) {
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()
            )).build()
            billingClient.queryProductDetailsAsync(params) { result, queryResult ->
                done(if (result.responseCode == BillingResponseCode.OK) queryResult.productDetailsList.firstOrNull { it.productId == id } else null)
            }
        }
        query(PRODUCT_DESIGN_PACK, BillingClient.ProductType.INAPP) { d ->
            designPackDetails = d
            _state.update { it.copy(designPackPrice = d?.oneTimePurchaseOfferDetails?.formattedPrice, shopQueried = true) }
        }
        query(PRODUCT_PREMIUM_PLUS, BillingClient.ProductType.SUBS) { d ->
            plusDetails = d
            fun price(plan: String) = d?.subscriptionOfferDetails?.firstOrNull { it.basePlanId == plan && it.offerId == null }
                ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
            val trialDays = d?.subscriptionOfferDetails?.firstNotNullOfOrNull { trialDays(it) }
            _state.update { it.copy(plusMonthlyPrice = price(BASE_PLAN_MONTHLY), plusYearlyPrice = price(BASE_PLAN_YEARLY),
                plusTrialDays = trialDays, shopQueried = true) }
        }
    }

    /**
     * Tage der Gratisphase eines Angebots (z. B. „trial-7-days“), sonst null.
     * Play liefert nur Angebote, für die der Nutzer berechtigt ist – wer schon getestet hat, bekommt keins.
     */
    private fun trialDays(offer: ProductDetails.SubscriptionOfferDetails): Int? {
        if (offer.offerId == null) return null
        val free = offer.pricingPhases.pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L } ?: return null
        return periodDays(free.billingPeriod)
    }

    /** Angebot für den Kauf wählen: erst ein Gratis-Angebot für den Basisplan, dann den Basisplan selbst. */
    private fun pickOffer(details: ProductDetails, basePlanId: String?): ProductDetails.SubscriptionOfferDetails? {
        val offers = details.subscriptionOfferDetails?.filter { it.basePlanId == basePlanId }.orEmpty()
        return offers.firstOrNull { trialDays(it) != null }
            ?: offers.firstOrNull { it.offerId == null }
            ?: offers.firstOrNull()
    }

    /** Kauf im Design-Shop: Design-Paket oder Premium Plus (monatlich/jährlich). */
    fun launchShopPurchase(activity: Activity, productId: String, basePlanId: String? = null) {
        val details = if (productId == PRODUCT_DESIGN_PACK) designPackDetails else plusDetails
        if (details == null) {
            if (!billingClient.isReady) start() else loadShopDetails()
            _events.trySend(Event.Error(BillingResponseCode.ITEM_UNAVAILABLE))
            return
        }
        val builder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
        if (productId == PRODUCT_PREMIUM_PLUS) {
            val offer = pickOffer(details, basePlanId)
            if (offer == null) { _events.trySend(Event.Error(BillingResponseCode.ITEM_UNAVAILABLE)); return }
            builder.setOfferToken(offer.offerToken)
        }
        val flowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(builder.build())).build()
        activity.runOnUiThread {
            val r = billingClient.launchBillingFlow(activity, flowParams)
            if (r.responseCode != BillingResponseCode.OK) _events.trySend(Event.Error(r.responseCode))
        }
    }

    /**
     * Design-Paket (INAPP) und Premium Plus (SUBS). [type] = Ergebnis einer verbindlichen Abfrage dieses Typs
     * (Besitz bestätigen ODER widerrufen); null = neuer Kauf aus dem Kaufdialog.
     */
    private fun handleShopPurchases(purchases: List<Purchase>, type: String?) {
        fun owned(id: String) = purchases.any { id in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        purchases.filter { (PRODUCT_DESIGN_PACK in it.products || PRODUCT_PREMIUM_PLUS in it.products) &&
            it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach { acknowledge(it) }
        val before = _state.value.designsUnlocked
        when (type) {
            BillingClient.ProductType.INAPP -> { packQueried = true; _state.update { it.copy(designPack = owned(PRODUCT_DESIGN_PACK)) } }
            BillingClient.ProductType.SUBS -> { subsQueried = true; _state.update { it.copy(plusActive = owned(PRODUCT_PREMIUM_PLUS)) } }
            else -> {
                if (owned(PRODUCT_DESIGN_PACK)) _state.update { it.copy(designPack = true) }
                if (owned(PRODUCT_PREMIUM_PLUS)) _state.update { it.copy(plusActive = true) }
                if (!before && _state.value.designsUnlocked) _events.trySend(Event.ShopPurchaseSuccess)
                val pendingShop = purchases.any { (PRODUCT_DESIGN_PACK in it.products || PRODUCT_PREMIUM_PLUS in it.products) &&
                    it.purchaseState == Purchase.PurchaseState.PENDING }
                if (pendingShop) _events.trySend(Event.PurchasePending)
            }
        }
        val st = _state.value
        scope.launch { settings.setPremiumCache(st.designPack, st.plusActive) }
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
                    productQueried = true,
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
                _events.trySend(Event.RestoreResult(owned || _state.value.designsUnlocked))
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

/** ISO-8601-Zeitraum aus Play (z. B. „P1W“, „P7D“, „P1M“) in Tage; unbekannt → null. */
internal fun periodDays(period: String): Int? {
    val m = Regex("^P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?$").matchEntire(period) ?: return null
    val (y, mo, w, d) = m.destructured
    val days = (y.toIntOrNull() ?: 0) * 365 + (mo.toIntOrNull() ?: 0) * 30 + (w.toIntOrNull() ?: 0) * 7 + (d.toIntOrNull() ?: 0)
    return days.takeIf { it > 0 }
}
