package com.autoledgercn

import com.autoledgercn.permission.PermissionGuideState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionGuideStateTest {
    @Test
    fun savedShownStatePreventsNextAutomaticPopup() {
        assertTrue(PermissionGuideState.shouldAutoShow(onboardingFinished = true, permissionGuideShown = false))
        assertFalse(PermissionGuideState.shouldAutoShow(onboardingFinished = true, permissionGuideShown = true))
    }

    @Test
    fun settingsCanAlwaysReopenPermissionGuide() {
        assertTrue(PermissionGuideState.canReopenFromSettings())
    }
}
