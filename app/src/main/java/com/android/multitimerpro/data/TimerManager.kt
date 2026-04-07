package com.android.multitimerpro.data

import android.graphics.Color
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class TimerManager @Inject constructor(
    private val repository: TimerRepository,
    private val historyRepository: HistoryRepository,
    private val firestore: FirebaseFirestore
) {
    private val TAG = "MT_DEBUG"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private val auth = FirebaseAuth.getInstance()

    private val _timers = MutableStateFlow<List<TimerEntity>>(emptyList())
    val timers: StateFlow<List<TimerEntity>> = _timers.asStateFlow()

    init {
        serviceScope.launch {
            repository.allTimers.collect { dbTimers ->
                _timers.update { currentList ->
                    dbTimers.map { dbTimer ->
                        val ticking = currentList.find { it.id == dbTimer.id && it.status == "LIVE" }
                        if (ticking != null && dbTimer.status == "LIVE") {
                            dbTimer.copy(remainingTime = ticking.remainingTime)
                        } else {
                            dbTimer
                        }
                    }
                }
                checkTickingState()
            }
        }

        auth.currentUser?.let { user ->
            Log.d(TAG, "TimerManager init: Syncing for ${user.uid}")
            syncFromCloud(user.uid)
            syncHistoryFromCloud(user.uid)
        }
    }

    fun reclaimLocalTimers(uid: String) {
        serviceScope.launch {
            try {
                val allTimers = repository.allTimers.first()
                val orphans = allTimers.filter { it.uid.isBlank() }
                Log.d(TAG, "[VINCULACION] DB local tiene ${allTimers.size} timers. Reclamando ${orphans.size} para $uid")
                orphans.forEach { timer ->
                    repository.update(timer.copy(uid = uid))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reclamando timers locales", e)
            }
        }
    }

    fun syncFromCloud(uid: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Iniciando descarga de timers desde Firestore para $uid")
                val snapshot = firestore.collection("users").document(uid).collection("timers").get().await()
                Log.d(TAG, "Firestore devolvió ${snapshot.size()} timers")
                snapshot.documents.forEach { doc ->
                    val timer = TimerEntity(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        duration = doc.getLong("duration") ?: 0L,
                        remainingTime = doc.getLong("remainingTime") ?: 0L,
                        status = doc.getString("status") ?: "PAUSED",
                        color = doc.getLong("color")?.toInt() ?: Color.BLUE,
                        category = doc.getString("category") ?: "ENFOQUE",
                        intervalsJson = doc.getString("intervalsJson") ?: "[]",
                        uid = doc.getString("uid") ?: uid,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    repository.insert(timer)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync timers failed", e)
            }
        }
    }

    fun syncHistoryFromCloud(uid: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Iniciando descarga de historial desde Firestore para $uid")
                val snapshot = firestore.collection("users").document(uid).collection("history").get().await()
                Log.d(TAG, "Firestore devolvió ${snapshot.size()} entradas de historial")
                snapshot.documents.forEach { doc ->
                    val history = HistoryEntity(
                        id = doc.getString("id") ?: doc.id,
                        timerName = doc.getString("timerName") ?: "",
                        category = doc.getString("category") ?: "",
                        durationMillis = doc.getLong("durationMillis") ?: 0L,
                        completedAt = doc.getLong("completedAt") ?: System.currentTimeMillis(),
                        uid = doc.getString("uid") ?: uid,
                        color = doc.getLong("color")?.toInt() ?: Color.BLUE,
                        notes = doc.getString("notes") ?: ""
                    )
                    historyRepository.insert(history)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync history failed", e)
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
        Log.d(TAG, "Tick JOB START")
        tickJob = serviceScope.launch {
            while (isActive) {
                delay(100)
                var timerToFinish: TimerEntity? = null
                
                _timers.update { currentList ->
                    currentList.map { timer ->
                        if (timer.status == "LIVE") {
                            val newRemaining = (timer.remainingTime - 100).coerceAtLeast(0)
                            if (newRemaining <= 0L && timer.remainingTime > 0) {
                                timerToFinish = timer
                                timer.copy(remainingTime = 0, status = "FINISHED")
                            } else {
                                timer.copy(remainingTime = newRemaining)
                            }
                        } else {
                            timer
                        }
                    }
                }
                
                timerToFinish?.let { 
                    handleTimerFinished(it) 
                    timerToFinish = null
                }
            }
        }
    }

    private fun handleTimerFinished(timer: TimerEntity) {
        serviceScope.launch(NonCancellable) {
            try {
                val finishedTimer = timer.copy(remainingTime = 0, status = "FINISHED")
                repository.update(finishedTimer)
                
                val currentUid = auth.currentUser?.uid ?: timer.uid
                Log.d(TAG, "[FINALIZADO] ${timer.name}. Guardando historial para UID: $currentUid")
                
                val historyEntry = HistoryEntity(
                    timerName = timer.name,
                    category = timer.category,
                    durationMillis = timer.duration,
                    uid = currentUid,
                    color = timer.color
                )
                historyRepository.insert(historyEntry)
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar fin de timer", e)
            }
        }
    }

    private fun stopTicking() {
        Log.d(TAG, "Tick JOB STOP")
        tickJob?.cancel()
        tickJob = null
    }

    suspend fun addTimer(name: String, durationMs: Long, color: Int, category: String, description: String = "") {
        val currentUid = auth.currentUser?.uid ?: ""
        val newTimer = TimerEntity(
            name = name,
            duration = durationMs,
            remainingTime = durationMs,
            status = "PAUSED",
            color = color,
            category = category,
            description = description,
            uid = currentUid
        )
        repository.insert(newTimer)
        Log.d(TAG, "Timer CREADO: ${name}. UID asignado: $currentUid")
    }

    suspend fun toggleTimer(timer: TimerEntity) {
        val newStatus = if (timer.status == "LIVE") "PAUSED" else "LIVE"
        Log.d(TAG, "Timer ${timer.name} cambiado a $newStatus")
        repository.update(timer.copy(status = newStatus))
    }

    suspend fun updateTimer(timer: TimerEntity) {
        repository.update(timer)
    }

    suspend fun deleteTimer(timer: TimerEntity) {
        repository.delete(timer)
    }

    suspend fun resetTimer(timer: TimerEntity) {
        repository.update(timer.copy(remainingTime = timer.duration, status = "PAUSED"))
    }
}
