package com.android.multitimerpro.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.multitimerpro.service.TimerService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerManager,
    private val googleAuthClient: GoogleAuthClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "TimerViewModel"
    val allTimers: StateFlow<List<TimerEntity>> = timerManager.timers

    // Auth states
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        _isAuthenticated.value = auth.currentUser != null
        Log.d(TAG, "Auth init: isAuthenticated = ${_isAuthenticated.value}")
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            Log.d(TAG, "Handling Google Sign In result...")
            val result = googleAuthClient.handleSignInResult(data)
            result.onSuccess { success ->
                if (success) {
                    Log.d(TAG, "Google Sign In Success!")
                    _isAuthenticated.value = true
                    _authError.value = null
                    timerManager.syncFromCloud(auth.currentUser?.uid ?: "")
                }
            }.onFailure { exception ->
                Log.e(TAG, "Google Sign In Failed", exception)
                _authError.value = "Google Error: ${exception.localizedMessage}"
                _isAuthenticated.value = false
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting Email Login for $email")
                auth.signInWithEmailAndPassword(email, password).await()
                Log.d(TAG, "Email Login Success!")
                _isAuthenticated.value = true
                _authError.value = null
                timerManager.syncFromCloud(auth.currentUser?.uid ?: "")
            } catch (e: Exception) {
                Log.e(TAG, "Email Login Failed", e)
                _authError.value = e.localizedMessage
                _isAuthenticated.value = false
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting Registration for $email")
                auth.createUserWithEmailAndPassword(email, password).await()
                Log.d(TAG, "Registration Success!")
                _isAuthenticated.value = true
                _authError.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Registration Failed", e)
                _authError.value = e.localizedMessage
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            googleAuthClient.signOut()
            _isAuthenticated.value = false
            Log.d(TAG, "User signed out")
        }
    }

    fun insert(name: String, duration: Long, color: Int, category: String, description: String = "") = viewModelScope.launch {
        timerManager.addTimer(name, duration, color, category, description)
    }

    fun update(timer: TimerEntity) = viewModelScope.launch {
        timerManager.toggleTimer(timer)
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
