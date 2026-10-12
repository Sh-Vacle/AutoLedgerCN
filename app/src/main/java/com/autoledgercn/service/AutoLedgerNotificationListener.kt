package com.autoledgercn.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.autoledgercn.AutoLedgerApp
import com.autoledgercn.data.repository.NotificationFinalAction
import com.autoledgercn.domain.parser.SourceAppMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AutoLedgerNotificationListener : NotificationListenerService() {
    private companion object {
        const val TAG = "AutoLedgerListener"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val packageName = sbn.packageName
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        Log.d(TAG, "notification posted package=$packageName title=$title text=$text")
        scope.launch {
            val app = application as AutoLedgerApp
            val monitoredApps = app.container.preferencesRepository.monitoredAppsFlow().first()
            val monitored = monitoredApps.firstOrNull { it.packageName == packageName && it.enabled }
            if (monitored != null) {
                Log.d(TAG, "passed monitoredApps whitelist package=$packageName appName=${monitored.name}")
                val appName = monitored.name.ifBlank { SourceAppMapper.appName(packageName) }
                runCatching {
                    app.container.ledgerRepository.processNotification(
                        packageName = packageName,
                        appName = appName,
                        title = title,
                        text = text,
                        bigText = bigText,
                        postTime = sbn.postTime
                    )
                }.onSuccess { result ->
                    Log.d(TAG, "notification process result: ${result.finalAction}, ${result.action}, reason=${result.reason}")
                }.onFailure { error ->
                    app.container.ledgerRepository.recordNotificationDiagnostic(
                        appName = appName,
                        packageName = packageName,
                        title = title,
                        text = text.ifBlank { bigText },
                        isMonitoredApp = true,
                        parserResult = "ERROR",
                        finalAction = NotificationFinalAction.ERROR,
                        ignoreReason = error.message.orEmpty()
                    )
                    Log.e(TAG, "notification process error package=$packageName", error)
                }
            } else {
                app.container.ledgerRepository.recordNotificationDiagnostic(
                    appName = SourceAppMapper.appName(packageName),
                    packageName = packageName,
                    title = title,
                    text = text.ifBlank { bigText },
                    isMonitoredApp = false,
                    parserResult = "SKIPPED",
                    finalAction = NotificationFinalAction.IGNORED_NOT_MONITORED,
                    ignoreReason = "Package is not enabled in monitored apps"
                )
                Log.d(TAG, "ignored because package not monitored: $packageName")
            }
        }
    }
}
