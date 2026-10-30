package com.lumora.compresso.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.lumora.compresso.model.SwipeAction
import com.lumora.compresso.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("compresso_settings", Application.MODE_PRIVATE)

    // Swipe actions
    private val _swipeLeftAction = MutableStateFlow(SwipeAction.DELETE)
    val swipeLeftAction: StateFlow<SwipeAction> = _swipeLeftAction.asStateFlow()

    private val _swipeRightAction = MutableStateFlow(SwipeAction.NONE)
    val swipeRightAction: StateFlow<SwipeAction> = _swipeRightAction.asStateFlow()

    // Theme Mode
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Save Folder
    private val _saveFolderUri = MutableStateFlow<Uri?>(null)
    val saveFolderUri: StateFlow<Uri?> = _saveFolderUri.asStateFlow()

    init {
        // Load saved folder from storage on startup
        val savedUriString = prefs.getString("save_folder_uri", null)
        if (savedUriString != null) {
            _saveFolderUri.value = Uri.parse(savedUriString)
        }
    }

    fun updateSwipeLeft(action: SwipeAction) { _swipeLeftAction.value = action }
    fun updateSwipeRight(action: SwipeAction) { _swipeRightAction.value = action }
    fun updateThemeMode(mode: ThemeMode) { _themeMode.value = mode }

    fun updateSaveFolder(uri: Uri) {
        _saveFolderUri.value = uri
        prefs.edit().putString("save_folder_uri", uri.toString()).apply()
    }
}