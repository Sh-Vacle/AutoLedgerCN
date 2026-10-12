package com.autoledgercn.domain.parser

import com.autoledgercn.domain.model.ParsedTransaction
import com.autoledgercn.domain.model.SourceKind
import com.autoledgercn.domain.model.TransactionType

class NotificationParser {
    private val amountRegexes = listOf(
        Regex("(?:¥|￥)\\s*([0-9]+(?:\\.[0-9]{1,2})?)"),
        Regex("人民币\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*元?"),
        Regex("(?:消费|支付|付款|扣款|收款|到账|入账|退款成功|转账)\\s*(?:人民币)?\\s*([0-9]+(?:\\.[0-9]{1,2})?)\\s*元?"),
        Regex("([0-9]+(?:\\.[0-9]{1,2})?)\\s*元")
    )
    private val expenseKeywords = listOf(
        "付款成功",
        "支付成功",
        "已成功付款",
        "成功付款",
        "消费",
        "扣款",
        "支出",
        "订单支付",
        "交易成功"
    )
    private val incomeKeywords = listOf(
        "收款",
        "到账",
        "入账",
        "转账给你",
        "向你转账",
        "收到",
        "退款成功",
        "收入"
    )
    private val hiddenAmountKeywords = listOf(
        "交易",
        "动账",
        "账户变动",
        "账户发生变动",
        "提现",
        "到账",
        "进度提醒",
        "已成功",
        "转入",
        "转出",
        "收款",
        "付款",
        "支付",
        "消费",
        "转账",
        "账单",
        "入账",
        "支出",
        "收入",
        "余额变动",
        "借记卡",
        "信用卡",
        "银行卡",
        "中国银行",
        "账户",
        "尾号",
        "人民币",
        "余额"
    )
    private val marketingKeywords = listOf(
        "优惠",
        "活动",
        "广告",
        "红包雨",
        "会员",
        "推荐",
        "促销"
    )
    private val transferPayerRegexes = listOf(
        Regex("([^\\s，,。:：]+)向你转账\\s*[0-9]+(?:\\.[0-9]{1,2})?\\s*元?"),
        Regex("收到\\s*([^\\s，,。:：]+)\\s*转账"),
        Regex("收款来自\\s*([^\\s，,。:：]+)")
    )
    private val merchantRegexes = listOf(
        Regex("商品[:：\\s]*([^，,。\\n\\s]+)"),
        Regex("(?:商户|商家|店铺)[:：\\s]*([^，,。\\n\\s]+)")
    )

    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        bigText: String?,
        postTime: Long,
        appNameOverride: String? = null
    ): ParsedTransaction? {
        val safeTitle = title.orEmpty()
        val safeText = text.orEmpty()
        val safeBigText = bigText.orEmpty()
        val combined = listOf(safeTitle, safeText, safeBigText).joinToString(" ")
        val amount = extractAmount(combined) ?: return null
        val type = detectType(combined) ?: return null
        val appName = appNameOverride?.takeIf { it.isNotBlank() } ?: SourceAppMapper.appName(packageName)
        val merchant = extractMerchant(combined, packageName, appName, type)
        val account = SourceAppMapper.accountFrom(packageName, combined)
        val rawFingerprint = Fingerprint.raw(packageName, safeTitle, safeText, safeBigText, postTime)
        val transactionFingerprint = Fingerprint.transaction(
            amount = amount,
            type = type,
            merchant = merchant,
            transactionTime = postTime,
            counterparty = merchant,
            sourceRole = SourceAppMapper.sourceKind(packageName).name
        )
        val confidence = baseConfidence(combined, merchant, type)
        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            account = account,
            sourceApp = appName,
            sourcePackage = packageName,
            rawTitle = safeTitle,
            rawText = safeText,
            rawBigText = safeBigText,
            transactionTime = postTime,
            confidence = confidence,
            rawFingerprint = rawFingerprint,
            transactionFingerprint = transactionFingerprint
        )
    }

    fun shouldCreateHiddenAmountPending(
        packageName: String,
        title: String?,
        text: String?,
        bigText: String?,
        isCustomApp: Boolean = false
    ): Boolean {
        val combined = listOf(title.orEmpty(), text.orEmpty(), bigText.orEmpty()).joinToString(" ")
        if (extractAmount(combined) != null) return false
        val eligibleSource = SourceAppMapper.sourceKind(packageName) in setOf(SourceKind.BANK, SourceKind.PAYMENT) || isCustomApp
        val hasTransactionKeyword = (hiddenAmountKeywords + visibleHiddenAmountKeywords).any { combined.contains(it) }
        val isMarketingOnly = marketingKeywords.any { combined.contains(it) } && !hasTransactionKeyword
        return eligibleSource && hasTransactionKeyword && !isMarketingOnly
    }

    private val visibleHiddenAmountKeywords = listOf(
        "交易",
        "动账",
        "账户变动",
        "提现",
        "到账",
        "进度提醒",
        "已成功",
        "转入",
        "转出",
        "收款",
        "付款",
        "支付",
        "消费",
        "转账",
        "账单",
        "入账",
        "支出",
        "收入",
        "余额变动",
        "借记卡",
        "信用卡",
        "银行卡",
        "中国银行",
        "账户",
        "尾号",
        "人民币",
        "余额"
    )

    private fun detectType(text: String): TransactionType? {
        if (incomeKeywords.any { text.contains(it) }) return TransactionType.INCOME
        if (expenseKeywords.any { text.contains(it) }) return TransactionType.EXPENSE
        return null
    }

    private fun extractAmount(text: String): Double? =
        amountRegexes.asSequence()
            .mapNotNull { it.find(text)?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.takeIf { amount -> amount.isFinite() && amount > 0.0 } }
            .firstOrNull()

    private fun extractMerchant(text: String, packageName: String, appName: String, type: TransactionType): String {
        if (type == TransactionType.INCOME && !isRefund(text)) {
            extractPayer(text)?.let { return it }
        }
        merchantRegexes.forEach { regex ->
            val value = regex.find(text)?.groupValues?.getOrNull(1)
            if (!value.isNullOrBlank()) return value.trim()
        }
        return when {
            text.contains("美团外卖") -> "美团外卖"
            text.contains("美团") || packageName == "com.sankuai.meituan" -> {
                if (text.contains("外卖")) "美团外卖" else "美团"
            }
            text.contains("饿了么") || packageName == "me.ele" -> "饿了么"
            text.contains("淘宝") || packageName == "com.taobao.taobao" -> "淘宝"
            text.contains("天猫") || packageName == "com.tmall.wireless" -> "天猫"
            text.contains("京东") || packageName == "com.jingdong.app.mall" -> "京东"
            text.contains("拼多多") || packageName == "com.xunmeng.pinduoduo" -> "拼多多"
            text.contains("妈妈") -> "妈妈"
            text.contains("母亲") -> "母亲"
            SourceAppMapper.sourceKind(packageName) == SourceKind.BANK -> {
                if (type == TransactionType.INCOME) "银行到账" else "银行消费"
            }
            else -> appName
        }
    }

    private fun extractPayer(text: String): String? {
        if (text.contains("对方向你转账")) return "对方"
        return transferPayerRegexes.asSequence()
            .mapNotNull { it.find(text)?.groupValues?.getOrNull(1)?.trim() }
            .firstOrNull { it.isNotBlank() }
    }

    private fun isRefund(text: String): Boolean = text.contains("退款成功") || text.contains("退款")

    private fun baseConfidence(text: String, merchant: String, type: TransactionType): Double {
        var score = 0.62
        if (merchant != "未知" && merchant != "银行消费" && merchant != "银行到账") score += 0.12
        if (type == TransactionType.INCOME && isRefund(text)) score += 0.1
        if (text.contains("商户") || text.contains("商家") || text.contains("商品")) score += 0.08
        if (type == TransactionType.INCOME && Fingerprint.hasConcreteCounterparty(merchant)) score += 0.08
        return score.coerceAtMost(0.98)
    }
}
