package com.lifeflow.pro.domain.model

import com.lifeflow.pro.ui.theme.AccentPalette
import com.lifeflow.pro.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppearanceSettingsTest {
    @Test
    fun `unknown theme mode falls back to system`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("QUALQUER_COISA"))
    }

    @Test
    fun `default accent palette option exists`() {
        assertNotNull(AccentPalette.find(AccentPalette.DEFAULT_KEY))
    }
}
