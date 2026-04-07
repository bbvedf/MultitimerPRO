package com.android.multitimerpro.data

import android.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManager @Inject constructor(
    private val repository: TimerRepository,
    private val firestore: FirebaseFirestore
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private val auth = FirebaseAuth.getInstance()

    private val _timers = MutableStateFlow<List<TimerEntity>>(emptyList())
    val timers: StateFlow<List<TimerEntity>> = _timers.asStateFlow()

    init {
        // Observe database changes and update our local state
        serviceScope.launch {
            repository.allTimers.collect { dbTimers ->
                _timers.value = dbTimers
                checkTickingState()
            }
        }

        // Initial sync if user is logged in
        auth.currentUser?.let { user ->
            syncFromCloud(user.uid)
        }
    }

    fun syncFromCloud(uid: String) {
        serviceScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .document(uid)
                    .collection("timers")
                    .get()
                    .await()

                snapshot.documents.forEach { doc ->
                    val timer = TimerEntity(
                        id = doc.getString("id")?.toInt() ?: 0,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        duration = doc.getLong("duration") ?: 0L,
                        remainingTime = doc.getLong("remainingTime") ?: 0L,
                        status = doc.getString("status") ?: "PAUSED",
                        color = doc.getLong("color")?.toInt() ?: Color.BLUE,
                        category = doc.getString("category") ?: "General",
                        intervalsJson = doc.getString("intervalsJson") ?: "[]",
                        uid = doc.getString("uid") ?: uid,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                    repository.insert(timer)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                delay(100) // Tick every 100ms for smooth UI, but we'll only update DB less often
                val currentTime = System.currentTimeMillis()

                _timers.update { currentList ->
                    currentList.map { timer ->
                        if (timer.status == "LIVE") {
                            val newRemaining = (timer.remainingTime - 100).coerceAtLeast(0)
                            if (newRemaining == 0L) {
                                // Timer finished!
                                val finishedTimer = timer.copy(
                                    remainingTime = 0,
                                    status = "FINISHED"
                                )
                                serviceScope.launch { repository.update(finishedTimer) }
                                finishedTimer
                            } else {
                                timer.copy(remainingTime = newRemaining)
                            }
                        } else {
                            timer
                        }
                    }
                }
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    suspend fun addTimer(name: String, durationMs: Long, color: Int, category: String, description: String = "") {
        val newTimer = TimerEntity(
            name = name,
            duration = durationMs,
            remainingTime = durationMs,
            status = "PAUSED",
            color = color,
            category = category,
            description = description,
            uid = auth.currentUser?.uid ?: ""
        )
        repository.insert(newTimer)
    }

    suspend fun toggleTimer(timer: TimerEntity) {
        val newStatus = if (timer.status == "LIVE") "PAUSED" else "LIVE"
        val updatedTimer = timer.copy(status = newStatus)
        repository.update(updatedTimer)
    }

    suspend fun updateTimer(timer: TimerEntity) {
        repository.update(timer)
    }

    suspend fun deleteTimer(timer: TimerEntity) {
        repository.delete(timer)
    }

    suspend fun resetTimer(timer: TimerEntity) {
        val resetTimer = timer.copy(
            remainingTime = timer.duration,
            status = "PAUSED"
        )
        repository.update(resetTimer)
    }
}
