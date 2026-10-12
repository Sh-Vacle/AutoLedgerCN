package com.autoledgercn.domain.classifier

import com.autoledgercn.data.entity.CategoryRuleEntity
import com.autoledgercn.domain.model.ClassifiedTransaction
import com.autoledgercn.domain.model.ParsedTransaction
import com.autoledgercn.domain.model.TransactionType

class LocalRuleClassifier {
    private val defaults = listOf(
        Rule(listOf("美团", "饿了么", "外卖", "骑手", "餐品"), "餐饮", "外卖", TransactionType.EXPENSE, 100),
        Rule(listOf("蜜雪冰城", "瑞幸", "星巴克", "霸王茶姬", "茶百道", "古茗", "喜茶", "奈雪", "库迪", "咖啡", "奶茶"), "餐饮", "奶茶咖啡", TransactionType.EXPENSE, 100),
        Rule(listOf("便利店", "罗森", "全家", "711", "零食"), "餐饮", "零食", TransactionType.EXPENSE, 90),
        Rule(listOf("水果", "果切", "鲜果"), "餐饮", "水果", TransactionType.EXPENSE, 90),
        Rule(listOf("生鲜", "盒马", "叮咚买菜", "山姆", "超市", "菜市场"), "餐饮", "食材生鲜", TransactionType.EXPENSE, 90),
        Rule(listOf("火锅", "烧烤", "烤肉", "海底捞", "聚餐"), "餐饮", "聚餐", TransactionType.EXPENSE, 95),
        Rule(listOf("滴滴", "高德打车", "T3出行", "曹操出行", "打车"), "交通", "打车", TransactionType.EXPENSE, 95),
        Rule(listOf("地铁", "公交", "乘车码"), "交通", "公交地铁", TransactionType.EXPENSE, 95),
        Rule(listOf("12306", "火车", "高铁", "铁路"), "交通", "火车高铁", TransactionType.EXPENSE, 95),
        Rule(listOf("机票", "航旅", "航空"), "交通", "机票", TransactionType.EXPENSE, 90),
        Rule(listOf("加油", "中石化", "中石油"), "交通", "加油", TransactionType.EXPENSE, 90),
        Rule(listOf("停车", "停车费"), "交通", "停车", TransactionType.EXPENSE, 90),
        Rule(listOf("键盘", "鼠标", "电脑配件", "机械键盘", "显示器"), "购物", "电脑配件", TransactionType.EXPENSE, 100),
        Rule(listOf("手机壳", "充电器", "数据线", "手机配件"), "购物", "手机配件", TransactionType.EXPENSE, 95),
        Rule(listOf("手机", "耳机"), "购物", "手机配件", TransactionType.EXPENSE, 80),
        Rule(listOf("衣服", "服饰", "鞋", "裤子", "帽子", "优衣库", "耐克", "阿迪"), "购物", "服饰鞋包", TransactionType.EXPENSE, 90),
        Rule(listOf("日用品", "纸巾", "洗衣液", "洗发水", "沐浴露"), "购物", "日用品", TransactionType.EXPENSE, 90),
        Rule(listOf("快递", "运费", "顺丰", "中通", "圆通", "韵达", "邮政"), "购物", "快递运费", TransactionType.EXPENSE, 90),
        Rule(listOf("淘宝", "天猫", "京东", "拼多多", "抖音商城", "快手小店", "小红书", "网购"), "购物", "网购", TransactionType.EXPENSE, 70),
        Rule(listOf("Steam", "腾讯游戏", "网易游戏", "米哈游", "王者荣耀", "三角洲", "无畏契约", "游戏充值"), "娱乐", "游戏", TransactionType.EXPENSE, 100),
        Rule(listOf("B站大会员", "腾讯视频", "爱奇艺", "优酷", "芒果TV", "Netflix"), "娱乐", "影视会员", TransactionType.EXPENSE, 95),
        Rule(listOf("网易云音乐", "QQ音乐", "Spotify"), "娱乐", "音乐会员", TransactionType.EXPENSE, 95),
        Rule(listOf("电影", "影院", "猫眼", "淘票票"), "娱乐", "电影", TransactionType.EXPENSE, 90),
        Rule(listOf("景区", "门票", "旅游", "酒店", "携程", "飞猪", "去哪儿"), "娱乐", "旅游", TransactionType.EXPENSE, 90),
        Rule(listOf("课程", "网课", "培训"), "学习", "课程", TransactionType.EXPENSE, 90),
        Rule(listOf("书籍", "教材", "资料", "文具"), "学习", "资料", TransactionType.EXPENSE, 80),
        Rule(listOf("考试", "报名费", "CET", "考研", "软考", "CSP"), "学习", "考试报名", TransactionType.EXPENSE, 90),
        Rule(listOf("软件", "工具", "订阅", "会员", "GitHub", "JetBrains", "Notion"), "学习", "软件工具", TransactionType.EXPENSE, 85),
        Rule(listOf("美团买药", "京东健康", "药店", "药"), "医疗健康", "药品", TransactionType.EXPENSE, 95),
        Rule(listOf("医院", "挂号", "问诊"), "医疗健康", "医院", TransactionType.EXPENSE, 90),
        Rule(listOf("健身", "Keep", "健身房", "运动"), "医疗健康", "运动健身", TransactionType.EXPENSE, 90),
        Rule(listOf("房租", "租金"), "居住", "房租", TransactionType.EXPENSE, 95),
        Rule(listOf("电费", "国家电网"), "居住", "电费", TransactionType.EXPENSE, 95),
        Rule(listOf("水费"), "居住", "水费", TransactionType.EXPENSE, 90),
        Rule(listOf("燃气"), "居住", "燃气费", TransactionType.EXPENSE, 90),
        Rule(listOf("物业"), "居住", "物业费", TransactionType.EXPENSE, 90),
        Rule(listOf("宽带"), "居住", "宽带", TransactionType.EXPENSE, 80),
        Rule(listOf("话费"), "通讯", "手机话费", TransactionType.EXPENSE, 85),
        Rule(listOf("流量包", "流量"), "通讯", "流量包", TransactionType.EXPENSE, 85),
        Rule(listOf("红包"), "人情社交", "红包", TransactionType.EXPENSE, 70),
        Rule(listOf("转账"), "人情社交", "转账", TransactionType.EXPENSE, 60),
        Rule(listOf("工资", "薪资", "薪酬", "奖金", "补贴"), "工资收入", "工资", TransactionType.INCOME, 100),
        Rule(listOf("报销"), "工资收入", "报销", TransactionType.INCOME, 95),
        Rule(listOf("红包"), "转账收入", "红包", TransactionType.INCOME, 95),
        Rule(listOf("向你转账", "收到转账", "收款", "朋友", "妈妈", "爸爸", "家人"), "转账收入", "普通转账", TransactionType.INCOME, 90),
        Rule(listOf("退款成功", "退回", "原路退回", "退款"), "退款收入", "其他退款", TransactionType.INCOME, 100),
        Rule(listOf("兼职", "接单", "稿费", "项目款"), "兼职副业", "兼职", TransactionType.INCOME, 90),
        Rule(listOf("奖学金", "助学金", "竞赛奖金"), "校园收入", "奖学金", TransactionType.INCOME, 90),
        Rule(listOf("利息", "分红", "理财收益"), "投资理财", "理财收益", TransactionType.INCOME, 90)
    )

