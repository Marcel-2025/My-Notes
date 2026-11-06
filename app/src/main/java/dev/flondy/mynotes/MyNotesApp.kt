package dev.flondy.mynotes

import android.app.Application
import dev.flondy.mynotes.ads.AdsManager
import dev.flondy.mynotes.billing.BillingManager
import dev.flondy.mynotes.data.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/** Einfache manuelle Dependency Injection – App-weite Singletons. */
class MyNotesApp : Application() {
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(dev.flondy.mynotes.settings.AppLanguage.wrap(base))
    }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: NoteRepository by lazy { NoteRepository(database) }
    val settings: SettingsDataStore by lazy { SettingsDataStore(this) }
    val billing: BillingManager by lazy { BillingManager(this, settings, appScope) }
    val ads: AdsManager by lazy { AdsManager(this, appScope) }
}

val Application.app: MyNotesApp get() = this as MyNotesApp
