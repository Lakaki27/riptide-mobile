package com.evertide.riptide.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evertide.riptide.data.model.TopArtist
import com.evertide.riptide.data.model.TopSong
import com.evertide.riptide.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class HeatmapRange {
    MONTH,
    YEAR,
    ALL
}

enum class StatsScope {
    ME,
    GLOBAL
}

data class HeatmapDayUi(val date: LocalDate, val count: Int)

data class StatisticsUiState(
        val loading: Boolean = true,
        val heatmapLoading: Boolean = false,
        val topSongs: List<TopSong> = emptyList(),
        val topArtists: List<TopArtist> = emptyList(),
        val weeks: List<List<HeatmapDayUi>> = emptyList(),
        val earliestDate: LocalDate? = null,
        val selectedRange: HeatmapRange = HeatmapRange.YEAR,
        val selectedYear: Int = LocalDate.now().year,
        val scope: StatsScope = StatsScope.ME,
        val selectedDay: HeatmapDayUi? = null,
)

@HiltViewModel
class StatisticsViewModel
@Inject
constructor(
        private val statsRepository: StatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState

    val availableYears: List<Int>
        get() {
            val earliest = _uiState.value.earliestDate ?: return listOf(LocalDate.now().year)
            val current = LocalDate.now().year
            return (current downTo earliest.year).toList()
        }

    init {
        viewModelScope.launch {
            val earliest =
                    runCatching { statsRepository.getEarliestPlay().date }.getOrNull()?.let {
                        runCatching { LocalDate.parse(it) }.getOrNull()
                    }
            _uiState.value = _uiState.value.copy(earliestDate = earliest)
            reload()
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun setScope(scope: StatsScope) {
        if (_uiState.value.scope == scope) return
        _uiState.value = _uiState.value.copy(scope = scope)
        viewModelScope.launch { reload() }
    }

    fun setRange(range: HeatmapRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range, selectedDay = null)
        viewModelScope.launch { loadHeatmap() }
    }

    fun setYear(year: Int) {
        _uiState.value = _uiState.value.copy(selectedYear = year, selectedDay = null)
        viewModelScope.launch { loadHeatmap() }
    }

    fun onDayTapped(day: HeatmapDayUi) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
    }

    private suspend fun reload() {
        loadLeaderboards()
        loadHeatmap()
    }

    private suspend fun loadLeaderboards() {
        val scopeParam = scopeParam()
        val songs =
                runCatching { statsRepository.getTopSongs(100, scopeParam).results }
                        .getOrDefault(emptyList())
        val artists =
                runCatching { statsRepository.getTopArtists(100, scopeParam).results }
                        .getOrDefault(emptyList())
        _uiState.value = _uiState.value.copy(topSongs = songs, topArtists = artists)
    }

    private suspend fun loadHeatmap() {
        _uiState.value = _uiState.value.copy(heatmapLoading = true)
        val (start, end) = computeRange()
        val response =
                runCatching {
                            statsRepository.getHeatmap(
                                    start.toString(),
                                    end.toString(),
                                    scopeParam()
                            )
                        }
                        .getOrNull()

        val countByDate =
                response?.results?.associate { LocalDate.parse(it.date) to it.count } ?: emptyMap()
        val filled = fillGaps(start, end, countByDate)
        val weeks = groupIntoWeeks(filled)

        _uiState.value = _uiState.value.copy(weeks = weeks, heatmapLoading = false)
    }

    private fun scopeParam() = if (_uiState.value.scope == StatsScope.ME) "me" else "global"

    private fun computeRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val state = _uiState.value
        return when (state.selectedRange) {
            HeatmapRange.MONTH -> today.withDayOfMonth(1) to today
            HeatmapRange.YEAR -> {
                val yearEnd = LocalDate.of(state.selectedYear, 12, 31)
                val end = if (state.selectedYear == today.year) today else yearEnd
                LocalDate.of(state.selectedYear, 1, 1) to end
            }
            HeatmapRange.ALL -> (state.earliestDate ?: today) to today
        }
    }

    private fun fillGaps(
            start: LocalDate,
            end: LocalDate,
            countByDate: Map<LocalDate, Int>
    ): List<HeatmapDayUi> {
        val days = mutableListOf<HeatmapDayUi>()
        var cursor = start
        while (!cursor.isAfter(end)) {
            days.add(HeatmapDayUi(cursor, countByDate[cursor] ?: 0))
            cursor = cursor.plusDays(1)
        }
        return days
    }

    /** Sunday-aligned weeks, same as the web version's grid derivation. */
    private fun groupIntoWeeks(days: List<HeatmapDayUi>): List<List<HeatmapDayUi>> {
        if (days.isEmpty()) return emptyList()

        val countByDate = days.associate { it.date to it.count }
        val first = days.first().date
        val last = days.last().date

        // DayOfWeek.SUNDAY = 7 in java.time; convert to 0=Sunday..6=Saturday like the web's
        // getDay().
        val firstDowFromSunday = first.dayOfWeek.value % 7
        var cursor = first.minusDays(firstDowFromSunday.toLong())

        val padded = mutableListOf<HeatmapDayUi>()
        while (!cursor.isAfter(last)) {
            padded.add(HeatmapDayUi(cursor, countByDate[cursor] ?: 0))
            cursor = cursor.plusDays(1)
        }

        return padded.chunked(7)
    }
}

fun HeatmapDayUi.formattedLong(): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
