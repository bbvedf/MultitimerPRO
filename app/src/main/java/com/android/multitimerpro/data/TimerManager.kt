package com.android.multitimerpro.data

import android.graphics.Color
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManager @Inject constructor(
    private val repository: TimerRepository
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private val _timers = MutableStateFlow<List<TimerEntity>>(emptyList())
    val timers: StateFlow<List<TimerEntity>> = _timers.asStateFlow()

    init {
        // Observe database changes and update our local state
        serviceScope.launch {
            repository.allTimers.collect { dbTimers ->
                _timers.value = dbTimers
                checkTickingState()
            }
        }
    }

    private fun checkTickingState() {
        val hasActiveTimers = _timers.value.any { it.status == "LIVE" }
        if (hasActiveTimers && tickJob == null) {
            startTicking()
        } else if (!hasActiveTimers && tickJob != null) {
            stopTicking()
        }
    }

    private fun startTicking() {
        tickJob = serviceScope.launch {
            while (isActive) {
                delay(100) // Tick every 100ms for smooth UI, but we'll only update DB less often

                _timers.update { currentList ->
                    currentList.map { timer ->
                        if (timer.status == "LIVE") {
                            val newRemaining = (timer.remainingTime - 100).coerceAtLeast(0)
                            if (newRemaining == 0L) {
                                // Timer finished!
                                val finishedTimer = timer.copy(
                                    remainingTime = 0,
                                    status = "FINISHED"
                                )
                                serviceScope.launch { repository.update(finishedTimer) }
                                finishedTimer
                            } else {
                                timer.copy(remainingTime = newRemaining)
                            }
                        } else {
                            timer
                        }
                    }
                }
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    suspend fun addTimer(name: String, durationMs: Long) {
        val newTimer = TimerEntity(
            name = name,
            duration = durationMs,
            remainingTime = durationMs,
            status = "PAUSED",
            color = Color.RED, // Default color
            category = "General"
        )
        repository.insert(newTimer)
    }

    suspend fun toggleTimer(timer: TimerEntity) {
        val newStatus = if (timer.status == "LIVE") "PAUSED" else "LIVE"
        val updatedTimer = timer.copy(status = newStatus)
        repository.update(updatedTimer)
    }

    suspend fun deleteTimer(timer: TimerEntity) {
        repository.delete(timer)
    }

    suspend fun resetTimer(timer: TimerEntity) {
        val resetTimer = timer.copy(
            remainingTime = timer.duration,
            status = "PAUSED"
        )
        repository.update(resetTimer)
    }
}
