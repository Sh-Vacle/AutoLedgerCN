package com.autoledgercn.data.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.text.Collator
import java.util.Locale

data class LaunchableApp(
    val appName: String,
    val packageName: String,
    val isSystem: Boolean = false,
    val isLaunchable: Boolean = false
)

class InstalledAppRepository(private val context: Context) {
    fun loadInstalledApps(): List<LaunchableApp> = loadInstalledApps(context)
    fun loadLaunchableApps(): List<LaunchableApp> = loadInstalledApps()

    companion object {
        fun loadInstalledApps(context: Context): List<LaunchableApp> {
            val pm = context.packageManager
            val launchablePackages = launcherPackages(pm)
            val appsByPackage = linkedMapOf<String, LaunchableApp>()

            installedApplications(pm).forEach { info ->
                val packageName = info.packageName?.trim().orEmpty()
                if (packageName.isBlank() || packageName == context.packageName) return@forEach
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                val isLaunchable = packageName in launchablePackages
                val isSystem = info.isSystemApp()
                appsByPackage[packageName] = LaunchableApp(
                    appName = label.ifBlank { packageName },
                    packageName = packageName,
                    isSystem = isSystem,
                    isLaunchable = isLaunchable
                )
            }

            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY).forEach { info ->
                val packageName = info.activityInfo?.packageName?.trim().orEmpty()
                if (packageName.isBlank() || packageName == context.packageName) return@forEach
                val existing = appsByPackage[packageName]
                if (existing == null) {
                    val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                    appsByPackage[packageName] = LaunchableApp(
                        appName = label.ifBlank { packageName },
                        packageName = packageName,
                        isSystem = false,
                        isLaunchable = true
                    )
                } else if (!existing.isLaunchable) {
                    appsByPackage[packageName] = existing.copy(isLaunchable = true)
                }
            }

            return sortApps(appsByPackage.values)
        }

        fun loadLaunchableApps(context: Context): List<LaunchableApp> = loadInstalledApps(context)

        fun mergeInstalledAndLaunchable(
            installedApps: List<LaunchableApp>,
            launchableApps: List<LaunchableApp>
        ): List<LaunchableApp> {
            val appsByPackage = linkedMapOf<String, LaunchableApp>()
            installedApps.forEach { app ->
                appsByPackage[app.packageName] = app
            }
            launchableApps.forEach { app ->
                val existing = appsByPackage[app.packageName]
                appsByPackage[app.packageName] = existing?.copy(isLaunchable = true) ?: app.copy(isLaunchable = true)
            }
            return sortApps(appsByPackage.values)
        }

        private fun installedApplications(pm: PackageManager): List<ApplicationInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }

        private fun launcherPackages(pm: PackageManager): Set<String> {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            return pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        }

        private fun ApplicationInfo.isSystemApp(): Boolean =
            flags and ApplicationInfo.FLAG_SYSTEM != 0 || flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

        private fun sortApps(apps: Collection<LaunchableApp>): List<LaunchableApp> {
            val collator = Collator.getInstance(Locale.CHINA)
            return apps.sortedWith { left, right ->
                val nameCompare = collator.compare(left.appName, right.appName)
                if (nameCompare != 0) nameCompare else left.packageName.compareTo(right.packageName)
            }
        }

        fun displayApps(apps: List<LaunchableApp>, query: String, showSystemApps: Boolean): List<LaunchableApp> {
            val keyword = query.trim()
            if (keyword.isNotBlank()) return filter(apps, keyword)
            return if (showSystemApps) apps else apps.filter { isDefaultVisible(it) }
        }

        fun isDefaultVisible(app: LaunchableApp): Boolean {
            if (app.packageName in commonUserPackages) return true
            if (app.isSystem && !app.isLaunchable) return false
            if (hasHiddenPackagePrefix(app.packageName)) return false
            if (hasHiddenLabel(app.appName)) return false
            return app.isLaunchable || !app.isSystem
        }

        fun filter(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
            val keyword = query.trim()
            if (keyword.isBlank()) return apps
            return apps.filter {
                val aliases = searchAliases[it.packageName].orEmpty()
                it.appName.contains(keyword, ignoreCase = true) ||
                    it.packageName.contains(keyword, ignoreCase = true) ||
                    aliases.any { alias -> alias.contains(keyword, ignoreCase = true) || keyword.contains(alias, ignoreCase = true) }
            }
        }

        private fun hasHiddenPackagePrefix(packageName: String): Boolean =
            hiddenPackagePrefixes.any { packageName.startsWith(it, ignoreCase = true) }

        private fun hasHiddenLabel(label: String): Boolean =
            hiddenLabelKeywords.any { label.contains(it, ignoreCase = true) }

        private val hiddenPackagePrefixes = listOf(
            "com.android.",
            "com.vivo.",
            "vivo.",
            "com.qualcomm.",
            "com.google.android.gms",
            "com.google.android.modulemetadata",
            "com.android.internal."
        )

        private val hiddenLabelKeywords = listOf(
            "Plugin",
            "Service",
            "Driver",
            "Graphics Driver",
            "Navigation Bar",
            "Ad Services",
            "Accessibility Enhance",
            "SystemUI",
            "Shell",
            "Package Installer"
        )

        private val commonUserPackages = setOf(
            "com.tencent.mm",
            "com.eg.android.AlipayGphone",
            "cn.com.boc",
            "com.cmbchina.ccd.pluto.cmbActivity",
            "com.sankuai.meituan",
            "com.taobao.taobao",
            "com.jingdong.app.mall",
            "com.xunmeng.pinduoduo",
            "me.ele",
            "com.ss.android.ugc.aweme",
            "com.smile.gifmaker",
            "com.xingin.xhs",
            "tv.danmaku.bili",
            "com.baidu.netdisk",
            "com.autonavi.minimap",
            "com.android.chrome",
            "com.chrome.beta",
            "com.openai.chatgpt",
            "com.tencent.mobileqq",
            "com.tencent.qqmusic",
            "com.baidu.BaiduMap",
            "com.baidu.searchbox"
        )

        private val searchAliases = mapOf(
            "com.tencent.mm" to listOf("微信", "WeChat", "com.tencent.mm"),
            "com.eg.android.AlipayGphone" to listOf("支付宝", "Alipay", "com.eg.android.AlipayGphone"),
            "cn.com.boc" to listOf("中国银行", "BOC", "cn.com.boc"),
            "com.cmbchina.ccd.pluto.cmbActivity" to listOf("招商银行", "CMB", "com.cmbchina.ccd.pluto.cmbActivity"),
            "com.sankuai.meituan" to listOf("美团", "Meituan"),
            "com.taobao.taobao" to listOf("淘宝", "Taobao"),
            "com.jingdong.app.mall" to listOf("京东", "JD"),
            "com.xunmeng.pinduoduo" to listOf("拼多多", "Pinduoduo"),
            "me.ele" to listOf("饿了么", "Eleme"),
            "com.baidu.netdisk" to listOf("百度网盘", "Baidu Netdisk"),
            "com.autonavi.minimap" to listOf("高德地图", "Amap"),
            "com.android.chrome" to listOf("Chrome", "谷歌浏览器"),
            "com.openai.chatgpt" to listOf("ChatGPT", "OpenAI")
        )
    }
}
