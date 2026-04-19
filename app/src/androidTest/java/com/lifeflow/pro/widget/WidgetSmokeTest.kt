
package com.lifeflow.pro.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetSmokeTest {
    @Test
    fun receiversInstantiate() {
        assertNotNull(FinanceSummaryWidgetReceiver())
        assertNotNull(TodayAgendaWidgetReceiver())
        assertNotNull(DebtHighlightWidgetReceiver())
    }
}
