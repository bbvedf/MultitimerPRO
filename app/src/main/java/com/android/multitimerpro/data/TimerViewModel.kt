package com.android.multitimerpro.data

import android.content.Context
import android.content.Intent
import android.os.Build
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
    private val presetRepository: PresetRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "MT_DEBUG"
    val allTimers: StateFlow<List<TimerEntity>> = timerManager.timers
    
    val history: StateFlow<List<HistoryEntity>> = historyRepository.allHistory
        .onEach { list -> Log.d(TAG, "History flow emitted ${list.size} items") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPresets: StateFlow<List<PresetEntity>> = presetRepository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Theme state: null means follow system, true/false for forced dark/light
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    // Snooze Preferences (in minutes)
    private val _snooze1 = MutableStateFlow(5)
    val snooze1: StateFlow<Int> = _snooze1.asStateFlow()

    private val _snooze2 = MutableStateFlow(10)
    val snooze2: StateFlow<Int> = _snooze2.asStateFlow()

    // Auth states
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

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

    fun setSnooze1(minutes: Int) { _snooze1.value = minutes }
    fun setSnooze2(minutes: Int) { _snooze2.value = minutes }

    fun toggleTheme(isDark: Boolean) {
        _isDarkMode.value = isDark
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _uiMessage.emit(message)
        }
    }

    private fun syncUserAndData(uid: String, email: String) {
        viewModelScope.launch {
            if (uid.isBlank()) return@launch
            
            try {
                val finalEmail = if (email.isBlank()) auth.currentUser?.email ?: "" else email
                if (finalEmail.isBlank()) {
                    Log.w(TAG, "Sync pospuesto: Email no disponible")
                    return@launch
                }

                // 1. Intentamos obtener el documento actual para no romper la regla de 'createdAt' inmutable
                val userDocRef = firestore.collection("users").document(uid)
                val existingDoc = try { userDocRef.get().await() } catch (e: Exception) { null }
                
                val createdAt = if (existingDoc != null && existingDoc.exists()) {
                    existingDoc.getLong("createdAt") ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }

                val userMap = mutableMapOf<String, Any>(
                    "uid" to uid,
                    "email" to finalEmail,
                    "createdAt" to createdAt
                )
                
                auth.currentUser?.displayName?.let { userMap["displayName"] = it }
                val photoUrl = auth.currentUser?.photoUrl?.toString()
                if (photoUrl != null && (photoUrl.startsWith("http://") || photoUrl.startsWith("https://"))) {
                    userMap["photoUrl"] = photoUrl
                }

                Log.d(TAG, "Sincronizando perfil usuario: $userMap")
                userDocRef.set(userMap, SetOptions.merge()).await()
                
                Log.d(TAG, "Perfil de usuario sincronizado (OK)")

                // Ahora sí, disparamos el resto de la sincronización
                timerManager.reclaimLocalTimers(uid)
                timerManager.syncFromCloud(uid)
                timerManager.syncHistoryFromCloud(uid)
            } catch (e: Exception) {
                Log.e(TAG, "Error crítico en syncUserAndData: ${e.message}")
            }
        }
    }

    suspend fun updateHistory(history: HistoryEntity) {
        historyRepository.update(history)
        showMessage("Cambios guardados")
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
            Log.d(TAG, "Manejando resultado de Google Sign In...")
            val result = googleAuthClient.handleSignInResult(data)
            result.onSuccess { success ->
                if (success) {
                    val user = auth.currentUser
                    Log.d(TAG, "Google Login Éxito! UID: ${user?.uid}")
                    _isAuthenticated.value = true
                    _authError.value = null
                    syncUserAndData(user?.uid ?: "", user?.email ?: "")
                    showMessage("¡Bienvenido!")
                }
            }.onFailure { exception ->
                Log.e(TAG, "Google Login Falló", exception)
                _authError.value = "Error: ${exception.localizedMessage}"
                _isAuthenticated.value = false
                showMessage("Error de login: ${exception.localizedMessage}")
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val user = auth.currentUser
                _isAuthenticated.value = true
                _authError.value = null
                syncUserAndData(user?.uid ?: "", email)
                showMessage("Sesión iniciada")
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
                showMessage("Error: ${e.localizedMessage}")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                val user = auth.currentUser
                _isAuthenticated.value = true
                _authError.value = null
                syncUserAndData(user?.uid ?: "", email)
                showMessage("Cuenta creada")
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
                showMessage("Error: ${e.localizedMessage}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            googleAuthClient.signOut()
            _isAuthenticated.value = false
            showMessage("Sesión cerrada")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun snoozeTimer(timer: TimerEntity, minutes: Int) = viewModelScope.launch {
        val additionalMs = minutes * 60 * 1000L
        val updatedTimer = timer.copy(
            remainingTime = additionalMs,
            status = "LIVE"
        )
        timerManager.updateTimer(updatedTimer)
        startService()
        showMessage("Snooze: +$minutes min")
    }

    fun updateTimer(timer: TimerEntity) = viewModelScope.launch {
        timerManager.updateTimer(timer)
    }

    fun delete(timer: TimerEntity) = viewModelScope.launch {
        timerManager.deleteTimer(timer)
        showMessage("Eliminado")
    }

    fun resetTimer(timer: TimerEntity) = viewModelScope.launch {
        timerManager.resetTimer(timer)
    }

    fun deleteHistoryEntry(history: HistoryEntity) = viewModelScope.launch {
        historyRepository.delete(history)
        showMessage("Historial eliminado")
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

    // Preset management
    fun saveAsPreset(name: String, duration: Long, color: Int, category: String, description: String = "") = viewModelScope.launch {
        val preset = PresetEntity(
            name = name,
            durationMillis = duration,
            color = color,
            category = category,
            description = description,
            uid = auth.currentUser?.uid ?: ""
        )
        presetRepository.insert(preset)
        showMessage("Preset guardado")
    }

    fun deletePreset(preset: PresetEntity) = viewModelScope.launch {
        presetRepository.delete(preset)
        showMessage("Preset eliminado")
    }

    fun startTimerFromPreset(preset: PresetEntity) = viewModelScope.launch {
        timerManager.addTimer(
            preset.name,
            preset.durationMillis,
            preset.color,
            preset.category,
            preset.description
        )
        showMessage("Temporizador añadido")
    }
}
