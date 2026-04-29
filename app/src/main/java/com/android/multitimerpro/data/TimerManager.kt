package com.android.multitimerpro.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.multitimerpro.data.remote.SupabaseService
import com.android.multitimerpro.service.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class TimerManager @Inject constructor(
    private val repository: TimerRepository,
    private val historyRepository: HistoryRepository,
    private val supabaseService: SupabaseService,
    @ApplicationContext private val context: Context
) {
    private val TAG = "MT_DEBUG"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private val _timers = MutableStateFlow<List<TimerEntity>>(emptyList())
    val timers: StateFlow<List<TimerEntity>> = _timers.asStateFlow()

    private fun isProUser(): Boolean {
        val prefs = context.getSharedPreferences("timers_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_pro", false)
    }

    var snooze1Min: Int = 5
    var snooze2Min: Int = 10

    init {
        // Cargar snoozes desde SharedPreferences al iniciar
        val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        snooze1Min = settings.getInt("snooze1", 5)
        snooze2Min = settings.getInt("snooze2", 10)

        serviceScope.launch {
            repository.allTimers.collect { dbTimers ->
                _timers.value = dbTimers.map { dbTimer ->
                    val ticking = _timers.value.find { it.id == dbTimer.id && it.status == "LIVE" }
                    if (ticking != null && dbTimer.status == "LIVE") {
                        dbTimer.copy(remainingTime = ticking.remainingTime)
                    } else {
                        dbTimer
                    }
                }
                checkTickingState()
            }
        }
    }

    fun reclaimLocalTimers(uid: String) {
        serviceScope.launch {
            try {
                val allTimers = repository.allTimers.first()
                val orphans = allTimers.filter { it.uid.isBlank() || it.uid == "ANONYMOUS" }
                Log.d(TAG, "[VINCULACION] Reclamando ${orphans.size} timers para $uid")
                orphans.forEach { timer ->
                    repository.update(timer.copy(uid = uid))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reclamando timers locales", e)
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
                val finishedTimers = mutableListOf<TimerEntity>()
                
                _timers.update { currentList ->
                    currentList.map { timer ->
                        if (timer.status == "LIVE") {
                            val newRemaining = (timer.remainingTime - 100).coerceAtLeast(0)
                            if (newRemaining <= 0L && timer.remainingTime > 0) {
                                val finished = timer.copy(remainingTime = 0, status = "FINISHED")
                                finishedTimers.add(finished)
                                finished
                            } else {
                                timer.copy(remainingTime = newRemaining)
                            }
                        } else {
                            timer
                        }
                    }
                }
                
                finishedTimers.forEach { 
                    Log.d(TAG, "!!! DETECTADO FIN DE TIMER: ${it.name} !!!")
                    handleTimerFinished(it) 
                }
            }
        }
    }

    private fun handleTimerFinished(timer: TimerEntity) {
        Log.d(TAG, "[TIMER_FINISH] Iniciando proceso para: ${timer.name}")
        serviceScope.launch(NonCancellable) {
            try {
                Log.d(TAG, "[TIMER_FINISH] 1. Preparando Intent")
                val intent = Intent(context, TimerService::class.java).apply {
                    action = TimerService.ACTION_FINISH_NOTIFY
                    putExtra(TimerService.EXTRA_TIMER_NAME, timer.name)
                    putExtra(TimerService.EXTRA_TIMER_ID, timer.id)
                    // Usar los valores configurados en lugar de fijos
                    putExtra(TimerService.EXTRA_SNOOZE_1, snooze1Min)
                    putExtra(TimerService.EXTRA_SNOOZE_2, snooze2Min)
                }
                context.startService(intent)
                
                Log.d(TAG, "[TIMER_FINISH] 2. Creando HistoryEntity")
                val historyId = if (timer.lastHistoryId.isNullOrBlank()) UUID.randomUUID().toString() else timer.lastHistoryId!!
                val historyEntry = HistoryEntity(
                    id = historyId,
                    timerName = timer.name,
                    category = timer.category,
                    durationMillis = timer.duration,
                    uid = timer.uid,
                    color = timer.color,
                    intervalsJson = timer.intervalsJson,
                    isSnoozed = timer.isSnoozed
                )
                
                Log.d(TAG, "[TIMER_FINISH] 3. Guardando en repositorio (UID: ${timer.uid})")
                if (timer.lastHistoryId != null) {
                    historyRepository.update(historyEntry)
                } else {
                    historyRepository.insert(historyEntry)
                }

                Log.d(TAG, "[TIMER_FINISH] 4. Actualizando estado del timer")
                updateTimer(timer.copy(
                    remainingTime = 0,
                    status = "FINISHED",
                    lastHistoryId = historyId
                ))
                
                Log.d(TAG, "[TIMER_FINISH] 5. Emitiendo evento de logros")
                _timerFinishedEvent.emit(historyEntry)
                
                Log.d(TAG, "[TIMER_FINISH] FINALIZADO CON ÉXITO")
                
            } catch (e: Exception) {
                Log.e(TAG, "[TIMER_FINISH] ERROR CRÍTICO: ${e.message}", e)
            }
        }
    }

    private val _timerFinishedEvent = MutableSharedFlow<HistoryEntity>()
    val timerFinishedEvent = _timerFinishedEvent.asSharedFlow()

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    suspend fun addTimer(name: String, durationMs: Long, color: Int, category: String, description: String = "", uid: String = "") {
        val newTimer = TimerEntity(
            name = name,
            duration = durationMs,
            remainingTime = durationMs,
            baseDuration = durationMs,
            status = "READY",
            color = color,
            category = category,
            description = description,
            isSnoozed = false,
            uid = uid
        )
        repository.insert(newTimer)
    }

    suspend fun toggleTimer(timer: TimerEntity) {
        val newStatus = if (timer.status == "LIVE") "PAUSED" else "LIVE"
        repository.update(timer.copy(status = newStatus))
    }

    suspend fun updateTimer(timer: TimerEntity) {
        repository.update(timer)
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
        ))
    }

    suspend fun clearAllTimers() {
        repository.clearAll()
    }

    suspend fun refreshTimersFromCloud(uid: String) {
        repository.refreshTimersFromCloud(uid)
    }

    suspend fun addInterval(timer: TimerEntity, label: String) {
        val intervals = timer.getIntervals().toMutableList()
        // Guardar el tiempo restante actual en lugar del wall clock (timestamp)
        intervals.add(TimerInterval(label, timer.remainingTime))
        val updatedTimer = timer.copy(intervalsJson = timer.setIntervals(intervals))
        repository.update(updatedTimer)
    }
}
