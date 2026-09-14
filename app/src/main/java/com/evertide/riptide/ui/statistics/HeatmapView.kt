package com.evertide.riptide.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Violet300 = Color(0xFFC4B5FD)
private val Violet500 = Color(0xFF8B5CF6)
private val Violet700 = Color(0xFF6D28D9)
private val Violet900 = Color(0xFF4C1D95)

private fun intensityColor(count: Int, zeroColor: Color): Color =
        when {
            count == 0 -> zeroColor
            count < 3 -> Violet300
            count < 6 -> Violet500
            count < 10 -> Violet700
            else -> Violet900
        }

@Composable
fun HeatmapView(
        weeks: List<List<HeatmapDayUi>>,
        onDayClick: (HeatmapDayUi) -> Unit,
        modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val zeroColor = MaterialTheme.colorScheme.surfaceVariant

    LaunchedEffect(weeks) { if (weeks.isNotEmpty()) scrollState.scrollTo(scrollState.maxValue) }

    Row(
            modifier =
                    modifier.horizontalScroll(scrollState)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    Box(
                            modifier =
                                    Modifier.size(11.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(intensityColor(day.count, zeroColor))
                                            .clickable { onDayClick(day) },
                    )
                }
            }
        }
    }
}
