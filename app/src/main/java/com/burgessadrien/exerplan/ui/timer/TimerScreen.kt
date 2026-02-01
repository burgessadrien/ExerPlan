package com.burgessadrien.exerplan.ui.timer

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.burgessadrien.exerplan.service.TimerPhase
import com.burgessadrien.exerplan.ui.viewmodel.TimerViewModel
import java.util.Locale

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    defaultSetupSeconds: Int = 0,
    onUpdateSetupTimer: (Int) -> Unit = {}
) {
    val isRunning by viewModel.isRunning.collectAsState()
    val timeLeftMillis by viewModel.timeLeft.collectAsState()
    val setupTimeLeftMillis by viewModel.setupTimeLeft.collectAsState()
    val currentPhase by viewModel.phaseName.collectAsState()
    val currentPhaseIndex by viewModel.currentPhaseIndex.collectAsState()
    val totalPhases by viewModel.totalPhases.collectAsState()
    val upcomingPhases by viewModel.upcomingPhases.collectAsState()

    val isTimerActive = timeLeftMillis > 0 || setupTimeLeftMillis > 0 || isRunning
    val isSetupActive = setupTimeLeftMillis > 0
    val displayTimeMillis = if (isSetupActive) setupTimeLeftMillis else timeLeftMillis
    val currentTimeSeconds = (displayTimeMillis / 1000).toInt()

    var selectedMinutes by remember { mutableIntStateOf(2) }
    var selectedSeconds by remember { mutableIntStateOf(0) }
    var selectedSetupSeconds by remember { mutableIntStateOf(defaultSetupSeconds) }
    var selectedGapSeconds by remember { mutableIntStateOf(0) }
    
    var localQueue by remember { mutableStateOf(listOf<TimerPhase>()) }

    LaunchedEffect(defaultSetupSeconds) {
        selectedSetupSeconds = defaultSetupSeconds
    }

    LaunchedEffect(isTimerActive) {
        if (isTimerActive) localQueue = emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (isTimerActive) {
            // Active Timer near the top
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isSetupActive) "Get Ready!" else if (totalPhases > 1) "$currentPhase (${currentPhaseIndex + 1}/$totalPhases)" else currentPhase,
                style = MaterialTheme.typography.headlineSmall,
                color = if (isSetupActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Text(
                text = formatTime(currentTimeSeconds),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 110.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(
                    onClick = { if (isRunning) viewModel.pauseTimer() else viewModel.resumeTimer() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { viewModel.stopTimer() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            Text(
                "Timer",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // Upcoming Queue Section (scrollable in the middle)
        val displayQueue = if (isTimerActive) upcomingPhases else localQueue
        if (displayQueue.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(if (isTimerActive) "Queue" else "Timer Sequence", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(displayQueue) { phase ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (phase.name == "Gap") "Gap: ${formatTime((phase.durationMillis / 1000).toInt())}" else formatTime((phase.durationMillis / 1000).toInt()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (phase.name == "Gap") MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f) else Color.Gray
                        )
                    }
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        // Configuration and Selection UI at the bottom
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            Spacer(Modifier.height(16.dp))

            if (!isTimerActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Get Ready delay:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        NumberWheelPicker(
                            value = selectedSetupSeconds,
                            onValueChange = { 
                                selectedSetupSeconds = it
                                onUpdateSetupTimer(it)
                            },
                            range = 0..60,
                            modifier = Modifier.height(80.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Gap between:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        NumberWheelPicker(
                            value = selectedGapSeconds,
                            onValueChange = { selectedGapSeconds = it },
                            range = 0..60,
                            modifier = Modifier.height(80.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text(
                text = if (isTimerActive || localQueue.isNotEmpty()) "Add to Queue" else "Duration",
                style = MaterialTheme.typography.titleMedium,
                color = if (isTimerActive || localQueue.isNotEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.height(120.dp)
            ) {
                NumberWheelPicker(
                    value = selectedMinutes,
                    onValueChange = { selectedMinutes = it },
                    range = 0..59
                )
                Text(":", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(horizontal = 16.dp))
                NumberWheelPicker(
                    value = selectedSeconds,
                    onValueChange = { selectedSeconds = it },
                    range = 0..59
                )
            }

            Spacer(Modifier.height(16.dp))

            val totalInputSecs = selectedMinutes * 60 + selectedSeconds
            
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (totalInputSecs > 0) {
                            if (isTimerActive) {
                                viewModel.addTimer(totalInputSecs)
                            } else {
                                // Start with current wheels + existing queue
                                val finalQueue = if (localQueue.isEmpty()) {
                                    listOf(TimerPhase(totalInputSecs * 1000L, "Timer"))
                                } else {
                                    val q = localQueue.toMutableList()
                                    if (selectedGapSeconds > 0) {
                                        q.add(TimerPhase(selectedGapSeconds * 1000L, "Gap"))
                                    }
                                    q.add(TimerPhase(totalInputSecs * 1000L, "Timer"))
                                    q
                                }
                                viewModel.startMultiTimer(
                                    finalQueue.map { (it.durationMillis / 1000).toInt() },
                                    finalQueue.map { it.name },
                                    selectedSetupSeconds
                                )
                            }
                        } else if (localQueue.isNotEmpty() && !isTimerActive) {
                            // Start existing queue if wheels are at 0
                            viewModel.startMultiTimer(
                                localQueue.map { (it.durationMillis / 1000).toInt() },
                                localQueue.map { it.name },
                                selectedSetupSeconds
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    val icon = if (isTimerActive) Icons.Default.Add else Icons.Default.PlayArrow
                    val text = if (isTimerActive) "Queue Timer" else if (localQueue.isNotEmpty() && totalInputSecs == 0) "Start Queue" else "Start Timer"
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text, fontWeight = FontWeight.Bold)
                }

                if (!isTimerActive && totalInputSecs > 0) {
                    FilledTonalIconButton(
                        onClick = {
                            val newQueue = localQueue.toMutableList()
                            if (newQueue.isNotEmpty() && selectedGapSeconds > 0) {
                                newQueue.add(TimerPhase(selectedGapSeconds * 1000L, "Gap"))
                            }
                            newQueue.add(TimerPhase(totalInputSecs * 1000L, "Timer"))
                            localQueue = newQueue
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add to local queue")
                    }
                }
            }

            if (!isTimerActive) {
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    listOf(60, 90, 120, 180).forEach { seconds ->
                        AssistChip(
                            onClick = {
                                selectedMinutes = seconds / 60
                                selectedSeconds = seconds % 60
                            },
                            label = { Text("${seconds}s") }
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun NumberWheelPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    val items = range.toList()
    val itemHeight = 40.dp
    val visibleItems = 3
    val state = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)

    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val centerIndex = state.firstVisibleItemIndex
            if (centerIndex in items.indices) {
                onValueChange(items[centerIndex])
            }
        }
    }

    LaunchedEffect(value) {
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != value) {
            state.scrollToItem(value)
        }
    }

    Box(modifier = modifier.width(60.dp), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = state,
            flingBehavior = flingBehavior,
            modifier = Modifier.height(itemHeight * visibleItems),
            contentPadding = PaddingValues(vertical = itemHeight),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items) { num ->
                val isSelected = num == value
                Box(
                    modifier = Modifier.height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", num),
                        style = if (isSelected) {
                            MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.bodyLarge.copy(color = Color.Gray.copy(alpha = 0.4f))
                        }
                    )
                }
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
}
