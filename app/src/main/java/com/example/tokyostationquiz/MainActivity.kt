package com.example.tokyostationquiz

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
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

// Data class for quiz state history
data class QuizState(
    val originStation: Station,
    val destinationStation: Station,
    val isOriginCardExpanded: Boolean,
    val isDestinationCardExpanded: Boolean
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
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromString<StationData>(jsonString).stations
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
    var isOriginCardExpanded by remember { mutableStateOf(false) }
    var isDestinationCardExpanded by remember { mutableStateOf(false) }

    // History management (max 100 items)
    var history by remember {
        mutableStateOf(
            listOf(
                QuizState(
                    originStation = originStation,
                    destinationStation = destinationStation,
                    isOriginCardExpanded = false,
                    isDestinationCardExpanded = false
                )
            )
        )
    }
    var currentHistoryIndex by remember { mutableIntStateOf(0) }

    fun nextQuestion() {
        isOriginCardExpanded = false
        isDestinationCardExpanded = false
        if (isDepartureFixed) {
            destinationStation = stations.shuffled().first { it != originStation }
        } else {
            val randomStations = stations.shuffled().take(2)
            originStation = randomStations[0]
            destinationStation = randomStations[1]
        }

        // Remove history after current index if we're not at the end
        val trimmedHistory = if (currentHistoryIndex < history.size - 1) {
            history.subList(0, currentHistoryIndex + 1)
        } else {
            history
        }

        // Add new state to history
        val newState = QuizState(
            originStation = originStation,
            destinationStation = destinationStation,
            isOriginCardExpanded = false,
            isDestinationCardExpanded = false
        )

        var newHistory = trimmedHistory + newState

        // Keep only last 100 items
        if (newHistory.size > 100) {
            newHistory = newHistory.drop(1)
        }

        history = newHistory
        currentHistoryIndex = newHistory.size - 1
    }

    fun previousQuestion() {
        if (currentHistoryIndex > 0) {
            currentHistoryIndex--
            val state = history[currentHistoryIndex]
            originStation = state.originStation
            destinationStation = state.destinationStation
            isOriginCardExpanded = state.isOriginCardExpanded
            isDestinationCardExpanded = state.isDestinationCardExpanded
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
                onCheckedChange = { isChecked ->
                    isDepartureFixed = isChecked
                    isOriginCardExpanded = false
                    isDestinationCardExpanded = false
                    if (isDepartureFixed) {
                        destinationStation = stations.shuffled().first { it != originStation }
                    } else {
                        val randomStations = stations.shuffled().take(2)
                        originStation = randomStations[0]
                        destinationStation = randomStations[1]
                    }
                    history = listOf(
                        QuizState(
                            originStation = originStation,
                            destinationStation = destinationStation,
                            isOriginCardExpanded = false,
                            isDestinationCardExpanded = false
                        )
                    )
                    currentHistoryIndex = 0
                }
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
                    modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                                isOriginCardExpanded = false
                                isDestinationCardExpanded = false
                                destinationStation = stations.shuffled().first { it != originStation }
                                // Reset history with new starting station
                                history = listOf(
                                    QuizState(
                                        originStation = originStation,
                                        destinationStation = destinationStation,
                                        isOriginCardExpanded = false,
                                        isDestinationCardExpanded = false
                                    )
                                )
                                currentHistoryIndex = 0
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "出発駅", fontSize = 24.sp)
        Text(text = originStation.name, fontSize = 48.sp)
        ExpandableCard(
            title = "ヒント",
            station = originStation,
            expanded = isOriginCardExpanded,
            onExpandChange = { isOriginCardExpanded = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "到着駅", fontSize = 24.sp)
        Text(text = destinationStation.name, fontSize = 48.sp)
        ExpandableCard(
            title = "ヒント",
            station = destinationStation,
            expanded = isDestinationCardExpanded,
            onExpandChange = { isDestinationCardExpanded = it }
        )


        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { previousQuestion() },
                enabled = currentHistoryIndex > 0
            ) {
                Text(text = "前の問題へ")
            }

            Button(
                onClick = { nextQuestion() }
            ) {
                Text(text = "次の問題へ")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val url = "https://www.google.com/maps/dir/?api=1&origin=${originStation.name}駅&destination=${destinationStation.name}駅"
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            }
        ) {
            Text(text = "Googleマップで確認")
        }
    }
}

@Composable
fun ExpandableCard(title: String, station: Station, expanded: Boolean, onExpandChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onExpandChange(!expanded) }
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
