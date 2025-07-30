package com.example.indytrail.data

// Ein paar Beispiel-Glyphen – erweitern wir später nach Bedarf
enum class Glyph { PSI, RHO, SIGMA, R, F, X, O }

// Hilfsfunktion: String -> Glyph?
fun parseGlyph(name: String?): Glyph? =
    name?.let { n -> Glyph.values().firstOrNull { it.name.equals(n, ignoreCase = true) } }

// Optionales Quest-Container-Modell (nutzen wir im nächsten Schritt)
data class QuestState(
    val id: String = "Q1",
    val slots: MutableMap<Int, Glyph?> = mutableMapOf(1 to null, 2 to null, 3 to null, 4 to null)
)
