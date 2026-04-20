package com.android.multitimerpro.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import com.android.multitimerpro.service.TimerService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val TAG = "MT_DEBUG"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private val auth = FirebaseAuth.getInstance()

    private val _timers = MutableStateFlow<List<TimerEntity>>(emptyList())
    val timers: StateFlow<List<TimerEntity>> = _timers.asStateFlow()

    private fun isProUser(): Boolean {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_pro", false)
    }

    var snooze1Min: Int = 5
    var snooze2Min: Int = 10

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
                    repository.update(timer.copy(uid = uid), isPro = isProUser())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reclamando timers locales", e)
            }
        }
    }

    fun syncFromCloud(uid: String) {
        serviceScope.launch {
            try {
                val snapshot = firestore.collection("users").document(uid).collection("timers").get().await()
                snapshot.documents.forEach { doc ->
                    val timer = TimerEntity(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        duration = doc.getLong("duration") ?: 0L,
                        remainingTime = doc.getLong("remainingTime") ?: 0L,
                        status = doc.getString("status") ?: "READY",
                        color = doc.getLong("color")?.toInt() ?: Color.BLUE,
                        category = doc.getString("category") ?: "ENFOQUE",
                        intervalsJson = doc.getString("intervalsJson") ?: "[]",
                        isSnoozed = doc.getBoolean("isSnoozed") ?: false,
                        baseDuration = doc.getLong("baseDuration") ?: (doc.getLong("duration") ?: 0L),
                        lastHistoryId = doc.getString("lastHistoryId")?.takeIf { it.isNotBlank() },
                        uid = doc.getString("uid") ?: uid,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    // Al descargar de la nube, no forzamos resincronización (isPro = false para evitar bucles)
                    repository.insert(timer, isPro = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync timers failed", e)
            }
        }
    }

    fun syncHistoryFromCloud(uid: String) {
        serviceScope.launch {
            try {
                val snapshot = firestore.collection("users").document(uid).collection("history").get().await()
                snapshot.documents.forEach { doc ->
                    val history = HistoryEntity(
                        id = doc.getString("id") ?: doc.id,
                        timerName = doc.getString("timerName") ?: "",
                        category = doc.getString("category") ?: "",
                        durationMillis = doc.getLong("duration") ?: 0L,
                        completedAt = doc.getLong("endTime") ?: System.currentTimeMillis(),
                        uid = doc.getString("uid") ?: uid,
                        color = doc.getLong("color")?.toInt() ?: Color.BLUE,
                        notes = doc.getString("notes") ?: "",
                        intervalsJson = doc.getString("intervalsJson") ?: "[]",
                        isSnoozed = doc.getBoolean("isSnoozed") ?: false
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
                // Notificar al servicio de la UI
                val intent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_FINISH_NOTIFY
                    putExtra(TimerService.EXTRA_TIMER_NAME, timer.name)
                    putExtra(TimerService.EXTRA_TIMER_ID, timer.id)
                    putExtra(TimerService.EXTRA_SNOOZE_1, snooze1Min)
                    putExtra(TimerService.EXTRA_SNOOZE_2, snooze2Min)
                }
                context.startService(intent)
                
                val currentUid = auth.currentUser?.uid ?: timer.uid
                
                // Registro de historia
                val historyEntry = HistoryEntity(
                    id = timer.lastHistoryId ?: UUID.randomUUID().toString(),
                    timerName = timer.name,
                    category = timer.category,
                    durationMillis = timer.duration,
                    uid = currentUid,
                    color = timer.color,
                    intervalsJson = timer.intervalsJson,
                    isSnoozed = timer.isSnoozed || (timer.lastHistoryId != null && timer.isSnoozed)
                )
                
                if (timer.lastHistoryId != null) {
                    historyRepository.update(historyEntry)
                } else {
                    historyRepository.insert(historyEntry)
                }

                // Actualizar el timer local y opcionalmente en la nube si es PRO
                repository.update(timer.copy(
                    remainingTime = 0,
                    status = "FINISHED",
                    lastHistoryId = historyEntry.id
                ), isPro = isProUser())
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar fin de timer", e)
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    suspend fun addTimer(name: String, durationMs: Long, color: Int, category: String, description: String = "") {
        val currentUid = auth.currentUser?.uid ?: ""
        val newTimer = TimerEntity(
            name = name,
            duration = durationMs,
            remainingTime = durationMs,
            baseDuration = durationMs,
            status = "READY",
            color = color,
            category = category,
            description = description,
            uid = currentUid
        )
        repository.insert(newTimer, isPro = isProUser())
    }

    suspend fun toggleTimer(timer: TimerEntity) {
        val newStatus = if (timer.status == "LIVE") "PAUSED" else "LIVE"
        repository.update(timer.copy(status = newStatus), isPro = isProUser())
    }

    suspend fun updateTimer(timer: TimerEntity) {
        repository.update(timer, isPro = isProUser())
    }

    suspend fun deleteTimer(timer: TimerEntity) {
        repository.delete(timer)
    }

    suspend fun resetTimer(timer: TimerEntity) {
        val targetDuration = if (timer.baseDuration > 0) timer.baseDuration else timer.duration
        repository.update(timer.copy(
            duration = targetDuration,
            remainingTime = targetDuration, 
            status = "READY", 
            intervalsJson = "[]",
            isSnoozed = false,
            lastHistoryId = null,
            lastSnoozeDuration = 0L
        ), isPro = isProUser())
    }

    suspend fun clearAllTimers() {
        repository.clearAll()
    }
}
