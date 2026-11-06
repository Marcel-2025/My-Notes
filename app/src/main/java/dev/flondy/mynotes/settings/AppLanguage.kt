package dev.flondy.mynotes.settings

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Sprache der App, unabhängig von der Systemsprache.
 *
 * - Android 13+: offizielle App-Sprache über [LocaleManager] (erscheint auch in den Systemeinstellungen
 *   unter „App-Sprachen“, siehe res/xml/locales_config.xml). Android startet die Activity selbst neu.
 * - Ältere Versionen: Auswahl wird lokal gespeichert und beim Start über [wrap] angewendet.
 *
 * Ein leerer Tag bedeutet „Systemsprache“.
 */
object AppLanguage {
    /** Alle Sprachen mit vollständiger Übersetzung (muss zu locales_config.xml passen). */
    val supported = listOf("de", "en", "es", "fr", "it", "pl", "pt", "ru", "tr")

    private const val PREFS = "app_language"
    private const val KEY = "tag"

    /** Name der Sprache in ihrer eigenen Schreibweise, z. B. „Deutsch“, „Español“, „Русский“. */
    fun nativeName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
    }

    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            return if (locales == null || locales.isEmpty) "" else locales[0].language
        }
        return stored(context)
    }

    fun set(activity: Activity, tag: String) {
        if (tag == current(activity)) return
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).commit()
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            activity.recreate()
        }
    }

    /** Für Android < 13: gespeicherte Sprache auf den Context anwenden (Activity und Application). */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = stored(base)
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    private fun stored(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") ?: ""
}
