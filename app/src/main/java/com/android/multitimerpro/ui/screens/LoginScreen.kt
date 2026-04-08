package com.android.multitimerpro.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.multitimerpro.R
import com.android.multitimerpro.data.TimerViewModel
import com.android.multitimerpro.ui.theme.*

@Composable
fun LoginScreen(
    onGoogleLogin: () -> Unit,
    onEmailLogin: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val authError by viewModel.authError.collectAsState()
    val isDarkModeOverride by viewModel.isDarkMode.collectAsState()
    
    val isDark = isDarkModeOverride ?: isSystemInDarkTheme()
    val logoRes = if (isDark) R.drawable.logo_dark else R.drawable.logo_light

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Theme Toggle Button
        IconButton(
            onClick = { viewModel.toggleTheme(!isDark) },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Theme",
                tint = if (isDark) Color.White else Color.Black
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Real Logo from resources
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = "MultiTimer PRO Logo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "MultiTimer PRO",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Display Error if any
            authError?.let { error ->
                Surface(
                    color = Color(0xFFFF4B4B).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4B4B)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "ERROR: $error",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Google Login Button with real logo
            Button(
                onClick = onGoogleLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color.White else Color(0xFFF2F2F2)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.google_logo),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google", 
                        color = Color.Black, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                Text(
                    text = " OR ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Email Input
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("EMAIL IDENTIFIER", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            TextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ACCESS KEY", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = { onEmailLogin(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "INITIALIZE SYSTEM",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDark) DeepBlack else Color.White,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "New operator? ", color = MaterialTheme.colorScheme.onBackground)
                TextButton(onClick = onRegisterClick) {
                    Text(
                        text = "Register now", 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
