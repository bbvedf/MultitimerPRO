package com.android.multitimerpro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.android.multitimerpro.R
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.ui.components.LegendaryBadge
import com.android.multitimerpro.ui.components.LogoutConfirmationDialog
import com.android.multitimerpro.ui.components.UpgradeProDialog
import com.android.multitimerpro.ui.theme.*
// Borrado FirebaseAuth import

@Composable
fun SettingsScreen(
    viewModel: TimerViewModel,
    onBack: () -> Unit
) {
    val userDisplayName by viewModel.userDisplayName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userPhotoUrl by viewModel.userPhotoUrl.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }
    
    val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val isLegendary by viewModel.isLegendary.collectAsState()
    val proEffectsEnabled by viewModel.proEffectsEnabled.collectAsState()
    val snooze1 by viewModel.snooze1.collectAsState()
    val snooze2 by viewModel.snooze2.collectAsState()

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.signOut()
                onBack()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bytes = com.android.multitimerpro.util.ShareUtils.compressImage(context, it)
            if (bytes != null) {
                viewModel.uploadUserAvatar(bytes)
            } else {
                viewModel.showMessage("Error al procesar la imagen.")
            }
        }
    }

    if (showProfileDialog) {
        EditProfileDialog(
            currentName = userDisplayName,
            currentAvatar = userPhotoUrl,
            isPro = isPro,
            onDismiss = { showProfileDialog = false },
            onCustomAvatarClick = { 
                if (isPro) {
                    photoPickerLauncher.launch("image/*")
                    showProfileDialog = false
                } else {
                    showProfileDialog = false
                    showProDialog = true
                }
            },
            onConfirm = { name, avatar ->
                viewModel.updateProfile(name, avatar)
                showProfileDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onConfirm = { langCode ->
                viewModel.setLanguage(langCode)
                showLanguageDialog = false
            }
        )
    }

    if (showProDialog) {
        UpgradeProDialog(
            onDismiss = { showProDialog = false },
            onUpgrade = { 
                // TODO: Implement real billing flow
                showProDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // User Profile Section
        Text(stringResource(R.string.operator_account), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(16.dp))

        val infiniteTransition = rememberInfiniteTransition(label = "ProAvatarGlow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowAlpha"
        )

        val isDark = isDarkModeOverride ?: isSystemInDarkTheme()
        val proBrush = Brush.linearGradient(
            colors = if (isDark) listOf(ProCardDarkStart, ProCardDarkMiddle, ProCardDarkEnd)
                     else listOf(ProCardLightStart, ProCardLightEnd)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showProfileDialog = true }
                .then(
                    if (isPro) Modifier.border(
                        width = 1.dp,
                        brush = Brush.sweepGradient(listOf(ProGlowGold, ProGlowOrange, ProGlowGold)),
                        shape = RoundedCornerShape(24.dp)
                    ) else Modifier
                ),
            color = if (isPro) Color.Transparent else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = if (!isPro) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) else null,
            shadowElevation = if (isPro) 12.dp else 0.dp
        ) {
            Box(modifier = Modifier.then(if (isPro) Modifier.background(proBrush) else Modifier)) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Avatar con Glow Animado
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .then(
                                if (isPro) {
                                    if (isDark) Modifier.border(2.dp, ProGlowGold.copy(alpha = glowAlpha), CircleShape)
                                    else Modifier.border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                                } else Modifier
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatar(photoUrl = userPhotoUrl, size = 64.dp)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userDisplayName.ifBlank { stringResource(R.string.unknown_operator) },
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isPro) (if (isDark) Color.White else CharcoalGrey) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (isLegendary) {
                                Spacer(modifier = Modifier.width(8.dp))
                                LegendaryBadge()
                            }
                        }
                        Text(
                            text = userEmail.ifBlank { stringResource(R.string.no_email) },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPro) (if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isPro) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = if (isDark) AccentCyan else CharcoalGrey,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PRO Features Section
        if (isPro) {
            Text(stringResource(R.string.settings_pro_config), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.settings_dynamic_effects), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.settings_dynamic_effects_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = proEffectsEnabled,
                        onCheckedChange = { viewModel.toggleProEffects(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.5f))
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Theme Section
        Text(stringResource(R.string.visual_preferences), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (isDarkModeOverride == true) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = if (isDarkModeOverride == true) stringResource(R.string.dark_mode) else stringResource(R.string.light_mode), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    if (!isPro) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("👑", fontSize = 12.sp)
                    }
                }
                Switch(
                    checked = isDarkModeOverride ?: false, 
                    onCheckedChange = { 
                        if (isPro) viewModel.toggleTheme(it) 
                        else showProDialog = true
                    }, 
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Language Section
        Text(stringResource(R.string.language), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { 
                if (isPro) showLanguageDialog = true 
                else showProDialog = true
            },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = languages.find { it.code == currentLanguage }?.name ?: "English", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    if (!isPro) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("👑", fontSize = 12.sp)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Snooze Section
        Text(stringResource(R.string.snooze_config), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Snooze, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(stringResource(R.string.snooze_1), color = MaterialTheme.colorScheme.onSurface)
                    }
                    SnoozeInput(value = snooze1.toString(), onValueChange = { viewModel.setSnooze1(it.toIntOrNull() ?: 0) })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Snooze, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(stringResource(R.string.snooze_2), color = MaterialTheme.colorScheme.onSurface)
                    }
                    SnoozeInput(value = snooze2.toString(), onValueChange = { viewModel.setSnooze2(it.toIntOrNull() ?: 0) })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Text(stringResource(R.string.actions), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed.copy(alpha = 0.1f), contentColor = DestructiveRed),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DestructiveRed.copy(alpha = 0.3f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.version_info), 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun UserAvatar(photoUrl: String, size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (photoUrl.startsWith("http")) {
            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            val icon = avatarPresets.find { it.id == photoUrl }?.icon ?: Icons.Default.Person
            val color = avatarPresets.find { it.id == photoUrl }?.color ?: MaterialTheme.colorScheme.primary
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(color.copy(alpha = 0.1f))) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size / 2))
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentAvatar: String,
    isPro: Boolean,
    onDismiss: () -> Unit,
    onCustomAvatarClick: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var selectedAvatar by remember { mutableStateOf(currentAvatar) }
    val isDark = isSystemInDarkTheme()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.edit_profile),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Grid de Avatares (6 Presets + 1 Slot Doble PRO)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Los primeros 6 presets (fila 1 completa + mitad de fila 2)
                    items(avatarPresets.take(6)) { avatar ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selectedAvatar == avatar.id) avatar.color.copy(alpha = 0.2f) 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable { selectedAvatar = avatar.id }
                                .border(
                                    if (selectedAvatar == avatar.id) 2.dp else 1.dp, 
                                    if (selectedAvatar == avatar.id) avatar.color else Color.Transparent, 
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                avatar.icon, 
                                contentDescription = null, 
                                tint = if (selectedAvatar == avatar.id) avatar.color 
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // 2. El slot DOBLE para UPLOAD PRO (ocupa los 2 últimos huecos)
                    item(span = { GridItemSpan(2) }) {
                        val proGradient = Brush.linearGradient(listOf(AccentCyan, NeonPurple))
                        val isCustomSelected = selectedAvatar.startsWith("http")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2.2f) // Ajustado para igualar la altura de los slots 1x1 + gap
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isCustomSelected) AccentCyan.copy(alpha = 0.1f)
                                    else if (isDark) CharcoalGrey
                                    else Color.White
                                )
                                .clickable { onCustomAvatarClick() }
                                .border(
                                    width = 2.dp,
                                    brush = proGradient,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.upload_avatar),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("👑", fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.operator_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Botones de Acción
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss, 
                        modifier = Modifier.weight(1f)
                    ) { 
                        Text(stringResource(R.string.cancel)) 
                    }
                    Button(
                        onClick = { onConfirm(name, selectedAvatar) }, 
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) { 
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) 
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                
                languages.forEach { lang ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onConfirm(lang.code) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = lang.flag, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = lang.name, color = if (currentLanguage == lang.code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (currentLanguage == lang.code) FontWeight.Bold else FontWeight.Normal)
                        }
                        if (currentLanguage == lang.code) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

