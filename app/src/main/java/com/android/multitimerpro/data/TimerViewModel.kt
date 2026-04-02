package com.android.multitimerpro.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerManager
) : ViewModel() {

    val allTimers: StateFlow<List<TimerEntity>> = timerManager.timers

    fun insert(timer: TimerEntity) = viewModelScope.launch {
        timerManager.addTimer(timer.name, timer.initialTimeMs)
    }

    fun update(timer: TimerEntity) = viewModelScope.launch {
        timerManager.toggleTimer(timer)
    }

    fun delete(timer: TimerEntity) = viewModelScope.launch {
        timerManager.deleteTimer(timer)
    }
    
    fun resetTimer(timer: TimerEntity) = viewModelScope.launch {
        timerManager.resetTimer(timer)
    }
}
