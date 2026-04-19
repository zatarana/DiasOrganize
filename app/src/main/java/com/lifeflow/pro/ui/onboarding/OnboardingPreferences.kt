package com.lifeflow.pro.ui.onboarding

import android.content.Context

private const val PREFS_NAME = "lifeflow_prefs"
private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

class OnboardingPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setCompleted(value: Boolean) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
    }
}
