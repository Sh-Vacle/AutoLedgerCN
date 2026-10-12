package com.autoledgercn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.autoledgercn.AutoLedgerApp
import com.autoledgercn.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val enabled = UserPreferencesRepository(context).preferences.first().bootKeepAliveEnabled
                if (enabled) {
                    AutoLedgerApp.scheduleStatsRefresh(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
