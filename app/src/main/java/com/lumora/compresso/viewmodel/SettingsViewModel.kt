package com.lumora.compresso.viewmodel

import androidx.lifecycle.ViewModel
import com.lumora.compresso.model.SwipeAction
import com.lumora.compresso.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {
    // Swipe actions
    private val _swipeLeftAction = MutableStateFlow(SwipeAction.DELETE)
    val swipeLeftAction: StateFlow<SwipeAction> = _swipeLeftAction.asStateFlow()

    private val _swipeRightAction = MutableStateFlow(SwipeAction.NONE)
    val swipeRightAction: StateFlow<SwipeAction> = _swipeRightAction.asStateFlow()

    // Theme Mode
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun updateSwipeLeft(action: SwipeAction) {
        _swipeLeftAction.value = action
    }

    fun updateSwipeRight(action: SwipeAction) {
        _swipeRightAction.value = action
    }

    fun updateThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }
}