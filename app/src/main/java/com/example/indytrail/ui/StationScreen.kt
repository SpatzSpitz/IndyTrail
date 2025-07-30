package com.example.indytrail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.indytrail.data.Station

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationScreen(
    station: Station,
    onBack: () -> Unit,
    onSolved: (Station) -> Unit = {}
) {
    var input by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(station.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(station.riddle, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Antwort") }
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                if (input.trim().uppercase() == station.answer) {
                    feedback = "Richtig!"
                    onSolved(station)
                } else {
                    feedback = "Noch nicht."
                }
            }) { Text("Prüfen") }
            feedback?.let {
                Spacer(Modifier.height(8.dp))
                Text(it)
            }
        }
    }
}
