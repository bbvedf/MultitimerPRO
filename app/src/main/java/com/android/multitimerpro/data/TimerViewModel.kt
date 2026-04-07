package com.android.multitimerpro.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.multitimerpro.service.TimerService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerManager,
    private val historyRepository: HistoryRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "MT_DEBUG"
    val allTimers: StateFlow<List<TimerEntity>> = timerManager.timers
    
    val history: StateFlow<List<HistoryEntity>> = historyRepository.allHistory
        .onEach { list -> Log.d(TAG, "History flow emitted ${list.size} items") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Auth states
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        val currentUser = auth.currentUser
        _isAuthenticated.value = currentUser != null
        Log.d(TAG, "Auth init: isAuthenticated = ${_isAuthenticated.value}, UID: ${currentUser?.uid}")
        
        if (currentUser != null) {
            syncUserAndData(currentUser.uid, currentUser.email ?: "")
        }
    }

    private fun syncUserAndData(uid: String, email: String) {
        viewModelScope.launch {
            try {
                // Crear/Actualizar el documento del usuario para cumplir con las Security Rules
                val userMap = mapOf(
                    "uid" to uid,
                    "email" to email,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid)
                    .set(userMap, SetOptions.merge())
                    .await()
                Log.d(TAG, "User document synced successfully for $uid")

                timerManager.reclaimLocalTimers(uid)
                timerManager.syncFromCloud(uid)
                timerManager.syncHistoryFromCloud(uid)
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user document: ${e.message}")
            }
        }
    }

    // Stats calculations
    val totalTimeSpent: StateFlow<Long> = history.map { list ->
        list.sumOf { it.durationMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val statsByCategory: StateFlow<Map<String, Long>> = history.map { list ->
        list.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.durationMillis } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            Log.d(TAG, "Handling Google Sign In result...")
            val result = googleAuthClient.handleSignInResult(data)
            result.onSuccess { success ->
                if (success) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""
                    Log.d(TAG, "Google Sign In Success! UID: $uid")
                    _isAuthenticated.value = true
                    _authError.value = null
                    syncUserAndData(uid, user?.email ?: "")
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
                val user = auth.currentUser
                val uid = user?.uid ?: ""
                Log.d(TAG, "Email Login Success! UID: $uid")
                _isAuthenticated.value = true
                _authError.value = null
                syncUserAndData(uid, email)
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
                val user = auth.currentUser
                val uid = user?.uid ?: ""
                Log.d(TAG, "Registration Success! UID: $uid")
                _isAuthenticated.value = true
                _authError.value = null
                syncUserAndData(uid, email)
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

    fun deleteHistoryEntry(history: HistoryEntity) = viewModelScope.launch {
        historyRepository.delete(history)
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
