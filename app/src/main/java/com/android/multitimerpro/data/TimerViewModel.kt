package com.android.multitimerpro.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.multitimerpro.R
import com.android.multitimerpro.service.TimerService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerManager,
    private val historyRepository: HistoryRepository,
    private val presetRepository: PresetRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "MT_DEBUG"
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("multitimer_prefs", Context.MODE_PRIVATE)

    // --- Core States ---
    val allTimers: StateFlow<List<TimerEntity>> = timerManager.timers

    val history: StateFlow<List<HistoryEntity>> = historyRepository.allHistory
        .onEach { list -> Log.d(TAG, "History flow emitted ${list.size} items") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPresets: StateFlow<List<PresetEntity>> = presetRepository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- PRO Status ---
    private val _isPro = MutableStateFlow(prefs.getBoolean("is_pro", false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    // --- Theme state ---
    private val _isDarkMode = MutableStateFlow<Boolean?>(false)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    // --- Language State ---
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage = _currentLanguage.asStateFlow()

    // --- Snooze Preferences ---
    private val _snooze1 = MutableStateFlow(5)
    val snooze1: StateFlow<Int> = _snooze1.asStateFlow()

    private val _snooze2 = MutableStateFlow(10)
    val snooze2: StateFlow<Int> = _snooze2.asStateFlow()

    // --- Auth states ---
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // --- User Profile State ---
    private val _userDisplayName = MutableStateFlow(auth.currentUser?.displayName ?: "")
    val userDisplayName = _userDisplayName.asStateFlow()

    private val _userPhotoUrl = MutableStateFlow(auth.currentUser?.photoUrl?.toString() ?: "")
    val userPhotoUrl = _userPhotoUrl.asStateFlow()

    // --- Filter States ---
    private val _historyShowFilters = MutableStateFlow(false)
    val historyShowFilters: StateFlow<Boolean> = _historyShowFilters.asStateFlow()

    private val _historySelectedCategory = MutableStateFlow("ALL")
    val historySelectedCategory: StateFlow<String> = _historySelectedCategory.asStateFlow()

    private val _historySelectedTimeFilter = MutableStateFlow(TimeFilter.ALL)
    val historySelectedTimeFilter: StateFlow<TimeFilter> = _historySelectedTimeFilter.asStateFlow()

    init {
        val currentUser = auth.currentUser
        _isAuthenticated.value = currentUser != null
        
        // Initial setup for Free/PRO
        checkInitialStatus()

        // Load snoozes
        val settingsPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val s1 = settingsPrefs.getInt("snooze1", 5)
        val s2 = settingsPrefs.getInt("snooze2", 10)
        _snooze1.value = s1
        _snooze2.value = s2
        timerManager.snooze1Min = s1
        timerManager.snooze2Min = s2

        if (currentUser != null) {
            syncUserAndData(currentUser.uid, currentUser.email ?: "")
        }
    }

    private fun checkInitialStatus() {
        if (!_isPro.value) {
            _isDarkMode.value = false
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
            AppCompatDelegate.setApplicationLocales(appLocale)
            _currentLanguage.value = "en"
        } else {
            // If PRO, load current locales if they exist
            if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
                _currentLanguage.value = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            }
        }
    }

    // --- Actions ---

    fun toggleProStatus() {
        val newState = !_isPro.value
        _isPro.value = newState
        prefs?.edit()?.putBoolean("is_pro", newState)?.apply()
        showMessage(if (newState) context.getString(R.string.pro_status_active) else context.getString(R.string.pro_status_free))
    }

    fun toggleTheme(isDark: Boolean) {
        if (!_isPro.value) return
        _isDarkMode.value = isDark
    }

    fun setLanguage(languageCode: String) {
        if (!_isPro.value) return
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        _currentLanguage.value = languageCode
        
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_REFRESH_NOTIFICATIONS
        }
        context.startService(intent)
    }

    fun setSnooze1(minutes: Int) { 
        _snooze1.value = minutes 
        timerManager.snooze1Min = minutes
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("snooze1", minutes).apply()
    }
    fun setSnooze2(minutes: Int) { 
        _snooze2.value = minutes 
        timerManager.snooze2Min = minutes
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("snooze2", minutes).apply()
    }

    fun setHistoryShowFilters(show: Boolean) { _historyShowFilters.value = show }
    fun setHistorySelectedCategory(category: String) { _historySelectedCategory.value = category }
    fun setHistorySelectedTimeFilter(filter: TimeFilter) { _historySelectedTimeFilter.value = filter }

    // --- Filtered History Logic ---
    val filteredHistory: StateFlow<List<HistoryEntity>> = combine(
        history,
        historySelectedCategory,
        historySelectedTimeFilter
    ) { historyItems, selectedCategory, selectedTimeFilter ->
        val now = System.currentTimeMillis()
        historyItems.filter { item ->
            val categoryMatch = selectedCategory == "ALL" || item.category == selectedCategory
            val timeMatch = when (selectedTimeFilter) {
                TimeFilter.ALL -> true
                TimeFilter.TODAY -> {
                    val itemCal = Calendar.getInstance().apply { timeInMillis = item.completedAt }
                    val nowCal = Calendar.getInstance()
                    itemCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) &&
                    itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
                }
                TimeFilter.WEEK -> item.completedAt >= (now - (7 * 24 * 60 * 60 * 1000L))
                TimeFilter.MONTH -> item.completedAt >= (now - (30 * 24 * 60 * 60 * 1000L))
            }
            categoryMatch && timeMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Auth Actions ---

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _isAuthenticated.value = true
                _authError.value = null
                syncUserAndData(auth.currentUser?.uid ?: "", email)
                showMessage(context.getString(R.string.msg_session_started))
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _isAuthenticated.value = true
                _authError.value = null
                syncUserAndData(auth.currentUser?.uid ?: "", email)
                showMessage(context.getString(R.string.msg_welcome))
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            if (email.isBlank()) {
                _authError.value = context.getString(R.string.forgot_error_empty)
                return@launch
            }
            try {
                auth.sendPasswordResetEmail(email).await()
                showMessage(context.getString(R.string.forgot_msg_sent, email))
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.localizedMessage
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            val result = googleAuthClient.handleSignInResult(data)
            result.onSuccess { success ->
                if (success) {
                    _isAuthenticated.value = true
                    _authError.value = null
                    syncUserAndData(auth.currentUser?.uid ?: "", auth.currentUser?.email ?: "")
                    showMessage(context.getString(R.string.msg_welcome))
                }
            }.onFailure { e ->
                _authError.value = "Error: ${e.localizedMessage}"
                _isAuthenticated.value = false
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            // Ya NO borramos todo por defecto al salir. 
            // Los datos se quedan en local. La limpieza se hará al ENTRAR con un usuario distinto.
            
            _userDisplayName.value = ""
            _userPhotoUrl.value = ""
            _isAuthenticated.value = false
            
            // Volvemos a valores por defecto de UI
            _isPro.value = false
            _currentLanguage.value = "en"
            _isDarkMode.value = false

            googleAuthClient.signOut()
            auth.signOut()
            showMessage(context.getString(R.string.logout))
        }
    }

    fun updateProfile(name: String, photoUrl: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            try {
                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                    photoUri = Uri.parse(photoUrl)
                }
                user.updateProfile(profileUpdates).await()
                firestore.collection("users").document(user.uid).update(
                    mapOf("displayName" to name, "photoUrl" to photoUrl)
                ).await()
                _userDisplayName.value = name
                _userPhotoUrl.value = photoUrl
                showMessage(context.getString(R.string.msg_profile_updated))
            } catch (e: Exception) {
                Log.e(TAG, "Update profile failed", e)
                showMessage(context.getString(R.string.msg_profile_error))
            }
        }
    }

    private fun syncUserAndData(uid: String, email: String) {
        viewModelScope.launch {
            if (uid.isBlank()) return@launch
            try {
                val finalEmail = if (email.isBlank()) auth.currentUser?.email ?: "" else email
                val userDocRef = firestore.collection("users").document(uid)
                val existingDoc = try { userDocRef.get().await() } catch (e: Exception) { null }
                val createdAt = if (existingDoc != null && existingDoc.exists()) {
                    existingDoc.getLong("createdAt") ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }
                val userMap = mutableMapOf<String, Any>("uid" to uid, "email" to finalEmail, "createdAt" to createdAt)
                auth.currentUser?.displayName?.let { 
                    userMap["displayName"] = it
                    _userDisplayName.value = it
                }
                val photoUrl = auth.currentUser?.photoUrl?.toString()
                if (photoUrl != null) {
                    userMap["photoUrl"] = photoUrl
                    _userPhotoUrl.value = photoUrl
                }
                userDocRef.set(userMap, SetOptions.merge()).await()
                
                // LIMPIEZA INTELIGENTE: 
                // Si el UID que entra es distinto al que había en los timers locales, 
                // entonces sí limpiamos para evitar mezclas.
                val currentLocalTimers = timerManager.timers.value
                val isDifferentUser = currentLocalTimers.any { it.uid.isNotEmpty() && it.uid != uid }
                
                if (isDifferentUser) {
                    Log.d(TAG, "[SYNC] Detectado cambio de usuario. Limpiando datos locales.")
                    timerManager.clearAllTimers()
                    historyRepository.clearAll()
                    presetRepository.clearAll()
                }

                // Cargamos datos de la nube
                timerManager.syncFromCloud(uid)
                timerManager.syncHistoryFromCloud(uid)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}")
            }
        }
    }

    // --- Timer Actions ---

    fun insert(name: String, duration: Long, color: Int, category: String, description: String = "") = viewModelScope.launch {
        timerManager.addTimer(name, duration, color, category, description)
    }

    fun update(timer: TimerEntity) = viewModelScope.launch {
        timerManager.toggleTimer(timer)
        if (timer.status != "LIVE") startService()
    }

    fun updateTimer(timer: TimerEntity) = viewModelScope.launch { 
        timerManager.updateTimer(timer.copy(baseDuration = timer.duration)) 
    }

    fun delete(timer: TimerEntity) = viewModelScope.launch { 
        timerManager.deleteTimer(timer)
        showMessage(context.getString(R.string.msg_deleted)) 
    }

    fun resetTimer(timer: TimerEntity) = viewModelScope.launch { timerManager.resetTimer(timer) }

    fun snoozeTimer(timer: TimerEntity, minutes: Int) = viewModelScope.launch {
        val additionalMs = minutes * 60 * 1000L
        val newDuration = timer.duration + additionalMs
        timerManager.updateTimer(timer.copy(
            remainingTime = additionalMs, 
            duration = newDuration,
            status = "LIVE",
            isSnoozed = true,
            lastSnoozeDuration = additionalMs
        ))
        startService()
        showMessage(context.getString(R.string.msg_snooze_added, minutes))
    }

    fun addInterval(timer: TimerEntity, label: String) = viewModelScope.launch {
        val minutes = (timer.remainingTime / 1000) / 60
        val seconds = (timer.remainingTime / 1000) % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)
        val newInterval = "$timeStr - $label"
        val currentIntervals = if (timer.intervalsJson == "[]") mutableListOf()
        else timer.intervalsJson.removeSurrounding("[", "]").split(", ").map { it.removeSurrounding("\"") }.toMutableList()
        currentIntervals.add(newInterval)
        val newIntervalsJson = "[\"${currentIntervals.joinToString("\", \"")}\"]"
        timerManager.updateTimer(timer.copy(intervalsJson = newIntervalsJson))
    }

    // --- History actions ---

    suspend fun updateHistory(history: HistoryEntity) {
        historyRepository.update(history)
        showMessage(context.getString(R.string.save))
    }

    fun deleteHistoryEntry(history: HistoryEntity) = viewModelScope.launch { 
        historyRepository.delete(history)
        showMessage(context.getString(R.string.msg_deleted)) 
    }

    // --- Presets ---

    fun saveAsPreset(name: String, duration: Long, color: Int, category: String, description: String = "") = viewModelScope.launch {
        presetRepository.insert(PresetEntity(name = name, durationMillis = duration, color = color, category = category, description = description, uid = auth.currentUser?.uid ?: ""))
        showMessage(context.getString(R.string.msg_preset_saved))
    }

    fun deletePreset(preset: PresetEntity) = viewModelScope.launch { 
        presetRepository.delete(preset)
        showMessage(context.getString(R.string.msg_preset_deleted)) 
    }

    fun startTimerFromPreset(preset: PresetEntity) = viewModelScope.launch {
        timerManager.addTimer(preset.name, preset.durationMillis, preset.color, preset.category, preset.description)
        showMessage(context.getString(R.string.msg_timer_added))
    }

    private fun startService() {
        val intent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _uiMessage.emit(message)
        }
    }

    // --- EXPORT TOOLS ---

    fun exportHistoryToCSV(items: List<HistoryEntity> = history.value) {
        viewModelScope.launch {
            if (items.isEmpty()) { showMessage(context.getString(R.string.msg_no_data)); return@launch }
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val csvHeader = "ID,Instrument,Category,Duration(ms),Completion Date,Notes\n"
            val csvRows = items.joinToString("\n") { 
                "${it.id},${it.timerName},${it.category},${it.durationMillis},${sdf.format(Date(it.completedAt))},\"${it.notes}\""
            }
            shareFile(csvHeader + csvRows, "MultiTimer_Export.csv", "text/csv")
        }
    }

    fun exportHistoryToPDF(items: List<HistoryEntity> = history.value) {
        viewModelScope.launch {
            if (items.isEmpty()) { showMessage("No hay datos"); return@launch }

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            // Header
            paint.color = Color.BLACK
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("MultiTimer PRO - Session Report", 50f, 50f, paint)
            
            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText("Generated on: ${sdf.format(Date())}", 50f, 80f, paint)

            // Table Header
            paint.isFakeBoldText = true
            canvas.drawText("Instrument", 50f, 130f, paint)
            canvas.drawText("Category", 200f, 130f, paint)
            canvas.drawText("Duration", 350f, 130f, paint)
            canvas.drawText("Completed At", 450f, 130f, paint)
            
            canvas.drawLine(50f, 140f, 550f, 140f, paint)

            // Rows
            paint.isFakeBoldText = false
            var y = 170f
            items.take(20).forEach { item -> 
                canvas.drawText(item.timerName.take(15), 50f, y, paint)
                canvas.drawText(item.category, 200f, y, paint)
                canvas.drawText(formatMillisToTime(item.durationMillis), 350f, y, paint)
                canvas.drawText(SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(item.completedAt)), 450f, y, paint)
                y += 30f
            }

            pdfDocument.finishPage(page)

            try {
                val fileName = "MultiTimer_Report_${System.currentTimeMillis()}.pdf"
                val file = File(context.cacheDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                pdfDocument.close()
                shareFileUri(file, "application/pdf")
            } catch (e: Exception) {
                Log.e(TAG, "PDF failed", e)
                showMessage(context.getString(R.string.msg_pdf_error))
            }
        }
    }

    private fun shareFile(content: String, fileName: String, mimeType: String) {
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        shareFileUri(file, mimeType)
    }

    private fun shareFileUri(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Exportar Reporte").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // --- GAMIFICATION ---
    private val _newAchievementEvent = MutableSharedFlow<String>(replay = 0)
    val newAchievementEvent = _newAchievementEvent.asSharedFlow()

    private var lastMedalsCount = -1

    val unlockedMedals: StateFlow<Set<String>> = history.map { list ->
        val medals = mutableSetOf<String>()
        val cal = Calendar.getInstance()
        
        if (list.any { it.durationMillis >= 2 * 3600000L }) medals.add("medal_deep_work")
        
        list.forEach { session ->
            cal.timeInMillis = session.completedAt
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour < 8) medals.add("medal_early_bird")
            if (hour >= 23) medals.add("medal_night_owl")
            
            val day = cal.get(Calendar.DAY_OF_WEEK)
            if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) medals.add("medal_weekend")
        }

        if (list.map { it.category }.distinct().size >= 3) medals.add("medal_collector")
        if (list.sumOf { it.durationMillis } >= 100 * 3600000L) medals.add("medal_veteran")
        
        val todayStart = Calendar.getInstance().apply { 
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        if (list.count { it.completedAt >= todayStart } >= 5) medals.add("medal_hyperfocus")
        if (checkStreak(list)) medals.add("medal_consistency")
        
        if (lastMedalsCount != -1 && medals.size > lastMedalsCount) {
            val newMedal = medals.subtract(unlockedMedals.value).firstOrNull()
            newMedal?.let { viewModelScope.launch { _newAchievementEvent.emit(it) } }
        }
        lastMedalsCount = medals.size
        medals
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private fun checkStreak(history: List<HistoryEntity>): Boolean {
        if (history.isEmpty()) return false
        val uniqueDays = history.map { 
            val c = Calendar.getInstance()
            c.timeInMillis = it.completedAt
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }.distinct().sortedDescending()
        
        if (uniqueDays.size < 7) return false
        
        var streak = 1
        for (i in 0 until uniqueDays.size - 1) {
            val diff = (uniqueDays[i] - uniqueDays[i+1]) / (1000 * 60 * 60 * 24)
            if (diff == 1L) {
                streak++
                if (streak >= 7) return true
            } else streak = 1
        }
        return false
    }

    val totalHoursFocused: StateFlow<Long> = history.map { list ->
        list.sumOf { it.durationMillis } / 3600000L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val currentXP: StateFlow<Long> = combine(history.map { it.sumOf { h -> h.durationMillis } }, unlockedMedals, history) { time, medals, historyList ->
        val minutesXP = time / 60000L
        val medalsXP = medals.size * 500L
        val streakXP = calculateCurrentStreak(historyList) * 50L
        minutesXP + medalsXP + streakXP
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private fun calculateCurrentStreak(history: List<HistoryEntity>): Int {
        if (history.isEmpty()) return 0
        val uniqueDays = history.map { 
            val c = Calendar.getInstance()
            c.timeInMillis = it.completedAt
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }.distinct().sortedDescending()

        val today = Calendar.getInstance().apply { 
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        if (uniqueDays.first() < today - (24 * 60 * 60 * 1000L)) return 0
        
        var streak = 0
        for (i in 0 until uniqueDays.size - 1) {
            if ((uniqueDays[i] - uniqueDays[i+1]) == (24 * 60 * 60 * 1000L)) streak++
            else break
        }
        return streak + 1
    }

    val currentRank: StateFlow<String> = currentXP.map { xp ->
        when {
            xp < 500 -> "rank_novice"
            xp < 2000 -> "rank_technician"
            xp < 5000 -> "rank_master"
            else -> "rank_architect"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "rank_novice")

    val nextRankXP: StateFlow<Long> = currentXP.map { xp ->
        when {
            xp < 500 -> 500L
            xp < 2000 -> 2000L
            xp < 5000 -> 5000L
            else -> 10000L
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 500L)

    private fun formatMillisToTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    val totalTimeSpent: StateFlow<Long> = filteredHistory.map { list ->
        list.sumOf { it.durationMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val globalTotalTime: StateFlow<Long> = history.map { list ->
        list.sumOf { it.durationMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val statsByCategory: StateFlow<Map<String, Long>> = filteredHistory.map { list ->
        list.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.durationMillis } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val averageSessionTime: StateFlow<Long> = filteredHistory.map { list ->
        if (list.isEmpty()) 0L else list.sumOf { it.durationMillis } / list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val mostProductiveDay: StateFlow<String> = filteredHistory.map { list ->
        if (list.isEmpty()) "---" else {
            val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
            list.groupBy { sdf.format(Date(it.completedAt)) }
                .maxByOrNull { it.value.sumOf { item -> item.durationMillis } }
                ?.key?.uppercase() ?: "---"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "---")

    val topTimerName: StateFlow<String> = filteredHistory.map { list ->
        if (list.isEmpty()) "---" else {
            list.groupBy { it.timerName }
                .maxByOrNull { it.value.sumOf { item -> item.durationMillis } }
                ?.key ?: "---"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "---")

    val activityLast7Days: StateFlow<Map<String, Long>> = combine(
        history,
        historySelectedCategory
    ) { historyItems, selectedCategory ->
        val result = mutableMapOf<String, Long>()
        val sdf = SimpleDateFormat("EE", Locale.getDefault())
        val last7DaysKeys = mutableListOf<String>()
        for (i in 0 until 7) {
            val tempCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayKey = sdf.format(tempCal.time).uppercase()
            last7DaysKeys.add(dayKey)
            result[dayKey] = 0L
        }
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        historyItems.filter { 
            it.completedAt >= sevenDaysAgo && (selectedCategory == "ALL" || it.category == selectedCategory)
        }.forEach { item ->
            val dayKey = sdf.format(Date(item.completedAt)).uppercase()
            if (result.containsKey(dayKey)) {
                result[dayKey] = (result[dayKey] ?: 0L) + item.durationMillis
            }
        }
        last7DaysKeys.reversed().associateWith { result[it] ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
