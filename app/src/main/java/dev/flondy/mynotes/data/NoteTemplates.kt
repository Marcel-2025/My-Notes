package dev.flondy.mynotes.data

/**
 * Notiz-Vorlagen (Version 2.4), in allen 9 App-Sprachen.
 * Checklisten-Vorlagen: eine Zeile = ein Eintrag; Textvorlagen nutzen die normale Formatierung (• Aufzählung).
 */
data class NoteTemplate(
    val id: String,
    val icon: String,          // Kategorie-Symbol-Schlüssel (CategoryIcons)
    val title: String,
    val text: String = "",
    val items: List<String> = emptyList(),
) {
    val isChecklist get() = items.isNotEmpty()
}

object NoteTemplates {
    val ids = listOf("shopping", "meeting", "packing", "week", "recipe", "journal")

    fun forLanguage(lang: String): List<NoteTemplate> {
        val t = texts[lang] ?: texts.getValue("en")
        return ids.map { id ->
            val e = t.getValue(id)
            val icon = when (id) { "shopping" -> "shopping"; "meeting" -> "work"; "packing" -> "travel"; "week" -> "tasks"; "recipe" -> "idea"; else -> "heart" }
            if (id in checklistIds) NoteTemplate(id, icon, e.first, items = e.second.split("|"))
            else NoteTemplate(id, icon, e.first, text = e.second.replace("|", "\n"))
        }
    }

    private val checklistIds = setOf("shopping", "packing", "week")

