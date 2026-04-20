package com.android.multitimerpro.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val firestore: FirebaseFirestore
) {
    private val TAG = "MT_DEBUG"
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun insert(history: HistoryEntity) {
        try {
            Log.d(TAG, "[HISTORIAL] Insertando local: ${history.timerName}")
            historyDao.insert(history)
            
            if (history.uid.isNotEmpty()) {
                repositoryScope.launch {
                    syncToCloud(history)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[HISTORIAL] Error en insert", e)
        }
    }

    suspend fun update(history: HistoryEntity) {
        try {
            historyDao.update(history)
            if (history.uid.isNotEmpty()) {
                repositoryScope.launch {
                    syncToCloud(history)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[HISTORIAL] Error en update", e)
        }
    }

    suspend fun delete(history: HistoryEntity) {
        historyDao.delete(history)
        if (history.uid.isNotEmpty()) {
            repositoryScope.launch {
                try {
                    firestore.collection("users")
                        .document(history.uid)
                        .collection("history")
                        .document(history.id)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "[HISTORIAL] Error borrando de nube", e)
                }
            }
        }
    }

    private suspend fun syncToCloud(history: HistoryEntity) {
        try {
            val historyMap = mapOf(
                "id" to history.id,
                "timerName" to history.timerName,
                "startTime" to (history.completedAt - history.durationMillis),
                "endTime" to history.completedAt,
                "duration" to history.durationMillis,
                "uid" to history.uid,
                "category" to history.category,
                "color" to history.color,
                "notes" to history.notes,
                "intervalsJson" to history.intervalsJson,
                "isSnoozed" to history.isSnoozed
            )

            firestore.collection("users")
                .document(history.uid)
                .collection("history")
                .document(history.id)
                .set(historyMap, SetOptions.merge())
                .await()
            Log.d(TAG, "[CLOUD] Sincronización de historial EXITOSA")
        } catch (e: Exception) {
            Log.e(TAG, "[CLOUD] ERROR historial: ${e.message}")
        }
    }

    suspend fun clearAll() {
        historyDao.clearAll()
    }
}
