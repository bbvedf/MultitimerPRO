package com.android.multitimerpro.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
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
    private val activeNotificationIds = mutableSetOf<Int>()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }

        // Escuchar cambios: SOLO procesamos timers LIVE o PAUSED
        serviceScope.launch {
            timerManager.timers.collect { timers ->
                refreshNotifications(timers)
            }
        }
    }

    private fun refreshNotifications(timers: List<TimerEntity>) {
        val currentActiveTimers = timers.filter { it.status == "LIVE" || it.status == "PAUSED" }
        
        // 1. Limpiar notificaciones de timers que ya no están activos/pausados
        val currentIds = currentActiveTimers.map { it.id.hashCode() }.toSet()
        val idsToRemove = activeNotificationIds.subtract(currentIds)
        idsToRemove.forEach { id ->
            notificationManager.cancel(id)
            activeNotificationIds.remove(id)
        }

        // 2. Gestionar el Foreground obligatorio
        if (currentActiveTimers.isEmpty()) {
            stopForegroundService()
        } else {
            // El primer timer LIVE (o el primero de la lista) mantiene el servicio vivo
            val foregroundTimer = currentActiveTimers.find { it.status == "LIVE" } ?: currentActiveTimers.first()
            startForeground(NOTIFICATION_ID, createBaseForegroundNotification(foregroundTimer))
            
            // Actualizar las demás notificaciones como normales
            currentActiveTimers.forEach { timer ->
                if (timer.id != foregroundTimer.id) {
                    updatePersistentNotification(timer)
                }
                activeNotificationIds.add(timer.id.hashCode())
            }
        }
    }

    private fun createBaseForegroundNotification(timer: TimerEntity): Notification {
        val minutes = (timer.remainingTime / 1000) / 60
        val seconds = (timer.remainingTime / 1000) % 60
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        val pauseIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_PAUSE_RESUME
            putExtra(EXTRA_TIMER_ID, timer.id)
        }
        val pausePendingIntent = PendingIntent.getService(this, timer.id.hashCode(), pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val isLive = timer.status == "LIVE"
        val actionText = if (isLive) getString(R.string.notif_pause) else getString(R.string.notif_resume)

        return NotificationCompat.Builder(this, CHANNEL_RUNNING_ID)
            .setContentTitle(timer.name)
            .setContentText(getString(R.string.notif_active_instrument, timeStr))
            .setSmallIcon(R.drawable.logo_dark)
            .setContentIntent(mainPendingIntent)
            .setOngoing(isLive)
            .setSilent(true)
            .addAction(0, actionText, pausePendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0))
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
                        val newDuration = it.duration + additionalMs
                        timerManager.updateTimer(it.copy(
                            remainingTime = additionalMs, 
                            duration = newDuration,
                            status = "LIVE",
                            isSnoozed = true
                        ))
                    }
                }
                notificationManager.cancel(timerId.hashCode())
            }
            ACTION_DISMISS -> {
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID)
                notificationManager.cancel(timerId.hashCode())
            }
            ACTION_FINISH_NOTIFY -> {
                val timerName = intent.getStringExtra(EXTRA_TIMER_NAME) ?: "Temporizador"
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: ""
                val s1 = intent.getIntExtra(EXTRA_SNOOZE_1, 5)
                val s2 = intent.getIntExtra(EXTRA_SNOOZE_2, 10)
                showFinishNotification(timerName, timerId, s1, s2)
            }
            ACTION_REFRESH_NOTIFICATIONS -> {
                // Forzar recreación de canales y refresco de notificaciones activas
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannels()
                }
                refreshNotifications(timerManager.timers.value)
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
        val actionText = if (isLive) getString(R.string.notif_pause) else getString(R.string.notif_resume)

        val notification = NotificationCompat.Builder(this, CHANNEL_RUNNING_ID)
            .setContentTitle(timer.name)
            .setContentText(getString(R.string.notif_in_progress, timeStr))
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

        val dismissIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_TIMER_ID, timerId)
        }
        val dismissPendingIntent = PendingIntent.getService(this, timerId.hashCode() + 20, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val s1Intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_SNOOZE_1
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_SNOOZE_1, s1Min)
        }
        val s1PendingIntent = PendingIntent.getService(this, timerId.hashCode() + 30, s1Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val s2Intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_SNOOZE_2
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_SNOOZE_2, s2Min)
        }
        val s2PendingIntent = PendingIntent.getService(this, timerId.hashCode() + 40, s2Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, CHANNEL_ALARM_ID)
            .setContentTitle(getString(R.string.notif_timer_finished))
            .setContentText(getString(R.string.notif_timer_completed, timerName))
            .setSmallIcon(R.drawable.logo_dark)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setFullScreenIntent(pendingIntent, true) // Forzar que aparezca arriba de todo
            .setAutoCancel(true)
            .addAction(0, getString(R.string.notif_ok), dismissPendingIntent)
            .addAction(0, "+$s1Min MIN", s1PendingIntent)
            .addAction(0, "+$s2Min MIN", s2PendingIntent)
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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val runningChannel = NotificationChannel(CHANNEL_RUNNING_ID, getString(R.string.notif_running_channel), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.notif_running_desc)
                setShowBadge(false)
            }
            val alarmChannel = NotificationChannel(CHANNEL_ALARM_ID, getString(R.string.notif_alarm_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                description = getString(R.string.notif_alarm_desc)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                setBypassDnd(true) // Intentar saltar el modo No Molestar
                setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
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
        const val ACTION_REFRESH_NOTIFICATIONS = "ACTION_REFRESH_NOTIFICATIONS"
        
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
