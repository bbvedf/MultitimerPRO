package com.android.multitimerpro.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val timerDao: TimerDao,
    private val firestore: FirebaseFirestore
) {
    private val TAG = "MT_DEBUG"
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allTimers: Flow<List<TimerEntity>> = timerDao.getAllTimers()

    fun getTimersByUid(uid: String): Flow<List<TimerEntity>> = timerDao.getTimersByUid(uid)

    suspend fun insert(timer: TimerEntity) {
        try {
            Log.d(TAG, "[TIMER] Insertando local: ${timer.name}")
            timerDao.insertTimer(timer)
            if (timer.uid.isNotEmpty()) {
                syncToCloud(timer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[TIMER] Error en insert", e)
        }
    }

    suspend fun update(timer: TimerEntity) {
        try {
            timerDao.updateTimer(timer)
            if (timer.uid.isNotEmpty()) {
                syncToCloud(timer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[TIMER] Error actualizando local", e)
        }
    }

    suspend fun delete(timer: TimerEntity) {
        try {
            timerDao.deleteTimer(timer)
            if (timer.uid.isNotEmpty()) {
                firestore.collection("users")
                    .document(timer.uid)
                    .collection("timers")
                    .document(timer.id)
                    .delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) Log.d(TAG, "[TIMER] Borrado de nube OK")
                        else Log.e(TAG, "[TIMER] Error borrando de nube: ${task.exception?.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[TIMER] Error en delete", e)
        }
    }

    private fun syncToCloud(timer: TimerEntity) {
        val timerMap = mapOf(
            "id" to timer.id,
            "name" to timer.name,
            "description" to timer.description,
            "duration" to timer.duration,
            "remainingTime" to timer.remainingTime,
            "status" to timer.status,
            "color" to timer.color,
            "category" to timer.category,
            "intervalsJson" to timer.intervalsJson,
            "isSnoozed" to timer.isSnoozed,
            "baseDuration" to timer.baseDuration,
            "lastHistoryId" to (timer.lastHistoryId ?: ""),
            "uid" to timer.uid,
            "createdAt" to timer.createdAt
        )

        Log.d(TAG, "[CLOUD] Intentando escribir timer: ${timer.name}")
        firestore.collection("users")
            .document(timer.uid)
            .collection("timers")
            .document(timer.id)
            .set(timerMap, SetOptions.merge())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "[CLOUD] Sincronización de timer EXITOSA: ${timer.name}")
                } else {
                    Log.e(TAG, "[CLOUD] ERROR sincronizando timer ${timer.name}: ${task.exception?.message}")
                }
            }
    }

    suspend fun getTimerById(id: String): TimerEntity? {
        return timerDao.getTimerById(id)
    }
}
