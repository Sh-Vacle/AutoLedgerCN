package com.autoledgercn.domain.dedup

import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.domain.model.DedupAction
import com.autoledgercn.domain.model.ParsedTransaction
import com.autoledgercn.domain.model.SourceKind
import com.autoledgercn.domain.model.SourceType
import com.autoledgercn.domain.model.TransactionType
import com.autoledgercn.domain.parser.Fingerprint
import com.autoledgercn.domain.parser.SourceAppMapper
import kotlin.math.abs
import kotlin.math.roundToLong

data class DedupDecision(
    val action: DedupAction,
    val target: TransactionEntity? = null,
    val reason: String = ""
)

class DeduplicationEngine {
    fun decide(parsed: ParsedTransaction, exact: TransactionEntity?, candidates: List<TransactionEntity>): DedupDecision {
        if (exact != null) return DedupDecision(DedupAction.MERGE_EXACT, exact, "交易指纹完全一致")

        val fuzzy = candidates.firstOrNull { isLikelySameTransaction(parsed, it) }
        if (fuzzy != null) return DedupDecision(DedupAction.MERGE_FUZZY, fuzzy, "跨来源交易语义一致")

        val suspicious = candidates.firstOrNull {
            parsed.type == TransactionType.EXPENSE && sameAmount(parsed.amount, it.amount) && parsed.type == it.type
        }
        if (parsed.confidence < 0.68 || suspicious != null) {
            return DedupDecision(DedupAction.PENDING_CONFIRMATION, suspicious, "置信度较低或存在同金额近时支出")
        }
        return DedupDecision(DedupAction.CREATE)
    }

    fun merge(existing: TransactionEntity, parsed: ParsedTransaction): TransactionEntity {
        val merchant = chooseMerchant(existing.merchant, parsed.merchant, existing.sourceApp, parsed.sourceApp)
        val account = chooseAccount(existing.account, parsed.account, existing.sourceApp, parsed.sourceApp)
        val useParsedCategory = categoryPriority(parsed.sourceApp) >= categoryPriority(existing.sourceApp)
        return existing.copy(
            merchant = merchant,
            account = account,
            sourceApp = chooseSourceApp(existing.sourceApp, parsed.sourceApp),
            sourcePackage = existing.sourcePackage + "," + parsed.sourcePackage,
            categoryPrimary = if (useParsedCategory) parsed.categoryPrimary else existing.categoryPrimary,
            categorySecondary = if (useParsedCategory) parsed.categorySecondary else existing.categorySecondary,
            confidence = maxOf(existing.confidence, parsed.confidence),
            duplicateGroupId = existing.duplicateGroupId ?: "dup-${existing.id}",
            relatedSourceCount = existing.relatedSourceCount + 1,
            rawText = listOf(existing.rawText, parsed.rawText).filter { it.isNotBlank() }.distinct().joinToString("\n"),
            rawBigText = listOf(existing.rawBigText, parsed.rawBigText).filter { it.isNotBlank() }.distinct().joinToString("\n")
        )
    }

    fun toEntity(parsed: ParsedTransaction, now: Long): TransactionEntity = TransactionEntity(
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
        createdAt = now,
        confidence = parsed.confidence
    )

    private fun isLikelySameTransaction(parsed: ParsedTransaction, existing: TransactionEntity): Boolean {
        if (!sameAmount(parsed.amount, existing.amount) || parsed.type != existing.type) return false
        return when (parsed.type) {
            TransactionType.EXPENSE -> isLikelySameExpense(parsed, existing)
            TransactionType.INCOME -> isLikelySameIncome(parsed, existing)
        }
    }

    private fun isLikelySameExpense(parsed: ParsedTransaction, existing: TransactionEntity): Boolean {
        if (abs(parsed.transactionTime - existing.transactionTime) > expenseWindowMillis) return false
        if (!isOrderPaymentPair(sourceKinds(parsed.sourcePackage), sourceKinds(existing.sourcePackage))) return false
        val combined = listOf(
            parsed.merchant,
            existing.merchant,
            parsed.rawTitle,
            parsed.rawText,
            parsed.rawBigText,
            existing.rawTitle,
            existing.rawText,
            existing.rawBigText
        ).joinToString(" ")
        return expenseAssociationKeywords.any { combined.contains(it) }
    }

