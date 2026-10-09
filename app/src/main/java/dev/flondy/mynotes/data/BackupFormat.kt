package dev.flondy.mynotes.data

import dev.flondy.mynotes.Category
import dev.flondy.mynotes.Note
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioniertes, lokales JSON-Backup. Keine Cloud – der Nutzer wählt selbst den Speicherort.
 *
 * {
 *   "format": "mynotes-backup", "version": 1, "exportedAt": 1700000000000,
 *   "categories": [{ "id": 1, "name": "Arbeit", "position": 0 }],
 *   "notes": [{ "title": "...", "content": "...", "createdAt": ..., "updatedAt": ..., ... }]
 * }
 */
object BackupFormat {
    const val FORMAT = "mynotes-backup"
    const val VERSION = 1

    data class BackupData(
        val version: Int,
        val exportedAt: Long,
        val categories: List<Category>,
        val notes: List<Note>,
    )

    class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun encode(notes: List<Note>, categories: List<Category>, now: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("exportedAt", now)
        root.put("categories", JSONArray().apply {
            categories.forEach { c ->
                put(JSONObject().put("id", c.id).put("name", c.name).put("position", c.position))
            }
        })
        root.put("notes", JSONArray().apply {
            notes.forEach { n ->
                put(JSONObject().apply {
                    put("title", n.title)
                    put("content", n.content)
                    put("createdAt", n.createdAt)
                    put("updatedAt", n.updatedAt)
                    put("displayOrder", n.displayOrder)
                    put("isPinned", n.isPinned)
                    put("isFavorite", n.isFavorite)
                    put("color", n.color)
                    put("noteType", n.noteType)
                    if (n.categoryId != null) put("categoryId", n.categoryId)
                    if (n.deletedAt != null) put("deletedAt", n.deletedAt)
                })
            }
        })
        return root.toString(2)
    }

    @Throws(InvalidBackupException::class)
    fun decode(json: String): BackupData {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw InvalidBackupException("not_json", e)
        }
        if (root.optString("format") != FORMAT) throw InvalidBackupException("wrong_format")
        val version = root.optInt("version", -1)
        if (version < 1) throw InvalidBackupException("wrong_format")
        if (version > VERSION) throw InvalidBackupException("too_new")
        try {
            val categories = root.optJSONArray("categories")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Category(id = o.getInt("id"), name = o.getString("name"), position = o.optInt("position", i))
                }
            } ?: emptyList()
            val notesArr = root.getJSONArray("notes")
            val notes = (0 until notesArr.length()).map { i ->
                val o = notesArr.getJSONObject(i)
                val updated = o.optLong("updatedAt", System.currentTimeMillis())
                Note(
                    id = 0,
                    title = o.optString("title", ""),
                    content = o.optString("content", ""),
                    updatedAt = updated,
                    createdAt = o.optLong("createdAt", updated),
                    displayOrder = o.optInt("displayOrder", i),
                    isPinned = o.optBoolean("isPinned", false),
                    isFavorite = o.optBoolean("isFavorite", false),
                    color = o.optInt("color", 0),
                    noteType = o.optInt("noteType", Note.TYPE_TEXT).takeIf { it == Note.TYPE_TEXT || it == Note.TYPE_CHECKLIST } ?: Note.TYPE_TEXT,
                    categoryId = if (o.has("categoryId") && !o.isNull("categoryId")) o.getInt("categoryId") else null,
                    deletedAt = if (o.has("deletedAt") && !o.isNull("deletedAt")) o.getLong("deletedAt") else null,
                )
            }
            return BackupData(version, root.optLong("exportedAt", 0L), categories, notes)
        } catch (e: InvalidBackupException) {
            throw e
        } catch (e: Exception) {
            throw InvalidBackupException("corrupt", e)
        }
    }

    /** Fingerabdruck zur Duplikaterkennung beim Import. */
    fun fingerprint(n: Note): String = "${n.title}\u0000${n.content}\u0000${n.createdAt}"
}
