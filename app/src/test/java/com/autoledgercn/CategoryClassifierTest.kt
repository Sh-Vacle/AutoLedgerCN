package com.autoledgercn

import com.autoledgercn.domain.classifier.LocalRuleClassifier
import com.autoledgercn.domain.model.ParsedTransaction
import com.autoledgercn.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryClassifierTest {
    private val classifier = LocalRuleClassifier()

    @Test
    fun classifiesDetailedExpenseRules() {
        assertCategory("美团外卖", "你已成功付款 ¥23.50，商户：美团外卖", TransactionType.EXPENSE, "餐饮", "外卖")
        assertCategory("蜜雪冰城", "蜜雪冰城支付成功 8 元", TransactionType.EXPENSE, "餐饮", "奶茶咖啡")
        assertCategory("滴滴", "滴滴打车支付成功 25 元", TransactionType.EXPENSE, "交通", "打车")
        assertCategory("淘宝", "订单支付成功 ￥89.90，商品：键盘", TransactionType.EXPENSE, "购物", "电脑配件")
        assertCategory("12306", "12306高铁票支付成功 188 元", TransactionType.EXPENSE, "交通", "火车高铁")
    }

    @Test
    fun classifiesDetailedIncomeRules() {
        assertCategory("公司", "工资到账 8000 元", TransactionType.INCOME, "工资收入", "工资")
        assertCategory("张三", "张三向你转账 200 元", TransactionType.INCOME, "转账收入", "普通转账")
        assertCategory("淘宝", "退款成功 18.00 元", TransactionType.INCOME, "退款收入", "其他退款")
        assertCategory("妈妈", "妈妈向你转账 2000.00 元", TransactionType.INCOME, "转账收入", "普通转账")
    }

    private fun assertCategory(merchant: String, text: String, type: TransactionType, primary: String, secondary: String) {
        val result = classifier.classify(base(merchant, text, type))
        assertEquals(primary, result.categoryPrimary)
        assertEquals(secondary, result.categorySecondary)
    }

    private fun base(merchant: String, text: String, type: TransactionType) = ParsedTransaction(
        amount = 1.0,
        type = type,
        merchant = merchant,
        account = "微信",
        sourceApp = "微信",
        sourcePackage = "com.tencent.mm",
        rawTitle = "",
        rawText = text,
        rawBigText = "",
        transactionTime = 1L,
        confidence = 0.8
    )
}
