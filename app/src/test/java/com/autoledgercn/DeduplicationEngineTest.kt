package com.autoledgercn

import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.domain.dedup.DeduplicationEngine
import com.autoledgercn.domain.model.DedupAction
import com.autoledgercn.domain.model.ParsedTransaction
import com.autoledgercn.domain.model.SourceType
import com.autoledgercn.domain.model.TransactionType
import com.autoledgercn.domain.parser.Fingerprint
import com.autoledgercn.domain.parser.SourceAppMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class DeduplicationEngineTest {
    private val engine = DeduplicationEngine()

    @Test
    fun mergesWechatPayAndMeituanExpense() {
        val time = 1_700_000_000_000L
        val wechat = expense(
            amount = 23.50,
            merchant = "美团外卖",
            account = "微信",
            app = "微信",
            pkg = "com.tencent.mm",
            title = "微信支付",
            text = "你已成功付款 ¥23.50，商户：美团外卖",
            time = time,
            primary = "餐饮",
            secondary = "外卖"
        )
        val meituan = expense(
            amount = 23.50,
            merchant = "美团外卖",
            account = "未知",
            app = "美团",
            pkg = "com.sankuai.meituan",
            title = "美团",
            text = "外卖订单支付成功 23.50 元",
            time = time + 60_000L,
            primary = "餐饮",
            secondary = "外卖"
        )

        val existing = entity(wechat)
        val decision = engine.decide(meituan, null, listOf(existing))
        assertEquals(DedupAction.MERGE_FUZZY, decision.action)

        val merged = engine.merge(existing, meituan)
        assertEquals(1, listOf(merged).size)
        assertEquals(2, merged.relatedSourceCount)
        assertEquals("美团外卖", merged.merchant)
        assertEquals("微信", merged.account)
        assertEquals("餐饮", merged.categoryPrimary)
        assertEquals("外卖", merged.categorySecondary)
    }

    @Test
    fun mergesTaobaoAndCmbExpense() {
        val time = 1_700_000_000_000L
        val taobao = expense(
            amount = 89.90,
            merchant = "键盘",
            account = "招商银行",
            app = "淘宝",
            pkg = "com.taobao.taobao",
            title = "淘宝",
            text = "订单支付成功 ￥89.90，商品：键盘，付款方式：招商银行",
            time = time,
            primary = "数码",
            secondary = "电脑配件"
        )
        val bank = expense(
            amount = 89.90,
            merchant = "银行消费",
            account = "招商银行",
            app = "招商银行",
            pkg = "com.cmbchina.ccd.pluto.cmbActivity",
            title = "账户通知",
            text = "账户尾号1234消费人民币89.90元",
            time = time + 60_000L,
            primary = "其他",
            secondary = "未分类"
        )

        val existing = entity(taobao)
        val decision = engine.decide(bank, null, listOf(existing))
        assertEquals(DedupAction.MERGE_FUZZY, decision.action)

        val merged = engine.merge(existing, bank)
        assertEquals(2, merged.relatedSourceCount)
        assertEquals("键盘", merged.merchant)
        assertEquals("招商银行", merged.account)
    }

    @Test
    fun doesNotMergeSmallIncomeFromDifferentPayers() {
        val time = 1_700_000_000_000L
        val zhangsan = entity(income("张三", 0.01, time, "张三向你转账 0.01 元"))
        val lisi = income("李四", 0.01, time + 60_000L, "李四向你转账 0.01 元")

        val decision = engine.decide(lisi, null, listOf(zhangsan))
        assertEquals(DedupAction.CREATE, decision.action)
    }

    @Test
    fun mergesSameIncomeWhenExactLayerFindsExistingTransaction() {
        val time = 1_700_000_000_000L
        val zhangsan = entity(income("张三", 0.01, time, "张三向你转账 0.01 元"))
        val duplicate = income("张三", 0.01, time, "张三向你转账 0.01 元")

        val decision = engine.decide(duplicate, zhangsan, listOf(zhangsan))
        assertEquals(DedupAction.MERGE_EXACT, decision.action)
    }

    @Test
    fun identicalRawNotificationKeepsSameRawFingerprint() {
        val time = 1_700_000_000_000L
        val first = Fingerprint.raw("com.tencent.mm", "微信转账", "张三向你转账 0.01 元", "", time)
        val second = Fingerprint.raw("com.tencent.mm", "微信转账", "张三向你转账 0.01 元", "", time)

        assertEquals(first, second)
    }

    @Test
    fun doesNotMergeAllowanceWithOtherSameAmountIncome() {
        val time = 1_700_000_000_000L
        val mom = entity(income("妈妈", 2000.0, time, "妈妈向你转账 2000.00 元"))
        val other = income("朋友", 2000.0, time + 60_000L, "朋友向你转账 2000.00 元")

        val decision = engine.decide(other, null, listOf(mom))
        assertEquals(DedupAction.CREATE, decision.action)
    }

    private fun expense(
        amount: Double,
        merchant: String,
        account: String,
        app: String,
        pkg: String,
        title: String,
        text: String,
        time: Long,
        primary: String,
        secondary: String
    ) = parsed(amount, TransactionType.EXPENSE, merchant, account, app, pkg, title, text, time, primary, secondary)

    private fun income(merchant: String, amount: Double, time: Long, text: String) =
        parsed(amount, TransactionType.INCOME, merchant, "微信", "微信", "com.tencent.mm", "微信转账", text, time, "收入", "其他收入")

    private fun parsed(
        amount: Double,
        type: TransactionType,
        merchant: String,
        account: String,
        app: String,
        pkg: String,
        title: String,
        text: String,
        time: Long,
        primary: String,
        secondary: String
    ) = ParsedTransaction(
        amount = amount,
        type = type,
        merchant = merchant,
        account = account,
        sourceApp = app,
        sourcePackage = pkg,
        rawTitle = title,
        rawText = text,
        rawBigText = "",
        transactionTime = time,
        confidence = 0.9,
        categoryPrimary = primary,
        categorySecondary = secondary,
        rawFingerprint = Fingerprint.raw(pkg, title, text, "", time),
        transactionFingerprint = Fingerprint.transaction(amount, type, merchant, time, merchant, SourceAppMapper.sourceKind(pkg).name)
    )

    private fun entity(parsed: ParsedTransaction) = TransactionEntity(
        id = 1,
        amount = parsed.amount,
        type = parsed.type,
        categoryPrimary = parsed.categoryPrimary,
        categorySecondary = parsed.categorySecondary,
        merchant = parsed.merchant,
        account = parsed.account,
        sourceApp = parsed.sourceApp,
        sourcePackage = parsed.sourcePackage,
        sourceType = SourceType.NOTIFICATION,
        rawTitle = parsed.rawTitle,
        rawText = parsed.rawText,
        rawBigText = parsed.rawBigText,
        rawFingerprint = parsed.rawFingerprint,
        transactionFingerprint = parsed.transactionFingerprint,
        transactionTime = parsed.transactionTime,
        createdAt = parsed.transactionTime,
        confidence = parsed.confidence
    )
}
