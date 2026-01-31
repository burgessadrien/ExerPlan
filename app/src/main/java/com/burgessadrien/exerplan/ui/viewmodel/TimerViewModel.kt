package com.burgessadrien.exerplan.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.burgessadrien.exerplan.service.TimerPhase
import com.burgessadrien.exerplan.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val _timeLeft = MutableStateFlow(0L)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _setupTimeLeft = MutableStateFlow(0L)
    val setupTimeLeft: StateFlow<Long> = _setupTimeLeft.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _phaseName = MutableStateFlow("")
    val phaseName: StateFlow<String> = _phaseName.asStateFlow()

    private val _nextPhaseName = MutableStateFlow<String?>(null)
    val nextPhaseName: StateFlow<String?> = _nextPhaseName.asStateFlow()

    private val _currentPhaseIndex = MutableStateFlow(0)
    val currentPhaseIndex: StateFlow<Int> = _currentPhaseIndex.asStateFlow()

    private val _totalPhases = MutableStateFlow(0)
    val totalPhases: StateFlow<Int> = _totalPhases.asStateFlow()

    private val _upcomingPhases = MutableStateFlow<List<TimerPhase>>(emptyList())
    val upcomingPhases: StateFlow<List<TimerPhase>> = _upcomingPhases.asStateFlow()

    private var timerService: TimerService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.TimerBinder
            val s = binder.getService()
            timerService = s
            isBound = true
            
            viewModelScope.launch {
                s.timeLeft.collect { _timeLeft.value = it }
            }
            viewModelScope.launch {
                s.setupTimeLeft.collect { _setupTimeLeft.value = it }
            }
            viewModelScope.launch {
                s.isRunning.collect { _isRunning.value = it }
            }
            viewModelScope.launch {
                s.phaseName.collect { _phaseName.value = it }
            }
            viewModelScope.launch {
                s.nextPhaseName.collect { _nextPhaseName.value = it }
            }
            viewModelScope.launch {
                s.currentPhaseIndex.collect { _currentPhaseIndex.value = it }
            }
            viewModelScope.launch {
                s.totalPhases.collect { _totalPhases.value = it }
            }
            viewModelScope.launch {
                s.upcomingPhases.collect { _upcomingPhases.value = it }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            timerService = null
        }
    }

    init {
        val intent = Intent(application, TimerService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun startTimer(durationSeconds: Int, setupSeconds: Int = 0) {
        startMultiTimer(listOf(durationSeconds), listOf("Timer"), setupSeconds)
    }

    fun startMultiTimer(durationsSeconds: List<Int>, names: List<String>, setupSeconds: Int = 0) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DURATIONS_MILLIS, durationsSeconds.map { it * 1000L }.toLongArray())
            putExtra(TimerService.EXTRA_PHASE_NAMES, names.toTypedArray())
            putExtra(TimerService.EXTRA_SETUP_MILLIS, setupSeconds * 1000L)
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun addTimer(durationSeconds: Int, name: String = "Queued Timer") {
        addMultiTimer(listOf(durationSeconds), listOf(name))
    }

    fun addMultiTimer(durationsSeconds: List<Int>, names: List<String>) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_ADD
            putExtra(TimerService.EXTRA_DURATIONS_MILLIS, durationsSeconds.map { it * 1000L }.toLongArray())
            putExtra(TimerService.EXTRA_PHASE_NAMES, names.toTypedArray())
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun pauseTimer() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun resumeTimer() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun stopTimer() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        getApplication<Application>().startForegroundService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }
}