data class Language(val name: String, val code: String, val flag: String)
val languages = listOf(
    Language("English", "en", "🇺🇸"),
    Language("Español", "es", "🇪🇸"),
    Language("Deutsch", "de", "🇩🇪"),
    Language("Français", "fr", "🇫🇷"),
    Language("Italiano", "it", "🇮🇹"),
    Language("Português", "pt", "🇵🇹")
)

data class AvatarPreset(val id: String, val icon: ImageVector, val color: Color)
val avatarPresets = listOf(
    AvatarPreset("robot", Icons.Default.SmartToy, NeonBlue),
    AvatarPreset("timer", Icons.Default.Timer, NeonGreen),
    AvatarPreset("star", Icons.Default.Grade, NeonOrange),
    AvatarPreset("bolt", Icons.Default.Bolt, NeonRed),
    AvatarPreset("ninja", Icons.Default.Shield, NeonPurple),
    AvatarPreset("space", Icons.Default.RocketLaunch, NeonBlueDim),
    AvatarPreset("gear", Icons.Default.Settings, NeonGreenDim),
    AvatarPreset("key", Icons.Default.VpnKey, Color.Gray)
)

@Composable
fun SnoozeInput(value: String, onValueChange: (String) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(value) }

    // Sincronizar con el valor externo solo si no tenemos el foco
    LaunchedEffect(value) {
        if (!isFocused) {
            textFieldValue = value
        }
    }

    TextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val digitsOnly = newValue.filter { it.isDigit() }
            if (digitsOnly.length <= 3) {
                textFieldValue = digitsOnly
                // Notificamos al padre, pero mantenemos nuestro estado local "" si está vacío
                onValueChange(digitsOnly.ifEmpty { "0" })
            }
        },
        modifier = Modifier
            .width(64.dp)
            .onFocusChanged { 
                isFocused = it.isFocused
                if (it.isFocused && (textFieldValue == "0" || textFieldValue == "00")) {
                    textFieldValue = ""
                }
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}
