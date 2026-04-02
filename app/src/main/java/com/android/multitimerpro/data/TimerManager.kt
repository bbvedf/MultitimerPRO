package com.android.multitimerpro.data

import android.content.Context
import android.content.Intent
import com.android.multitimerpro.data.local.TimerEntity
import com.android.multitimerpro.service.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManager @Inject constructor(
    private val repository: TimerRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _timers = MutableStateFlow<List<TimerEntity>>(emptyList())
    val timers: StateFlow<List<TimerEntity>> = _timers.asStateFlow()

    init {
        scope.launch {
            repository.getAllTimers().collect {
                _timers.value = it
            }
        }
        
        // Timer tick loop
        scope.launch {
            while (isActive) {
                val currentTimers = _timers.value
                val anyRunning = currentTimers.any { it.isRunning }
                
                if (anyRunning) {
                    val updatedList = currentTimers.map { timer ->
                        if (timer.isRunning && timer.remainingTimeMs > 0) {
                            val now = System.currentTimeMillis()
                            val diff = now - timer.lastUpdatedMs
                            val newRemaining = (timer.remainingTimeMs - diff).coerceAtLeast(0)
                            
                            timer.copy(
                                remainingTimeMs = newRemaining,
                                lastUpdatedMs = now,
                                isCompleted = newRemaining == 0L
                            )
                        } else {
                            timer
                        }
                    }
                    _timers.value = updatedList
                }
                delay(100) // 100ms precision for animations
            }
        }
    }

    suspend fun addTimer(name: String, timeMs: Long) {
        repository.addTimer(name, timeMs)
    }

    suspend fun toggleTimer(timer: TimerEntity) {
        val now = System.currentTimeMillis()
        val newState = !timer.isRunning
        
        // Update persistent state
        repository.updateTimer(timer.copy(
            isRunning = newState,
            lastUpdatedMs = now
        ))
        
        checkServiceState()
    }

    suspend fun resetTimer(timer: TimerEntity) {
        repository.updateTimer(timer.copy(
            remainingTimeMs = timer.initialTimeMs,
            isRunning = false,
            isCompleted = false,
            lastUpdatedMs = System.currentTimeMillis()
        ))
        checkServiceState()
    }

    suspend fun deleteTimer(timer: TimerEntity) {
        repository.deleteTimer(timer)
        checkServiceState()
    }

    private fun checkServiceState() {
        val anyRunning = _timers.value.any { it.isRunning }
        val intent = Intent(context, TimerService::class.java).apply {
            action = if (anyRunning) TimerService.ACTION_START else TimerService.ACTION_STOP
        }
        if (anyRunning) {
            context.startForegroundService(intent)
        } else {
            context.stopService(intent)
        }
    }
}
