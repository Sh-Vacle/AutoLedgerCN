package com.autoledgercn.data.app

object AppInitialHelper {
    private val packageInitials = mapOf(
        "com.cainiao.wireless" to "C",
        "com.clover.daysmatter" to "D",
        "com.shizhuang.duapp" to "D",
        "com.chaozhi.iReader.widget" to "D",
        "com.ss.android.auto" to "D",
        "com.tencent.mm" to "W",
        "com.eg.android.AlipayGphone" to "Z",
        "cn.com.boc" to "Z",
        "com.cmbchina.ccd.pluto.cmbActivity" to "Z",
        "com.sankuai.meituan" to "M",
        "com.taobao.taobao" to "T",
        "com.jingdong.app.mall" to "J",
        "com.xunmeng.pinduoduo" to "P",
        "me.ele" to "E",
        "com.baidu.netdisk" to "B",
        "com.autonavi.minimap" to "G",
        "com.android.chrome" to "C",
        "com.openai.chatgpt" to "C"
    )

    private val appNameInitials = listOf(
        "菜鸟" to "C",
        "倒数日" to "D",
        "得物" to "D",
        "电子书" to "D",
        "懂车帝" to "D",
        "番茄" to "F",
        "红果" to "H",
        "微信" to "W",
        "支付宝" to "Z",
        "中国银行" to "Z",
        "招商银行" to "Z",
        "美团" to "M",
        "淘宝" to "T",
        "京东" to "J",
        "拼多多" to "P",
        "饿了么" to "E",
        "抖音" to "D",
        "快手" to "K",
        "小红书" to "X",
        "哔哩哔哩" to "B",
        "百度网盘" to "B",
        "高德地图" to "G",
        "知乎" to "Z",
        "钉钉" to "D",
        "飞书" to "F"
    )

    private val commonChineseInitials = mapOf(
        '菜' to "C",
        '倒' to "D",
        '得' to "D",
        '电' to "D",
        '懂' to "D",
        '番' to "F",
        '红' to "H",
        '支' to "Z",
        '中' to "Z",
        '招' to "Z",
        '微' to "W",
        '美' to "M",
        '淘' to "T",
        '京' to "J",
        '拼' to "P",
        '饿' to "E",
        '百' to "B",
        '高' to "G",
        '哔' to "B",
        '小' to "X",
        '抖' to "D",
        '快' to "K",
        '知' to "Z",
        '钉' to "D",
        '飞' to "F"
    )

    fun groupKey(appName: String, packageName: String): String {
        packageInitials[packageName]?.let { return it }

        val normalizedName = appName.trim().dropWhile { !it.isLetterOrDigit() }
        appNameInitials.firstOrNull { (prefix, _) -> normalizedName.startsWith(prefix) }?.let { (_, initial) ->
            return initial
        }

        val first = firstEffectiveChar(appName) ?: return "#"
        if (first.isDigit()) return "#"

        transliteratedInitial(first)?.let { return it }
        commonChineseInitials[first]?.let { return it }
        latinInitial(first)?.let { return it }

        return "#"
    }

    private fun firstEffectiveChar(value: String): Char? =
        value.trim().firstOrNull { it.isLetterOrDigit() }

    private fun transliteratedInitial(first: Char): String? {
        val pinyin = HanLatinTransliterator.transliterate(first.toString()) ?: return null
        return latinInitial(pinyin.firstOrNull { it.isLetter() } ?: return null)
    }

    private fun latinInitial(first: Char): String? = when (first) {
        in 'A'..'Z' -> first.toString()
        in 'a'..'z' -> first.uppercaseChar().toString()
        else -> null
    }

    private object HanLatinTransliterator {
        private val transliterator: Any? by lazy {
            runCatching {
                val clazz = Class.forName("android.icu.text.Transliterator")
                clazz.getMethod("getInstance", String::class.java)
                    .invoke(null, "Han-Latin; Latin-ASCII")
            }.getOrNull()
        }

        fun transliterate(value: String): String? {
            val instance = transliterator ?: return null
            return runCatching {
                instance.javaClass.getMethod("transliterate", String::class.java)
                    .invoke(instance, value) as? String
            }.getOrNull()
        }
    }
}
