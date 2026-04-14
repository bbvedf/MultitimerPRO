package com.android.multitimerpro.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.android.multitimerpro.R
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.ui.components.LogoutConfirmationDialog
import com.android.multitimerpro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsScreen(
    viewModel: TimerViewModel,
    onBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    
    val userDisplayName by viewModel.userDisplayName.collectAsState()
    val userPhotoUrl by viewModel.userPhotoUrl.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
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

    if (showProfileDialog) {
        EditProfileDialog(
            currentName = userDisplayName,
            currentAvatar = userPhotoUrl,
            onDismiss = { showProfileDialog = false },
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

        Surface(
            modifier = Modifier.fillMaxWidth().clickable { showProfileDialog = true },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(photoUrl = userPhotoUrl, size = 56.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = userDisplayName.ifBlank { stringResource(R.string.unknown_operator) }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(text = user?.email ?: stringResource(R.string.no_email), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                }
                Switch(checked = isDarkModeOverride ?: false, onCheckedChange = { viewModel.toggleTheme(it) }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Language Section
        Text(stringResource(R.string.language), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { showLanguageDialog = true },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = languages.find { it.code == currentLanguage }?.name ?: "English", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B4B).copy(alpha = 0.1f), contentColor = Color(0xFFFF4B4B)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4B4B).copy(alpha = 0.3f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text(stringResource(R.string.version_info), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun UserAvatar(photoUrl: String, size: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size / 4),
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
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var selectedAvatar by remember { mutableStateOf(currentAvatar) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.edit_profile), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(140.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(avatarPresets) { avatar ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedAvatar == avatar.id) avatar.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { selectedAvatar = avatar.id }
                                .border(if (selectedAvatar == avatar.id) 2.dp else 0.dp, avatar.color, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(avatar.icon, contentDescription = null, tint = if (selectedAvatar == avatar.id) avatar.color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
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
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirm(name, selectedAvatar) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.save)) }
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
    TextField(
        value = value,
        onValueChange = {
            val digitsOnly = it.filter { char -> char.isDigit() }
            if (digitsOnly.length <= 3) { // Permitimos hasta 999 min
                onValueChange(digitsOnly)
            }
        },
        modifier = Modifier.width(80.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    )
}
