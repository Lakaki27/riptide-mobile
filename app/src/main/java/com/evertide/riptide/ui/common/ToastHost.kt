package com.evertide.riptide.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ToastEntry(val id: Long, val message: String)

@Composable
fun ToastHost(bus: ToastBus) {
    val toasts = remember { mutableStateListOf<ToastEntry>() }
    var nextId by remember { mutableStateOf(0L) }

    LaunchedEffect(bus) {
        bus.messages.collect { message ->
            val id = nextId++
            toasts.add(ToastEntry(id, message))
            launch {
                delay(1800)
                toasts.removeAll { it.id == id }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
                modifier = Modifier.padding(bottom = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            toasts.forEach { toast ->
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                            modifier =
                                    Modifier.background(
                                                    MaterialTheme.colorScheme.onBackground,
                                                    shape =
                                                            androidx.compose.foundation.shape
                                                                    .RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text(toast.message, color = MaterialTheme.colorScheme.background) }
                }
            }
        }
    }
}
