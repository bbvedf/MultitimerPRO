package com.android.multitimerpro.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.android.multitimerpro.MainActivity
import com.android.multitimerpro.data.TimerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var repository: TimerRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundService()
            ACTION_STOP -> stopForegroundService()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification("Active timers running...")
        startForeground(NOTIFICATION_ID, notification)
        
        if (tickJob == null) {
            startTicking()
        }
    }

    private fun stopForegroundService() {
        tickJob?.cancel()
        tickJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicking() {
        tickJob = serviceScope.launch {
            while (isActive) {
                // In a production app, we'd handle this more efficiently than 
                // updating DB every second, but for a "step by step" it's a solid start.
                // Better approach: Update a SharedFlow/StateFlow in memory and DB every 5s or when paused.
                
                // For now, let's just keep the service alive.
                // The actual "ticking" logic will be refined in the ViewModel/Manager.
                delay(1000)
            }
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "TIMER_CHANNEL")
            .setContentTitle("MultiTimer Pro")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