    /** (Titel, Inhalt) – Inhalt mit „|“ als Zeilentrenner. */
    private val texts: Map<String, Map<String, Pair<String, String>>> = mapOf(
        "de" to mapOf(
            "shopping" to ("Einkaufsliste" to "Brot|Milch|Eier|Obst|Gemüse|Kaffee"),
            "meeting" to ("Besprechung" to "Datum: |Teilnehmer: ||**Themen**|• |||**Ergebnisse**|• |||**Nächste Schritte**|• "),
            "packing" to ("Packliste" to "Ausweis|Ladekabel|Kleidung|Zahnbürste|Medikamente|Sonnenbrille"),
            "week" to ("Wochenplan" to "Montag: |Dienstag: |Mittwoch: |Donnerstag: |Freitag: |Wochenende: "),
            "recipe" to ("Rezept" to "**Zutaten**|• ||**Zubereitung**|1. ||Portionen: |Zeit: "),
            "journal" to ("Tagebuch" to "**Heute war schön:**|||**Dafür bin ich dankbar:**|||**Morgen möchte ich:**|"),
        ),
        "en" to mapOf(
            "shopping" to ("Shopping list" to "Bread|Milk|Eggs|Fruit|Vegetables|Coffee"),
            "meeting" to ("Meeting" to "Date: |Attendees: ||**Topics**|• |||**Results**|• |||**Next steps**|• "),
            "packing" to ("Packing list" to "ID card|Charger|Clothes|Toothbrush|Medication|Sunglasses"),
            "week" to ("Weekly plan" to "Monday: |Tuesday: |Wednesday: |Thursday: |Friday: |Weekend: "),
            "recipe" to ("Recipe" to "**Ingredients**|• ||**Method**|1. ||Servings: |Time: "),
            "journal" to ("Journal" to "**Today was nice:**|||**I’m grateful for:**|||**Tomorrow I want to:**|"),
        ),
        "es" to mapOf(
            "shopping" to ("Lista de la compra" to "Pan|Leche|Huevos|Fruta|Verdura|Café"),
            "meeting" to ("Reunión" to "Fecha: |Participantes: ||**Temas**|• |||**Resultados**|• |||**Próximos pasos**|• "),
            "packing" to ("Lista de equipaje" to "Documento de identidad|Cargador|Ropa|Cepillo de dientes|Medicamentos|Gafas de sol"),
            "week" to ("Plan semanal" to "Lunes: |Martes: |Miércoles: |Jueves: |Viernes: |Fin de semana: "),
            "recipe" to ("Receta" to "**Ingredientes**|• ||**Preparación**|1. ||Raciones: |Tiempo: "),
            "journal" to ("Diario" to "**Hoy fue bonito:**|||**Estoy agradecido por:**|||**Mañana quiero:**|"),
        ),
        "fr" to mapOf(
            "shopping" to ("Liste de courses" to "Pain|Lait|Œufs|Fruits|Légumes|Café"),
            "meeting" to ("Réunion" to "Date : |Participants : ||**Sujets**|• |||**Résultats**|• |||**Prochaines étapes**|• "),
            "packing" to ("Liste de bagages" to "Carte d’identité|Chargeur|Vêtements|Brosse à dents|Médicaments|Lunettes de soleil"),
            "week" to ("Planning de la semaine" to "Lundi : |Mardi : |Mercredi : |Jeudi : |Vendredi : |Week-end : "),
            "recipe" to ("Recette" to "**Ingrédients**|• ||**Préparation**|1. ||Portions : |Temps : "),
            "journal" to ("Journal" to "**Aujourd’hui, c’était bien :**|||**Je suis reconnaissant pour :**|||**Demain, je veux :**|"),
        ),
        "it" to mapOf(
            "shopping" to ("Lista della spesa" to "Pane|Latte|Uova|Frutta|Verdura|Caffè"),
            "meeting" to ("Riunione" to "Data: |Partecipanti: ||**Argomenti**|• |||**Risultati**|• |||**Prossimi passi**|• "),
            "packing" to ("Lista bagagli" to "Documento|Caricatore|Vestiti|Spazzolino|Medicinali|Occhiali da sole"),
            "week" to ("Piano settimanale" to "Lunedì: |Martedì: |Mercoledì: |Giovedì: |Venerdì: |Fine settimana: "),
            "recipe" to ("Ricetta" to "**Ingredienti**|• ||**Preparazione**|1. ||Porzioni: |Tempo: "),
            "journal" to ("Diario" to "**Oggi è stato bello:**|||**Sono grato per:**|||**Domani voglio:**|"),
        ),
        "pl" to mapOf(
            "shopping" to ("Lista zakupów" to "Chleb|Mleko|Jajka|Owoce|Warzywa|Kawa"),
            "meeting" to ("Spotkanie" to "Data: |Uczestnicy: ||**Tematy**|• |||**Wyniki**|• |||**Kolejne kroki**|• "),
            "packing" to ("Lista pakowania" to "Dowód osobisty|Ładowarka|Ubrania|Szczoteczka do zębów|Leki|Okulary przeciwsłoneczne"),
            "week" to ("Plan tygodnia" to "Poniedziałek: |Wtorek: |Środa: |Czwartek: |Piątek: |Weekend: "),
            "recipe" to ("Przepis" to "**Składniki**|• ||**Przygotowanie**|1. ||Porcje: |Czas: "),
            "journal" to ("Dziennik" to "**Dziś było miło:**|||**Jestem wdzięczny za:**|||**Jutro chcę:**|"),
        ),
        "pt" to mapOf(
            "shopping" to ("Lista de compras" to "Pão|Leite|Ovos|Frutas|Legumes|Café"),
            "meeting" to ("Reunião" to "Data: |Participantes: ||**Assuntos**|• |||**Resultados**|• |||**Próximos passos**|• "),
            "packing" to ("Lista de mala" to "Documento|Carregador|Roupas|Escova de dentes|Remédios|Óculos de sol"),
            "week" to ("Plano semanal" to "Segunda: |Terça: |Quarta: |Quinta: |Sexta: |Fim de semana: "),
            "recipe" to ("Receita" to "**Ingredientes**|• ||**Modo de preparo**|1. ||Porções: |Tempo: "),
            "journal" to ("Diário" to "**Hoje foi bom:**|||**Sou grato por:**|||**Amanhã quero:**|"),
        ),
        "ru" to mapOf(
            "shopping" to ("Список покупок" to "Хлеб|Молоко|Яйца|Фрукты|Овощи|Кофе"),
            "meeting" to ("Встреча" to "Дата: |Участники: ||**Темы**|• |||**Итоги**|• |||**Следующие шаги**|• "),
            "packing" to ("Список вещей" to "Паспорт|Зарядка|Одежда|Зубная щётка|Лекарства|Солнечные очки"),
            "week" to ("План на неделю" to "Понедельник: |Вторник: |Среда: |Четверг: |Пятница: |Выходные: "),
            "recipe" to ("Рецепт" to "**Ингредиенты**|• ||**Приготовление**|1. ||Порции: |Время: "),
            "journal" to ("Дневник" to "**Сегодня было хорошо:**|||**Я благодарен за:**|||**Завтра я хочу:**|"),
        ),
        "tr" to mapOf(
            "shopping" to ("Alışveriş listesi" to "Ekmek|Süt|Yumurta|Meyve|Sebze|Kahve"),
            "meeting" to ("Toplantı" to "Tarih: |Katılımcılar: ||**Konular**|• |||**Sonuçlar**|• |||**Sonraki adımlar**|• "),
            "packing" to ("Bavul listesi" to "Kimlik|Şarj aleti|Kıyafetler|Diş fırçası|İlaçlar|Güneş gözlüğü"),
            "week" to ("Haftalık plan" to "Pazartesi: |Salı: |Çarşamba: |Perşembe: |Cuma: |Hafta sonu: "),
            "recipe" to ("Tarif" to "**Malzemeler**|• ||**Hazırlanışı**|1. ||Porsiyon: |Süre: "),
            "journal" to ("Günlük" to "**Bugün güzeldi:**|||**Şunun için minnettarım:**|||**Yarın şunu yapmak istiyorum:**|"),
        ),
    )
}


/** 2.6: Eigene Vorlagen (lokal gespeichert als JSON in den Einstellungen). */
object CustomTemplates {
    fun decode(json: String): List<NoteTemplate> = runCatching {
        if (json.isBlank()) return emptyList()
        val a = org.json.JSONArray(json)
        List(a.length()) { i ->
            val o = a.getJSONObject(i)
            val items = o.optJSONArray("items")?.let { arr -> List(arr.length()) { arr.getString(it) } } ?: emptyList()
            NoteTemplate(o.getString("id"), o.optString("icon", "folder"), o.optString("title"), o.optString("text"), items)
        }
    }.getOrDefault(emptyList())

    fun encode(list: List<NoteTemplate>): String = org.json.JSONArray().apply {
        list.forEach { t ->
            put(org.json.JSONObject().put("id", t.id).put("icon", t.icon).put("title", t.title).put("text", t.text)
                .put("items", org.json.JSONArray().apply { t.items.forEach { put(it) } }))
        }
    }.toString()
}
