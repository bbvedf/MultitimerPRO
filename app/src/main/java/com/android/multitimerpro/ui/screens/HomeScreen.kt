package com.android.multitimerpro.ui.screens

import androidx.compose.runtime.Composable
import com.android.multitimerpro.data.TimerViewModel

@Composable
fun HomeScreen(viewModel: TimerViewModel, onNavigateToCreate: (Int?) -> Unit) {
    MultiTimerHomeScreen(viewModel, onNavigateToCreate)
}
