package com.autoledgercn.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autoledgercn.data.entity.CategoryRuleEntity
import com.autoledgercn.data.entity.NotificationDiagnosticEntity
import com.autoledgercn.data.entity.PendingTransactionEntity
import com.autoledgercn.data.entity.RawNotificationEntity
import com.autoledgercn.data.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        RawNotificationEntity::class,
        NotificationDiagnosticEntity::class,
        CategoryRuleEntity::class,
        PendingTransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "auto_ledger_cn.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notification_diagnostics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timeMillis INTEGER NOT NULL,
                        appName TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        title TEXT NOT NULL,
                        text TEXT NOT NULL,
                        isMonitoredApp INTEGER NOT NULL,
                        parserResult TEXT NOT NULL,
                        finalAction TEXT NOT NULL,
                        ignoreReason TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
