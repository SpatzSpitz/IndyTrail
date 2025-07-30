package com.example.indytrail.data

/**
 * Einfaches Mapping:
 *  - Code (aus QR: "PSI", "R", "F", "X") -> sichtbares Symbol
 *  - Taste (Emitter: "1".."9") -> Code
 *
 * Du kannst die Zuordnungen jederzeit anpassen/erweitern.
 */
object GlyphCatalog {

    // Code -> sichtbares Symbol (Runen/Griechisch o.ä.)
    private val codeToSymbol: Map<String, String> = mapOf(
        "PSI" to "Ψ",   // Beispiel: griechisches Psi
        "R"   to "ᚱ",   // Runen-R (Raido)
        "F"   to "ᚠ",   // Runen-F (Fehu)
        "X"   to "ᛉ",   // Runen-Algiz (X-ähnlich)
    )

    // Taste (Emitter) -> Code
    private val keyToCode: Map<String, String> = mapOf(
        "1" to "PSI",
        "2" to "R",
        "3" to "F",
        "4" to "X",
        // Weitere Zuordnungen sind möglich:
        // "5" to "...", "6" to "...", ...
    )

    /** Sichtbares Symbol für einen Code. Fallback: "?" */
    fun symbol(code: String?): String = codeToSymbol[code] ?: "?"

    /** Code zu einer Tasten-Eingabe (z.B. "1" -> "PSI"). */
    fun codeFromKey(key: String?): String? = keyToCode[key]

    /** Rückrichtung: welche Taste gehört zu diesem Code (z.B. "PSI" -> "1")? */
    fun keyFromCode(code: String?): String? =
        keyToCode.entries.firstOrNull { it.value == code }?.key
}
