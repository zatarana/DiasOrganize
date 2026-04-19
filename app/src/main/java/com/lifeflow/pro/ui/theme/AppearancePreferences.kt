package com.lifeflow.pro.ui.theme

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance_settings")

enum class ThemeMode(val storageValue: String) {
    SYSTEM("SYSTEM"),
    LIGHT("LIGHT"),
    DARK("DARK");

    companion object {
        fun fromStorage(value: String?): ThemeMode = entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val accentColorKey: String = AccentPalette.DEFAULT_KEY,
)

object AccentPalette {
    const val DEFAULT_KEY = "green"
    val options = listOf(
        AccentOption("green", "Verde", 0xFF2D6A4F, 0xFFA7D5C1),
        AccentOption("blue", "Azul", 0xFF445E91, 0xFFB8C4FF),
        AccentOption("violet", "Violeta", 0xFF6A4C93, 0xFFD1C4E9),
        AccentOption("orange", "Laranja", 0xFFB85C38, 0xFFFFCCBC),
        AccentOption("rose", "Rosa", 0xFFA33E6A, 0xFFF8BBD0),
        AccentOption("gold", "Dourado", 0xFF8C6D1F, 0xFFFFE082),
        AccentOption("teal", "Turquesa", 0xFF006D77, 0xFF9EE7E5),
        AccentOption("slate", "Cinza azulado", 0xFF455A64, 0xFFB0BEC5),
    )

    fun find(key: String): AccentOption = options.firstOrNull { it.key == key } ?: options.first()
}

data class AccentOption(
    val key: String,
    val label: String,
    val lightPrimary: Long,
    val darkPrimary: Long,
)

class AppearancePreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val accentColor = stringPreferencesKey("accent_color")
    }

    val settingsFlow: Flow<AppearanceSettings> = context.appearanceDataStore.data.map { prefs: Preferences ->
        AppearanceSettings(
            themeMode = ThemeMode.fromStorage(prefs[Keys.themeMode]),
            dynamicColorEnabled = prefs[Keys.dynamicColor] ?: true,
            accentColorKey = prefs[Keys.accentColor] ?: AccentPalette.DEFAULT_KEY,
        )
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.appearanceDataStore.edit { it[Keys.themeMode] = mode.storageValue }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.appearanceDataStore.edit { it[Keys.dynamicColor] = enabled }
    }

    suspend fun updateAccentColor(key: String) {
        context.appearanceDataStore.edit { it[Keys.accentColor] = key }
    }
}
