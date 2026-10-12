package com.autoledgercn.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autoledgercn.data.local.AppDatabase

class StatsRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        AppDatabase.get(applicationContext).ledgerDao().getTransactions()
        return Result.success()
    }
}
