package com.autoledgercn.domain.parser

import com.autoledgercn.domain.model.SourceKind

data class TransactionCandidateDecision(
    val isCandidate: Boolean,
    val hasAmount: Boolean,
    val diagnosticResult: String
)

fun isTransactionCandidate(
    packageName: String,
    appName: String,
    title: String?,
    text: String?
): Boolean =
    transactionCandidateDecision(packageName, appName, title, text).isCandidate

fun transactionCandidateDecision(
    packageName: String,
    appName: String,
    title: String?,
    text: String?,
    bigText: String? = null
): TransactionCandidateDecision {
    val safeTitle = title.orEmpty()
    val content = listOf(safeTitle, text.orEmpty(), bigText.orEmpty())
        .joinToString(" ")
        .trim()
    val hasAmount = amountRegex.containsMatchIn(content)
    val hasTransactionAction = transactionActionKeywords.any { content.contains(it, ignoreCase = true) }
    val isChatApp = isChatApp(packageName, appName)
    val isStrongTransactionSource = isBankApp(packageName, appName) || isPaymentApp(packageName, appName)
    val isCommerceOrLife = isCommerceOrLifeApp(packageName, appName)

    if (content.isBlank()) {
        return ignored(hasAmount, "未发现交易金额或交易意图，已忽略")
    }

    if (isChatApp) {
        val trustedPaymentTitle = trustedChatPaymentTitles.any { safeTitle.contains(it, ignoreCase = true) }
        val trustedPaymentContent = trustedChatPaymentKeywords.any { content.contains(it, ignoreCase = true) }
        if (looksLikeOrdinaryChatTitle(safeTitle, appName) && !trustedPaymentTitle) {
            return ignored(hasAmount, "普通聊天消息，已忽略")
        }
        if (!trustedPaymentTitle && !(trustedPaymentContent && hasAmount)) {
            return ignored(hasAmount, "普通聊天消息，已忽略")
        }
    }

    if (logisticsKeywords.any { content.contains(it, ignoreCase = true) } && !hasPaymentOrRefundAction(content)) {
        return ignored(hasAmount, "未发现交易金额或交易意图，已忽略")
    }

    val hasRecommendation = recommendationKeywords.any { content.contains(it, ignoreCase = true) }
    val hasAdvertising = advertisingKeywords.any { content.contains(it, ignoreCase = true) }
    if ((hasRecommendation || hasAdvertising) && !hasPaymentOrRefundAction(content)) {
        return ignored(hasAmount, if (hasRecommendation) "内容推荐通知，已忽略" else "广告营销通知，已忽略")
    }

    if (isCommerceOrLife) {
        return if (hasAmount && commerceTransactionKeywords.any { content.contains(it, ignoreCase = true) }) {
            TransactionCandidateDecision(true, hasAmount = true, diagnosticResult = "交易候选")
        } else {
            ignored(hasAmount, "未发现交易金额或交易意图，已忽略")
        }
    }

    if (hasAmount && hasTransactionAction) {
        return TransactionCandidateDecision(true, hasAmount = true, diagnosticResult = "交易候选")
    }

    val hiddenAmountTransaction = !hasAmount &&
        isStrongTransactionSource &&
        hiddenAmountKeywords.any { content.contains(it, ignoreCase = true) } &&
        !couponOnlyKeywords.any { content.contains(it, ignoreCase = true) }
    if (hiddenAmountTransaction) {
        return TransactionCandidateDecision(true, hasAmount = false, diagnosticResult = "交易候选")
    }

    return ignored(hasAmount, "未发现交易金额或交易意图，已忽略")
}

private fun ignored(hasAmount: Boolean, result: String): TransactionCandidateDecision =
    TransactionCandidateDecision(false, hasAmount = hasAmount, diagnosticResult = result)