    private fun isLikelySameIncome(parsed: ParsedTransaction, existing: TransactionEntity): Boolean {
        if (abs(parsed.transactionTime - existing.transactionTime) > incomeWindowMillis) return false
        val parsedCounterparty = Fingerprint.normalizeCounterparty(parsed.merchant)
        val existingCounterparty = Fingerprint.normalizeCounterparty(existing.merchant)
        val sameConcreteCounterparty = parsedCounterparty == existingCounterparty &&
            Fingerprint.hasConcreteCounterparty(parsed.merchant) &&
            Fingerprint.hasConcreteCounterparty(existing.merchant)

        if (parsed.amount <= smallIncomeAmount && !sameConcreteCounterparty) return false
        if (sameConcreteCounterparty) return true

        val parsedRefund = isRefund(parsed.rawTitle, parsed.rawText, parsed.rawBigText)
        val existingRefund = isRefund(existing.rawTitle, existing.rawText, existing.rawBigText)
        return parsedRefund && existingRefund &&
            parsedCounterparty == existingCounterparty &&
            Fingerprint.hasConcreteCounterparty(parsed.merchant)
    }

    private fun sourceKinds(sourcePackages: String): Set<SourceKind> = sourcePackages
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { SourceAppMapper.sourceKind(it) }
        .toSet()

    private fun isOrderPaymentPair(left: Set<SourceKind>, right: Set<SourceKind>): Boolean {
        val leftOrder = left.any { it == SourceKind.ECOMMERCE || it == SourceKind.FOOD_DELIVERY }
        val rightOrder = right.any { it == SourceKind.ECOMMERCE || it == SourceKind.FOOD_DELIVERY }
        val leftPayment = left.any { it == SourceKind.PAYMENT || it == SourceKind.BANK }
        val rightPayment = right.any { it == SourceKind.PAYMENT || it == SourceKind.BANK }
        return (leftOrder && rightPayment) || (rightOrder && leftPayment)
    }

    private fun sameAmount(left: Double, right: Double): Boolean =
        (left * 100).roundToLong() == (right * 100).roundToLong()

    private fun isRefund(vararg textParts: String): Boolean {
        val text = textParts.joinToString(" ")
        return text.contains("退款成功") || text.contains("退款")
    }

    private fun chooseMerchant(current: String, incoming: String, currentSource: String, incomingSource: String): String {
        return if (merchantPriority(incoming, incomingSource) > merchantPriority(current, currentSource)) incoming else current
    }

    private fun merchantPriority(merchant: String, source: String): Int = when {
        listOf("美团", "饿了么", "淘宝", "天猫", "京东", "拼多多").any {
            merchant.contains(it) || source.contains(it)
        } -> 4
        merchant !in genericMerchants && !source.contains("银行") -> 3
        source.contains("微信") || source.contains("支付宝") -> 2
        source.contains("银行") || merchant.contains("银行") -> 1
        else -> 2
    }

    private fun chooseAccount(current: String, incoming: String, currentSource: String, incomingSource: String): String {
        return if (accountPriority(incoming, incomingSource) > accountPriority(current, currentSource)) incoming else current
    }

    private fun accountPriority(account: String, source: String): Int = when {
        listOf("招商银行", "建设银行", "工商银行", "交通银行", "银行").any {
            account.contains(it) || source.contains(it)
        } -> 3
        account == "支付宝" || account == "微信" || source.contains("支付宝") || source.contains("微信") -> 2
        else -> 1
    }

    private fun categoryPriority(source: String): Int = when {
        listOf("美团", "饿了么", "淘宝", "天猫", "京东", "拼多多").any { source.contains(it) } -> 3
        source.contains("微信") || source.contains("支付宝") -> 2
        source.contains("银行") -> 1
        else -> 2
    }

    private fun chooseSourceApp(current: String, incoming: String): String =
        if (categoryPriority(incoming) > categoryPriority(current)) incoming else current

    private companion object {
        const val expenseWindowMillis = 10 * 60_000L
        const val incomeWindowMillis = 5 * 60_000L
        const val smallIncomeAmount = 1.0
        val expenseAssociationKeywords = listOf(
            "美团",
            "外卖",
            "饿了么",
            "淘宝",
            "天猫",
            "京东",
            "拼多多",
            "订单",
            "付款",
            "支付成功",
            "消费",
            "扣款"
        )
        val genericMerchants = setOf("银行消费", "银行到账", "微信", "支付宝", "未知商户", "未知")
    }
}
