package com.example.indytrail.data

/**
 * Tracks found glyphs per quest slot (1..4).
 * Simple in-memory storage without persistence.
 */
class QuestStore {

    // questId -> (slot -> glyph)
    private val map: MutableMap<String, MutableMap<Int, String>> = mutableMapOf()

    sealed interface ApplyResult {
        data class Updated(val progress: Map<Int, String>) : ApplyResult
        data class Completed(val progress: Map<Int, String>) : ApplyResult
        data object NoChange : ApplyResult
    }

    /**
     * Insert a glyph into the given slot. Slots are 1-based.
     */
    fun applyGlyph(questId: String, slot: Int, glyph: String): ApplyResult {
        val q = map.getOrPut(questId) { mutableMapOf() }
        val prev = q[slot]
        if (prev == glyph) return ApplyResult.NoChange

        q[slot] = glyph

        // completed when slots 1..4 are filled
        val complete = (1..4).all { q[it]?.isNotEmpty() == true }
        return if (complete) ApplyResult.Completed(q.toMap()) else ApplyResult.Updated(q.toMap())
    }

    /** Convenient alias. */
    fun setGlyph(questId: String, slot: Int, glyph: String): ApplyResult =
        applyGlyph(questId, slot, glyph)

    /** Current glyphs for a quest. */
    private fun glyphs(questId: String): Map<Int, String> =
        map[questId]?.toMap().orEmpty()

    /** Number of filled slots (1..4). */
    fun foundCount(questId: String): Int =
        map[questId]?.count { it.key in 1..4 && it.value.isNotEmpty() } ?: 0

    /** For debugging or logging. */
    fun snapshot(questId: String): Map<Int, String> = glyphs(questId)
}
