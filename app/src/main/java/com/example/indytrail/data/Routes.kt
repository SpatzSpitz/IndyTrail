package com.example.indytrail.data

// Was aus einem QR kommen kann
sealed class ScanRoute {
    data class Station(val stationId: String) : ScanRoute()
    data class QuestGlyph(val questId: String, val slot: Int, val glyph: Glyph) : ScanRoute()
}

// Text -> Route?

