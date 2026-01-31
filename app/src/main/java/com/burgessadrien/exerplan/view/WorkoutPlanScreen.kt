package com.burgessadrien.exerplan.view

// Import TimerScreen
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burgessadrien.exerplan.data.WorkoutDayWithWorkouts
import com.burgessadrien.exerplan.data.WorkoutPlanWithDays
import com.burgessadrien.exerplan.model.DayType
import com.burgessadrien.exerplan.model.PersonalBestLift
import com.burgessadrien.exerplan.model.WeightUnit
import com.burgessadrien.exerplan.model.WorkoutBlock
import com.burgessadrien.exerplan.model.WorkoutDay
import com.burgessadrien.exerplan.model.WorkoutPlan
import com.burgessadrien.exerplan.model.workout.LiftingWorkout
import com.burgessadrien.exerplan.ui.theme.LocalSpacing
import com.burgessadrien.exerplan.ui.timer.TimerScreen
import com.burgessadrien.exerplan.ui.viewmodel.AppViewModelProvider
import com.burgessadrien.exerplan.ui.viewmodel.TimerViewModel
import com.burgessadrien.exerplan.ui.viewmodel.WorkoutPlanViewModel
import com.burgessadrien.exerplan.utils.FuzzyMatcher
import com.burgessadrien.exerplan.utils.StrengthMath
import java.util.Locale
import kotlin.math.roundToInt


object Routes {
    const val PLAN_SELECTION = "plan_selection"
    const val ACTIVE_PLAN = "active_plan"
    const val WORKOUT_DETAILS_ROUTE = "workout"
    const val PERSONAL_BESTS = "personal_bests"
    const val TIMER = "timer"
    const val SETTINGS = "settings"
    fun workoutDetails(dayId: Long) = "$WORKOUT_DETAILS_ROUTE/$dayId"
}

data class ScreenNavigation(val route: String, val title: String, val icon: ImageVector)

val bottomNavItems = listOf(
    ScreenNavigation(Routes.PLAN_SELECTION, "Plans", Icons.Default.DateRange),
    ScreenNavigation(Routes.ACTIVE_PLAN, "Workout", Icons.Default.FitnessCenter),
    ScreenNavigation(Routes.TIMER, "Timer", Icons.Default.PlayArrow),
    ScreenNavigation(Routes.PERSONAL_BESTS, "Records", Icons.Default.EmojiEvents),
    ScreenNavigation(Routes.SETTINGS, "Settings", Icons.Default.Settings)
)

private const val KG_TO_LBS = 2.20462

private val DEFAULT_PR_TYPES = listOf(
    "Back Squat", "Front Squat", "Deadlift", "Bench Press", "Clean", "Clean and Jerk", "Snatch"
)

private fun Double.format(unit: WeightUnit): String {
    val value = if (unit == WeightUnit.LBS) this * KG_TO_LBS else this
    return String.format(Locale.getDefault(), "%.1f %s", value, unit.name.lowercase())
}

private fun parseRestToDurations(rest: String): List<Int> {
    val isMin = rest.contains("min", ignoreCase = true)
    val multiplier = if (isMin) 60 else 1
    
    val rangeRegex = Regex("""(\d+)\s*-\s*(\d+)""")
    val rangeMatch = rangeRegex.find(rest)
    if (rangeMatch != null) {
        val start = rangeMatch.groupValues[1].toIntOrNull() ?: 0
        val end = rangeMatch.groupValues[2].toIntOrNull() ?: 0
        if (end > start) {
            return listOf(start * multiplier, (end - start) * multiplier)
        }
        return listOf(start * multiplier)
    }

    val singleRegex = Regex("""(\d+)""")
    val singleMatch = singleRegex.find(rest)
    val value = singleMatch?.value?.toIntOrNull() ?: 0
    return listOf(value * multiplier)
}

