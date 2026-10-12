package com.autoledgercn.domain.parser

import com.autoledgercn.domain.model.SourceKind

object SourceAppMapper {
    private val packageMap = mapOf(
        "com.eg.android.AlipayGphone" to ("支付宝" to SourceKind.PAYMENT),
        "com.tencent.mm" to ("微信" to SourceKind.PAYMENT),
        "com.unionpay" to ("云闪付" to SourceKind.PAYMENT),
        "com.taobao.taobao" to ("淘宝" to SourceKind.ECOMMERCE),
        "com.tmall.wireless" to ("天猫" to SourceKind.ECOMMERCE),
        "com.jingdong.app.mall" to ("京东" to SourceKind.ECOMMERCE),
        "com.xunmeng.pinduoduo" to ("拼多多" to SourceKind.ECOMMERCE),
        "com.sankuai.meituan" to ("美团" to SourceKind.FOOD_DELIVERY),
        "me.ele" to ("饿了么" to SourceKind.FOOD_DELIVERY),
        "cn.com.boc" to ("中国银行" to SourceKind.BANK),
        "com.cmbchina.ccd.pluto.cmbActivity" to ("招商银行" to SourceKind.BANK),
        "com.chinamworld.main" to ("建设银行" to SourceKind.BANK),
        "com.icbc" to ("工商银行" to SourceKind.BANK),
        "com.android.bankabc" to ("农业银行" to SourceKind.BANK),
        "com.bankcomm.Bankcomm" to ("交通银行" to SourceKind.BANK)
    )

    fun appName(packageName: String): String = packageMap[packageName]?.first ?: "未知"

    fun sourceKind(packageName: String): SourceKind = packageMap[packageName]?.second ?: SourceKind.OTHER

    fun accountFrom(packageName: String, text: String): String {
        val combined = packageName + text
        return when {
            combined.contains("招商") || combined.contains("cmb", ignoreCase = true) -> "招商银行"
            combined.contains("中国银行") || combined.contains("boc", ignoreCase = true) -> "中国银行"
            combined.contains("建设银行") || combined.contains("chinamworld", ignoreCase = true) -> "建设银行"
            combined.contains("工商银行") || combined.contains("icbc", ignoreCase = true) -> "工商银行"
            combined.contains("农业银行") || combined.contains("bankabc", ignoreCase = true) -> "农业银行"
            combined.contains("交通银行") || combined.contains("bankcomm", ignoreCase = true) -> "交通银行"
            packageName == "com.eg.android.AlipayGphone" || text.contains("支付宝") -> "支付宝"
            packageName == "com.tencent.mm" || text.contains("微信") -> "微信"
            packageName == "com.unionpay" || text.contains("云闪付") -> "云闪付"
            else -> "未知"
        }
    }
}
