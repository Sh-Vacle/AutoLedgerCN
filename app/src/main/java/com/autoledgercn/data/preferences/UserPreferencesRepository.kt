package com.autoledgercn.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.autoledgercn.AutoLedgerApp
import com.autoledgercn.domain.category.CategoryGroup
import com.autoledgercn.domain.model.AccountQuickOptions
import com.autoledgercn.domain.model.ThemeMode
import com.autoledgercn.domain.model.TransactionType
import com.autoledgercn.domain.stats.StatsRangeDefaults
import com.autoledgercn.domain.time.DateRange
import com.autoledgercn.domain.time.DateRangeCalculator
import com.autoledgercn.domain.time.TimeRangeType
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_preferences")

data class UserPreference(
    val themeMode: ThemeMode = ThemeMode.YIER,
    val onboardingFinished: Boolean = false,
    val preferencesLoaded: Boolean = false,
    val notificationAccessHintShown: Boolean = false,
    val permissionGuideShown: Boolean = false,
    val permissionGuideCompleted: Boolean = false,
    val bootKeepAliveEnabled: Boolean = false,
    val bootKeepAliveGuideShown: Boolean = false
)

data class MonitoredApp(
    val packageName: String,
    val name: String,
    val enabled: Boolean = true,
    val isDefault: Boolean = false
)

data class HomeRangePreference(
    val selectedRange: TimeRangeType,
    val customStart: String,
    val customEnd: String,
    val customRange: DateRange?
)

data class StatsRangePreference(
    val selectedRange: TimeRangeType,
    val selectedDate: Long,
    val customStart: String,
    val customEnd: String,
    val customRange: DateRange
)

