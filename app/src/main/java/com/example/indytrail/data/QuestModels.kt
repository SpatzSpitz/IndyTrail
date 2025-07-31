package com.example.indytrail.data

// Example glyph set – extend as needed
enum class Glyph { PSI, RHO, SIGMA, R, F, X, O }

// Helper to parse a glyph from its string name
fun parseGlyph(name: String?): Glyph? =
    name?.let { n -> Glyph.entries.firstOrNull { it.name.equals(n, ignoreCase = true) } }

// Optional quest container model
data class QuestState(
    val id: String = "Q1",
    val slots: MutableMap<Int, Glyph?> = mutableMapOf(1 to null, 2 to null, 3 to null, 4 to null)
)
