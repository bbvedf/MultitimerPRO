package com.android.multitimerpro.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.android.multitimerpro.MainActivity
import com.android.multitimerpro.R
import com.android.multitimerpro.data.TimerManager
import com.android.multitimerpro.data.TimerEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import java.util.Locale

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var timerManager: TimerManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }

        // Escuchar cambios en los timers para gestionar notificaciones individuales
        serviceScope.launch {
            timerManager.timers.collect { timers ->
                val activeTimers = timers.filter { it.status == "LIVE" || it.status == "PAUSED" }
                
                if (activeTimers.isEmpty()) {
                    stopForegroundService()
                } else {
                    // Actualizamos cada notificación individualmente
                    activeTimers.forEach { timer ->
                        updatePersistentNotification(timer)
                    }
                    
                    // Si el primer timer de la lista cambia, nos aseguramos de que el servicio siga vivo
                    // Usamos un ID fijo (1) para el Foreground obligatorio de Android
                    if (activeTimers.isNotEmpty()) {
                        startForeground(NOTIFICATION_ID, createBaseForegroundNotification())
                    }
                }
            }
        }
    }

    private fun createBaseForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_RUNNING_ID)
            .setContentTitle("MultiTimer PRO")
            .setContentText("Gestionando instrumentos activos...")
            .setSmallIcon(R.drawable.logo_dark)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE_RESUME -> {
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID)
                serviceScope.launch {
                    timerManager.timers.value.find { it.id == timerId }?.let {
                        timerManager.toggleTimer(it)
                    }
                }
            }
            ACTION_RESET -> {
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID)
                serviceScope.launch {
                    timerManager.timers.value.find { it.id == timerId }?.let {
                        timerManager.resetTimer(it)
                    }
                }
            }
            ACTION_SNOOZE_1, ACTION_SNOOZE_2 -> {
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID)
                val minutes = intent.getIntExtra(if (intent.action == ACTION_SNOOZE_1) EXTRA_SNOOZE_1 else EXTRA_SNOOZE_2, 5)
                serviceScope.launch {
                    timerManager.timers.value.find { it.id == timerId }?.let {
                        val additionalMs = minutes * 60 * 1000L
                        timerManager.updateTimer(it.copy(remainingTime = additionalMs, status = "LIVE"))
                    }
                }
                notificationManager.cancel(timerId.hashCode())
            }
            ACTION_DISMISS -> {
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID)
                notificationManager.cancel(timerId.hashCode())
            }
            ACTION_STOP -> stopForegroundService()
            ACTION_FINISH_NOTIFY -> {
                val timerName = intent.getStringExtra(EXTRA_TIMER_NAME) ?: "Temporizador"
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: ""
                val s1 = intent.getIntExtra(EXTRA_SNOOZE_1, 5)
                val s2 = intent.getIntExtra(EXTRA_SNOOZE_2, 10)
                showFinishNotification(timerName, timerId, s1, s2)
            }
        }
        return START_STICKY
    }

    private fun updatePersistentNotification(timer: TimerEntity) {
        val minutes = (timer.remainingTime / 1000) / 60
        val seconds = (timer.remainingTime / 1000) % 60
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        val pauseIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_PAUSE_RESUME
            putExtra(EXTRA_TIMER_ID, timer.id)
        }
        val pausePendingIntent = PendingIntent.getService(this, timer.id.hashCode() + 10, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val isLive = timer.status == "LIVE"
        val actionText = if (isLive) "PAUSAR" else "REANUDAR"

        val notification = NotificationCompat.Builder(this, CHANNEL_RUNNING_ID)
            .setContentTitle(timer.name)
            .setContentText("Tiempo restante: $timeStr")
            .setSmallIcon(R.drawable.logo_dark)
            .setContentIntent(mainPendingIntent)
            .setOngoing(isLive)
            .setSilent(true)
            .addAction(0, actionText, pausePendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0))
            .build()

        notificationManager.notify(timer.id.hashCode(), notification)
    }

    private fun showFinishNotification(timerName: String, timerId: String, s1Min: Int, s2Min: Int) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Acción OK / CERRAR
        val dismissIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        val dismissPendingIntent = PendingIntent.getService(this, timerId.hashCode() + 20, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Snooze 1
        val s1Intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_SNOOZE_1
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_SNOOZE_1, s1Min)
        }
        val s1PendingIntent = PendingIntent.getService(this, timerId.hashCode() + 30, s1Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Snooze 2
        val s2Intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_SNOOZE_2
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_SNOOZE_2, s2Min)
        }
        val s2PendingIntent = PendingIntent.getService(this, timerId.hashCode() + 40, s2Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, CHANNEL_ALARM_ID)
            .setContentTitle("¡TIEMPO FINALIZADO!")
            .setContentText("'$timerName' ha terminado.")
            .setSmallIcon(R.drawable.logo_dark)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .addAction(0, "OK", dismissPendingIntent) // BOTÓN OK AÑADIDO
            .addAction(0, "+$s1Min min", s1PendingIntent)
            .addAction(0, "+$s2Min min", s2PendingIntent)
            .build()

        notificationManager.notify(timerId.hashCode(), notification)
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val runningChannel = NotificationChannel(CHANNEL_RUNNING_ID, "Instrumentos en curso", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Monitoreo de tiempos activos."
                setShowBadge(false)
            }
            val alarmChannel = NotificationChannel(CHANNEL_ALARM_ID, "Alarmas de finalización", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisos críticos de tiempo agotado."
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(runningChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_RUNNING_ID = "TIMER_RUNNING"
        const val CHANNEL_ALARM_ID = "TIMER_ALARM"
        
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE_RESUME = "ACTION_PAUSE_RESUME"
        const val ACTION_RESET = "ACTION_RESET"
        const val ACTION_SNOOZE_1 = "ACTION_SNOOZE_1"
        const val ACTION_SNOOZE_2 = "ACTION_SNOOZE_2"
        const val ACTION_DISMISS = "ACTION_DISMISS"
        const val ACTION_FINISH_NOTIFY = "ACTION_FINISH_NOTIFY"
        
        const val EXTRA_TIMER_NAME = "EXTRA_TIMER_NAME"
        const val EXTRA_TIMER_ID = "EXTRA_TIMER_ID"
        const val EXTRA_SNOOZE_1 = "EXTRA_SNOOZE_1"
        const val EXTRA_SNOOZE_2 = "EXTRA_SNOOZE_2"
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
