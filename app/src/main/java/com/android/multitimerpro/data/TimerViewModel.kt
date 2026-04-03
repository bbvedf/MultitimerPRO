package com.android.multitimerpro.data

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    val allTimers: StateFlow<List<TimerEntity>> = timerManager.timers

    fun insert(name: String, duration: Long, color: Int, category: String, description: String = "") = viewModelScope.launch {
        timerManager.addTimer(name, duration, color, category, description)
    }

    fun update(timer: TimerEntity) = viewModelScope.launch {
        timerManager.toggleTimer(timer)
        // If we are starting a timer, ensure the service is running
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

    fun updateTimer(timer: TimerEntity) = viewModelScope.launch {
        timerManager.updateTimer(timer)
    }

    fun delete(timer: TimerEntity) = viewModelScope.launch {
        timerManager.deleteTimer(timer)
    }

    fun resetTimer(timer: TimerEntity) = viewModelScope.launch {
        timerManager.resetTimer(timer)
    }

    fun addInterval(timer: TimerEntity, label: String) = viewModelScope.launch {
        val minutes = (timer.remainingTime / 1000) / 60
        val seconds = (timer.remainingTime / 1000) % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)
        val newInterval = "$timeStr - $label"

        val currentIntervals = if (timer.intervalsJson == "[]") mutableListOf<String>()
        else timer.intervalsJson.removeSurrounding("[", "]").split(", ").map { it.removeSurrounding("\"") }.toMutableList()

        currentIntervals.add(newInterval)
        val newIntervalsJson = "[\"${currentIntervals.joinToString("\", \"")}\"]"

        timerManager.updateTimer(timer.copy(intervalsJson = newIntervalsJson))
    }
}
