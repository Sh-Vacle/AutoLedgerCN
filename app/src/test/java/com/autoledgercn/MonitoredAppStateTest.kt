package com.autoledgercn

import com.autoledgercn.data.preferences.MonitoredAppState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoredAppStateTest {
    @Test
    fun selectingChinaBankAddsItEnabledAndUnselectingKeepsItDisabled() {
        val enabled = MonitoredAppState.updated(emptyList(), "中国银行", "cn.com.boc", enabled = true)
        assertTrue(enabled.single { it.packageName == "cn.com.boc" }.enabled)

        val disabled = MonitoredAppState.updated(enabled, "中国银行", "cn.com.boc", enabled = false)
        assertFalse(disabled.single { it.packageName == "cn.com.boc" }.enabled)
    }

    @Test
    fun reopeningSelectionUsesSavedState() {
        val selected = MonitoredAppState.updated(emptyList(), "中国银行", "cn.com.boc", enabled = true)
        val reopened = selected.toList()

        assertTrue(reopened.single { it.packageName == "cn.com.boc" }.enabled)
    }
}
