package com.android.multitimerpro.ui.screens

import androidx.compose.runtime.Composable
import com.android.multitimerpro.ui.TimerViewModel

@Composable
fun HomeScreen(viewModel: TimerViewModel, onNavigateToCreate: () -> Unit) {
    MultiTimerHomeScreen(viewModel, onNavigateToCreate)
}
