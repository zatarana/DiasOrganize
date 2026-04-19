package com.lifeflow.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeflow.pro.ui.navigation.LifeFlowAppRoot
import com.lifeflow.pro.ui.theme.AppearancePreferences
import com.lifeflow.pro.ui.theme.AppearanceSettings
import com.lifeflow.pro.ui.theme.LifeFlowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember { AppearancePreferences(applicationContext) }
            val settings by preferences.settingsFlow.collectAsStateWithLifecycle(initialValue = AppearanceSettings())
            LifeFlowTheme(settings = settings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LifeFlowAppRoot()
                }
            }
        }
    }
}
