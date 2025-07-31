package com.example.indytrail.data

/**
 * Simple mappings:
 *  - Code (from QR like "PSI", "R", "F", "X") -> visible symbol
 *  - Key (emitter: "1".."9") -> code
 *
 * Adjust the mappings as needed.
 */
object GlyphCatalog {

    // Code -> visible symbol (runes/Greek etc.)
    private val codeToSymbol: Map<String, String> = mapOf(
        "PSI" to "Ψ",   // Greek psi
        "R"   to "ᚱ",   // Rune R (Raido)
        "F"   to "ᚠ",   // Rune F (Fehu)
        "X"   to "ᛉ",   // Rune Algiz (resembles X)
        "U"   to "ᚢ",   // Rune Uruz
        "T"   to "ᛏ",   // Rune Tiwaz
        "S"   to "ᛋ",   // Rune Sowilo
        "E"   to "ᛖ",   // Rune Ehwaz
        "B"   to "ᛒ"    // Rune Berkana
    )

    // Key (emitter) -> code
    private val keyToCode: Map<String, String> = mapOf(
        "1" to "PSI",
        "2" to "R",
        "3" to "F",
        "4" to "X",
        "5" to "U",
        "6" to "T",
        "7" to "S",
        "8" to "E",
        "9" to "B",
    )

    /** Visible symbol for a code. Fallback: "?" */
    fun symbol(code: String?): String = codeToSymbol[code] ?: "?"

    /** Code from a key press (e.g. "1" -> "PSI"). */
    fun codeFromKey(key: String?): String? = keyToCode[key]

    /** Reverse lookup: which key belongs to this code (e.g. "PSI" -> "1")? */
    fun keyFromCode(code: String?): String? =
        keyToCode.entries.firstOrNull { it.value == code }?.key
}
