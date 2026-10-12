package com.autoledgercn.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autoledgercn.data.app.InstalledAppRepository
import com.autoledgercn.data.app.LaunchableApp
import com.autoledgercn.data.entity.NotificationDiagnosticEntity
import com.autoledgercn.data.entity.PendingTransactionEntity
import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.data.preferences.BudgetSettingsPreference
import com.autoledgercn.data.preferences.DefaultAppWhitelist
import com.autoledgercn.data.preferences.UserPreference
import com.autoledgercn.data.preferences.UserPreferencesRepository
import com.autoledgercn.data.repository.CsvTransactionDraft
import com.autoledgercn.data.repository.ImportTransactionsResult
import com.autoledgercn.data.repository.LedgerRepository
import com.autoledgercn.domain.category.CategoryCatalog
import com.autoledgercn.domain.category.CategoryGroup
import com.autoledgercn.domain.model.AccountQuickOptions
import com.autoledgercn.domain.model.ThemeMode
import com.autoledgercn.domain.model.TransactionType
import com.autoledgercn.domain.stats.StatsRangeDefaults
import com.autoledgercn.domain.time.DateRange
import com.autoledgercn.domain.time.DateRangeCalculator
import com.autoledgercn.domain.time.TimeRangeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class LedgerUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val pending: List<PendingTransactionEntity> = emptyList(),
    val preferences: UserPreference = UserPreference(),
    val whitelist: Map<String, Boolean> = emptyMap()
)

data class StatsUiState(
    val selectedRange: TimeRangeType,
    val selectedDate: Long,
    val customStart: String,
    val customEnd: String,
    val customRange: DateRange?,
    val customError: String? = null
)

data class HomeUiState(
    val selectedRange: TimeRangeType,
    val customStart: String,
    val customEnd: String,
    val customRange: DateRange?,
    val customError: String? = null
)

private fun initialHomeUiState(): HomeUiState {
    val now = System.currentTimeMillis()
    val customStart = DateRangeCalculator.dateString(DateRangeCalculator.month(now).startMillis)
    val customEnd = DateRangeCalculator.dateString(now)
    val customRange = DateRangeCalculator.custom(customStart, customEnd).range
    return HomeUiState(
        selectedRange = TimeRangeType.MONTH,
        customStart = customStart,
        customEnd = customEnd,
        customRange = customRange
    )
}

private fun initialStatsUiState(): StatsUiState {
    val defaults = StatsRangeDefaults.initial()
    return StatsUiState(
        selectedRange = defaults.selectedRange,
        selectedDate = System.currentTimeMillis(),
        customStart = defaults.customStart,
        customEnd = defaults.customEnd,
        customRange = defaults.customRange
    )
}

