package com.autoledgercn

import com.autoledgercn.data.entity.CategoryRuleEntity
import com.autoledgercn.data.entity.NotificationDiagnosticEntity
import com.autoledgercn.data.entity.PendingTransactionEntity
import com.autoledgercn.data.entity.RawNotificationEntity
import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.data.local.LedgerDao
import com.autoledgercn.data.repository.LedgerRepository
import com.autoledgercn.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRuleRepositoryTest {
    @Test
    fun addAndDeleteRuleUpdatesStoredRules() = runTest {
        val dao = RuleFakeDao()
        val repository = LedgerRepository(dao)

        repository.addCategoryRule("蜜雪冰城", "餐饮", "奶茶咖啡", TransactionType.EXPENSE)

        assertEquals(1, dao.rules.size)
        assertEquals("蜜雪冰城", dao.rules.single().keyword)

        repository.deleteCategoryRule(dao.rules.single().id)

        assertTrue(dao.rules.isEmpty())
    }

    private class RuleFakeDao : LedgerDao {
        val rules = mutableListOf<CategoryRuleEntity>()
        private var nextRuleId = 1L

        override fun observeTransactions(): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun getTransactions(): List<TransactionEntity> = emptyList()
        override suspend fun getTransaction(id: Long): TransactionEntity? = null
        override suspend fun findTransactionByFingerprint(fingerprint: String): TransactionEntity? = null
        override suspend fun findCandidates(amount: Double, type: TransactionType, start: Long, end: Long): List<TransactionEntity> = emptyList()
        override suspend fun insertTransaction(entity: TransactionEntity): Long = 1L
        override suspend fun updateTransaction(entity: TransactionEntity) = Unit
        override suspend fun deleteTransaction(id: Long) = Unit
        override suspend fun rawFingerprintExists(fingerprint: String): Boolean = false
        override suspend fun insertRawNotification(entity: RawNotificationEntity): Long = 1L
        override suspend fun linkRawNotification(rawId: Long, transactionId: Long) = Unit
        override fun observeNotificationDiagnostics(limit: Int): Flow<List<NotificationDiagnosticEntity>> = flowOf(emptyList())
        override suspend fun insertNotificationDiagnostic(entity: NotificationDiagnosticEntity): Long = 1L
        override suspend fun trimNotificationDiagnostics(keep: Int) = Unit
        override fun observePending(): Flow<List<PendingTransactionEntity>> = flowOf(emptyList())
        override suspend fun insertPending(entity: PendingTransactionEntity): Long = 1L
        override suspend fun deletePending(id: Long) = Unit
        override fun observeCategoryRules(): Flow<List<CategoryRuleEntity>> = flowOf(rules.toList())
        override suspend fun getEnabledCategoryRules(): List<CategoryRuleEntity> = rules.filter { it.enabled }
        override suspend fun insertCategoryRule(entity: CategoryRuleEntity): Long {
            val id = nextRuleId++
            rules += entity.copy(id = id)
            return id
        }
        override suspend fun deleteCategoryRule(id: Long) {
            rules.removeAll { it.id == id }
        }
    }
}