data class BudgetSettingsPreference(
    val nextIncomeDate: String = defaultBudgetNextIncomeDate(),
    val safeBalance: Double = 300.0,
    val essentialCategories: String = "餐饮、交通、居住、通讯、医疗健康"
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val onboardingFinished = booleanPreferencesKey("onboarding_finished")
        val notificationAccessHintShown = booleanPreferencesKey("notification_access_hint_shown")
        val permissionGuideShown = booleanPreferencesKey("permission_guide_shown")
        val permissionGuideCompleted = booleanPreferencesKey("permission_guide_completed")
        val bootKeepAliveEnabled = booleanPreferencesKey("boot_keep_alive_enabled")
        val bootKeepAliveGuideShown = booleanPreferencesKey("boot_keep_alive_guide_shown")
        val customMonitoredApps = stringPreferencesKey("custom_monitored_apps")
        val customCategories = stringPreferencesKey("custom_categories")
        val hiddenCategories = stringPreferencesKey("hidden_categories")
        val accountQuickOptions = stringPreferencesKey("account_quick_options")
        val homeRangeType = stringPreferencesKey("home_range_type")
        val homeCustomStartDate = stringPreferencesKey("home_custom_start_date")
        val homeCustomEndDate = stringPreferencesKey("home_custom_end_date")
        val homeAppliedCustomStartDate = stringPreferencesKey("home_applied_custom_start_date")
        val homeAppliedCustomEndDate = stringPreferencesKey("home_applied_custom_end_date")
        val statsRangeType = stringPreferencesKey("stats_range_type")
        val statsSelectedDate = stringPreferencesKey("stats_selected_date")
        val statsCustomStartDate = stringPreferencesKey("stats_custom_start_date")
        val statsCustomEndDate = stringPreferencesKey("stats_custom_end_date")
        val statsAppliedCustomStartDate = stringPreferencesKey("stats_applied_custom_start_date")
        val statsAppliedCustomEndDate = stringPreferencesKey("stats_applied_custom_end_date")
        val budgetNextIncomeDate = stringPreferencesKey("budget_next_income_date")
        val budgetSafeBalance = doublePreferencesKey("budget_safe_balance")
        val budgetEssentialCategories = stringPreferencesKey("budget_essential_categories")
        val whitelistPrefix = "whitelist_"
    }

    val preferences: Flow<UserPreference> = context.dataStore.data.map { prefs ->
        UserPreference(
            themeMode = decodeThemeMode(prefs[Keys.themeMode]),
            onboardingFinished = prefs[Keys.onboardingFinished] ?: false,
            preferencesLoaded = true,
            notificationAccessHintShown = prefs[Keys.notificationAccessHintShown] ?: false,
            permissionGuideShown = prefs[Keys.permissionGuideShown] ?: false,
            permissionGuideCompleted = prefs[Keys.permissionGuideCompleted] ?: false,
            bootKeepAliveEnabled = prefs[Keys.bootKeepAliveEnabled] ?: false,
            bootKeepAliveGuideShown = prefs[Keys.bootKeepAliveGuideShown] ?: false
        )
    }

    fun whitelistFlow(): Flow<Map<String, Boolean>> = context.dataStore.data.map { prefs ->
        (DefaultAppWhitelist.packages + decodeApps(prefs[Keys.customMonitoredApps]).map { it.packageName }).associateWith { pkg ->
            prefs[booleanPreferencesKey(Keys.whitelistPrefix + pkg)] ?: true
        }
    }

    fun monitoredAppsFlow(): Flow<List<MonitoredApp>> = context.dataStore.data.map { prefs ->
        val defaults = DefaultAppWhitelist.packageNames.map { (pkg, name) ->
            MonitoredApp(
                packageName = pkg,
                name = name,
                enabled = prefs[booleanPreferencesKey(Keys.whitelistPrefix + pkg)] ?: true,
                isDefault = true
            )
        }
        val custom = decodeApps(prefs[Keys.customMonitoredApps]).map {
            it.copy(enabled = prefs[booleanPreferencesKey(Keys.whitelistPrefix + it.packageName)] ?: true)
        }
        defaults + custom
    }

    fun customCategoriesFlow(): Flow<List<CategoryGroup>> = context.dataStore.data.map { prefs ->
        decodeCategories(prefs[Keys.customCategories])
    }

    fun hiddenCategoriesFlow(): Flow<Set<String>> = context.dataStore.data.map { prefs ->
        decodeHiddenCategories(prefs[Keys.hiddenCategories])
    }

    fun accountOptionsFlow(): Flow<List<String>> = context.dataStore.data.map { prefs ->
        AccountQuickOptions.merged(decodeAccountOptions(prefs[Keys.accountQuickOptions]))
    }

    fun budgetSettingsFlow(): Flow<BudgetSettingsPreference> = context.dataStore.data.map { prefs ->
        BudgetSettingsPreference(
            nextIncomeDate = validDateOrFallback(prefs[Keys.budgetNextIncomeDate], defaultBudgetNextIncomeDate()),
            safeBalance = (prefs[Keys.budgetSafeBalance] ?: 300.0).takeIf { it >= 0.0 } ?: 300.0,
            essentialCategories = prefs[Keys.budgetEssentialCategories]?.trim()
                ?.ifBlank { null }
                ?: "餐饮、交通、居住、通讯、医疗健康"
        )
    }

    fun homeRangeFlow(): Flow<HomeRangePreference> = context.dataStore.data.map { prefs ->
        val now = System.currentTimeMillis()
        val defaults = StatsRangeDefaults.initial(now)
        val applied = decodeCustomRange(prefs[Keys.homeAppliedCustomStartDate], prefs[Keys.homeAppliedCustomEndDate])
        val savedRange = decodeRangeType(prefs[Keys.homeRangeType]) ?: TimeRangeType.MONTH
        val custom = validCustomDraft(
            start = prefs[Keys.homeCustomStartDate],
            end = prefs[Keys.homeCustomEndDate],
            fallbackStart = applied?.start ?: defaults.customStart,
            fallbackEnd = applied?.end ?: defaults.customEnd
        ) ?: applied ?: StoredCustomRange(defaults.customStart, defaults.customEnd, defaults.customRange)
        HomeRangePreference(
            selectedRange = savedRange,
            customStart = custom.start,
            customEnd = custom.end,
            customRange = applied?.range ?: defaults.customRange
        )
    }

    fun statsRangeFlow(): Flow<StatsRangePreference> = context.dataStore.data.map { prefs ->
        val now = System.currentTimeMillis()
        val defaults = StatsRangeDefaults.initial(now)
        val applied = decodeCustomRange(prefs[Keys.statsAppliedCustomStartDate], prefs[Keys.statsAppliedCustomEndDate])
            ?: StoredCustomRange(defaults.customStart, defaults.customEnd, defaults.customRange)
        val custom = validCustomDraft(
            start = prefs[Keys.statsCustomStartDate],
            end = prefs[Keys.statsCustomEndDate],
            fallbackStart = applied.start,
            fallbackEnd = applied.end
        ) ?: applied
        StatsRangePreference(
            selectedRange = decodeRangeType(prefs[Keys.statsRangeType]) ?: defaults.selectedRange,
            selectedDate = decodeDateMillis(prefs[Keys.statsSelectedDate]) ?: now,
            customStart = custom.start,
            customEnd = custom.end,
            customRange = applied.range
        )
    }

    suspend fun finishOnboardingWithTheme(mode: ThemeMode) {
        context.dataStore.edit {
            it[Keys.themeMode] = supportedTheme(mode).name
            it[Keys.onboardingFinished] = true
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.themeMode] = supportedTheme(mode).name }
    }

    suspend fun setHomeRangeType(range: TimeRangeType) {
        context.dataStore.edit { it[Keys.homeRangeType] = range.name }
    }

    suspend fun setHomeCustomStartDate(value: String) {
        context.dataStore.edit { it[Keys.homeCustomStartDate] = value.trim() }
    }

    suspend fun setHomeCustomEndDate(value: String) {
        context.dataStore.edit { it[Keys.homeCustomEndDate] = value.trim() }
    }

    suspend fun applyHomeCustomRange(start: String, end: String) {
        val cleanStart = start.trim()
        val cleanEnd = end.trim()
        context.dataStore.edit {
            it[Keys.homeCustomStartDate] = cleanStart
            it[Keys.homeCustomEndDate] = cleanEnd
            it[Keys.homeAppliedCustomStartDate] = cleanStart
            it[Keys.homeAppliedCustomEndDate] = cleanEnd
            it[Keys.homeRangeType] = TimeRangeType.CUSTOM.name
        }
    }

    suspend fun setStatsRangeType(range: TimeRangeType) {
        context.dataStore.edit { it[Keys.statsRangeType] = range.name }
    }

    suspend fun setStatsSelectedDate(dateMillis: Long) {
        context.dataStore.edit { it[Keys.statsSelectedDate] = DateRangeCalculator.dateString(dateMillis) }
    }

    suspend fun setStatsCustomStartDate(value: String) {
        context.dataStore.edit { it[Keys.statsCustomStartDate] = value.trim() }
    }

    suspend fun setStatsCustomEndDate(value: String) {
        context.dataStore.edit { it[Keys.statsCustomEndDate] = value.trim() }
    }

    suspend fun saveBudgetSettings(nextIncomeDate: String, safeBalance: Double, essentialCategories: String) {
        context.dataStore.edit {
            it[Keys.budgetNextIncomeDate] = validDateOrFallback(nextIncomeDate, defaultBudgetNextIncomeDate())
            it[Keys.budgetSafeBalance] = safeBalance.coerceAtLeast(0.0)
            it[Keys.budgetEssentialCategories] = essentialCategories.trim().ifBlank { "餐饮、交通、居住、通讯、医疗健康" }
        }
    }

    suspend fun applyStatsCustomRange(start: String, end: String) {
        val cleanStart = start.trim()
        val cleanEnd = end.trim()
        context.dataStore.edit {
            it[Keys.statsCustomStartDate] = cleanStart
            it[Keys.statsCustomEndDate] = cleanEnd
            it[Keys.statsAppliedCustomStartDate] = cleanStart
            it[Keys.statsAppliedCustomEndDate] = cleanEnd
            it[Keys.statsRangeType] = TimeRangeType.CUSTOM.name
        }
    }

    suspend fun setWhitelist(packageName: String, enabled: Boolean) {
        context.dataStore.edit { it[booleanPreferencesKey(Keys.whitelistPrefix + packageName)] = enabled }
    }

    suspend fun setBootKeepAlive(enabled: Boolean) {
        context.dataStore.edit { it[Keys.bootKeepAliveEnabled] = enabled }
        if (enabled) {
            AutoLedgerApp.scheduleStatsRefresh(context)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(AutoLedgerApp.STATS_REFRESH_WORK_NAME)
        }
    }

    suspend fun setBootKeepAliveGuideShown(shown: Boolean) {
        context.dataStore.edit { it[Keys.bootKeepAliveGuideShown] = shown }
    }

    suspend fun setPermissionGuideShown(shown: Boolean) {
        context.dataStore.edit { it[Keys.permissionGuideShown] = shown }
    }

    suspend fun setPermissionGuideCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.permissionGuideCompleted] = completed }
    }

    suspend fun addCustomMonitoredApp(name: String, packageName: String) {
        val cleanPackage = packageName.trim()
        if (cleanPackage.isBlank()) return
        context.dataStore.edit { prefs ->
            val apps = decodeApps(prefs[Keys.customMonitoredApps])
                .filterNot { it.packageName == cleanPackage } +
                MonitoredApp(packageName = cleanPackage, name = name.trim().ifBlank { cleanPackage }, enabled = true, isDefault = false)
            prefs[Keys.customMonitoredApps] = encodeApps(apps)
            prefs[booleanPreferencesKey(Keys.whitelistPrefix + cleanPackage)] = true
        }
    }

    suspend fun deleteCustomMonitoredApp(packageName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.customMonitoredApps] = encodeApps(decodeApps(prefs[Keys.customMonitoredApps]).filterNot { it.packageName == packageName })
            prefs.remove(booleanPreferencesKey(Keys.whitelistPrefix + packageName))
        }
    }

    suspend fun addCustomCategory(type: TransactionType, primary: String, secondary: String) {
        val cleanPrimary = primary.trim()
        val cleanSecondary = secondary.trim()
        if (cleanPrimary.isBlank() || cleanSecondary.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = decodeCategories(prefs[Keys.customCategories]).toMutableList()
            val index = current.indexOfFirst { it.type == type && it.primary == cleanPrimary }
            if (index >= 0) {
                val existing = current[index]
                current[index] = existing.copy(secondary = (existing.secondary + cleanSecondary).distinct())
            } else {
                current += CategoryGroup(type = type, primary = cleanPrimary, secondary = listOf(cleanSecondary), isDefault = false)
            }
            prefs[Keys.customCategories] = encodeCategories(current)
        }
    }

    suspend fun addAccountOption(name: String): Boolean {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return false
        context.dataStore.edit { prefs ->
            val updated = AccountQuickOptions.addOption(decodeAccountOptions(prefs[Keys.accountQuickOptions]), cleanName)
            prefs[Keys.accountQuickOptions] = encodeAccountOptions(updated)
        }
        return true
    }

    suspend fun deleteCustomCategory(type: TransactionType, primary: String, secondary: String? = null) {
        context.dataStore.edit { prefs ->
            val updated = decodeCategories(prefs[Keys.customCategories]).mapNotNull { group ->
                if (group.type != type || group.primary != primary) {
                    group
                } else if (secondary == null) {
                    null
                } else {
                    group.copy(secondary = group.secondary.filterNot { it == secondary }).takeIf { it.secondary.isNotEmpty() }
                }
            }
            prefs[Keys.customCategories] = encodeCategories(updated)
        }
    }

    suspend fun hideCategory(type: TransactionType, primary: String, secondary: String? = null) {
        context.dataStore.edit { prefs ->
            val updated = decodeHiddenCategories(prefs[Keys.hiddenCategories]) + categoryKey(type, primary, secondary)
            prefs[Keys.hiddenCategories] = encodeHiddenCategories(updated)
        }
    }

    suspend fun restoreCategory(key: String) {
        context.dataStore.edit { prefs ->
            val updated = decodeHiddenCategories(prefs[Keys.hiddenCategories]) - key
            prefs[Keys.hiddenCategories] = encodeHiddenCategories(updated)
        }
    }

    private fun encodeApps(apps: List<MonitoredApp>): String =
        apps.joinToString("\n") { "${it.name.escape()}|${it.packageName.escape()}" }

    private fun decodeApps(raw: String?): List<MonitoredApp> =
        raw.orEmpty().lineSequence().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 2) null else MonitoredApp(
                name = parts[0].unescape(),
                packageName = parts[1].unescape(),
                isDefault = false
            )
        }.toList()

    private fun encodeCategories(groups: List<CategoryGroup>): String =
        groups.joinToString("\n") { group ->
            "${group.type.name}|${group.primary.escape()}|${group.secondary.joinToString(";;") { it.escape() }}"
        }

    private fun decodeCategories(raw: String?): List<CategoryGroup> =
        raw.orEmpty().lineSequence().mapNotNull { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size < 3) {
                null
            } else {
                val type = runCatching { TransactionType.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
                CategoryGroup(
                    type = type,
                    primary = parts[1].unescape(),
                    secondary = parts[2].split(";;").map { it.unescape() }.filter { it.isNotBlank() },
                    isDefault = false
                )
            }
        }.filter { it.primary.isNotBlank() && it.secondary.isNotEmpty() }.toList()

    private fun encodeHiddenCategories(keys: Set<String>): String =
        keys.sorted().joinToString("\n") { it.escape() }

    private fun decodeHiddenCategories(raw: String?): Set<String> =
        raw.orEmpty().lineSequence().map { it.unescape() }.filter { it.isNotBlank() }.toSet()

    private fun encodeAccountOptions(options: List<String>): String =
        options.joinToString("\n") { it.escape() }

    private fun decodeAccountOptions(raw: String?): List<String> =
        raw.orEmpty().lineSequence().map { it.unescape().trim() }.filter { it.isNotBlank() }.toList()

    private data class StoredCustomRange(
        val start: String,
        val end: String,
        val range: DateRange
    )

    private fun decodeRangeType(value: String?): TimeRangeType? =
        runCatching { TimeRangeType.valueOf(value.orEmpty()) }.getOrNull()

    private fun decodeCustomRange(start: String?, end: String?): StoredCustomRange? {
        val cleanStart = start?.trim().orEmpty()
        val cleanEnd = end?.trim().orEmpty()
        if (cleanStart.isBlank() || cleanEnd.isBlank()) return null
        val range = DateRangeCalculator.custom(cleanStart, cleanEnd).range ?: return null
        return StoredCustomRange(cleanStart, cleanEnd, range)
    }

    private fun decodeDateMillis(value: String?): Long? {
        val clean = value?.trim().orEmpty()
        if (clean.isBlank()) return null
        return DateRangeCalculator.parseDateTime(clean, "00:00").millis
    }

    private fun decodeThemeMode(value: String?): ThemeMode =
        supportedTheme(runCatching { ThemeMode.valueOf(value.orEmpty()) }.getOrNull() ?: ThemeMode.YIER)

    private fun supportedTheme(mode: ThemeMode): ThemeMode = when (mode) {
        ThemeMode.BUBU -> ThemeMode.BUBU
        // COUPLE is an old stored value. The current UI supports only YIER and BUBU.
        ThemeMode.YIER, ThemeMode.COUPLE -> ThemeMode.YIER
    }

    private fun validDateOrFallback(value: String?, fallback: String): String {
        val clean = value?.trim().orEmpty()
        return if (decodeDateMillis(clean) != null) clean else fallback
    }

    private fun validCustomDraft(
        start: String?,
        end: String?,
        fallbackStart: String,
        fallbackEnd: String
    ): StoredCustomRange? {
        val cleanStart = validDateOrFallback(start, fallbackStart)
        val cleanEnd = validDateOrFallback(end, fallbackEnd)
        return decodeCustomRange(cleanStart, cleanEnd)
    }

    private fun String.escape(): String = replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace(";;", "\\s")

    private fun String.unescape(): String = replace("\\s", ";;").replace("\\n", "\n").replace("\\p", "|").replace("\\\\", "\\")
}

