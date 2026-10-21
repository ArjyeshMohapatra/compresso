package com.lumora.compresso.viewmodel

import androidx.lifecycle.ViewModel
import com.lumora.compresso.model.SwipeAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {
    // Default actions
    private val _swipeLeftAction = MutableStateFlow(SwipeAction.DELETE)
    val swipeLeftAction: StateFlow<SwipeAction> = _swipeLeftAction.asStateFlow()

    private val _swipeRightAction = MutableStateFlow(SwipeAction.NONE)
    val swipeRightAction: StateFlow<SwipeAction> = _swipeRightAction.asStateFlow()

    fun updateSwipeLeft(action: SwipeAction) {
        _swipeLeftAction.value = action
    }

    fun updateSwipeRight(action: SwipeAction) {
        _swipeRightAction.value = action
    }
}