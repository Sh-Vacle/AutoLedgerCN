package com.autoledgercn.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.autoledgercn.data.entity.CategoryRuleEntity
import com.autoledgercn.data.entity.NotificationDiagnosticEntity
import com.autoledgercn.data.entity.PendingTransactionEntity
import com.autoledgercn.data.entity.RawNotificationEntity
import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM transactions ORDER BY transactionTime DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY transactionTime DESC")
    suspend fun getTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransaction(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transactionFingerprint = :fingerprint LIMIT 1")
    suspend fun findTransactionByFingerprint(fingerprint: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE amount = :amount AND type = :type AND transactionTime BETWEEN :start AND :end ORDER BY transactionTime DESC")
    suspend fun findCandidates(amount: Double, type: TransactionType, start: Long, end: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(entity: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM raw_notifications WHERE rawFingerprint = :fingerprint)")
    suspend fun rawFingerprintExists(fingerprint: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRawNotification(entity: RawNotificationEntity): Long

    @Query("UPDATE raw_notifications SET parsedTransactionId = :transactionId WHERE id = :rawId")
    suspend fun linkRawNotification(rawId: Long, transactionId: Long)

    @Query("SELECT * FROM notification_diagnostics ORDER BY timeMillis DESC, id DESC LIMIT :limit")
    fun observeNotificationDiagnostics(limit: Int = 30): Flow<List<NotificationDiagnosticEntity>>

    @Insert
    suspend fun insertNotificationDiagnostic(entity: NotificationDiagnosticEntity): Long

    @Query("DELETE FROM notification_diagnostics WHERE id NOT IN (SELECT id FROM notification_diagnostics ORDER BY timeMillis DESC, id DESC LIMIT :keep)")
    suspend fun trimNotificationDiagnostics(keep: Int = 100)

    @Query("DELETE FROM notification_diagnostics")
    suspend fun clearNotificationDiagnostics()

    @Query("SELECT * FROM pending_transactions ORDER BY createdAt DESC")
    fun observePending(): Flow<List<PendingTransactionEntity>>

    @Insert
    suspend fun insertPending(entity: PendingTransactionEntity): Long

    @Query("SELECT * FROM pending_transactions WHERE sourcePackage = :packageName AND amount = 0.0 AND createdAt >= :since ORDER BY createdAt DESC")
    suspend fun findRecentHiddenAmountPending(packageName: String, since: Long): List<PendingTransactionEntity>

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deletePending(id: Long)

    @Query("SELECT * FROM category_rules ORDER BY priority DESC, createdAt DESC")
    fun observeCategoryRules(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules WHERE enabled = 1 ORDER BY priority DESC, createdAt DESC")
    suspend fun getEnabledCategoryRules(): List<CategoryRuleEntity>

    @Insert
    suspend fun insertCategoryRule(entity: CategoryRuleEntity): Long

    @Query("DELETE FROM category_rules WHERE id = :id")
    suspend fun deleteCategoryRule(id: Long)
}
