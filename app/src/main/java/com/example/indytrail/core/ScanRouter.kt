package com.example.indytrail.core

sealed class ScanRoute {
    data class Station(val stationId: String) : ScanRoute()
    data class QuestGlyph(val questId: String, val slot: Int, val glyph: String) : ScanRoute()
    data class QuestOpen(val questId: String) : ScanRoute()
}

private val RX_STATION    = Regex("""^trail://station/([A-Za-z0-9_-]+)$""")
private val RX_QUEST_GLY  = Regex("""^trail://quest/([A-Za-z0-9_-]+)/slot/(\d+)/glyph/([A-Za-z0-9_-]+)$""")
private val RX_QUEST_OPEN = Regex("""^trail://quest/([A-Za-z0-9_-]+)$""")

fun parseScanUri(text: String): ScanRoute? {
    RX_STATION.matchEntire(text)?.let {
        return ScanRoute.Station(it.groupValues[1])
    }
    RX_QUEST_GLY.matchEntire(text)?.let {
        val id   = it.groupValues[1]
        val slot = it.groupValues[2].toIntOrNull() ?: return null
        val gly  = it.groupValues[3]
        return ScanRoute.QuestGlyph(id, slot, gly)
    }
    RX_QUEST_OPEN.matchEntire(text)?.let {
        return ScanRoute.QuestOpen(it.groupValues[1])
    }
    return null
}
