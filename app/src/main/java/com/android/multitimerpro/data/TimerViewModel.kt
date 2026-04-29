package com.android.multitimerpro.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.multitimerpro.R
import com.android.multitimerpro.data.remote.ProfileDto
import com.android.multitimerpro.data.remote.SupabaseService
import com.android.multitimerpro.ui.theme.*
import com.android.multitimerpro.util.Constants
import com.android.multitimerpro.util.ShareUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import io.github.jan.supabase.postgrest.from
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerManager,
    private val historyRepository: HistoryRepository,
    private val presetRepository: PresetRepository,
    private val profileRepository: ProfileRepository,
    private val supabaseService: SupabaseService,
    private val googleAuthClient: GoogleAuthClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "TimerViewModel"
    private val prefs: SharedPreferences = context.getSharedPreferences("timers_prefs", Context.MODE_PRIVATE)

    // --- Auth State ---
    val currentUser: StateFlow<UserInfo?> = supabaseService.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // --- StateFlows (Room & Supabase) ---
    val allTimers: StateFlow<List<TimerEntity>> = timerManager.timers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timersCount: StateFlow<Int> = allTimers.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allPresets: StateFlow<List<PresetEntity>> = presetRepository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presetsCount: StateFlow<Int> = allPresets.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val history: StateFlow<List<HistoryEntity>> = currentUser.flatMapLatest { user ->
        val uid = user?.id ?: "ANONYMOUS"
        historyRepository.getHistoryByUid(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyCount: StateFlow<Int> = history.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isRecoveryMode = MutableStateFlow(false)
    val isRecoveryMode: StateFlow<Boolean> = _isRecoveryMode.asStateFlow()

    private val _userDisplayName = MutableStateFlow("")
    val userDisplayName: StateFlow<String> = _userDisplayName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userPhotoUrl = MutableStateFlow("robot")
    val userPhotoUrl: StateFlow<String> = _userPhotoUrl.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    // --- UI & Settings State ---
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _proEffectsEnabled = MutableStateFlow(true)
    val proEffectsEnabled: StateFlow<Boolean> = _proEffectsEnabled.asStateFlow()

    private val _snooze1 = MutableStateFlow(5)
    val snooze1: StateFlow<Int> = _snooze1.asStateFlow()

    private val _snooze2 = MutableStateFlow(10)
    val snooze2: StateFlow<Int> = _snooze2.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _showProUpgradeDialog = MutableStateFlow(false)
    val showProUpgradeDialog: StateFlow<Boolean> = _showProUpgradeDialog.asStateFlow()

    // --- History Filters ---
    private val _historyShowFilters = MutableStateFlow(false)
    val historyShowFilters: StateFlow<Boolean> = _historyShowFilters.asStateFlow()

    private val _historySelectedCategory = MutableStateFlow("ALL")
    val historySelectedCategory: StateFlow<String> = _historySelectedCategory.asStateFlow()

    private val _historySelectedTimeFilter = MutableStateFlow(TimeFilter.ALL)
    val historySelectedTimeFilter: StateFlow<TimeFilter> = _historySelectedTimeFilter.asStateFlow()

    val filteredHistory: StateFlow<List<HistoryEntity>> = combine(
        history, _historySelectedCategory, _historySelectedTimeFilter
    ) { list, cat, time ->
        list.filter { entry ->
            val matchesCategory = if (cat == "ALL") true else entry.category == cat
            val matchesTime = when (time) {
                TimeFilter.ALL -> true
                TimeFilter.TODAY -> isSameDay(entry.completedAt, System.currentTimeMillis())
                TimeFilter.WEEK -> isSameWeek(entry.completedAt, System.currentTimeMillis())
                TimeFilter.MONTH -> isSameMonth(entry.completedAt, System.currentTimeMillis())
            }
            matchesCategory && matchesTime
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Achievements & Rewards ---
    private val _permanentMedals = MutableStateFlow<Set<String>>(emptySet())
    val permanentMedals: StateFlow<Set<String>> = _permanentMedals.asStateFlow()

    private val _colorTokens = MutableStateFlow(0)
    val colorTokens: StateFlow<Int> = _colorTokens.asStateFlow()

    private val _unlockedProColors = MutableStateFlow<Set<String>>(emptySet())
    val unlockedProColors: StateFlow<Set<String>> = _unlockedProColors.asStateFlow()

    private val _silverRewardRedeemed = MutableStateFlow(false)
    val silverRewardRedeemed: StateFlow<Boolean> = _silverRewardRedeemed.asStateFlow()

    private val _silverRewardConsumed = MutableStateFlow(false)

    private val _newAchievementEvent = MutableSharedFlow<String>()
    val newAchievementEvent: SharedFlow<String> = _newAchievementEvent.asSharedFlow()

    private val _showCollectionCompleteDialog = MutableStateFlow<String?>(null)
    val showCollectionCompleteDialog: StateFlow<String?> = _showCollectionCompleteDialog.asStateFlow()

    private val _freeTimerLimit = MutableStateFlow(3)
    val freeTimerLimit: StateFlow<Int> = _freeTimerLimit.asStateFlow()

    private val _showTimerTokenAviso = MutableStateFlow(false)
    val showTimerTokenAviso: StateFlow<Boolean> = _showTimerTokenAviso.asStateFlow()

    // XP & Ranks
    private val _lastCheckIn = MutableStateFlow(0L)
    val lastCheckIn: StateFlow<Long> = _lastCheckIn.asStateFlow()

    private val _checkInStreak = MutableStateFlow(0)
    val checkInStreak: StateFlow<Int> = _checkInStreak.asStateFlow()

    private val _totalCheckIns = MutableStateFlow(0)
    val totalCheckIns: StateFlow<Int> = _totalCheckIns.asStateFlow()

    private val _extraXPFromCheckIn = MutableStateFlow(0L)

    val unlockedMedals: StateFlow<Set<String>> = combine(history, allPresets, _permanentMedals) { list, presets, perm ->
        val medals = mutableSetOf<String>()
        medals.addAll(perm)
        
        if (list.isEmpty() && presets.isEmpty() && perm.isEmpty()) return@combine medals
        
        val cal = Calendar.getInstance()
        fun getDayStart(m: Long): Long { 
            cal.timeInMillis = m
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis 
        }

        // Deep Work: 2h, 4h, 8h
        val maxDuration = if (list.isEmpty()) 0L else list.maxOf { it.durationMillis }
        if (maxDuration >= 8 * 3600000L) { medals.add("medal_deep_work_1"); medals.add("medal_deep_work_2"); medals.add("medal_deep_work_3") }
        else if (maxDuration >= 4 * 3600000L) { medals.add("medal_deep_work_1"); medals.add("medal_deep_work_2") }
        else if (maxDuration >= 2 * 3600000L) { medals.add("medal_deep_work_1") }

        // Finisher: 50, 250, 750
        if (list.size >= 750) { medals.add("medal_finisher_1"); medals.add("medal_finisher_2"); medals.add("medal_finisher_3") }
        else if (list.size >= 250) { medals.add("medal_finisher_1"); medals.add("medal_finisher_2") }
        else if (list.size >= 50) { medals.add("medal_finisher_1") }

        // Veteran: 100h, 500h, 1000h
        val totalMillis = list.sumOf { it.durationMillis }
        if (totalMillis >= 1000 * 3600000L) { medals.add("medal_veteran_1"); medals.add("medal_veteran_2"); medals.add("medal_veteran_3") }
        else if (totalMillis >= 500 * 3600000L) { medals.add("medal_veteran_1"); medals.add("medal_veteran_2") }
        else if (totalMillis >= 100 * 3600000L) { medals.add("medal_veteran_1") }
        
        // Consistency: 7, 30, 90 days
        val streak = calculateCurrentStreak(list)
        if (streak >= 90) { medals.add("medal_consistency_1"); medals.add("medal_consistency_2"); medals.add("medal_consistency_3") }
        else if (streak >= 30) { medals.add("medal_consistency_1"); medals.add("medal_consistency_2") }
        else if (streak >= 7) { medals.add("medal_consistency_1") }

        // Architect: 5, 15, 30 presets
        val psCount = presets.size
        if (psCount >= 30) { medals.add("medal_architect_1"); medals.add("medal_architect_2"); medals.add("medal_architect_3") }
        else if (psCount >= 15) { medals.add("medal_architect_1"); medals.add("medal_architect_2") }
        else if (psCount >= 5) { medals.add("medal_architect_1") }

        // Collector: 5h/20h in 4+ categories
        val cats = list.groupBy { it.category }
        if (cats.keys.size >= 4) {
            val minHours = cats.values.minOf { it.sumOf { s -> s.durationMillis } }
            if (minHours >= 20 * 3600000L) { medals.add("medal_collector_1"); medals.add("medal_collector_2"); medals.add("medal_collector_3") }
            else if (minHours >= 5 * 3600000L) { medals.add("medal_collector_1"); medals.add("medal_collector_2") }
            else medals.add("medal_collector_1")
        }

        // Early Bird & Night Owl
        val earlyDays = mutableSetOf<Long>(); val nightDays = mutableSetOf<Long>()
        list.forEach {
            cal.timeInMillis = it.completedAt
            val h = cal.get(Calendar.HOUR_OF_DAY)
            if (h < 7) earlyDays.add(getDayStart(it.completedAt))
            if (h >= 23) nightDays.add(getDayStart(it.completedAt))
        }
        if (earlyDays.size >= 90) { medals.add("medal_early_bird_1"); medals.add("medal_early_bird_2"); medals.add("medal_early_bird_3") }
        else if (earlyDays.size >= 30) { medals.add("medal_early_bird_1"); medals.add("medal_early_bird_2") }
        else if (earlyDays.size >= 7) medals.add("medal_early_bird_1")
        
        if (nightDays.size >= 90) { medals.add("medal_night_owl_1"); medals.add("medal_night_owl_2"); medals.add("medal_night_owl_3") }
        else if (nightDays.size >= 30) { medals.add("medal_night_owl_1"); medals.add("medal_night_owl_2") }
        else if (nightDays.size >= 7) medals.add("medal_night_owl_1")

        // Zen Master & Polymath
        val zen = list.count { it.durationMillis >= 3600000L && !it.isSnoozed }
        if (zen >= 150) { medals.add("medal_zen_master_1"); medals.add("medal_zen_master_2"); medals.add("medal_zen_master_3") }
        else if (zen >= 50) { medals.add("medal_zen_master_1"); medals.add("medal_zen_master_2") }
        else if (zen >= 10) { medals.add("medal_zen_master_1") }

        val polyGold = cats.values.count { it.sumOf { s -> s.durationMillis } >= 100 * 3600000L }
        if (polyGold >= 3) { medals.add("medal_polymath_1"); medals.add("medal_polymath_2"); medals.add("medal_polymath_3") }
        else if (cats.values.count { it.sumOf { s -> s.durationMillis } >= 25 * 3600000L } >= 3) { medals.add("medal_polymath_1"); medals.add("medal_polymath_2") }
        else if (cats.values.count { it.sumOf { s -> s.durationMillis } >= 5 * 3600000L } >= 3) medals.add("medal_polymath_1")

        // Weekend & Hyperfocus
        val wks = mutableSetOf<Int>()
        list.forEach { cal.timeInMillis = it.completedAt; if (cal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) wks.add(cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR)) }
        if (wks.size >= 24) { medals.add("medal_weekend_1"); medals.add("medal_weekend_2"); medals.add("medal_weekend_3") }
        else if (wks.size >= 12) { medals.add("medal_weekend_1"); medals.add("medal_weekend_2") }
        else if (wks.size >= 4) medals.add("medal_weekend_1")

        val maxInDay = list.groupBy { getDayStart(it.completedAt) }.values.map { it.size }.maxOrNull() ?: 0
        if (maxInDay >= 15) { medals.add("medal_hyperfocus_1"); medals.add("medal_hyperfocus_2"); medals.add("medal_hyperfocus_3") }
        else if (maxInDay >= 10) { medals.add("medal_hyperfocus_1"); medals.add("medal_hyperfocus_2") }
        else if (maxInDay >= 5) { medals.add("medal_hyperfocus_1") }

        medals
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val currentXP: StateFlow<Long> = combine(history, unlockedMedals, _extraXPFromCheckIn) { hist, medals, extra ->
        (hist.sumOf { it.durationMillis / 60000 }) + (medals.size * 500L) + extra
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val currentRank: StateFlow<String> = currentXP.map { xp ->
        when {
            xp >= 2000000L -> "${Constants.RANK_SUPREME}|${((xp - 2000000L) / 500000L) + 1}"
            xp >= 1000000L -> Constants.RANK_GRAND_ARCHITECT
            xp >= 600000L -> Constants.RANK_ARCHITECT
            xp >= 300000L -> Constants.RANK_MASTER
            xp >= 150000L -> Constants.RANK_SPECIALIST
            xp >= 70000L -> Constants.RANK_TECHNICIAN
            xp >= 30000L -> Constants.RANK_INITIATE
            xp >= 10000L -> Constants.RANK_APPRENTICE
            else -> Constants.RANK_NOVICE
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Constants.RANK_NOVICE)

    val nextRankXP: StateFlow<Long> = currentXP.map { xp ->
        when {
            xp >= 2000000L -> 2000000L + (((xp - 2000000L) / 500000L) + 1) * 500000L
            xp >= 1000000L -> 2000000L
            xp >= 600000L -> 1000000L
            xp >= 300000L -> 600000L
            xp >= 150000L -> 300000L
            xp >= 70000L -> 150000L
            xp >= 30000L -> 70000L
            xp >= 10000L -> 30000L
            else -> 10000L
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 10000L)

    val minRankXP: StateFlow<Long> = currentXP.map { xp ->
        when {
            xp >= 2000000L -> 2000000L + ((xp - 2000000L) / 500000L) * 500000L
            xp >= 1000000L -> 1000000L
            xp >= 600000L -> 600000L
            xp >= 300000L -> 300000L
            xp >= 150000L -> 150000L
            xp >= 70000L -> 150000L
            xp >= 30000L -> 70000L
            xp >= 10000L -> 30000L
            else -> 0L
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val rewardStatus: StateFlow<Map<String, Boolean>> = unlockedMedals.map { medals ->
        mapOf(
            "bronze_complete" to (medals.count { it.endsWith("_1") } >= 12),
            "silver_complete" to (medals.count { it.endsWith("_2") } >= 12),
            "gold_complete" to (medals.count { it.endsWith("_3") } >= 12)
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val isLegendary: StateFlow<Boolean> = unlockedMedals.map { medals ->
        medals.count { it.endsWith("_3") } >= 12
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Statistics
    val statsByCategory: StateFlow<Map<String, Float>> = history.map { list ->
        list.groupBy { it.category }.mapValues { it.value.sumOf { s -> s.durationMillis }.toFloat() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val totalTimeSpent: StateFlow<Long> = history.map { it.sumOf { s -> s.durationMillis } }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val activityLast7Days: StateFlow<Map<String, Long>> = history.map { list ->
        val cal = Calendar.getInstance()
        val result = mutableMapOf<String, Long>()
        val days = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        for (i in 0..6) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            result[days[c.get(Calendar.DAY_OF_WEEK) - 1]] = 0L
        }
        list.filter { it.completedAt > System.currentTimeMillis() - 7 * 86400000L && it.durationMillis < 86400000L }.forEach {
            cal.timeInMillis = it.completedAt
            val dayName = days[cal.get(Calendar.DAY_OF_WEEK) - 1]
            result[dayName] = (result[dayName] ?: 0L) + it.durationMillis
        }
        result
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val averageSessionTime: StateFlow<Long> = history.map { if (it.isEmpty()) 0L else it.sumOf { s -> s.durationMillis } / it.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val mostProductiveDay: StateFlow<String> = history.map { "Monday" }.stateIn(viewModelScope, SharingStarted.Eagerly, "Monday")
    val topTimerName: StateFlow<String> = history.map { it.groupBy { h -> h.timerName }.maxByOrNull { e -> e.value.size }?.key ?: "None" }.stateIn(viewModelScope, SharingStarted.Eagerly, "None")

    private val isHacking = AtomicBoolean(false)

    init {
        loadLocalSettings()
        setupAuthObserver()
        setupAchievementObserver()
        setupTimerLimitObserver()
        
        // Timer finishing event for achievements
        viewModelScope.launch {
            timerManager.timerFinishedEvent.collect { historyEntry ->
                // Medals are derived from history
            }
        }
    }

    private fun loadLocalSettings() {
        val s1 = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("snooze1", 5)
        val s2 = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("snooze2", 10)
        _snooze1.value = s1; _snooze2.value = s2
        timerManager.snooze1Min = s1; timerManager.snooze2Min = s2

        _isDarkMode.value = if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false) else null
        _proEffectsEnabled.value = prefs.getBoolean("pro_effects", true)
        _currentLanguage.value = prefs.getString("language", "en") ?: "en"
        updateAppLocale(_currentLanguage.value)

        _lastCheckIn.value = prefs.getLong("last_check_in", 0L)
        _checkInStreak.value = prefs.getInt("check_in_streak", 0)
        _totalCheckIns.value = prefs.getInt("total_check_ins", 0)
        _extraXPFromCheckIn.value = prefs.getLong("extra_xp_checkin", 0L)
        _colorTokens.value = prefs.getInt("color_tokens", 0)
        _unlockedProColors.value = prefs.getStringSet("unlocked_pro_colors", emptySet()) ?: emptySet()
        _permanentMedals.value = prefs.getStringSet("permanent_gold_medals", emptySet()) ?: emptySet()
        _silverRewardRedeemed.value = prefs.getBoolean("silver_reward_redeemed", false)
        _silverRewardConsumed.value = prefs.getBoolean("silver_reward_consumed", false)
    }

    private fun setupAuthObserver() {
        viewModelScope.launch {
            currentUser.collect { user ->
                _isAuthenticated.value = user != null
                if (user != null) {
                    _userEmail.value = user.email ?: ""
                    syncUserAndData(user.id, user.email ?: "")
                } else {
                    _userEmail.value = ""
                    _userDisplayName.value = ""
                    _userPhotoUrl.value = "robot"
                    _isPro.value = false
                }
            }
        }
    }

    private fun setupAchievementObserver() {
        viewModelScope.launch {
            var prevMedals = emptySet<String>()
            unlockedMedals.collect { medals ->
                if (prevMedals.isNotEmpty() && medals.size > prevMedals.size) {
                    val newGold = medals.filter { it.endsWith("_3") }.toSet()
                    val currentStored = _permanentMedals.value
                    if (!currentStored.containsAll(newGold)) {
                        val updated = currentStored + newGold
                        _permanentMedals.value = updated
                        prefs.edit().putStringSet("permanent_gold_medals", updated).apply()
                        saveProfileToCloud()
                    }
                    medals.subtract(prevMedals).forEach { _newAchievementEvent.emit(it) }
                }
                prevMedals = medals
                checkCollectionRewards(medals)
            }
        }
    }

    private fun setupTimerLimitObserver() {
        viewModelScope.launch {
            combine(_isPro, rewardStatus, _silverRewardRedeemed, _silverRewardConsumed) { p, r, red, cons -> 
                val silverComplete = r["silver_complete"] ?: false
                _freeTimerLimit.value = if (p) 100 else if (red) 4 else 3
                _showTimerTokenAviso.value = silverComplete && !cons
            }.collect()
        }

        viewModelScope.launch {
            combine(timerManager.timers, _silverRewardRedeemed) { list, redeemed ->
                list.size >= 4 && redeemed
            }.collect { isConsumed ->
                if (isConsumed && !_silverRewardConsumed.value) {
                    _silverRewardConsumed.value = true
                    prefs.edit().putBoolean("silver_reward_consumed", true).apply()
                    saveProfileToCloud()
                }
            }
        }
    }

    // --- Auth Actions ---
    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            try {
                supabaseService.login(email, pass)
                showMessage("Login exitoso")
            } catch (e: Exception) {
                _authError.value = e.message
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            try {
                supabaseService.signUp(email, pass)
                showMessage("Registro exitoso. Revisa tu email.")
            } catch (e: Exception) {
                _authError.value = e.message
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                supabaseService.resetPassword(email)
                showMessage("Email de recuperación enviado")
            } catch (e: Exception) {
                _authError.value = e.message
            }
        }
    }

    fun updatePassword(newPass: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Intentando actualizar contraseña...")
                supabaseService.updatePassword(newPass)
                _isRecoveryMode.value = false
                showMessage("Contraseña actualizada correctamente")
            } catch (e: Exception) {
                Log.e(TAG, "CRASH PREVENIDO: Error al actualizar contraseña", e)
                _authError.value = "Error al actualizar: ${e.message}"
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val result: Result<String> = googleAuthClient.handleSignInResult(data)
                if (result.isSuccess) {
                    val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.await()
                    
                    // IMPORTANTE: Primero cargamos el perfil de la nube.
                    // NO actualizamos el perfil con los datos de Google aquí mismo.
                    // La carga del perfil se dispara por el cambio en currentUser en el init{} o via loadUserProfile

                    showMessage("Sesión iniciada con Google")
                } else {
                    _authError.value = "Error Google: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _authError.value = "Error Google: ${e.message}"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            googleAuthClient.signOut()
            supabaseService.logout()

            // Clear local data
            timerManager.clearAllTimers()
            historyRepository.clearAll()
            presetRepository.clearAll()

            // Reset ViewModel StateFlows
            _isAuthenticated.value = false
            _isRecoveryMode.value = false
            _userDisplayName.value = ""
            _userEmail.value = ""
            _userPhotoUrl.value = "robot"
            _isPro.value = false
            
            // Limpieza selectiva: No borramos 'dark_mode' para que sea persistente en el dispositivo
            prefs.edit()
                .remove("display_name")
                .remove("photo_url")
                .remove("is_pro")
                .remove("permanent_gold_medals")
                .remove("color_tokens")
                .remove("unlocked_pro_colors")
                .remove("silver_reward_redeemed")
                .remove("silver_reward_consumed")
                .remove("last_check_in")
                .remove("check_in_streak")
                .remove("total_check_ins")
                .remove("extra_xp_checkin")
                .apply()

            _permanentMedals.value = emptySet()
            _colorTokens.value = 0
            _unlockedProColors.value = emptySet()
            _silverRewardRedeemed.value = false
            _silverRewardConsumed.value = false
            _lastCheckIn.value = 0L
            _checkInStreak.value = 0
            _totalCheckIns.value = 0
            _extraXPFromCheckIn.value = 0L

            // Clear SharedPreferences for user-specific data (KEEPING dark_mode)
            prefs.edit()
                .remove("permanent_gold_medals")
                .remove("color_tokens")
                .remove("unlocked_pro_colors")
                .remove("silver_reward_redeemed")
                .remove("silver_reward_consumed")
                .remove("bronze_reward_given")
                .remove("silver_reward_given")
                .remove("gold_reward_given")
                .remove("last_check_in")
                .remove("check_in_streak")
                .remove("total_check_ins")
                .remove("extra_xp_checkin")
                .apply()
            
            // Also clear settings SharedPreferences related to pro status / user data if any
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().remove("is_pro").apply()
        }
    }

    // --- Profile Actions ---
    private fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            val profile = profileRepository.getProfile(userId)
            if (profile != null) {
                _userDisplayName.value = profile.displayName ?: "Operator"
                _userPhotoUrl.value = profile.photoUrl ?: "robot"
                _isPro.value = profile.isPro
                
                // Ajustes de Snooze y Tema desde la nube
                _snooze1.value = profile.snooze1
                _snooze2.value = profile.snooze2
                timerManager.snooze1Min = profile.snooze1
                timerManager.snooze2Min = profile.snooze2
                
                // Persistencia local de snooze para evitar desincronización al reiniciar
                context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                    .putInt("snooze1", profile.snooze1)
                    .putInt("snooze2", profile.snooze2)
                    .apply()

                if (!profile.isPro) {
                    _isDarkMode.value = false
                    prefs.edit().putBoolean("dark_mode", false).apply()
                } else {
                    _isDarkMode.value = profile.darkMode
                    prefs.edit().putBoolean("dark_mode", profile.darkMode).apply()
                }

                _permanentMedals.value = profile.permanentGoldMedals.toSet()
                _colorTokens.value = profile.colorTokens
                _unlockedProColors.value = profile.unlockedProColors.toSet()
                _silverRewardRedeemed.value = profile.silverRewardRedeemed
                _silverRewardConsumed.value = profile.silverRewardConsumed
                _lastCheckIn.value = profile.lastCheckIn
                _checkInStreak.value = profile.checkInStreak
                _totalCheckIns.value = profile.totalCheckIns
                _extraXPFromCheckIn.value = profile.extraXpCheckIn
                userCreatedAt = profile.createdAt
                
                prefs.edit()
                    .putBoolean("is_pro", profile.isPro)
                    .putStringSet("permanent_gold_medals", _permanentMedals.value)
                    .putInt("color_tokens", _colorTokens.value)
                    .putStringSet("unlocked_pro_colors", _unlockedProColors.value)
                    .putBoolean("silver_reward_redeemed", _silverRewardRedeemed.value)
                    .putBoolean("silver_reward_consumed", _silverRewardConsumed.value)
                    .putLong("last_check_in", _lastCheckIn.value)
                    .putInt("check_in_streak", _checkInStreak.value)
                    .putInt("total_check_ins", _totalCheckIns.value)
                    .putLong("extra_xp_checkin", _extraXPFromCheckIn.value)
                    .apply()
            } else {
                val created = System.currentTimeMillis()
                userCreatedAt = created
                val newProfile = ProfileDto(id = userId, email = _userEmail.value, displayName = "Operator", photoUrl = "robot", createdAt = created)
                profileRepository.saveProfile(newProfile)
                _userDisplayName.value = "Operator"
                _userPhotoUrl.value = "robot"
            }
        }
    }

    fun updateProfile(name: String, photoUrl: String? = null) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            _userDisplayName.value = name
            photoUrl?.let { _userPhotoUrl.value = it }
            
            // Persistencia local inmediata para evitar "rollbacks" visuales
            prefs.edit()
                .putString("display_name", name)
                .apply()

            saveProfileToCloud()
        }
    }

    fun uploadUserAvatar(imageBytes: ByteArray) {
        viewModelScope.launch {
            val user = currentUser.value ?: run { showMessage("Usuario no autenticado."); return@launch }
            try {
                val avatarUrl = supabaseService.uploadAvatar(user.id, imageBytes)
                if (avatarUrl != null) {
                    _userPhotoUrl.value = avatarUrl
                    saveProfileToCloud()
                    showMessage("Avatar actualizado.")
                } else {
                    showMessage("Error al subir el avatar.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error subiendo avatar: ${e.message}", e)
                showMessage("Error al subir el avatar: ${e.message}")
            }
        }
    }

    private var userCreatedAt: Long? = null

    private fun saveProfileToCloud(forcedDarkMode: Boolean? = null) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                // Usamos el valor forzado si viene del botón, si no el del estado actual
                val targetDarkMode = forcedDarkMode ?: (_isDarkMode.value ?: false)
                
                Log.d(TAG, ">>> SUBIENDO A SUPABASE: ID=${user.id}, dark_mode=$targetDarkMode, s1=${_snooze1.value}, s2=${_snooze2.value}")
                
                val profile = ProfileDto(
                    id = user.id,
                    email = user.email,
                    displayName = _userDisplayName.value.ifEmpty { "Operator" },
                    photoUrl = _userPhotoUrl.value,
                    createdAt = userCreatedAt ?: System.currentTimeMillis().also { userCreatedAt = it },
                    isPro = _isPro.value,
                    darkMode = targetDarkMode,
                    snooze1 = _snooze1.value,
                    snooze2 = _snooze2.value,
                    permanentGoldMedals = _permanentMedals.value.toList(),
                    colorTokens = _colorTokens.value,
                    unlockedProColors = _unlockedProColors.value.toList(),
                    silverRewardRedeemed = _silverRewardRedeemed.value,
                    silverRewardConsumed = _silverRewardConsumed.value,
                    lastCheckIn = _lastCheckIn.value,
                    checkInStreak = _checkInStreak.value,
                    totalCheckIns = _totalCheckIns.value,
                    extraXpCheckIn = _extraXPFromCheckIn.value
                )
                
                profileRepository.saveProfile(profile)
                Log.d(TAG, ">>> SUPABASE OK: Celda actualizada.")
            } catch (e: Exception) {
                Log.e(TAG, ">>> SUPABASE ERROR: No se pudo actualizar la celda: ${e.message}")
            }
        }
    }

    private suspend fun syncUserAndData(userId: String, email: String) {
        loadUserProfile(userId)
        timerManager.reclaimLocalTimers(userId)
        presetRepository.reclaimLocalPresets(userId)
        historyRepository.reclaimLocalHistory(userId)

        timerManager.refreshTimersFromCloud(userId)
        historyRepository.refreshHistoryFromCloud(userId)
        presetRepository.refreshPresetsFromCloud(userId)
    }

    // --- Timer Operations ---
    fun saveTimer(name: String, durationMs: Long, color: Int, category: String, description: String = "") = viewModelScope.launch {
        val count = allTimers.value.size
        if (count >= _freeTimerLimit.value && !_isPro.value) {
            _showProUpgradeDialog.value = true
            showMessage(context.getString(R.string.msg_timer_limit_reached))
        } else {
            timerManager.addTimer(name, durationMs, color, category, description, currentUser.value?.id ?: "")
            showMessage(context.getString(R.string.msg_timer_created))
        }
    }

    fun toggleTimer(timer: TimerEntity) = viewModelScope.launch { timerManager.toggleTimer(timer) }
    fun deleteTimer(timer: TimerEntity) = viewModelScope.launch { timerManager.deleteTimer(timer) }
    fun delete(timer: TimerEntity) = viewModelScope.launch { timerManager.deleteTimer(timer) }
    fun updateTimer(timer: TimerEntity) = viewModelScope.launch { timerManager.updateTimer(timer) }
    fun update(timer: TimerEntity) = viewModelScope.launch { timerManager.updateTimer(timer) }
    fun resetTimer(timer: TimerEntity) = viewModelScope.launch { timerManager.resetTimer(timer) }
    fun addInterval(timer: TimerEntity, label: String) = viewModelScope.launch { timerManager.addInterval(timer, label) }
    fun insert(name: String, durationMs: Long, color: Int, category: String, description: String = "") = saveTimer(name, durationMs, color, category, description)

    fun startTimerFromPreset(preset: PresetEntity) = saveTimer(preset.name, preset.durationMillis, preset.color, preset.category, preset.description)

    fun savePreset(name: String, durationMs: Long, color: Int, category: String, description: String = "") = viewModelScope.launch {
        val preset = PresetEntity(name = name, durationMillis = durationMs, color = color, category = category, description = description, uid = currentUser.value?.id ?: "")
        presetRepository.insert(preset)
        showMessage("PRESET SAVED")
    }
    fun deletePreset(preset: PresetEntity) = viewModelScope.launch { presetRepository.delete(preset) }
    fun deleteAllPresets() = viewModelScope.launch { presetRepository.clearAll(); showMessage(context.getString(R.string.msg_preset_deleted)) }
    fun deleteAllTimers() = viewModelScope.launch { timerManager.clearAllTimers(); showMessage(context.getString(R.string.msg_deleted)) }
    fun saveAsPreset(name: String, durationMs: Long, color: Int, category: String, description: String = "", id: String? = null) = savePreset(name, durationMs, color, category, description)

    // --- History Operations ---
    fun deleteHistoryEntry(entry: HistoryEntity) = viewModelScope.launch { historyRepository.delete(entry) }
    fun updateHistory(entry: HistoryEntity) = viewModelScope.launch { historyRepository.update(entry) }
    fun setHistoryFilters(show: Boolean, cat: String, time: TimeFilter) { _historyShowFilters.value = show; _historySelectedCategory.value = cat; _historySelectedTimeFilter.value = time }
    fun setHistorySelectedTimeFilter(filter: TimeFilter) { _historySelectedTimeFilter.value = filter }
    fun setHistorySelectedCategory(category: String) { _historySelectedCategory.value = category }
    fun setHistoryShowFilters(show: Boolean) { _historyShowFilters.value = show }

    fun exportHistoryToPDF(items: List<HistoryEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = ShareUtils.generatePDF(context, items)
            uri?.let {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, it)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Export PDF")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        }
    }

    fun exportHistoryToCSV(items: List<HistoryEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = ShareUtils.generateCSV(context, items)
            uri?.let {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, it)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Export CSV")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        }
    }
    
    fun shareMedal(medalId: String) {
        val tier = if (medalId.endsWith("_3")) 3 else if (medalId.endsWith("_2")) 2 else 1
        val name = medalId.replace("medal_", "").replace("_", " ").uppercase()
        val desc = "Unocked after reaching milestone in Multitimer PRO"
        val uri = ShareUtils.generateMedalShareCard(context, name, desc, tier, currentRank.value)
        uri?.let {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, it)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Medal").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    // --- Settings & UI Logic ---
    fun toggleTheme(dark: Boolean? = null) {
        val newValue = dark ?: !(_isDarkMode.value ?: false)
        
        Log.d(TAG, "BOTÓN PULSADO: Cambiando tema a ${if(newValue) "DARK" else "LIGHT"}")
        
        // 1. Actualización local inmediata para que el usuario vea el cambio ya
        _isDarkMode.value = newValue
        prefs.edit().putBoolean("dark_mode", newValue).apply()
        
        // 2. Sincronización forzosa con Supabase
        if (isAuthenticated.value) {
            saveProfileToCloud(forcedDarkMode = newValue)
        }
    }
    fun setDarkMode(enabled: Boolean) = toggleTheme(enabled)

    fun toggleProEffects(enabled: Boolean) {
        _proEffectsEnabled.value = enabled
        prefs.edit().putBoolean("pro_effects", enabled).apply()
    }

    fun setLanguage(langCode: String) {
        _currentLanguage.value = langCode
        prefs.edit().putString("language", langCode).apply()
        updateAppLocale(langCode)
    }

    private fun updateAppLocale(langCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun setSnooze1(min: Int) {
        _snooze1.value = min
        timerManager.snooze1Min = min
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("snooze1", min).apply()
        if (isAuthenticated.value) saveProfileToCloud()
    }

    fun setSnooze2(min: Int) {
        _snooze2.value = min
        timerManager.snooze2Min = min
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("snooze2", min).apply()
        if (isAuthenticated.value) saveProfileToCloud()
    }

    fun dismissProUpgradeDialog() { _showProUpgradeDialog.value = false }
    fun dismissCollectionDialog() { _showCollectionCompleteDialog.value = null }
    fun setRecoveryMode(enabled: Boolean) { _isRecoveryMode.value = enabled }

    fun resetAllAchievements() {
        viewModelScope.launch(Dispatchers.IO) {
            _permanentMedals.value = emptySet(); _colorTokens.value = 0; _unlockedProColors.value = emptySet()
            _silverRewardRedeemed.value = false; _silverRewardConsumed.value = false
            _checkInStreak.value = 0; _totalCheckIns.value = 0; _extraXPFromCheckIn.value = 0L; _lastCheckIn.value = 0L
            
            prefs.edit()
                .remove("permanent_gold_medals").remove("color_tokens").remove("unlocked_pro_colors")
                .remove("silver_reward_redeemed").remove("silver_reward_consumed").remove("bronze_reward_given")
                .remove("silver_reward_given").remove("gold_reward_given").remove("last_check_in")
                .remove("check_in_streak").remove("total_check_ins").remove("extra_xp_checkin")
                .apply()
            
            saveProfileToCloud()
            historyRepository.clearAll(currentUser.value?.id)
            withContext(Dispatchers.Main) { showMessage("ACHIEVEMENTS RESET.") }
        }
    }

    fun handleManualRecovery(uri: Uri) {
        val uriString = uri.toString()
        val fragment = uri.fragment ?: uriString.substringAfter("#", "").substringBefore("?")
        
        if (fragment.contains("access_token=")) {
            val params = fragment.split("&").associate {
                val split = it.split("=")
                split[0] to split.getOrElse(1) { "" }
            }
            val accessToken = params["access_token"]
            val refreshToken = params["refresh_token"]
            
            if (!accessToken.isNullOrBlank()) {
                viewModelScope.launch {
                    try {
                        val success = supabaseService.importSessionManual(accessToken, refreshToken ?: "")
                        if (!success) {
                            _authError.value = "Error de sincronización de sesión. Inténtalo de nuevo."
                        }
                        _isRecoveryMode.value = true
                    } catch (e: Exception) {
                        // Silent fail
                    }
                }
            }
        } else {
            _isRecoveryMode.value = true
        }
    }

    // --- Achievement & Rewards Logic ---
    fun performDailyCheckIn() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        
        if (_lastCheckIn.value < todayStart) {
            val yesterdayStart = todayStart - 86400000L
            if (_lastCheckIn.value >= yesterdayStart) {
                _checkInStreak.value = (_checkInStreak.value % 7) + 1
            } else {
                _checkInStreak.value = 1
            }
            val rewards = listOf(5, 10, 15, 20, 25, 30, 50)
            val rewardXP = rewards[(_checkInStreak.value - 1).coerceIn(0, 6)].toLong()
            
            _lastCheckIn.value = todayStart
            _totalCheckIns.value += 1
            _extraXPFromCheckIn.value += rewardXP
            
            prefs.edit()
                .putLong("last_check_in", todayStart)
                .putInt("check_in_streak", _checkInStreak.value)
                .putInt("total_check_ins", _totalCheckIns.value)
                .putLong("extra_xp_checkin", _extraXPFromCheckIn.value)
                .apply()
            
            saveProfileToCloud()
            showMessage("CHECK-IN DAY ${_checkInStreak.value}: +$rewardXP XP")
        }
    }

    fun unlockProColor(colorName: String) {
        if (_colorTokens.value >= 1 && !_unlockedProColors.value.contains(colorName)) {
            _colorTokens.value -= 1
            _unlockedProColors.value = _unlockedProColors.value + colorName
            prefs.edit()
                .putInt("color_tokens", _colorTokens.value)
                .putStringSet("unlocked_pro_colors", _unlockedProColors.value)
                .apply()
            saveProfileToCloud()
            showMessage("COLOR UNLOCKED: $colorName")
        }
    }
    fun unlockColorWithToken(color: String) = unlockProColor(color)

    fun redeemTimerToken() {
        _silverRewardRedeemed.value = true
        _freeTimerLimit.value = 4
        prefs.edit().putBoolean("silver_reward_redeemed", true).apply()
        saveProfileToCloud()
        showMessage(context.getString(R.string.reward_silver))
    }

    private fun checkCollectionRewards(medals: Set<String>) {
        val br = medals.count { it.endsWith("_1") } >= 12
        if (br && !prefs.getBoolean("bronze_reward_given", false)) {
            _colorTokens.value += 1
            prefs.edit().putBoolean("bronze_reward_given", true).apply()
            saveProfileToCloud()
            _showCollectionCompleteDialog.value = "BRONZE"
        }
        val sl = medals.count { it.endsWith("_2") } >= 12
        if (sl && !prefs.getBoolean("silver_reward_given", false)) { 
            prefs.edit().putBoolean("silver_reward_given", true).apply()
            saveProfileToCloud()
            _showCollectionCompleteDialog.value = "SILVER"
        }
        val gl = medals.count { it.endsWith("_3") } >= 12
        if (gl && !prefs.getBoolean("gold_reward_given", false)) { 
            prefs.edit().putBoolean("gold_reward_given", true).apply()
            saveProfileToCloud()
            _showCollectionCompleteDialog.value = "GOLD"
        }
    }

    // --- Helpers ---
    fun showMessage(msg: String) {
        viewModelScope.launch { _uiMessage.emit(msg) }
    }
    fun clearAuthError() { _authError.value = null }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    fun calculateCurrentStreak(history: List<HistoryEntity>): Int {
        if (history.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val days = history.map {
            cal.timeInMillis = it.completedAt
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()
        var streak = 0
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        var currentCheck = cal.timeInMillis
        for (day in days) { 
            if (day == currentCheck) { streak++; currentCheck -= 86400000L } 
            else if (day < currentCheck) break
        }
        return streak
    }

}
