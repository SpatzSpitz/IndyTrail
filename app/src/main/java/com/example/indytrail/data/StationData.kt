package com.example.indytrail.data

data class Station(val id: String, val title: String, val riddle: String, val answer: String)

object Stations {
    val all = listOf(
        Station("S1", "Der vergessene Obelisk",
            "Was füllt einen Raum, nimmt aber keinen Platz ein?", "LICHT"),
        Station("S2", "Die stumme Tastatur",
            "Ich habe Schlüssel, aber keine Schlösser. Ich habe Platz, aber keinen Raum. Du kannst eintreten, aber nicht hinausgehen.", "TASTATUR"),
        Station("S3", "Die Schattenkammer",
            "Je mehr davon da ist, desto weniger siehst du.", "DUNKELHEIT")
    )
    fun byId(id: String) = all.find { it.id.equals(id, true) }
}