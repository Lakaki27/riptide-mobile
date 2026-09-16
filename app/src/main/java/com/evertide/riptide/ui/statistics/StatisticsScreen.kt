package com.evertide.riptide.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evertide.riptide.R

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.statistics), style = MaterialTheme.typography.titleLarge)

            SingleChoiceSegmented(
                    options = listOf("My stats" to StatsScope.ME, "Everyone" to StatsScope.GLOBAL),
                    selected = state.scope,
                    onSelect = viewModel::setScope,
            )
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                                stringResource(R.string.listening_activity),
                                style = MaterialTheme.typography.titleMedium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RangeDropdown(state.selectedRange, viewModel::setRange)
                            if (state.selectedRange == HeatmapRange.YEAR) {
                                YearDropdown(
                                        state.selectedYear,
                                        viewModel.availableYears,
                                        viewModel::setYear
                                )
                            }
                        }
                    }

                    if (state.heatmapLoading) {
                        Box(
                                Modifier.fillMaxWidth().height(96.dp),
                                contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    } else {
                        HeatmapView(
                                weeks = state.weeks,
                                onDayClick = viewModel::onDayTapped,
                                modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    state.selectedDay?.let { day ->
                        Text(
                                "${day.formattedLong()} — ${pluralStringResource(R.plurals.play_count, day.count, day.count)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                LeaderboardSection(
                        title = "Top songs",
                        rows = state.topSongs.map { it.title to it.playCount },
                )
            }

            item {
                LeaderboardSection(
                        title = "Top artists",
                        rows = state.topArtists.map { it.name to it.playCount },
                )
            }
        }
    }
}

@Composable
private fun <T> SingleChoiceSegmented(
        options: List<Pair<String, T>>,
        selected: T,
        onSelect: (T) -> Unit,
) {
    Row(
            modifier =
                    Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp),
    ) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            Box(
                    modifier =
                            Modifier.clip(
                                            androidx.compose.foundation.shape.RoundedCornerShape(
                                                    6.dp
                                            )
                                    )
                                    .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    .clickable { onSelect(value) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                        label,
                        color =
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun RangeDropdown(selected: HeatmapRange, onSelect: (HeatmapRange) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label =
            when (selected) {
                HeatmapRange.MONTH -> "This month"
                HeatmapRange.YEAR -> "This year"
                HeatmapRange.ALL -> "All time"
            }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.heatmap_this_month)) },
                    onClick = {
                        onSelect(HeatmapRange.MONTH)
                        expanded = false
                    }
            )
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.heatmap_this_year)) },
                    onClick = {
                        onSelect(HeatmapRange.YEAR)
                        expanded = false
                    }
            )
            DropdownMenuItem(
                    text = { Text(stringResource(R.string.heatmap_all_time)) },
                    onClick = {
                        onSelect(HeatmapRange.ALL)
                        expanded = false
                    }
            )
        }
    }
}

@Composable
private fun YearDropdown(selected: Int, years: List<Int>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(selected.toString()) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            years.forEach { year ->
                DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            onSelect(year)
                            expanded = false
                        }
                )
            }
        }
    }
}

@Composable
private fun LeaderboardSection(title: String, rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(8.dp),
        ) {
            if (rows.isEmpty()) {
                Text(
                        stringResource(R.string.no_plays_yet),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                rows.forEachIndexed { index, (label, count) ->
                    Row(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                                "${index + 1}",
                                modifier = Modifier.width(24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                                label,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                        )
                        Text(count, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