class LedgerViewModel(
    private val repository: LedgerRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val installedAppRepository: InstalledAppRepository
) : ViewModel() {
    val transactions = repository.observeTransactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val pending = repository.observePending().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rules = repository.observeCategoryRules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notificationDiagnostics = repository.observeNotificationDiagnostics(30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<NotificationDiagnosticEntity>())
    val preferences = preferencesRepository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreference())
    val whitelist = preferencesRepository.whitelistFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val monitoredApps = preferencesRepository.monitoredAppsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val customCategories = preferencesRepository.customCategoriesFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val hiddenCategories = preferencesRepository.hiddenCategoriesFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val accountOptions = preferencesRepository.accountOptionsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountQuickOptions.defaults)
    val budgetSettings = preferencesRepository.budgetSettingsFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetSettingsPreference())
    private val _installedApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val installedApps = _installedApps.asStateFlow()
    private val _refreshTick = MutableStateFlow(0L)
    val refreshTick = _refreshTick.asStateFlow()
    private val _homeUiState = MutableStateFlow(initialHomeUiState())
    val homeUiState = _homeUiState.asStateFlow()
    private val _statsUiState = MutableStateFlow(initialStatsUiState())
    val statsUiState = _statsUiState.asStateFlow()

    init {
        reloadInstalledApps()
        viewModelScope.launch {
            val saved = preferencesRepository.homeRangeFlow().first()
            _homeUiState.value = _homeUiState.value.copy(
                selectedRange = saved.selectedRange,
                customStart = saved.customStart,
                customEnd = saved.customEnd,
                customRange = saved.customRange,
                customError = null
            )
        }
        viewModelScope.launch {
            val saved = preferencesRepository.statsRangeFlow().first()
            _statsUiState.value = _statsUiState.value.copy(
                selectedRange = saved.selectedRange,
                selectedDate = saved.selectedDate,
                customStart = saved.customStart,
                customEnd = saved.customEnd,
                customRange = saved.customRange,
                customError = null
            )
        }
    }

    fun finishOnboarding(mode: ThemeMode) = viewModelScope.launch {
        preferencesRepository.finishOnboardingWithTheme(mode)
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    fun setWhitelist(packageName: String, enabled: Boolean) = viewModelScope.launch { preferencesRepository.setWhitelist(packageName, enabled) }
    fun setBootKeepAlive(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setBootKeepAlive(enabled) }
    fun setBootKeepAliveGuideShown(shown: Boolean) = viewModelScope.launch { preferencesRepository.setBootKeepAliveGuideShown(shown) }
    fun setPermissionGuideShown(shown: Boolean) = viewModelScope.launch { preferencesRepository.setPermissionGuideShown(shown) }
    fun setPermissionGuideCompleted(completed: Boolean) = viewModelScope.launch { preferencesRepository.setPermissionGuideCompleted(completed) }
    fun addCustomMonitoredApp(name: String, packageName: String) =
        viewModelScope.launch { preferencesRepository.addCustomMonitoredApp(name, packageName) }
    fun deleteCustomMonitoredApp(packageName: String) =
        viewModelScope.launch { preferencesRepository.deleteCustomMonitoredApp(packageName) }
    fun addCustomCategory(type: TransactionType, primary: String, secondary: String) =
        viewModelScope.launch { preferencesRepository.addCustomCategory(type, primary, secondary) }
    fun addAccountOption(name: String) =
        viewModelScope.launch { preferencesRepository.addAccountOption(name) }
    fun deleteCustomCategory(type: TransactionType, primary: String, secondary: String? = null) =
        viewModelScope.launch { preferencesRepository.deleteCustomCategory(type, primary, secondary) }
    fun hideCategory(type: TransactionType, primary: String, secondary: String? = null) =
        viewModelScope.launch { preferencesRepository.hideCategory(type, primary, secondary) }
    fun restoreCategory(key: String) =
        viewModelScope.launch { preferencesRepository.restoreCategory(key) }
    fun reloadInstalledApps() = viewModelScope.launch(Dispatchers.IO) {
        _installedApps.value = installedAppRepository.loadInstalledApps()
    }

    fun refreshAll() {
        _refreshTick.value = System.currentTimeMillis()
        reloadInstalledApps()
    }
    fun setHomeRange(range: TimeRangeType) {
        _homeUiState.value = _homeUiState.value.copy(selectedRange = range, customError = null)
        viewModelScope.launch { preferencesRepository.setHomeRangeType(range) }
    }
    fun updateHomeRange(range: TimeRangeType) = setHomeRange(range)
    fun setHomeCustomStart(value: String) {
        _homeUiState.value = _homeUiState.value.copy(customStart = value)
        viewModelScope.launch { preferencesRepository.setHomeCustomStartDate(value) }
    }
    fun setHomeCustomEnd(value: String) {
        _homeUiState.value = _homeUiState.value.copy(customEnd = value)
        viewModelScope.launch { preferencesRepository.setHomeCustomEndDate(value) }
    }
    fun setHomeCustomRange(range: DateRange?, error: String?) {
        val current = _homeUiState.value
        _homeUiState.value = current.copy(customRange = range ?: current.customRange, customError = error)
        if (range != null) {
            viewModelScope.launch { preferencesRepository.applyHomeCustomRange(current.customStart, current.customEnd) }
        }
    }
    fun updateHomeCustomRange(range: DateRange?, error: String?) = setHomeCustomRange(range, error)
    fun setStatsRange(range: TimeRangeType) {
        _statsUiState.value = _statsUiState.value.copy(selectedRange = range, customError = null)
        viewModelScope.launch { preferencesRepository.setStatsRangeType(range) }
    }
    fun updateStatsRange(range: TimeRangeType) = setStatsRange(range)
    fun setStatsSelectedDate(date: Long) {
        _statsUiState.value = _statsUiState.value.copy(selectedDate = date)
        viewModelScope.launch { preferencesRepository.setStatsSelectedDate(date) }
    }
    fun updateStatsSelectedDate(date: Long) = setStatsSelectedDate(date)
    fun setStatsCustomStart(value: String) {
        _statsUiState.value = _statsUiState.value.copy(customStart = value)
        viewModelScope.launch { preferencesRepository.setStatsCustomStartDate(value) }
    }
    fun setStatsCustomEnd(value: String) {
        _statsUiState.value = _statsUiState.value.copy(customEnd = value)
        viewModelScope.launch { preferencesRepository.setStatsCustomEndDate(value) }
    }
    fun setStatsCustomRange(range: DateRange?, error: String?) {
        val current = _statsUiState.value
        _statsUiState.value = current.copy(customRange = range ?: current.customRange, customError = error)
        if (range != null) {
            viewModelScope.launch { preferencesRepository.applyStatsCustomRange(current.customStart, current.customEnd) }
        }
    }
    fun updateStatsCustomRange(range: DateRange?, error: String?) = setStatsCustomRange(range, error)
    fun setInstalledAppMonitored(app: LaunchableApp, enabled: Boolean) = viewModelScope.launch {
        if (enabled && app.packageName !in DefaultAppWhitelist.packages) {
            preferencesRepository.addCustomMonitoredApp(app.appName, app.packageName)
        }
        preferencesRepository.setWhitelist(app.packageName, enabled)
    }
    fun categoryGroups(type: TransactionType, custom: List<CategoryGroup>): List<CategoryGroup> =
        CategoryCatalog.groupsFor(type, custom)
    fun filterInstalledApps(apps: List<LaunchableApp>, query: String): List<LaunchableApp> =
        InstalledAppRepository.filter(apps, query)

    fun addManual(amount: Double, type: TransactionType, primary: String, secondary: String, merchant: String, account: String, note: String, transactionTime: Long) =
        viewModelScope.launch { repository.addManualTransaction(amount, type, primary, secondary, merchant, account, note, transactionTime) }

    fun updateTransaction(entity: TransactionEntity) = viewModelScope.launch { repository.updateTransaction(entity) }
    fun deleteTransaction(id: Long) = viewModelScope.launch { repository.deleteTransaction(id) }
    fun confirmPendingAsNew(pending: PendingTransactionEntity) = viewModelScope.launch { repository.confirmPendingAsNew(pending) }
    fun completePending(pending: PendingTransactionEntity, amount: Double, type: TransactionType, primary: String, secondary: String, merchant: String, account: String, note: String, transactionTime: Long) =
        viewModelScope.launch { repository.completePending(pending, amount, type, primary, secondary, merchant, account, note, transactionTime) }
    fun mergePending(pending: PendingTransactionEntity) = viewModelScope.launch { repository.mergePending(pending) }
    fun deletePending(id: Long) = viewModelScope.launch { repository.deletePending(id) }
    fun clearNotificationDiagnostics() = viewModelScope.launch { repository.clearNotificationDiagnostics() }
    fun addRule(keyword: String, primary: String, secondary: String, type: TransactionType) =
        viewModelScope.launch { repository.addCategoryRule(keyword, primary, secondary, type) }
    fun deleteRule(id: Long) = viewModelScope.launch { repository.deleteCategoryRule(id) }

    fun saveBudgetSettings(nextIncomeDate: String, safeBalance: Double, essentialCategories: String) =
        viewModelScope.launch { preferencesRepository.saveBudgetSettings(nextIncomeDate, safeBalance, essentialCategories) }

    suspend fun importCsvTransactions(rows: List<CsvTransactionDraft>): ImportTransactionsResult =
        repository.importTransactions(rows)
}

class LedgerViewModelFactory(
    private val repository: LedgerRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val installedAppRepository: InstalledAppRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LedgerViewModel(repository, preferencesRepository, installedAppRepository) as T
}