@Composable
fun ExerPlanApp(navController: NavHostController = rememberNavController()) {
    val viewModel: WorkoutPlanViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val timerViewModel: TimerViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val workoutPlanUiState by viewModel.workoutPlanUiState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    var hasInitialNavigated by remember { mutableStateOf(false) }

    LaunchedEffect(workoutPlanUiState.workoutPlans) {
        if (!hasInitialNavigated) {
            val primaryPlan = workoutPlanUiState.workoutPlans.find { it.plan.isPrimary }
            if (primaryPlan != null) {
                navController.navigate(Routes.ACTIVE_PLAN) {
                    popUpTo(Routes.PLAN_SELECTION) { inclusive = true }
                }
            }
            hasInitialNavigated = true
        }
    }

    Scaffold(
        bottomBar = { AppBottomNavigationBar(navController) }
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.PLAN_SELECTION,
            modifier = Modifier.padding(it)
        ) {
            composable(Routes.PLAN_SELECTION) {
                PlanSelectionScreen(
                    workoutPlans = workoutPlanUiState.workoutPlans,
                    onPlanClick = { planId -> 
                        viewModel.setPrimaryPlan(planId)
                        navController.navigate(Routes.ACTIVE_PLAN) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onPlanLongClick = { planId -> viewModel.setPrimaryPlan(planId) },
                    onDeletePlan = { plan -> viewModel.deleteWorkoutPlan(plan.plan) },
                    onAddPlan = { name -> viewModel.createWorkoutPlan(name) },
                    onEditPlan = { plan, newName -> viewModel.updateWorkoutPlan(plan.copy(name = newName)) }
                )
            }
            composable(Routes.ACTIVE_PLAN) {
                val activePlan = workoutPlanUiState.workoutPlans.find { it.plan.isPrimary }
                if (activePlan != null) {
                    WorkoutDayListScreen(
                        plan = activePlan,
                        onDayClick = { dayId -> navController.navigate(Routes.workoutDetails(dayId)) },
                        onAddDay = { name, type, blockId -> viewModel.createWorkoutDay(activePlan.plan.id, name, type, blockId) },
                        onEditDay = { day -> viewModel.updateWorkoutDay(day) },
                        onDeleteDay = { day -> viewModel.deleteWorkoutDay(day) },
                        onAddBlock = { name -> viewModel.createWorkoutBlock(activePlan.plan.id, name) },
                        onUpdateBlock = { block -> viewModel.updateWorkoutBlock(block) },
                        onDeleteBlock = { block -> viewModel.deleteWorkoutBlock(block) },
                        onToggleBlock = { block -> viewModel.toggleBlockCompletion(block) }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No plan selected", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.navigate(Routes.PLAN_SELECTION) }) { 
                                Text("Choose a Plan") 
                            }
                        }
                    }
                }
            }
            composable(
                route = "${Routes.WORKOUT_DETAILS_ROUTE}/{dayId}",
                arguments = listOf(navArgument("dayId") { type = NavType.LongType })
            ) { backStackEntry ->
                val dayId = backStackEntry.arguments?.getLong("dayId")
                val day = workoutPlanUiState.workoutPlans.flatMap { it.workoutDays }.find { it.workoutDay.id == dayId }
                if (day != null) {
                    val personalBests by viewModel.personalBests.collectAsState()
                    WorkoutDayDetailScreen(
                        day = day,
                        weightUnit = workoutPlanUiState.weightUnit,
                        personalBests = personalBests,
                        onBack = { navController.popBackStack() },
                        onStartTimer = { durations, names -> 
                            timerViewModel.startMultiTimer(durations, names, userSettings.setupTimerSeconds)
                            navController.navigate(Routes.TIMER)
                        },
                        onSavePb = { pb -> viewModel.savePersonalBest(pb) },
                        onAddWorkout = { workout -> viewModel.createWorkout(day.workoutDay.id, workout) },
                        onEditWorkout = { workout -> viewModel.updateWorkout(workout) },
                        onDeleteWorkout = { workout -> viewModel.deleteWorkout(workout) },
                        onToggleCompletion = { workout -> viewModel.toggleWorkoutCompletion(workout) },
                        onToggleDayCompletion = { viewModel.updateWorkoutDay(day.workoutDay.copy(isCompleted = !day.workoutDay.isCompleted)) }
                    )
                }
            }
            composable(Routes.PERSONAL_BESTS) {
                val personalBests by viewModel.personalBests.collectAsState()
                PersonalBestsScreen(
                    personalBests = personalBests, 
                    weightUnit = workoutPlanUiState.weightUnit,
                    onAddPb = { pb -> viewModel.savePersonalBest(pb) },
                    onEditPb = { pb -> viewModel.updatePersonalBest(pb) },
                    onDeletePb = { pb -> viewModel.deletePersonalBest(pb) }
                )
            }
            composable(Routes.TIMER) {
                TimerScreen(
                    viewModel = timerViewModel,
                    onStop = { navController.popBackStack() },
                    defaultSetupSeconds = userSettings.setupTimerSeconds,
                    onUpdateSetupTimer = { seconds -> viewModel.updateUserSettings(userSettings.copy(setupTimerSeconds = seconds)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    currentUnit = workoutPlanUiState.weightUnit,
                    onUpdateUnit = { unit -> viewModel.updateWeightUnit(unit) },
                    workoutPlans = workoutPlanUiState.workoutPlans,
                    onImportExcel = { uri, targetId, name -> viewModel.importExcelPlan(navController.context, uri, targetId, name) },
                    onImportMoose = { uri, targetId, name -> viewModel.importMoosePlan(navController.context, uri, targetId, name) },
                    onImportNippard = { uri, targetId, name -> viewModel.importNippardPlan(navController.context, uri, targetId, name) },
                    setupTimerSeconds = userSettings.setupTimerSeconds,
                    onUpdateSetupTimer = { seconds -> viewModel.updateUserSettings(userSettings.copy(setupTimerSeconds = seconds)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlanSelectionScreen(
    workoutPlans: List<WorkoutPlanWithDays>,
    onPlanClick: (Long) -> Unit,
    onPlanLongClick: (Long) -> Unit,
    onDeletePlan: (WorkoutPlanWithDays) -> Unit,
    onAddPlan: (String) -> Unit,
    onEditPlan: (WorkoutPlan, String) -> Unit
) {
    val spacing = LocalSpacing.current
    var showAddDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<WorkoutPlan?>(null) }

    if (showAddDialog || planToEdit != null) {
        var name by remember { mutableStateOf(planToEdit?.name ?: "") }
        var notes by remember { mutableStateOf(planToEdit?.notes?.joinToString("\n") ?: "") }
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                planToEdit = null 
            },
            title = { Text(if (planToEdit != null) "Edit Plan" else "New Plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Plan Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Plan Notes / Goals") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val noteList = if (notes.isBlank()) emptyList() else notes.split("\n").filter { it.isNotBlank() }
                    if (planToEdit != null) {
                        onEditPlan(planToEdit!!.copy(name = name, notes = noteList), name)
                    } else {
                        onAddPlan(name)
                    }
                    showAddDialog = false
                    planToEdit = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    planToEdit = null 
                }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Plan")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(spacing.medium)) {
            Text(text = "Workout Plans", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(spacing.medium))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(workoutPlans, key = { it.plan.id }) { plan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.small)
                            .combinedClickable(
                                onClick = { onPlanClick(plan.plan.id) },
                                onLongClick = { onPlanLongClick(plan.plan.id) }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(spacing.medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = plan.plan.name, style = MaterialTheme.typography.headlineSmall)
                                if (plan.plan.isPrimary) {
                                    Text(text = "Primary Plan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(onClick = { planToEdit = plan.plan }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Plan")
                            }
                            IconButton(onClick = { onDeletePlan(plan) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Plan")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDayListScreen(
    plan: WorkoutPlanWithDays, 
    onDayClick: (Long) -> Unit,
    onAddDay: (String, DayType, Long?) -> Unit,
    onEditDay: (WorkoutDay) -> Unit,
    onDeleteDay: (WorkoutDay) -> Unit,
    onAddBlock: (String) -> Unit,
    onUpdateBlock: (WorkoutBlock) -> Unit,
    onDeleteBlock: (WorkoutBlock) -> Unit,
    onToggleBlock: (WorkoutBlock) -> Unit
) {
    val spacing = LocalSpacing.current
    var showBlockDialog by remember { mutableStateOf(false) }
    var blockToEdit by remember { mutableStateOf<WorkoutBlock?>(null) }
    var showDayDialog by remember { mutableStateOf(false) }
    var dayToEdit by remember { mutableStateOf<WorkoutDay?>(null) }
    
    var selectedBlockId by remember { mutableStateOf<Long?>(null) }

    val filteredDays = if (selectedBlockId == null) {
        plan.workoutDays
    } else {
        plan.workoutDays.filter { it.workoutDay.blockId == selectedBlockId }
    }

    val sortedBlocks = plan.blocks.sortedBy { it.isCompleted }
    val sortedDays = filteredDays.sortedWith(compareBy({ it.workoutDay.isCompleted }, { dayWithWorkouts -> 
        dayWithWorkouts.workouts.all { it.isCompleted } 
    }))

    if (showBlockDialog || blockToEdit != null) {
        var name by remember { mutableStateOf(blockToEdit?.name ?: "") }
        var notes by remember { mutableStateOf(blockToEdit?.notes?.joinToString("\n") ?: "") }
        AlertDialog(
            onDismissRequest = { 
                showBlockDialog = false
                blockToEdit = null 
            },
            title = { Text(if (blockToEdit != null) "Edit Block" else "New Block") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Block Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Block Notes / Goals") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val noteList = if (notes.isBlank()) emptyList() else notes.split("\n").filter { it.isNotBlank() }
                    if (blockToEdit != null) onUpdateBlock(blockToEdit!!.copy(name = name, notes = noteList)) else onAddBlock(name)
                    showBlockDialog = false
                    blockToEdit = null
                }) { Text("Save") }
            }
        )
    }

    if (showDayDialog || dayToEdit != null) {
        var name by remember { mutableStateOf(dayToEdit?.name ?: "") }
        var type by remember { mutableStateOf(dayToEdit?.dayType ?: DayType.WORKING) }
        var blockId by remember { mutableStateOf(dayToEdit?.blockId) }
        var blockExpanded by remember { mutableStateOf(false) }
        var typeExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                showDayDialog = false
                dayToEdit = null 
            },
            title = { Text(if (dayToEdit != null) "Edit Day" else "New Day") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Day Name") }, modifier = Modifier.fillMaxWidth())
                    
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = !typeExpanded }
                    ) {
                        OutlinedTextField(
                            value = type.name,
                            onValueChange = {}, 
                            readOnly = true,
                            label = { Text("Day Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            DayType.entries.forEach { dayType ->
                                DropdownMenuItem(
                                    text = { Text(dayType.name) },
                                    onClick = { 
                                        type = dayType
                                        typeExpanded = false 
                                    }
                                )
                            }
                        }
                    }

                    if (plan.blocks.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = blockExpanded,
                            onExpandedChange = { blockExpanded = !blockExpanded }
                        ) {
                            OutlinedTextField(
                                value = plan.blocks.find { it.id == blockId }?.name ?: "No Block",
                                onValueChange = {}, 
                                readOnly = true,
                                label = { Text("Block") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = blockExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = blockExpanded,
                                onDismissRequest = { blockExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("No Block") },
                                    onClick = { blockId = null; blockExpanded = false }
                                )
                                plan.blocks.forEach { block ->
                                    DropdownMenuItem(
                                        text = { Text(block.name) },
                                        onClick = { blockId = block.id; blockExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (dayToEdit != null) {
                        onEditDay(dayToEdit!!.copy(name = name, dayType = type, blockId = blockId))
                    } else {
                        onAddDay(name, type, blockId)
                    }
                    showDayDialog = false
                    dayToEdit = null
                }) { Text("Save") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                FloatingActionButton(onClick = { showBlockDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Add Block")
                }
                FloatingActionButton(onClick = { showDayDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Day")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(spacing.medium)) {
            Text(text = plan.plan.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            if (sortedBlocks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(spacing.small))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedBlockId == null,
                            onClick = { selectedBlockId = null },
                            label = { Text("All") }
                        )
                    }
                    items(sortedBlocks, key = { it.id }) { block ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = selectedBlockId == block.id,
                                onClick = { selectedBlockId = block.id },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(block.name)
                                        if (block.isCompleted) {
                                            Spacer(modifier = Modifier.width(spacing.extraSmall))
                                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            )
                            if (selectedBlockId == block.id) {
                                IconButton(onClick = { blockToEdit = block }) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                                }
                                Checkbox(
                                    checked = block.isCompleted,
                                    onCheckedChange = { onToggleBlock(block) }
                                )
                                IconButton(onClick = { onDeleteBlock(block) }) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
                
                // Block Notes section
                val currentBlock = plan.blocks.find { it.id == selectedBlockId }
                if (currentBlock != null && currentBlock.notes.isNotEmpty()) {
                    var showNotes by remember(selectedBlockId) { mutableStateOf(true) }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.small),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(spacing.medium)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showNotes = !showNotes }) {
                                Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(spacing.small))
                                Text("Block Goals & Notes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            AnimatedVisibility(visible = showNotes) {
                                Text(
                                    text = currentBlock.notes.joinToString("\n"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(top = spacing.small)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.medium))
            LazyColumn {
                items(sortedDays, key = { it.workoutDay.id }) { day ->
                    val isDayComplete = day.workoutDay.isCompleted || (day.workouts.isNotEmpty() && day.workouts.all { it.isCompleted })
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = spacing.small)
                            .clickable { onDayClick(day.workoutDay.id) },
                        colors = if (isDayComplete) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) else CardDefaults.cardColors()
                    ) {
                        Row(
                            modifier = Modifier.padding(spacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = day.workoutDay.name, style = MaterialTheme.typography.headlineSmall)
                                    if (isDayComplete) {
                                        Spacer(modifier = Modifier.width(spacing.small))
                                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                day.workoutDay.blockId?.let { bid ->
                                    plan.blocks.find { it.id == bid }?.let { block ->
                                        Text(text = block.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                if (day.workouts.isNotEmpty()) {
                                    Text(
                                        text = day.workouts.joinToString(", ") { it.exerciseName },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (day.workoutDay.dayType == DayType.REST) {
                                Icon(Icons.Default.Hotel, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { dayToEdit = day.workoutDay }) { Icon(Icons.Default.Edit, null) }
                            IconButton(onClick = { onDeleteDay(day.workoutDay) }) { Icon(Icons.Default.Delete, null) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutDayDetailScreen(
    day: WorkoutDayWithWorkouts,
    weightUnit: WeightUnit,
    personalBests: List<PersonalBestLift>,
    onBack: () -> Unit,
    onStartTimer: (List<Int>, List<String>) -> Unit,
    onSavePb: (PersonalBestLift) -> Unit,
    onAddWorkout: (LiftingWorkout) -> Unit,
    onEditWorkout: (LiftingWorkout) -> Unit,
    onDeleteWorkout: (LiftingWorkout) -> Unit,
    onToggleCompletion: (LiftingWorkout) -> Unit,
    onToggleDayCompletion: () -> Unit
) {
    val spacing = LocalSpacing.current
    var showWorkoutDialog by remember { mutableStateOf<LiftingWorkout?>(null) }
    var isAdding by remember { mutableStateOf(false) }
    var workoutToMarkDone by remember { mutableStateOf<LiftingWorkout?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

    if (showWorkoutDialog != null || isAdding) {
        WorkoutEditDialog(
            workout = showWorkoutDialog ?: LiftingWorkout(exerciseName = "", sets = 0, warmUpSets = 0, workingSets = 0, reps = 0, load = null, rpe = "", rest = "", notes = ""),
            weightUnit = weightUnit,
            onConfirm = { 
                if (isAdding) onAddWorkout(it) else onEditWorkout(it)
                showWorkoutDialog = null
                isAdding = false
            },
            onDismiss = { 
                showWorkoutDialog = null
                isAdding = false 
            }
        )
    }

    if (workoutToMarkDone != null) {
        val pbNames = (personalBests.map { it.exerciseName } + DEFAULT_PR_TYPES).distinct()
        val bestMatchName = FuzzyMatcher.findBestMatch(workoutToMarkDone!!.exerciseName, pbNames) ?: workoutToMarkDone!!.exerciseName
        
        val existingPb = personalBests.find { it.exerciseName == bestMatchName && it.repCount == workoutToMarkDone!!.reps }
        
        WorkoutCompletionDialog(
            workout = workoutToMarkDone!!,
            suggestedName = bestMatchName,
            weightUnit = weightUnit,
            existingPb = existingPb,
            onConfirm = { pb, isSuccess, shouldSavePb ->
                if (isSuccess && shouldSavePb) onSavePb(pb)
                onToggleCompletion(workoutToMarkDone!!)
                workoutToMarkDone = null
            },
            onDismiss = { workoutToMarkDone = null }
        )
    }

    val sortedWorkouts = day.workouts.sortedBy { it.isCompleted }

    Scaffold(
        floatingActionButton = {
            if (day.workoutDay.dayType == DayType.WORKING) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    FloatingActionButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(if (isEditMode) Icons.Default.CheckCircle else Icons.Default.Edit, contentDescription = "Toggle Edit Mode")
                    }
                    if (isEditMode) {
                        FloatingActionButton(onClick = { isAdding = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Workout")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(spacing.small))
                Text(text = day.workoutDay.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(spacing.medium))
            if (day.workoutDay.dayType == DayType.REST) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Hotel, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(spacing.medium))
                        Text("Rest Day", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
                        Text("Recover and grow!")
                        Spacer(modifier = Modifier.height(spacing.large))
                        Button(
                            onClick = onToggleDayCompletion,
                            colors = if (day.workoutDay.isCompleted) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
                        ) {
                            Icon(if (day.workoutDay.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle, null)
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(if (day.workoutDay.isCompleted) "Completed" else "Mark as Complete")
                        }
                    }
                }
            } else {
                LazyColumn {
                    items(sortedWorkouts, key = { it.id }) { workout ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                WorkoutItemCard(
                                    workout = workout,
                                    weightUnit = weightUnit,
                                    personalBests = personalBests,
                                    onStartTimer = onStartTimer,
                                    onToggleDone = { workoutToMarkDone = workout }
                                )
                            }
                            if (isEditMode) {
                                Column {
                                    IconButton(onClick = { showWorkoutDialog = workout }) { Icon(Icons.Default.Edit, null) }
                                    IconButton(onClick = { onDeleteWorkout(workout) }) { Icon(Icons.Default.Delete, null) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutEditDialog(
    workout: LiftingWorkout,
    weightUnit: WeightUnit,
    onConfirm: (LiftingWorkout) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    var name by remember { mutableStateOf(workout.exerciseName) }
    var sets by remember { mutableStateOf(workout.sets.toString()) }
    var warmups by remember { mutableStateOf(workout.warmUpSets.toString()) }
    var reps by remember { mutableStateOf(workout.reps?.toString() ?: "") }
    var time by remember { mutableStateOf(workout.time ?: "") }
    
    val initialLoadValue = workout.load?.let { if (weightUnit == WeightUnit.LBS) it * KG_TO_LBS else it }
    var loadInput by remember { mutableStateOf(initialLoadValue?.toString() ?: "") }
    
    var rpe by remember { mutableStateOf(workout.rpe) }
    var rest by remember { mutableStateOf(workout.rest) }
    var notes by remember { mutableStateOf(workout.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (workout.id == 0L) "New Workout" else "Edit Workout") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Sets") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (e.g. 1:00)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = warmups, onValueChange = { warmups = it }, label = { Text("Warmups") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        OutlinedTextField(
                            value = loadInput,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) loadInput = it },
                            label = { Text("Load ($weightUnit)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(value = rpe, onValueChange = { rpe = it }, label = { Text("RPE") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(value = rest, onValueChange = { rest = it }, label = { Text("Rest (e.g. 2min)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val inputVal = loadInput.toDoubleOrNull()
                val loadInKg = if (inputVal != null) {
                    if (weightUnit == WeightUnit.LBS) inputVal / KG_TO_LBS else inputVal
                } else null

                onConfirm(workout.copy(
                    exerciseName = name,
                    sets = sets.toIntOrNull() ?: 0,
                    warmUpSets = warmups.toIntOrNull() ?: 0,
                    reps = reps.toIntOrNull(),
                    time = time.ifBlank { null },
                    load = loadInKg,
                    rpe = rpe,
                    rest = rest,
                    notes = notes
                ))
            }) { Text("Save") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCompletionDialog(
    workout: LiftingWorkout,
    suggestedName: String,
    weightUnit: WeightUnit,
    existingPb: PersonalBestLift?,
    onConfirm: (PersonalBestLift, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    val initialLoadValue = if (weightUnit == WeightUnit.LBS) (workout.load ?: 0.0) * KG_TO_LBS else (workout.load ?: 0.0)
    var loadInput by remember { mutableStateOf(String.format(Locale.getDefault(), "%.1f", initialLoadValue)) }
    var reps by remember { mutableStateOf(workout.reps?.toString() ?: "") }
    var name by remember { mutableStateOf(suggestedName) }
    var isSuccess by remember { mutableStateOf(true) }
    var resultExpanded by remember { mutableStateOf(false) }
    var nameExpanded by remember { mutableStateOf(false) }

    fun getCurrentInputLoadKg(): Double {
        val inputVal = loadInput.toDoubleOrNull() ?: 0.0
        return if (weightUnit == WeightUnit.LBS) inputVal / KG_TO_LBS else inputVal
    }

    val isNewPb = isSuccess && (existingPb == null || getCurrentInputLoadKg() > existingPb.load)

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Complete Workout") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = nameExpanded,
                        onExpandedChange = { nameExpanded = !nameExpanded }
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Exercise Name (for Records)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = nameExpanded,
                            onDismissRequest = { nameExpanded = false }
                        ) {
                            DEFAULT_PR_TYPES.forEach { prType ->
                                DropdownMenuItem(
                                    text = { Text(prType) },
                                    onClick = { 
                                        name = prType
                                        nameExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    ExposedDropdownMenuBox(
                        expanded = resultExpanded,
                        onExpandedChange = { resultExpanded = !resultExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (isSuccess) "Success" else "Failure",
                            onValueChange = {}, 
                            readOnly = true,
                            label = { Text("Result") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resultExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = resultExpanded,
                            onDismissRequest = { resultExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Success") },
                                onClick = { isSuccess = true; resultExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Failure") },
                                onClick = { isSuccess = false; resultExpanded = false }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = loadInput,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) loadInput = it },
                        label = { Text("Actual Load (${weightUnit.name.lowercase()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = reps, 
                        onValueChange = { if (it.all { c -> c.isDigit() }) reps = it }, 
                        label = { Text("Reps") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (isNewPb) {
                    item {
                        Text(text = "New Personal Record detected!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    PersonalBestLift(
                        exerciseName = name,
                        load = getCurrentInputLoadKg(),
                        repCount = reps.toIntOrNull() ?: 0
                    ),
                    isSuccess,
                    isNewPb
                )
            }) { Text("Confirm Done") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalBestsScreen(
    personalBests: List<PersonalBestLift>, 
    weightUnit: WeightUnit,
    onAddPb: (PersonalBestLift) -> Unit,
    onEditPb: (PersonalBestLift) -> Unit,
    onDeletePb: (PersonalBestLift) -> Unit
) {
    val spacing = LocalSpacing.current
    var pbToEdit by remember { mutableStateOf<PersonalBestLift?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog || pbToEdit != null) {
        val initialLoadValue = if (pbToEdit != null) {
            if (weightUnit == WeightUnit.LBS) pbToEdit!!.load * KG_TO_LBS else pbToEdit!!.load
        } else 0.0
        
        var loadInput by remember { mutableStateOf(if (pbToEdit != null) String.format(Locale.getDefault(), "%.1f", initialLoadValue) else "") }
        var reps by remember { mutableStateOf(pbToEdit?.repCount?.toString() ?: "") }
        var name by remember { mutableStateOf(pbToEdit?.exerciseName ?: "") }
        var nameExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                pbToEdit = null
                showAddDialog = false
            },
            title = { Text(if (pbToEdit != null) "Edit Record" else "Add Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    ExposedDropdownMenuBox(
                        expanded = nameExpanded,
                        onExpandedChange = { nameExpanded = !nameExpanded }
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Exercise Name") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = nameExpanded,
                            onDismissRequest = { nameExpanded = false }
                        ) {
                            val recommendations = (DEFAULT_PR_TYPES + personalBests.map { it.exerciseName }).distinct()
                            recommendations.filter { it.contains(name, ignoreCase = true) }.forEach { recommendation ->
                                DropdownMenuItem(
                                    text = { Text(recommendation) },
                                    onClick = { 
                                        name = recommendation
                                        nameExpanded = false 
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = loadInput,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it.endsWith(".")) loadInput = it },
                        label = { Text("Load (${weightUnit.name.lowercase()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reps, 
                        onValueChange = { if (it.all { c -> c.isDigit() }) reps = it }, 
                        label = { Text("Reps") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val inputVal = loadInput.toDoubleOrNull() ?: 0.0
                    val loadInKg = if (weightUnit == WeightUnit.LBS) inputVal / KG_TO_LBS else inputVal
                    val pb = PersonalBestLift(
                        id = pbToEdit?.id ?: 0L,
                        exerciseName = name,
                        load = loadInKg,
                        repCount = reps.toIntOrNull() ?: 0
                    )
                    if (pbToEdit != null) onEditPb(pb) else onAddPb(pb)
                    pbToEdit = null
                    showAddDialog = false
                }) { Text("Save") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Record")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(spacing.medium)) {
            Text(text = "Personal Records", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(spacing.medium))
            
            if (personalBests.isEmpty()) {
                Text("No records yet.")
            } else {
                val (oneRepMaxes, repMaxes) = personalBests.partition { it.repCount == 1 }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    if (oneRepMaxes.isNotEmpty()) {
                        item {
                            Text(
                                text = "1 Rep Maxes (1RM)", 
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(oneRepMaxes.sortedByDescending { it.load }, key = { "1rm_${it.id}" }) { lift ->
                            PersonalBestItem(
                                lift = lift,
                                weightUnit = weightUnit,
                                onEditPb = onEditPb,
                                onDeletePb = onDeletePb
                            )
                        }
                        item { 
                            Spacer(modifier = Modifier.height(spacing.small)) 
                        }
                    }

                    if (repMaxes.isNotEmpty()) {
                        item {
                            Text(text = "Other Rep Records", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        items(repMaxes.sortedWith(compareByDescending<PersonalBestLift> { it.repCount }.thenByDescending { it.load }), key = { "rm_${it.id}" }) { lift ->
                            PersonalBestItem(
                                lift = lift,
                                weightUnit = weightUnit,
                                onEditPb = onEditPb,
                                onDeletePb = onDeletePb
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalBestItem(
    lift: PersonalBestLift,
    weightUnit: WeightUnit,
    onEditPb: (PersonalBestLift) -> Unit,
    onDeletePb: (PersonalBestLift) -> Unit
) {
    val spacing = LocalSpacing.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(spacing.medium), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(lift.exerciseName, fontWeight = FontWeight.Bold)
                Text("Reps: ${lift.repCount}")
            }
            Text(lift.load.format(weightUnit), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onEditPb(lift) }) { Icon(Icons.Default.Edit, null) }
            IconButton(onClick = { onDeletePb(lift) }) { Icon(Icons.Default.Delete, null) }
        }
    }
}

@Composable
fun WorkoutItemCard(
    workout: LiftingWorkout,
    weightUnit: WeightUnit,
    personalBests: List<PersonalBestLift>,
    onStartTimer: (List<Int>, List<String>) -> Unit,
    onToggleDone: () -> Unit
) {
    val spacing = LocalSpacing.current
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    val valueStyle = MaterialTheme.typography.bodySmall
    
    val displayLoad: String? = run {
        if (workout.load == 0.0) {
            "BW"
        } 
        else if (workout.load != null && workout.load > 0) {
            workout.load.format(weightUnit)
        } 
        else {
            val targetRpe = StrengthMath.parseRpe(workout.rpe)
            val targetReps = workout.reps
            if (targetRpe != null && targetReps != null) {
                val pbNames = (personalBests.map { it.exerciseName } + DEFAULT_PR_TYPES).distinct()
                val bestMatchName = FuzzyMatcher.findBestMatch(workout.exerciseName, pbNames) 

                if (bestMatchName != null) {
                    val matchingPbs = personalBests.filter { it.exerciseName == bestMatchName }
                    if (matchingPbs.isNotEmpty()) {
                        val maxOneRepMax = matchingPbs.maxOf { StrengthMath.calculateOneRepMax(it.load, it.repCount) }
                        val estimatedLoad = StrengthMath.estimateLoad(maxOneRepMax, targetReps, targetRpe)
                        "Est. ${estimatedLoad.format(weightUnit)}"
                    } else null
                } else null 
            } else null 
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.small),
        colors = if (workout.isCompleted) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleDone) {
                Icon(
                    imageVector = if (workout.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = "Mark done",
                    tint = if (workout.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.exerciseName, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Bold,
                    color = if (workout.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                if (workout.warmUpSets > 0 || workout.sets > 0) {
                    Row {
                        if (workout.warmUpSets > 0) {
                            Text(text = "Warmup: ", style = labelStyle)
                            Text(text = "${workout.warmUpSets}", style = valueStyle)
                            Spacer(modifier = Modifier.width(spacing.medium))
                        }
                        if (workout.sets > 0) {
                            Text(text = "Sets: ", style = labelStyle)
                            Text(text = "${workout.sets}", style = valueStyle)
                        }
                    }
                }
                if ((displayLoad != null) || (workout.rpe.isNotBlank() && workout.rpe != "N/A")) {
                    Row {
                        if (displayLoad != null) {
                            Text(text = "Load: ", style = labelStyle)
                            Text(text = displayLoad, style = valueStyle)
                            Spacer(modifier = Modifier.width(spacing.medium))
                        }
                        if (workout.rpe.isNotBlank() && workout.rpe != "N/A") {
                            Text(text = "RPE: ", style = labelStyle)
                            Text(text = workout.rpe, style = valueStyle)
                        }
                    }
                }
                if (workout.reps != null || workout.time != null) {
                    Row {
                        if (workout.reps != null) {
                            Text(text = "Reps: ", style = labelStyle)
                            Text(text = "${workout.reps}", style = valueStyle)
                            Spacer(modifier = Modifier.width(spacing.medium))
                        }
                        if (workout.time != null) {
                            Text(text = "Time: ", style = labelStyle)
                            Text(text = workout.time, style = valueStyle)
                        }
                    }
                }
                if (workout.rest.isNotBlank() && workout.rest != "N/A") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Rest: ", style = labelStyle)
                        Text(text = workout.rest, style = valueStyle)
                        IconButton(
                            onClick = { 
                                val durations = parseRestToDurations(workout.rest)
                                val names = if (durations.size > 1) {
                                    listOf("Minimum Rest", "Maximum Rest Extension")
                                } else {
                                    listOf("Rest")
                                }
                                onStartTimer(durations, names) 
                            },
                            modifier = Modifier.size(20.dp).padding(start = spacing.extraSmall)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start Rest Timer", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (workout.notes.isNotBlank() && workout.notes != "N/A") {
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(text = "Notes:", style = labelStyle)
                    Text(text = workout.notes, style = valueStyle)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUnit: WeightUnit,
    onUpdateUnit: (WeightUnit) -> Unit,
    workoutPlans: List<WorkoutPlanWithDays>,
    onImportExcel: (Uri, Long?, String?) -> Unit,
    onImportMoose: (Uri, Long?, String?) -> Unit,
    onImportNippard: (Uri, Long?, String?) -> Unit,
    setupTimerSeconds: Int,
    onUpdateSetupTimer: (Int) -> Unit
) {
    val spacing = LocalSpacing.current
    var expanded by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var importType by remember { mutableStateOf<String?>(null) } 

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            importType = "EXCEL"
        }
    }

    val mooseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            importType = "MOOSE"
        }
    }

    val nippardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            importType = "NIPPARD"
        }
    }

    if (selectedUri != null && importType != null) {
        ImportConfigurationDialog(
            uri = selectedUri!!,
            workoutPlans = workoutPlans,
            onConfirm = { targetId, name ->
                when (importType) {
                    "EXCEL" -> onImportExcel(selectedUri!!, targetId, name)
                    "MOOSE" -> onImportMoose(selectedUri!!, targetId, name)
                    "NIPPARD" -> onImportNippard(selectedUri!!, targetId, name)
                }
                selectedUri = null
                importType = null
            },
            onDismiss = {
                selectedUri = null
                importType = null
            }
        )
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(spacing.medium)) {
            Text(text = "Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(spacing.large))
            
            Text(text = "General", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(spacing.small))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Weight Unit", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(text = "Select preferred measurement system", style = MaterialTheme.typography.bodySmall)
                        }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = currentUnit.name,
                                onValueChange = {}, 
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().width(120.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                WeightUnit.entries.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit.name) },
                                        onClick = { 
                                            onUpdateUnit(unit)
                                            expanded = false 
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(spacing.large))
                    
                    Column {
                        Text(text = "Setup Timer: $setupTimerSeconds seconds", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Delay before the rest timer starts", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = setupTimerSeconds.toFloat(),
                            onValueChange = { onUpdateSetupTimer(it.roundToInt()) },
                            valueRange = 0f..30f,
                            steps = 30
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.large))
            Text(text = "Importers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(spacing.small))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    Text(text = "Standard Import", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { excelLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.extraSmall)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(spacing.small))
                        Text("Excel Importer")
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))
                    
                    Text(text = "Nippard Programming", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { nippardLauncher.launch("text/comma-separated-values") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.extraSmall)
                    ) {
                        Text("Jeff Nippard CSV Importer")
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))

                    Text(text = "Moose Coaching", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { mooseLauncher.launch("text/comma-separated-values") },
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(align = Alignment.CenterVertically),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(
                            "Power Building 3.0 Importer",
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportConfigurationDialog(
    uri: Uri,
    workoutPlans: List<WorkoutPlanWithDays>,
    onConfirm: (Long?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    var importAsNewPlan by remember { mutableStateOf(true) }
    var selectedPlanId by remember { mutableStateOf<Long?>(workoutPlans.firstOrNull()?.plan?.id) }
    var customName by remember { mutableStateOf(uri.lastPathSegment?.substringBeforeLast(".") ?: "Imported Plan") }
    var planExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = !modeExpanded }
                ) {
                    OutlinedTextField(
                        value = if (importAsNewPlan) "Create New Workout Plan" else "Add as Block to Existing Plan",
                        onValueChange = {}, 
                        readOnly = true,
                        label = { Text("Import Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false }
                        ) {
                        DropdownMenuItem(
                            text = { Text("Create New Workout Plan") },
                            onClick = { 
                                importAsNewPlan = true
                                modeExpanded = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add as Block to Existing Plan") },
                            onClick = { 
                                importAsNewPlan = false
                                modeExpanded = false 
                            },
                            enabled = workoutPlans.isNotEmpty()
                        )
                    }
                }

                if (!importAsNewPlan) {
                    ExposedDropdownMenuBox(
                        expanded = planExpanded,
                        onExpandedChange = { planExpanded = !planExpanded }
                    ) {
                        OutlinedTextField(
                            value = workoutPlans.find { it.plan.id == selectedPlanId }?.plan?.name ?: "Select Plan",
                            onValueChange = {}, 
                            readOnly = true,
                            label = { Text("Target Plan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = planExpanded,
                            onDismissRequest = { planExpanded = false }
                        ) {
                            workoutPlans.forEach { planWithDays ->
                                DropdownMenuItem(
                                    text = { Text(planWithDays.plan.name) },
                                    onClick = {
                                        selectedPlanId = planWithDays.plan.id
                                        planExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text(if (importAsNewPlan) "Plan Name" else "Block Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(if (importAsNewPlan) null else selectedPlanId, customName)
            }) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AppBottomNavigationBar(navController: NavController) {
    NavigationBar {
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route || (screen.route == Routes.ACTIVE_PLAN && currentRoute?.startsWith(Routes.WORKOUT_DETAILS_ROUTE) == true)
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = isSelected,
                onClick = { 
                    navController.navigate(screen.route) { 
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