private fun defaultBudgetNextIncomeDate(now: Long = System.currentTimeMillis()): String {
    val cal = Calendar.getInstance(Locale.CHINA).apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 10)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now) add(Calendar.MONTH, 1)
    }
    return DateRangeCalculator.dateString(cal.timeInMillis)
}

fun categoryKey(type: TransactionType, primary: String, secondary: String? = null): String =
    listOf(type.name, primary, secondary.orEmpty()).joinToString("|")

object DefaultAppWhitelist {
    val packageNames = mapOf(
        "com.tencent.mm" to "微信",
        "com.eg.android.AlipayGphone" to "支付宝",
        "com.unionpay" to "云闪付",
        "com.taobao.taobao" to "淘宝",
        "com.jingdong.app.mall" to "京东",
        "com.xunmeng.pinduoduo" to "拼多多",
        "com.sankuai.meituan" to "美团",
        "me.ele" to "饿了么",
        "cn.com.boc" to "中国银行",
        "com.cmbchina.ccd.pluto.cmbActivity" to "招商银行",
        "com.chinamworld.main" to "中国建设银行",
        "com.icbc" to "中国工商银行",
        "com.android.bankabc" to "中国农业银行",
        "com.bankcomm.Bankcomm" to "交通银行"
    )
    val packages: Set<String> = packageNames.keys
}
