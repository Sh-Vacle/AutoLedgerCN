package com.autoledgercn.permission

object PermissionGuideState {
    fun shouldAutoShow(onboardingFinished: Boolean, permissionGuideShown: Boolean): Boolean =
        onboardingFinished && !permissionGuideShown

    fun canReopenFromSettings(): Boolean = true
}
