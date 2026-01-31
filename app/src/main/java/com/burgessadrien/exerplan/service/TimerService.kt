package com.burgessadrien.exerplan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.burgessadrien.exerplan.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class TimerService : Service() {

    private val binder = TimerBinder()
    
    private val _timeLeft = MutableStateFlow(0L)
    val timeLeft = _timeLeft.asStateFlow()

    private val _setupTimeLeft = MutableStateFlow(0L)
    val setupTimeLeft = _setupTimeLeft.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _currentPhaseIndex = MutableStateFlow(0)
    val currentPhaseIndex = _currentPhaseIndex.asStateFlow()

    private val _totalPhases = MutableStateFlow(0)
    val totalPhases = _totalPhases.asStateFlow()

    private val _phaseName = MutableStateFlow("")
    val phaseName = _phaseName.asStateFlow()

    private val _nextPhaseName = MutableStateFlow<String?>(null)
    val nextPhaseName = _nextPhaseName.asStateFlow()

    private val _upcomingPhases = MutableStateFlow<List<TimerPhase>>(emptyList())
    val upcomingPhases = _upcomingPhases.asStateFlow()

    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val phaseQueue = mutableListOf<TimerPhase>()
    private var isSetupActive = false

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "timer_channel"

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_ADD = "ACTION_ADD"
        const val EXTRA_DURATIONS_MILLIS = "EXTRA_DURATIONS_MILLIS"
        const val EXTRA_PHASE_NAMES = "EXTRA_PHASE_NAMES"
        const val EXTRA_SETUP_MILLIS = "EXTRA_SETUP_MILLIS"
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durations = intent.getLongArrayExtra(EXTRA_DURATIONS_MILLIS) ?: longArrayOf()
                val names = intent.getStringArrayExtra(EXTRA_PHASE_NAMES) ?: arrayOf()
                val setup = intent.getLongExtra(EXTRA_SETUP_MILLIS, 0L)
                
                val phases = durations.zip(names.toList().ifEmpty { List(durations.size) { "" } }) { d, n ->
                    TimerPhase(d, n)
                }
                startTimer(phases, setup)
            }
            ACTION_ADD -> {
                val durations = intent.getLongArrayExtra(EXTRA_DURATIONS_MILLIS) ?: longArrayOf()
                val names = intent.getStringArrayExtra(EXTRA_PHASE_NAMES) ?: arrayOf()
                val newPhases = durations.zip(names.toList().ifEmpty { List(durations.size) { "" } }) { d, n ->
                    TimerPhase(d, n)
                }
                addTimers(newPhases)
            }
            ACTION_STOP -> stopTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
        }
        return START_NOT_STICKY
    }

    fun startTimer(phases: List<TimerPhase>, setupMillis: Long = 0L) {
        timerJob?.cancel()
        phaseQueue.clear()
        
        phaseQueue.addAll(phases)
        _totalPhases.value = phaseQueue.size
        _currentPhaseIndex.value = 0
        
        if (setupMillis > 0) {
            isSetupActive = true
            _setupTimeLeft.value = setupMillis
            _phaseName.value = "Setup"
            _timeLeft.value = phaseQueue.firstOrNull()?.durationMillis ?: 0L
            _nextPhaseName.value = if (phaseQueue.size > 1) phaseQueue[1].name else null
        } else {
            isSetupActive = false
            _setupTimeLeft.value = 0
            if (phaseQueue.isNotEmpty()) {
                val first = phaseQueue[0]
                _timeLeft.value = first.durationMillis
                _phaseName.value = first.name
                _nextPhaseName.value = if (phaseQueue.size > 1) phaseQueue[1].name else null
            } else {
                _timeLeft.value = 0
                _phaseName.value = ""
                _nextPhaseName.value = null
            }
        }

        _isRunning.value = true
        updateUpcomingPhases()
        startTimerJob()
    }

    fun addTimers(phases: List<TimerPhase>) {
        if (!_isRunning.value || (_timeLeft.value == 0L && _setupTimeLeft.value == 0L)) {
            startTimer(phases)
            return
        }
        phaseQueue.addAll(phases)
        _totalPhases.value = phaseQueue.size
        updateNextPhaseName()
        updateUpcomingPhases()
        updateNotification()
    }

    private fun updateNextPhaseName() {
        val nextIdx = _currentPhaseIndex.value + 1
        _nextPhaseName.value = if (nextIdx < phaseQueue.size) {
            phaseQueue[nextIdx].name
        } else {
            null
        }
    }

    private fun updateUpcomingPhases() {
        _upcomingPhases.value = if (_currentPhaseIndex.value + 1 < phaseQueue.size) {
            phaseQueue.subList(_currentPhaseIndex.value + 1, phaseQueue.size).toList()
        } else {
            emptyList()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _isRunning.value = false
        updateNotification()
    }

    fun resumeTimer() {
        if (_timeLeft.value > 0 || _setupTimeLeft.value > 0 || _currentPhaseIndex.value < phaseQueue.size - 1) {
            _isRunning.value = true
            startTimerJob()
        }
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, 
                NOTIFICATION_ID, 
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE 
                else 0
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        timerJob = serviceScope.launch {
            // Setup phase
            while (_setupTimeLeft.value > 0) {
                delay(1000)
                _setupTimeLeft.value -= 1000
                updateNotification()
            }
            isSetupActive = false
            
            // Re-sync phase info if transitioning from setup
            if (phaseQueue.isNotEmpty() && _currentPhaseIndex.value == 0 && _phaseName.value == "Setup") {
                val first = phaseQueue[0]
                _timeLeft.value = first.durationMillis
                _phaseName.value = first.name
                updateNextPhaseName()
                updateUpcomingPhases()
            }

            // Main phases
            while (true) {
                while (_timeLeft.value > 0) {
                    delay(1000)
                    _timeLeft.value -= 1000
                    updateNotification()
                }

                if (_currentPhaseIndex.value < phaseQueue.size - 1) {
                    _currentPhaseIndex.value++
                    val next = phaseQueue[_currentPhaseIndex.value]
                    _timeLeft.value = next.durationMillis
                    _phaseName.value = next.name
                    updateNextPhaseName()
                    updateUpcomingPhases()
                    updateNotification()
                } else {
                    break
                }
            }

            _isRunning.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timeLeft.value = 0
        _setupTimeLeft.value = 0
        _isRunning.value = false
        _currentPhaseIndex.value = 0
        _totalPhases.value = 0
        _phaseName.value = ""
        _nextPhaseName.value = null
        _upcomingPhases.value = emptyList()
        phaseQueue.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)

        val resumeIntent = Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }
        val resumePendingIntent = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, TimerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val phase = if (_setupTimeLeft.value > 0) "Setup" else _phaseName.value
        val title = if (_isRunning.value) "Timer: $phase" else "Timer Paused"
        val progress = if (_totalPhases.value > 1) " (${_currentPhaseIndex.value + 1}/${_totalPhases.value})" else ""
        val timeDisplay = if (_setupTimeLeft.value > 0) formatTime(_setupTimeLeft.value) else formatTime(_timeLeft.value)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title + progress)
            .setContentText(timeDisplay)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (_isRunning.value) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
    }
}
