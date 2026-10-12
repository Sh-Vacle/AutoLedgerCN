package com.autoledgercn

import com.autoledgercn.domain.model.TransactionType
import com.autoledgercn.domain.parser.NotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationParserTest {
    private val parser = NotificationParser()

    @Test
    fun rejectsOverflowingAmount() {
        val text = "支付成功 ￥" + "9".repeat(400)
        assertNull(parser.parse("com.tencent.mm", "支付", text, "", 1_700_000_000_000L))
    }

    @Test
    fun rejectsZeroAmount() {
        assertNull(parser.parse("com.tencent.mm", "支付", "支付成功 ￥0.00", "", 1_700_000_000_000L))
    }

    @Test
    fun parsesTaobaoExpense() {
        val parsed = parser.parse(
            "com.taobao.taobao",
            "淘宝",
            "订单支付成功 ￥89.90，商品：键盘，付款方式：招商银行",
            "",
            1_700_000_000_000L
        )
        assertNotNull(parsed)
        assertEquals(89.90, parsed!!.amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("键盘", parsed.merchant)
        assertEquals("招商银行", parsed.account)
    }

    @Test
    fun extractsTransferPayerAsMerchant() {
        val zhangsan = parser.parse("com.tencent.mm", "微信转账", "张三向你转账 0.01 元", "", 1_700_000_000_000L)
        val lisi = parser.parse("com.tencent.mm", "微信转账", "李四向你转账 0.01 元", "", 1_700_000_060_000L)

        assertNotNull(zhangsan)
        assertNotNull(lisi)
        assertEquals(TransactionType.INCOME, zhangsan!!.type)
        assertEquals("张三", zhangsan.merchant)
        assertEquals("李四", lisi!!.merchant)
    }

    @Test
    fun parsesAllowanceIncome() {
        val parsed = parser.parse("com.tencent.mm", "微信转账", "妈妈向你转账 2000.00 元", "", 1_700_000_000_000L)
        assertNotNull(parsed)
        assertEquals(2000.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals("妈妈", parsed.merchant)
    }

    @Test
    fun parsesRefundMerchant() {
        val parsed = parser.parse("com.eg.android.AlipayGphone", "退款成功", "退款成功 18.00 元，商家：饿了么", "", 1_700_000_000_000L)
        assertNotNull(parsed)
        assertEquals(18.0, parsed!!.amount, 0.001)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals("饿了么", parsed.merchant)
    }

    @Test
    fun detectsHiddenAmountBankTransaction() {
        val parsed = parser.parse("cn.com.boc", "账户动账提醒", "您有一笔交易，请登录手机银行查看", "", 1_700_000_000_000L)
        assertNull(parsed)
        assertTrue(parser.shouldCreateHiddenAmountPending("cn.com.boc", "账户动账提醒", "您有一笔交易，请登录手机银行查看", ""))
    }

    @Test
    fun detectsChineseHiddenAmountBankTransaction() {
        val parsed = parser.parse("cn.com.boc", "动账提醒", "您有一笔交易，请登录手机银行查看", "", 1_700_000_000_000L)
        assertNull(parsed)
        assertTrue(parser.shouldCreateHiddenAmountPending("cn.com.boc", "动账提醒", "您有一笔交易，请登录手机银行查看", ""))
    }

    @Test
    fun ignoresMarketingNotificationWithoutAmount() {
        val parsed = parser.parse("com.taobao.taobao", "淘宝活动", "今晚八点红包雨，点击查看", "", 1_700_000_000_000L)
        assertNull(parsed)
        assertFalse(parser.shouldCreateHiddenAmountPending("com.taobao.taobao", "淘宝活动", "今晚八点红包雨，点击查看", ""))
    }
}
