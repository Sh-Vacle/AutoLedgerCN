package com.autoledgercn

import com.autoledgercn.data.app.InstalledAppRepository
import com.autoledgercn.data.app.AppInitialHelper
import com.autoledgercn.data.app.LaunchableApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppListRepositoryTest {
    @Test
    fun mergeKeepsInstalledAppsThatAreNotLaunchable() {
        val installedOnly = LaunchableApp("中国银行", "cn.com.boc")
        val launcherOnly = LaunchableApp("设置", "com.android.settings")

        val apps = InstalledAppRepository.mergeInstalledAndLaunchable(
            installedApps = listOf(installedOnly),
            launchableApps = listOf(launcherOnly)
        )

        assertEquals(setOf("cn.com.boc", "com.android.settings"), apps.map { it.packageName }.toSet())
    }

    @Test
    fun filterSearchesAppNamePackageNameAndAliases() {
        val apps = listOf(
            LaunchableApp("微信", "com.tencent.mm"),
            LaunchableApp("示例记账", "com.example.ledger"),
            LaunchableApp("中国银行", "cn.com.boc")
        )

        assertEquals(listOf(apps[0]), InstalledAppRepository.filter(apps, "微信"))
        assertEquals(listOf(apps[0]), InstalledAppRepository.filter(apps, "WeChat"))
        assertEquals(listOf(apps[0]), InstalledAppRepository.filter(apps, "com.tencent.mm"))
        assertEquals(listOf(apps[2]), InstalledAppRepository.filter(apps, "中国银行"))
        assertEquals(listOf(apps[2]), InstalledAppRepository.filter(apps, "cn.com.boc"))
        assertEquals(listOf(apps[1]), InstalledAppRepository.filter(apps, "ledger"))
    }

    @Test
    fun searchUsesFullInstalledListNotFirstEightyOnly() {
        val apps = (1..100).map { LaunchableApp("App $it", "com.example.app$it") } +
            LaunchableApp("微信", "com.tencent.mm")

        assertEquals(listOf(apps.last()), InstalledAppRepository.filter(apps, "微信"))
        assertEquals(listOf(apps.last()), InstalledAppRepository.filter(apps, "com.tencent.mm"))
    }

    @Test
    fun defaultDisplayHidesSystemComponentsButKeepsCommonApps() {
        val systemComponent = LaunchableApp(
            appName = "3 Button Navigation Bar",
            packageName = "com.android.internal.systemui.navbar.threebutton",
            isSystem = true,
            isLaunchable = false
        )
        val apps = listOf(
            systemComponent,
            LaunchableApp("微信", "com.tencent.mm", isSystem = false, isLaunchable = true),
            LaunchableApp("支付宝", "com.eg.android.AlipayGphone", isSystem = false, isLaunchable = true),
            LaunchableApp("中国银行", "cn.com.boc", isSystem = false, isLaunchable = true)
        )

        val visiblePackages = InstalledAppRepository.displayApps(apps, query = "", showSystemApps = false)
            .map { it.packageName }

        assertFalse(systemComponent.packageName in visiblePackages)
        assertEquals(
            setOf("com.tencent.mm", "com.eg.android.AlipayGphone", "cn.com.boc"),
            visiblePackages.toSet()
        )
    }

    @Test
    fun searchUsesFullListEvenWhenSystemAppsAreHidden() {
        val systemComponent = LaunchableApp(
            appName = "3 Button Navigation Bar",
            packageName = "com.android.internal.systemui.navbar.threebutton",
            isSystem = true,
            isLaunchable = false
        )

        assertEquals(
            listOf(systemComponent),
            InstalledAppRepository.displayApps(listOf(systemComponent), "threebutton", showSystemApps = false)
        )
    }

    @Test
    fun appInitialHelperUsesPinyinStyleGroupsOnly() {
        val cases = listOf(
            Triple("任意名称", "com.cainiao.wireless", "C"),
            Triple("任意名称", "com.clover.daysmatter", "D"),
            Triple("任意名称", "com.shizhuang.duapp", "D"),
            Triple("任意名称", "com.chaozhi.iReader.widget", "D"),
            Triple("任意名称", "com.ss.android.auto", "D"),
            Triple("支付宝", "com.eg.android.AlipayGphone", "Z"),
            Triple("中国银行", "cn.com.boc", "Z"),
            Triple("微信", "com.tencent.mm", "W"),
            Triple("美团", "com.sankuai.meituan", "M"),
            Triple("淘宝", "com.taobao.taobao", "T"),
            Triple("拼多多", "com.xunmeng.pinduoduo", "P"),
            Triple("饿了么", "me.ele", "E"),
            Triple("Chrome", "com.android.chrome", "C")
        )

        cases.forEach { (name, packageName, expected) ->
            assertEquals(expected, AppInitialHelper.groupKey(name, packageName))
        }
        assertFalse(AppInitialHelper.groupKey("支付宝", "unknown") in setOf("支", "中", "微"))
    }

    @Test
    fun appInitialHelperGroupsCommonChineseAppNames() {
        val cases = listOf(
            "菜鸟" to "C",
            "倒数日" to "D",
            "得物" to "D",
            "电子书-快速阅读" to "D",
            "懂车帝" to "D",
            "番茄免费小说" to "F",
            "红果免费短剧" to "H",
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

        cases.forEach { (name, expected) ->
            assertEquals(expected, AppInitialHelper.groupKey(name, "unknown.package"))
        }
    }

    @Test
    fun appInitialHelperKeepsUnknownStartsInHashGroup() {
        assertEquals("#", AppInitialHelper.groupKey("123 记账", "unknown.package"))
        assertEquals("C", AppInitialHelper.groupKey("Chrome", "unknown.package"))
        assertEquals("#", AppInitialHelper.groupKey("", "unknown.package"))
        assertEquals("#", AppInitialHelper.groupKey("！？【】", "unknown.package"))

        val invalidGroups = setOf("菜", "倒", "支", "中", "微")
        listOf("菜鸟", "倒数日", "支付宝", "中国银行", "微信").forEach { name ->
            assertFalse(AppInitialHelper.groupKey(name, "unknown.package") in invalidGroups)
        }
    }
}
