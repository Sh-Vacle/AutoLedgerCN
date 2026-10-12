package com.autoledgercn.domain.model

object AccountQuickOptions {
    val defaults = listOf("微信", "支付宝", "银行卡", "现金", "其他")

    fun merged(customOptions: List<String>): List<String> =
        (defaults + customOptions)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    fun addOption(options: List<String>, name: String): List<String> {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return options
        return (options + cleanName)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
