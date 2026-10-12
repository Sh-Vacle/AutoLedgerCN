package com.autoledgercn.domain.parser

import com.autoledgercn.domain.model.TransactionType
import java.security.MessageDigest
import kotlin.math.floor
import kotlin.math.roundToLong

object Fingerprint {
    private const val minuteMillis = 60_000L
    private const val fiveMinutesMillis = 5 * minuteMillis

    fun raw(packageName: String, title: String, text: String, bigText: String, postTime: Long): String {
        val minute = floor(postTime.toDouble() / minuteMillis).toLong()
        return sha256(listOf(packageName, title, text, bigText, minute.toString()).joinToString("|"))
    }

    fun transaction(
        amount: Double,
        type: TransactionType,
        merchant: String,
        transactionTime: Long,
        counterparty: String = merchant,
        sourceRole: String = ""
    ): String {
        val cents = (amount * 100).roundToLong()
        val bucket = floor(transactionTime.toDouble() / fiveMinutesMillis).toLong()
        val normalizedMerchant = normalizeMerchant(merchant)
        val normalizedCounterparty = normalizeCounterparty(counterparty)
        val parts = mutableListOf(
            type.name,
            cents.toString(),
            normalizedMerchant,
            sourceRole,
            bucket.toString()
        )
        if (type == TransactionType.INCOME) {
            parts.add(normalizedCounterparty)
        }
        return sha256(parts.joinToString("|"))
    }

    fun normalizeMerchant(raw: String): String = raw
        .trim()
        .replace(Regex("\\s+"), "")
        .removePrefix("商户：")
        .removePrefix("商家：")
        .removePrefix("商品：")
        .replace("股份有限公司", "")
        .replace("有限公司", "")
        .replace("官方旗舰店", "")
        .replace("旗舰店", "")
        .replace("订单", "")
        .ifBlank { "未知商户" }

    fun normalizeCounterparty(raw: String): String = normalizeMerchant(raw)

    fun hasConcreteCounterparty(raw: String): Boolean {
        val normalized = normalizeCounterparty(raw)
        return normalized !in genericCounterparties
    }

    private val genericCounterparties = setOf(
        "未知商户",
        "未知",
        "收入",
        "转账",
        "转账收入",
        "微信",
        "支付宝",
        "微信转账",
        "支付宝转账",
        "银行到账",
        "银行消费",
        "对方"
    )

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
