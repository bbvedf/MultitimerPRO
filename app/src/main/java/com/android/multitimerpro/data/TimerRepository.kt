package com.android.multitimerpro.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val timerDao: TimerDao,
    private val firestore: FirebaseFirestore
) {
    val allTimers: Flow<List<TimerEntity>> = timerDao.getAllTimers()

    fun getTimersByUid(uid: String): Flow<List<TimerEntity>> = timerDao.getTimersByUid(uid)

    suspend fun insert(timer: TimerEntity) {
        timerDao.insertTimer(timer)
        if (timer.uid.isNotEmpty()) {
            syncToCloud(timer)
        }
    }

    suspend fun update(timer: TimerEntity) {
        timerDao.updateTimer(timer)
        if (timer.uid.isNotEmpty()) {
            syncToCloud(timer)
        }
    }

    suspend fun delete(timer: TimerEntity) {
        timerDao.deleteTimer(timer)
        if (timer.uid.isNotEmpty()) {
            firestore.collection("users")
                .document(timer.uid)
                .collection("timers")
                .document(timer.id.toString())
                .delete()
                .await()
        }
    }

    private suspend fun syncToCloud(timer: TimerEntity) {
        val timerMap = mapOf(
            "id" to timer.id.toString(),
            "name" to timer.name,
            "description" to timer.description,
            "duration" to timer.duration,
            "remainingTime" to timer.remainingTime,
            "status" to timer.status,
            "color" to timer.color,
            "category" to timer.category,
            "intervalsJson" to timer.intervalsJson,
            "uid" to timer.uid,
            "createdAt" to timer.createdAt
        )

        firestore.collection("users")
            .document(timer.uid)
            .collection("timers")
            .document(timer.id.toString())
            .set(timerMap, SetOptions.merge())
            .await()
    }

    suspend fun getTimerById(id: Int): TimerEntity? {
        return timerDao.getTimerById(id)
    }
}
