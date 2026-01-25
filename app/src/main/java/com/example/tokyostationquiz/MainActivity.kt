package com.example.tokyostationquiz

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tokyostationquiz.ui.theme.TokyoStationQuizTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

// Data classes for JSON parsing
@Serializable
data class Station(
    val name: String,
    val ward: String,
    val lines: List<String>
)

@Serializable
data class StationData(
    val stations: List<Station>
)

// Function to read JSON from assets
private fun readStationsJson(context: Context): List<Station> {
    val jsonString: String
    try {
        jsonString = context.assets.open("stations.json").bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        return emptyList()
    }
    return Json { ignoreUnknownKeys = true }.decodeFromString<StationData>(jsonString).stations
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stations = readStationsJson(this).sortedBy { it.name }
        enableEdgeToEdge()
        setContent {
            TokyoStationQuizTheme {
                QuizApp(stations = stations)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizApp(stations: List<Station>) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("東京路線クイズ") })
        }
    ) { innerPadding ->
        QuizScreen(stations = stations, modifier = Modifier.padding(innerPadding))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(stations: List<Station>, modifier: Modifier = Modifier) {
    var isDepartureFixed by remember { mutableStateOf(false) }
    var originStation by remember { mutableStateOf(stations.first()) }
    var destinationStation by remember { mutableStateOf(stations.shuffled().first { it != originStation }) }
    val context = LocalContext.current

    fun nextQuestion() {
        if (isDepartureFixed) {
            destinationStation = stations.shuffled().first { it != originStation }
        } else {
            val randomStations = stations.shuffled().take(2)
            originStation = randomStations[0]
            destinationStation = randomStations[1]
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("出発駅を固定する")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = isDepartureFixed,
                onCheckedChange = { isDepartureFixed = it }
            )
        }

        if (isDepartureFixed) {
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                TextField(
                    value = originStation.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("出発駅") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    stations.forEach { station ->
                        DropdownMenuItem(
                            text = { Text(station.name) },
                            onClick = {
                                originStation = station
                                expanded = false
                                nextQuestion()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "出発駅", fontSize = 24.sp)
        Text(text = originStation.name, fontSize = 48.sp)
        ExpandableCard(title = "ヒント", station = originStation)

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "到着駅", fontSize = 24.sp)
        Text(text = destinationStation.name, fontSize = 48.sp)
        ExpandableCard(title = "ヒント", station = destinationStation)


        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                val url = "https://www.google.com/maps/dir/?api=1&origin=${originStation.name}駅&destination=${destinationStation.name}駅"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }) {
                Text(text = "Googleマップで確認")
            }

            Button(onClick = { nextQuestion() }) {
                Text(text = "次の問題へ")
            }
        }
    }
}

@Composable
fun ExpandableCard(title: String, station: Station) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = "区: ${station.ward}")
                    Text(text = "路線: ${station.lines.joinToString()}")
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun QuizAppPreview() {
    TokyoStationQuizTheme {
        val previewStations = listOf(
            Station("新宿", "新宿区", listOf("JR山手線", "JR中央線")),
            Station("渋谷", "渋谷区", listOf("JR山手線", "JR埼京線")),
            Station("池袋", "豊島区", listOf("JR山手線", "JR埼京線"))
        )
        QuizApp(stations = previewStations)
    }
}
