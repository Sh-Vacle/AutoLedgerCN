package com.autoledgercn

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.autoledgercn.data.app.InstalledAppRepository
import com.autoledgercn.data.local.AppDatabase
import com.autoledgercn.data.preferences.UserPreferencesRepository
import com.autoledgercn.data.repository.LedgerRepository
import com.autoledgercn.service.StatsRefreshWorker
import java.util.concurrent.TimeUnit

class AutoLedgerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        scheduleStatsRefresh(this)
    }

    companion object {
        const val STATS_REFRESH_WORK_NAME = "stats_refresh"

        fun scheduleStatsRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<StatsRefreshWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                STATS_REFRESH_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

class AppContainer(application: Application) {
    private val database = AppDatabase.get(application)
    val ledgerRepository = LedgerRepository(database.ledgerDao())
    val preferencesRepository = UserPreferencesRepository(application)
    val installedAppRepository = InstalledAppRepository(application)
}
