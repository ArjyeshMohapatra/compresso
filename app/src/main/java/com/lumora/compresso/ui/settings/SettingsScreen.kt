package com.lumora.compresso.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumora.compresso.model.SwipeAction
import com.lumora.compresso.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel) {
    val leftAction by settingsViewModel.swipeLeftAction.collectAsState()
    val rightAction by settingsViewModel.swipeRightAction.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Gestures", style = MaterialTheme.typography.titleMedium)

            SwipeActionDropdown(
                label = "Swipe Left Action",
                currentAction = leftAction,
                onActionSelected = { settingsViewModel.updateSwipeLeft(it) }
            )

            SwipeActionDropdown(
                label = "Swipe Right Action",
                currentAction = rightAction,
                onActionSelected = { settingsViewModel.updateSwipeRight(it) }
            )

            Spacer(Modifier.height(16.dp))
            Text("Defaults", style = MaterialTheme.typography.titleMedium)

            SettingsRow("Default save folder", "Not set")
            SettingsRow("Default image format", "JPEG")
            SettingsRow("Default compression quality", "80%")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActionDropdown(
    label: String,
    currentAction: SwipeAction,
    onActionSelected: (SwipeAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentAction.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SwipeAction.entries.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.name) },
                    onClick = {
                        onActionSelected(action)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}