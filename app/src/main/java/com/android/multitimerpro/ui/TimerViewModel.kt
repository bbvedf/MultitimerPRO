package com.android.multitimerpro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.multitimerpro.data.TimerManager
import com.android.multitimerpro.data.local.TimerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerManager
) : ViewModel() {

    val timers: StateFlow<List<TimerEntity>> = timerManager.timers

    fun addTimer(name: String, hours: Int, minutes: Int, seconds: Int) {
        val totalMs = (hours * 3600 + minutes * 60 + seconds) * 1000L
        if (totalMs > 0) {
            viewModelScope.launch {
                timerManager.addTimer(name.ifBlank { "Timer" }, totalMs)
            }
        }
    }

    fun toggleTimer(timer: TimerEntity) {
        viewModelScope.launch {
            timerManager.toggleTimer(timer)
        }
    }

    fun resetTimer(timer: TimerEntity) {
        viewModelScope.launch {
            timerManager.resetTimer(timer)
        }
    }

    fun deleteTimer(timer: TimerEntity) {
        viewModelScope.launch {
            timerManager.deleteTimer(timer)
        }
    }
}