    fun classify(parsed: ParsedTransaction, customRules: List<CategoryRuleEntity> = emptyList()): ClassifiedTransaction {
        val text = listOf(parsed.merchant, parsed.sourceApp, parsed.rawTitle, parsed.rawText, parsed.rawBigText).joinToString(" ")
        val custom = customRules
            .filter { it.enabled && it.transactionType == parsed.type && text.contains(it.keyword, ignoreCase = true) }
            .maxByOrNull { it.priority }
        if (custom != null) {
            return ClassifiedTransaction(custom.categoryPrimary, custom.categorySecondary, 0.18)
        }
        val rule = defaults
            .filter { it.type == parsed.type && it.keywords.any { keyword -> text.contains(keyword, ignoreCase = true) } }
            .maxByOrNull { it.priority }
        return if (rule != null) {
            ClassifiedTransaction(rule.primary, rule.secondary, if (rule.priority >= 90) 0.18 else 0.08)
        } else if (parsed.type == TransactionType.INCOME) {
            ClassifiedTransaction("其他收入", "其他", 0.0)
        } else {
            ClassifiedTransaction("其他", "未分类", -0.05)
        }
    }

    private data class Rule(
        val keywords: List<String>,
        val primary: String,
        val secondary: String,
        val type: TransactionType,
        val priority: Int
    )
}
