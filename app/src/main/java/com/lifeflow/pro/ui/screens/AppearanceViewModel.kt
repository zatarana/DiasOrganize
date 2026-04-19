package com.lifeflow.pro.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeflow.pro.ui.theme.AccentPalette
import com.lifeflow.pro.ui.theme.AppearancePreferences
import com.lifeflow.pro.ui.theme.AppearanceSettings
import com.lifeflow.pro.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val preferences: AppearancePreferences,
) : ViewModel() {
    val uiState: StateFlow<AppearanceSettings> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceSettings())

    val palette = AccentPalette.options

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.updateThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferences.updateDynamicColor(enabled) }
    }

    fun setAccentColor(key: String) {
        viewModelScope.launch { preferences.updateAccentColor(key) }
    }
}
