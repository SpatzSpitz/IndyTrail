package com.example.indytrail.data

/**
 * Hält pro Quest die gefundenen Glyphen nach Slot (1..4).
 * Minimal, ohne Persistenz – nur In‑Memory.
 */
class QuestStore {

    // questId -> (slot -> glyph)
    private val map: MutableMap<String, MutableMap<Int, String>> = mutableMapOf()

    sealed interface ApplyResult {
        data class Updated(val progress: Map<Int, String>) : ApplyResult
        data class Completed(val progress: Map<Int, String>) : ApplyResult
        data object NoChange : ApplyResult
    }

    /** Glyph in Slot übernehmen. Slots sind 1‑basiert. */
    fun applyGlyph(questId: String, slot: Int, glyph: String): ApplyResult {
        val q = map.getOrPut(questId) { mutableMapOf() }
        val prev = q[slot]
        if (prev == glyph) return ApplyResult.NoChange

        q[slot] = glyph

        // Completed, wenn Slots 1..4 belegt sind
        val complete = (1..4).all { q[it]?.isNotEmpty() == true }
        return if (complete) ApplyResult.Completed(q.toMap()) else ApplyResult.Updated(q.toMap())
    }

    /** Bequemer Alias. */
    fun setGlyph(questId: String, slot: Int, glyph: String): ApplyResult =
        applyGlyph(questId, slot, glyph)

    /** Aktueller Stand der Glyphen für eine Quest. */
    fun glyphs(questId: String): Map<Int, String> =
        map[questId]?.toMap().orEmpty()

    /** Anzahl belegter Slots (1..4). */
    fun foundCount(questId: String): Int =
        map[questId]?.count { it.key in 1..4 && it.value.isNotEmpty() } ?: 0

    /** Für Debug/Logging. */
    fun snapshot(questId: String): Map<Int, String> = glyphs(questId)
}
