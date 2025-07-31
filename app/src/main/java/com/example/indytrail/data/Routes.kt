package com.example.indytrail.data

// Possible routes parsed from a QR code
sealed class ScanRoute {
    data class Station(val stationId: String) : ScanRoute()
    data class QuestGlyph(val questId: String, val slot: Int, val glyph: Glyph) : ScanRoute()
}

// Parse a text value into a scan route?

