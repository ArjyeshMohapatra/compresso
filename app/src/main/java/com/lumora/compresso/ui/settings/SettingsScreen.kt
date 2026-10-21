package com.lumora.compresso.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumora.compresso.model.SwipeAction
import com.lumora.compresso.model.ThemeMode
import com.lumora.compresso.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel) {
    val leftAction by settingsViewModel.swipeLeftAction.collectAsState()
    val rightAction by settingsViewModel.swipeRightAction.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()

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
            // APPEARANCE SECTION
            Text("Appearance", style = MaterialTheme.typography.titleMedium)

            SettingsDropdown(
                label = "App Theme",
                currentValue = themeMode.label,
                options = ThemeMode.entries.map { it.label },
                onValueSelected = { selectedLabel ->
                    val selectedMode = ThemeMode.entries.find { it.label == selectedLabel }
                    if (selectedMode != null) settingsViewModel.updateThemeMode(selectedMode)
                }
            )

            Spacer(Modifier.height(8.dp))

            // GESTURES SECTION
            Text("Gestures", style = MaterialTheme.typography.titleMedium)

            SettingsDropdown(
                label = "Swipe Left Action",
                currentValue = leftAction.name,
                options = SwipeAction.entries.map { it.name },
                onValueSelected = { selectedName ->
                    val selectedAction = SwipeAction.entries.find { it.name == selectedName }
                    if (selectedAction != null) settingsViewModel.updateSwipeLeft(selectedAction)
                }
            )

            SettingsDropdown(
                label = "Swipe Right Action",
                currentValue = rightAction.name,
                options = SwipeAction.entries.map { it.name },
                onValueSelected = { selectedName ->
                    val selectedAction = SwipeAction.entries.find { it.name == selectedName }
                    if (selectedAction != null) settingsViewModel.updateSwipeRight(selectedAction)
                }
            )

            Spacer(Modifier.height(8.dp))

            // DEFAULTS SECTION
            Text("Defaults", style = MaterialTheme.typography.titleMedium)

            SettingsRow("Default save folder", "Not set")
            SettingsRow("Default image format", "JPEG")
            SettingsRow("Default compression quality", "80%")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    label: String,
    currentValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentValue,
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
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueSelected(option)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}