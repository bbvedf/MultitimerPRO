package com.android.multitimerpro.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.multitimerpro.data.TimerEntity
import com.android.multitimerpro.data.TimerManager
import com.android.multitimerpro.service.TimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val timers: StateFlow<List<TimerEntity>> = timerManager.timers

    fun insert(name: String, duration: Long, color: Int, category: String) = viewModelScope.launch {
        timerManager.addTimer(name, duration, color, category)
    }

    fun addTimer(name: String, hours: Int, minutes: Int, seconds: Int, color: Int, category: String) {
        val totalMs = (hours * 3600 + minutes * 60 + seconds) * 1000L
        if (totalMs > 0) {
            insert(name.ifBlank { "Timer" }, totalMs, color, category)
        }
    }

    fun toggleTimer(timer: TimerEntity) = viewModelScope.launch {
        timerManager.toggleTimer(timer)
        // If we are starting a timer (it was PAUSED/FINISHED), ensure the service is running
        if (timer.status != "LIVE") {
            startService()
        }
    }

    private fun startService() {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun resetTimer(timer: TimerEntity) = viewModelScope.launch {
        timerManager.resetTimer(timer)
    }

    fun deleteTimer(timer: TimerEntity) = viewModelScope.launch {
        timerManager.deleteTimer(timer)
    }
}
