package com.autoledgercn

import com.autoledgercn.data.entity.CategoryRuleEntity
import com.autoledgercn.data.entity.NotificationDiagnosticEntity
import com.autoledgercn.data.entity.PendingTransactionEntity
import com.autoledgercn.data.entity.RawNotificationEntity
import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.data.local.LedgerDao
import com.autoledgercn.data.repository.LedgerRepository
import com.autoledgercn.domain.model.DedupAction
import com.autoledgercn.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerRepositoryNotificationTest {
    @Test
    fun hiddenAmountBankNotificationCreatesPendingOnly() = runTest {
        val dao = FakeLedgerDao()
        val result = LedgerRepository(dao).processNotification(
            packageName = "cn.com.boc",
            appName = "中国银行",
            title = "账户动账提醒",
            text = "您有一笔交易，请登录手机银行查看",
            bigText = "",
            postTime = 1_700_000_000_000L
        )

        assertEquals(DedupAction.PENDING_CONFIRMATION, result.action)
        assertEquals(0, dao.transactions.size)
        assertEquals(1, dao.pending.size)
        assertTrue(dao.pending.single().reason.contains("金额被隐藏") || dao.pending.single().reason.contains("请手动补录"))
    }

    @Test
    fun marketingNotificationWithoutAmountIsIgnored() = runTest {
        val dao = FakeLedgerDao()
        val result = LedgerRepository(dao).processNotification(
            packageName = "com.taobao.taobao",
            appName = "淘宝",
            title = "淘宝活动",
            text = "今晚八点红包雨，点击查看",
            bigText = "",
            postTime = 1_700_000_000_000L
        )

        assertEquals(DedupAction.IGNORE_RAW, result.action)
        assertEquals(0, dao.transactions.size)
        assertEquals(0, dao.pending.size)
    }

    @Test
    fun alipayProgressNotificationWithoutAmountCreatesPendingOnly() = runTest {
        val dao = FakeLedgerDao()
        val result = LedgerRepository(dao).processNotification(
            packageName = "com.eg.android.AlipayGphone",
            appName = "支付宝",
            title = "进度提醒",
            text = "提现到中国银行(1597)已成功，点此查看详情。",
            bigText = "",
            postTime = 1_700_000_000_000L
        )

        assertEquals(DedupAction.PENDING_CONFIRMATION, result.action)
        assertEquals(0, dao.transactions.size)
        assertEquals(1, dao.pending.size)
        assertEquals("支付宝", dao.pending.single().sourceApp)
        assertEquals("com.eg.android.AlipayGphone", dao.pending.single().sourcePackage)
        assertTrue(dao.pending.single().reason.contains("金额被隐藏，请手动补录"))
    }

    @Test
    fun chinaBankBriefNotificationWithoutAmountCreatesPendingOnly() = runTest {
        val dao = FakeLedgerDao()
        val result = LedgerRepository(dao).processNotification(
            packageName = "cn.com.boc",
            appName = "中国银行",
            title = "您有1条消息",
            text = "借记卡动账提醒",
            bigText = "",
            postTime = 1_700_000_000_000L
        )

        assertEquals(DedupAction.PENDING_CONFIRMATION, result.action)
        assertEquals(0, dao.transactions.size)
        assertEquals(1, dao.pending.size)
        assertEquals("中国银行", dao.pending.single().sourceApp)
        assertEquals("cn.com.boc", dao.pending.single().sourcePackage)
        assertTrue(dao.pending.single().reason.contains("金额被隐藏，请手动补录"))
    }

    @Test
    fun customUnknownAppCanCreateHiddenAmountPending() = runTest {
        val dao = FakeLedgerDao()
        LedgerRepository(dao).processNotification(
            packageName = "com.example.wallet",
            appName = "示例钱包",
            title = "账户变动",
            text = "您有一笔交易，请打开 App 查看",
            bigText = "",
            postTime = 1_700_000_000_000L
        )

        assertEquals(1, dao.pending.size)
        assertEquals("示例钱包", dao.pending.single().sourceApp)
    }

    @Test
    fun monitoredChinaBankHiddenAmountNotificationCreatesPendingOnly() = runTest {
        val dao = FakeLedgerDao()
        val result = LedgerRepository(dao).processNotification(
            packageName = "cn.com.boc",
            appName = "中国银行",
            title = "动账提醒",
            text = "您有一笔交易，请登录手机银行查看",
            bigText = "",
            postTime = 1_700_000_000_000L
        )

        assertEquals(DedupAction.PENDING_CONFIRMATION, result.action)
        assertEquals(0, dao.transactions.size)
        assertEquals(1, dao.pending.size)
        assertEquals("cn.com.boc", dao.pending.single().sourcePackage)
        assertEquals("中国银行", dao.pending.single().sourceApp)
        assertTrue(dao.pending.single().reason.contains("金额被隐藏，请手动补录"))
    }

    @Test
    fun manualTransactionUsesProvidedTransactionTime() = runTest {
        val dao = FakeLedgerDao()
        val time = 1_780_304_600_000L

        LedgerRepository(dao).addManualTransaction(
            amount = 12.0,
            type = TransactionType.EXPENSE,
            primary = "餐饮",
            secondary = "外卖",
            merchant = "测试商户",
            account = "现金",
            note = "备注测试",
            transactionTime = time
        )

        assertEquals(time, dao.transactions.single().transactionTime)
        assertEquals("备注测试", dao.transactions.single().note)
    }

    @Test
    fun updatingTransactionTimePersistsNewTime() = runTest {
        val dao = FakeLedgerDao()
        val originalTime = 1_780_304_600_000L
        val newTime = 1_780_391_000_000L
        val repository = LedgerRepository(dao)
        repository.addManualTransaction(
            amount = 12.0,
            type = TransactionType.EXPENSE,
            primary = "餐饮",
            secondary = "外卖",
            merchant = "测试商户",
            account = "现金",
            note = "",
            transactionTime = originalTime
        )

        val tx = dao.transactions.single()
        repository.updateTransaction(tx.copy(transactionTime = newTime, note = "改备注"))

        assertEquals(newTime, dao.transactions.single().transactionTime)
        assertEquals("改备注", dao.transactions.single().note)
    }

    private class FakeLedgerDao : LedgerDao {
        val transactions = mutableListOf<TransactionEntity>()
        val pending = mutableListOf<PendingTransactionEntity>()
        private val raws = mutableListOf<RawNotificationEntity>()

        override fun observeTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override suspend fun getTransactions(): List<TransactionEntity> = transactions
        override suspend fun getTransaction(id: Long): TransactionEntity? = transactions.firstOrNull { it.id == id }
        override suspend fun findTransactionByFingerprint(fingerprint: String): TransactionEntity? =
            transactions.firstOrNull { it.transactionFingerprint == fingerprint }
        override suspend fun findCandidates(amount: Double, type: TransactionType, start: Long, end: Long): List<TransactionEntity> =
            transactions.filter { it.amount == amount && it.type == type && it.transactionTime in start..end }
        override suspend fun insertTransaction(entity: TransactionEntity): Long {
            val id = (transactions.size + 1).toLong()
            transactions += entity.copy(id = id)
            return id
        }
        override suspend fun updateTransaction(entity: TransactionEntity) {
            transactions.removeAll { it.id == entity.id }
            transactions += entity
        }
        override suspend fun deleteTransaction(id: Long) {
            transactions.removeAll { it.id == id }
        }
        override suspend fun rawFingerprintExists(fingerprint: String): Boolean =
            raws.any { it.rawFingerprint == fingerprint }
        override suspend fun insertRawNotification(entity: RawNotificationEntity): Long {
            val id = (raws.size + 1).toLong()
            raws += entity.copy(id = id)
            return id
        }
        override suspend fun linkRawNotification(rawId: Long, transactionId: Long) = Unit
        override fun observeNotificationDiagnostics(limit: Int): Flow<List<NotificationDiagnosticEntity>> = flowOf(emptyList())
        override suspend fun insertNotificationDiagnostic(entity: NotificationDiagnosticEntity): Long = 1L
        override suspend fun trimNotificationDiagnostics(keep: Int) = Unit
        override fun observePending(): Flow<List<PendingTransactionEntity>> = flowOf(pending)
        override suspend fun insertPending(entity: PendingTransactionEntity): Long {
            val id = (pending.size + 1).toLong()
            pending += entity.copy(id = id)
            return id
        }
        override suspend fun deletePending(id: Long) {
            pending.removeAll { it.id == id }
        }
        override fun observeCategoryRules(): Flow<List<CategoryRuleEntity>> = flowOf(emptyList())
        override suspend fun getEnabledCategoryRules(): List<CategoryRuleEntity> = emptyList()
        override suspend fun insertCategoryRule(entity: CategoryRuleEntity): Long = 1L
        override suspend fun deleteCategoryRule(id: Long) = Unit
    }
}
