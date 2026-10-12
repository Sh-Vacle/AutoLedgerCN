package com.autoledgercn.domain.category

import com.autoledgercn.domain.model.TransactionType

data class CategoryGroup(
    val type: TransactionType,
    val primary: String,
    val secondary: List<String>,
    val isDefault: Boolean = true
)

object CategoryCatalog {
    val defaultExpense = listOf(
        group(TransactionType.EXPENSE, "餐饮", "早餐", "午餐", "晚餐", "外卖", "饮品", "奶茶咖啡", "零食", "水果", "夜宵", "食材生鲜", "聚餐", "其他餐饮"),
        group(TransactionType.EXPENSE, "交通", "公交地铁", "打车", "共享单车", "火车高铁", "机票", "长途客车", "加油", "停车", "高速过路费", "车辆维修", "其他交通"),
        group(TransactionType.EXPENSE, "购物", "日用品", "服饰鞋包", "数码电子", "电脑配件", "手机配件", "家居用品", "美妆护肤", "个护清洁", "快递运费", "网购", "其他购物"),
        group(TransactionType.EXPENSE, "居住", "房租", "水费", "电费", "燃气费", "物业费", "宽带", "维修", "家具家电", "其他居住"),
        group(TransactionType.EXPENSE, "娱乐", "游戏", "影视会员", "音乐会员", "直播打赏", "电影", "演出展览", "旅游", "景点门票", "桌游密室", "其他娱乐"),
        group(TransactionType.EXPENSE, "学习", "书籍", "课程", "资料", "考试报名", "文具", "软件工具", "论文资料", "其他学习"),
        group(TransactionType.EXPENSE, "医疗健康", "药品", "医院", "挂号", "体检", "运动健身", "保健护理", "其他医疗健康"),
        group(TransactionType.EXPENSE, "通讯", "手机话费", "流量包", "宽带", "通讯设备", "其他通讯"),
        group(TransactionType.EXPENSE, "人情社交", "红包", "转账", "礼物", "请客", "婚礼份子钱", "节日礼金", "其他人情"),
        group(TransactionType.EXPENSE, "金融", "手续费", "利息", "还款", "信用卡还款", "贷款还款", "保险", "理财支出", "其他金融"),
        group(TransactionType.EXPENSE, "工作", "办公用品", "通勤", "差旅", "报销垫付", "其他工作"),
        group(TransactionType.EXPENSE, "宠物", "宠物食品", "宠物用品", "宠物医疗", "其他宠物"),
        group(TransactionType.EXPENSE, "其他", "未分类", "其他支出")
    )

    val defaultIncome = listOf(
        group(TransactionType.INCOME, "工资收入", "工资", "奖金", "补贴", "报销", "其他工资收入"),
        group(TransactionType.INCOME, "转账收入", "家人转账", "朋友转账", "普通转账", "红包", "收款", "其他转账"),
        group(TransactionType.INCOME, "退款收入", "购物退款", "外卖退款", "交通退款", "订阅退款", "其他退款"),
        group(TransactionType.INCOME, "兼职副业", "兼职", "接单", "项目收入", "创作收入", "其他副业"),
        group(TransactionType.INCOME, "校园收入", "奖学金", "助学金", "竞赛奖金", "其他校园收入"),
        group(TransactionType.INCOME, "投资理财", "利息", "分红", "基金股票", "理财收益", "其他理财收入"),
        group(TransactionType.INCOME, "其他收入", "未分类收入", "其他")
    )

    val defaults: List<CategoryGroup> = defaultExpense + defaultIncome

    fun merge(custom: List<CategoryGroup>): List<CategoryGroup> {
        val merged = defaults.associateBy { it.type to it.primary }.toMutableMap()
        custom.forEach { customGroup ->
            val key = customGroup.type to customGroup.primary
            val existing = merged[key]
            merged[key] = if (existing == null) {
                customGroup.copy(isDefault = false)
            } else {
                existing.copy(secondary = (existing.secondary + customGroup.secondary).distinct())
            }
        }
        return merged.values.sortedWith(compareBy<CategoryGroup> { it.type.ordinal }.thenBy { it.primary })
    }

    fun visible(custom: List<CategoryGroup>, hiddenKeys: Set<String>): List<CategoryGroup> =
        merge(custom).mapNotNull { group ->
            val primaryKey = key(group.type, group.primary)
            if (primaryKey in hiddenKeys) {
                null
            } else {
                val visibleSecondary = group.secondary.filterNot {
                    key(group.type, group.primary, it) in hiddenKeys
                }
                group.copy(secondary = visibleSecondary).takeIf { it.secondary.isNotEmpty() }
            }
        }

    fun groupsFor(type: TransactionType, custom: List<CategoryGroup> = emptyList()): List<CategoryGroup> =
        merge(custom).filter { it.type == type }

    fun firstPrimary(type: TransactionType): String =
        groupsFor(type).firstOrNull()?.primary ?: if (type == TransactionType.EXPENSE) "其他" else "其他收入"

    fun firstSecondary(type: TransactionType, primary: String): String =
        groupsFor(type).firstOrNull { it.primary == primary }?.secondary?.firstOrNull()
            ?: if (type == TransactionType.EXPENSE) "未分类" else "未分类收入"

    private fun group(type: TransactionType, primary: String, vararg secondary: String) =
        CategoryGroup(type = type, primary = primary, secondary = secondary.toList(), isDefault = true)

    private fun key(type: TransactionType, primary: String, secondary: String? = null): String =
        listOf(type.name, primary, secondary.orEmpty()).joinToString("|")
}