private val amountRegex = Regex(
    pattern = "(?:¥|￥)\\s*\\d+(?:\\.\\d{1,2})?|(?:RMB|CNY)\\s*\\d+(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?\\s*元",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val transactionActionKeywords = listOf(
    "支付成功",
    "付款成功",
    "已支付",
    "支付",
    "付款",
    "扣款",
    "扣费",
    "消费",
    "收款",
    "收款到账",
    "到账",
    "入账",
    "出账",
    "转账",
    "退款",
    "退款成功",
    "提现",
    "充值",
    "代扣",
    "免密支付",
    "借记卡动账",
    "信用卡交易",
    "银行卡交易",
    "余额变动",
    "交易提醒",
    "订单支付"
)

private val commerceTransactionKeywords = listOf(
    "支付成功",
    "付款成功",
    "订单支付",
    "已支付",
    "扣款",
    "退款成功",
    "退款"
)

private val hiddenAmountKeywords = listOf(
    "借记卡动账提醒",
    "动账提醒",
    "交易提醒",
    "提现到中国银行",
    "动账",
    "入账",
    "出账",
    "交易",
    "提现",
    "到账",
    "转账",
    "付款",
    "收款",
    "支付",
    "进度提醒",
    "余额变动",
    "银行卡交易"
)

private val trustedChatPaymentTitles = listOf(
    "微信支付",
    "QQ钱包",
    "支付凭证",
    "收款到账通知"
)

private val trustedChatPaymentKeywords = listOf(
    "微信支付",
    "QQ钱包",
    "支付凭证",
    "收款到账通知",
    "付款成功",
    "收款成功",
    "转账已收款"
)

private val recommendationKeywords = listOf(
    "推荐",
    "热门",
    "直播",
    "评论",
    "点赞",
    "收藏",
    "关注",
    "私信",
    "回复",
    "笔记",
    "种草",
    "上新",
    "浏览记录",
    "猜你喜欢",
    "为你推荐",
    "消息提醒",
    "新闻",
    "视频",
    "开播",
    "新动态"
)

private val advertisingKeywords = listOf(
    "活动",
    "广告",
    "促销",
    "优惠券",
    "福利",
    "签到",
    "会员日",
    "秒杀",
    "红包",
    "领现金",
    "砍价"
)

private val couponOnlyKeywords = listOf(
    "优惠券",
    "红包到账",
    "外卖红包",
    "福利",
    "签到",
    "会员日"
)

private val logisticsKeywords = listOf(
    "物流提醒",
    "包裹派送",
    "包裹正在派送",
    "快递",
    "物流",
    "派送",
    "订单状态",
    "已发货",
    "已签收"
)

private fun hasPaymentOrRefundAction(content: String): Boolean =
    commerceTransactionKeywords.any { content.contains(it, ignoreCase = true) } ||
        listOf("扣费", "代扣", "免密支付", "借记卡动账", "信用卡交易", "银行卡交易").any {
            content.contains(it, ignoreCase = true)
        }

private fun isChatApp(packageName: String, appName: String): Boolean =
    packageName in setOf(
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.tencent.tim",
        "com.tencent.wework",
        "com.alibaba.android.rimet"
    ) || listOf("微信", "QQ", "企业微信", "钉钉").any { appName.contains(it, ignoreCase = true) }

private fun looksLikeOrdinaryChatTitle(title: String, appName: String): Boolean {
    if (title.isBlank()) return false
    if (trustedChatPaymentTitles.any { title.contains(it, ignoreCase = true) }) return false
    return title.startsWith("微信 -") ||
        title.startsWith("QQ -") ||
        title.contains("文件传输助手") ||
        title.contains("群") ||
        (title != appName && !title.contains("支付", ignoreCase = true))
}

private fun isPaymentApp(packageName: String, appName: String): Boolean =
    SourceAppMapper.sourceKind(packageName) == SourceKind.PAYMENT ||
        listOf("支付宝", "微信支付", "云闪付", "QQ钱包").any { appName.contains(it, ignoreCase = true) }

private fun isBankApp(packageName: String, appName: String): Boolean =
    SourceAppMapper.sourceKind(packageName) == SourceKind.BANK ||
        listOf("银行", "农商", "信用社", "招商", "中行", "建行", "工行", "农行", "交行", "邮储", "浦发", "民生", "平安", "中信", "光大", "广发", "兴业")
            .any { appName.contains(it, ignoreCase = true) }

private fun isCommerceOrLifeApp(packageName: String, appName: String): Boolean =
    SourceAppMapper.sourceKind(packageName) in setOf(SourceKind.ECOMMERCE, SourceKind.FOOD_DELIVERY) ||
        listOf("淘宝", "天猫", "京东", "拼多多", "得物", "美团", "饿了么", "抖音商城")
            .any { appName.contains(it, ignoreCase = true) } ||
        packageName in setOf(
            "com.shizhuang.duapp",
            "com.ss.android.ugc.aweme",
            "com.xingin.xhs"
        )
