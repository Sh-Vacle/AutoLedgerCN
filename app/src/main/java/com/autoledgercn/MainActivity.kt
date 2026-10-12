package com.autoledgercn

import android.Manifest
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.autoledgercn.data.app.AppInitialHelper
import com.autoledgercn.data.app.InstalledAppRepository
import com.autoledgercn.data.app.LaunchableApp
import com.autoledgercn.data.entity.CategoryRuleEntity
import com.autoledgercn.data.entity.NotificationDiagnosticEntity
import com.autoledgercn.data.entity.PendingTransactionEntity
import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.data.preferences.BudgetSettingsPreference
import com.autoledgercn.data.preferences.DefaultAppWhitelist
import com.autoledgercn.data.preferences.MonitoredApp
import com.autoledgercn.data.preferences.categoryKey
import com.autoledgercn.data.repository.CsvTransactionDraft
import com.autoledgercn.domain.category.CategoryCatalog
import com.autoledgercn.domain.category.CategoryGroup
import com.autoledgercn.domain.model.ThemeMode
import com.autoledgercn.domain.model.TransactionType
import com.autoledgercn.domain.stats.StatsRangeDefaults
import com.autoledgercn.domain.time.DateRange
import com.autoledgercn.domain.time.DateRangeCalculator
import com.autoledgercn.domain.time.TimeRangeType
import com.autoledgercn.permission.PermissionStatus
import com.autoledgercn.permission.PermissionGuideState
import com.autoledgercn.permission.PermissionStatusReader
import com.autoledgercn.ui.components.MascotEmptyState
import com.autoledgercn.ui.components.MascotEmptyType
import com.autoledgercn.ui.components.MascotHeader
import com.autoledgercn.ui.components.MascotNavIcon
import com.autoledgercn.ui.components.MascotNavItem
import com.autoledgercn.ui.components.MascotOnboardingTopImage
import com.autoledgercn.ui.components.MascotPage
import com.autoledgercn.ui.components.MascotThemeCardImage
import com.autoledgercn.ui.screen.LedgerViewModel
import com.autoledgercn.ui.screen.LedgerViewModelFactory
import com.autoledgercn.ui.screen.HomeUiState
import com.autoledgercn.ui.screen.StatsUiState
import com.autoledgercn.ui.theme.AutoLedgerTheme
import com.autoledgercn.ui.theme.paletteFor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AutoLedgerApp
        setContent {
            val vm: LedgerViewModel = viewModel(
                factory = LedgerViewModelFactory(
                    app.container.ledgerRepository,
                    app.container.preferencesRepository,
                    app.container.installedAppRepository
                )
            )
            val prefs by vm.preferences.collectAsState()
            AutoLedgerTheme(prefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        !prefs.preferencesLoaded -> StartupLoadingScreen()
                        prefs.onboardingFinished -> MainShell(vm = vm, mode = prefs.themeMode)
                        else -> OnboardingScreen(vm = vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("正在加载设置...", style = MaterialTheme.typography.bodyMedium)
    }
}

private data class Tab(val route: String, val label: String, val navItem: MascotNavItem)

private data class BudgetAnalysisSettings(
    val nextIncomeDate: String = defaultBudgetNextIncomeDate(),
    val minimumSafeBalance: Double = 300.0
)

private data class BudgetAnalysis(
    val hasEnoughData: Boolean,
    val riskLevel: String,
    val daysUntilIncome: Int,
    val recent7DailyExpense: Double,
    val recent30DailyExpense: Double,
    val expectedExpenseBeforeIncome: Double,
    val currentBalance: Double,
    val recommendedReserve: Double,
    val monthExpenseSpeed: String,
    val topExpenseCategory: String,
    val recentTrend: String,
    val anomalyCategory: String,
    val expenseRecordCount: Int
)

private data class WantPurchaseAdvice(
    val conclusion: String,
    val reason: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(vm: LedgerViewModel, mode: ThemeMode) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val transactions by vm.transactions.collectAsState()
    val pending by vm.pending.collectAsState()
    val rules by vm.rules.collectAsState()
    val notificationDiagnostics by vm.notificationDiagnostics.collectAsState()
    val preferences by vm.preferences.collectAsState()
    val monitoredApps by vm.monitoredApps.collectAsState()
    val installedApps by vm.installedApps.collectAsState()
    val customCategories by vm.customCategories.collectAsState()
    val hiddenCategories by vm.hiddenCategories.collectAsState()
    val accountOptions by vm.accountOptions.collectAsState()
    val homeUiState by vm.homeUiState.collectAsState()
    val statsUiState by vm.statsUiState.collectAsState()
    val budgetSettings by vm.budgetSettings.collectAsState()
    val categories = CategoryCatalog.visible(customCategories, hiddenCategories)
    var showManual by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    var showPermissionGuide by remember { mutableStateOf(false) }
    var showBootKeepAliveGuide by remember { mutableStateOf(false) }
    var autoGuideConsumed by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importResultMessage by remember { mutableStateOf<String?>(null) }
    var importInProgress by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var permissionStatus by remember {
        mutableStateOf(PermissionStatusReader.read(context, preferences.bootKeepAliveEnabled))
    }

    fun refreshPermissionStatus() {
        permissionStatus = PermissionStatusReader.read(context, preferences.bootKeepAliveEnabled)
        vm.setPermissionGuideCompleted(permissionStatus.allReady)
    }

    fun refreshAllWithFeedback() {
        refreshPermissionStatus()
        vm.refreshAll()
        Toast.makeText(context, "已刷新", Toast.LENGTH_SHORT).show()
    }

    fun requestBootKeepAlive(enabled: Boolean) {
        vm.setBootKeepAlive(enabled)
        if (enabled) {
            vm.setBootKeepAliveGuideShown(true)
            showBootKeepAliveGuide = true
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshPermissionStatus()
    }
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val exported = runCatching { writeTransactionsCsv(context, uri, transactions) }
            Toast.makeText(
                context,
                if (exported.isSuccess) "CSV 导出成功" else "CSV 导出失败",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    DisposableEffect(lifecycleOwner, preferences.bootKeepAliveEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissionStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(preferences.onboardingFinished, preferences.permissionGuideShown) {
        if (PermissionGuideState.shouldAutoShow(preferences.onboardingFinished, preferences.permissionGuideShown) && !autoGuideConsumed) {
            autoGuideConsumed = true
            showPermissionGuide = true
            vm.setPermissionGuideShown(true)
        }
    }

    LaunchedEffect(preferences.bootKeepAliveEnabled) {
        refreshPermissionStatus()
    }

    val tabs = listOf(
        Tab("dashboard", "首页", MascotNavItem.HOME),
        Tab("transactions", "流水", MascotNavItem.RECORDS),
        Tab("stats", "统计", MascotNavItem.STATS),
        Tab("pending", "待处理", MascotNavItem.PENDING),
        Tab("settings", "设置", MascotNavItem.SETTINGS)
    )
    val entry by nav.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route ?: "dashboard"
    val showPrimaryChrome = currentRoute != "budgetAnalysis"

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.app_name)) }) },
        bottomBar = {
            if (showPrimaryChrome) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.navigate(tab.route) { launchSingleTop = true } },
                            icon = { MascotNavIcon(item = tab.navItem, mode = mode, selected = currentRoute == tab.route) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showPrimaryChrome) {
                FloatingActionButton(onClick = { showManual = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "新增")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        NavHost(navController = nav, startDestination = "dashboard", modifier = Modifier.fillMaxSize()) {
            composable("dashboard") {
                DashboardScreen(
                    transactions = transactions,
                    pending = pending,
                    homeUiState = homeUiState,
                    mode = mode,
                    permissionStatus = permissionStatus,
                    onRangeChange = { vm.updateHomeRange(it) },
                    onCustomStartChange = { vm.setHomeCustomStart(it) },
                    onCustomEndChange = { vm.setHomeCustomEnd(it) },
                    onCustomRangeApply = { range, error -> vm.updateHomeCustomRange(range, error) },
                    onContinuePermissions = { showPermissionGuide = true },
                    onPendingClick = { nav.navigate("pending") },
                    onBudgetAnalysisClick = { nav.navigate("budgetAnalysis") },
                    onRefresh = { refreshAllWithFeedback() }
                )
            }
            composable("budgetAnalysis") {
                BudgetAnalysisDetailScreen(
                    transactions = transactions,
                    mode = mode,
                    categories = categories,
                    budgetSettings = budgetSettings,
                    onSaveBudgetSettings = { nextIncomeDate, safeBalance, essentialCategories ->
                        vm.saveBudgetSettings(nextIncomeDate, safeBalance, essentialCategories)
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("transactions") {
                TransactionsScreen(transactions, mode, onEdit = { editing = it }, onDelete = { vm.deleteTransaction(it) }, onRefresh = { refreshAllWithFeedback() })
            }
            composable("stats") {
                StatsScreen(
                    transactions = transactions,
                    mode = mode,
                    statsUiState = statsUiState,
                    onRangeChange = { vm.updateStatsRange(it) },
                    onDateChange = { vm.updateStatsSelectedDate(it) },
                    onCustomStartChange = { vm.setStatsCustomStart(it) },
                    onCustomEndChange = { vm.setStatsCustomEnd(it) },
                    onCustomRangeApply = { range, error -> vm.updateStatsCustomRange(range, error) },
                    onRefresh = { refreshAllWithFeedback() }
                )
            }
            composable("pending") {
                PendingScreen(
                    pending = pending,
                    mode = mode,
                    categories = categories,
                    accountOptions = accountOptions,
                    onAddAccountOption = { vm.addAccountOption(it) },
                    onComplete = { p, amount, type, primary, secondary, merchant, account, note, transactionTime ->
                        vm.completePending(p, amount, type, primary, secondary, merchant, account, note, transactionTime)
                    },
                    onNew = { vm.confirmPendingAsNew(it) },
                    onMerge = { vm.mergePending(it) },
                    onDelete = { vm.deletePending(it) },
                    onRefresh = { refreshAllWithFeedback() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    mode = mode,
                    permissionStatus = permissionStatus,
                    rules = rules,
                    monitoredApps = monitoredApps,
                    installedApps = installedApps,
                    customCategories = customCategories,
                    hiddenCategories = hiddenCategories,
                    visibleCategories = categories,
                    notificationDiagnostics = notificationDiagnostics,
                    onTheme = { vm.setTheme(it) },
                    onBoot = { requestBootKeepAlive(it) },
                    onOpenPermissionGuide = { showPermissionGuide = true },
                    onOpenDiagnostics = { nav.navigate("notificationDiagnostics") },
                    onOpenNotificationListener = { openNotificationListenerSettings(context) },
                    onRequestPostNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenAppSettings = { openAppSettings(context) },
                    onOpenBatterySettings = { openBatterySettings(context) },
                    onWhitelist = { packageName, enabled -> vm.setWhitelist(packageName, enabled) },
                    onToggleInstalledApp = { app, enabled -> vm.setInstalledAppMonitored(app, enabled) },
                    onAddApp = { name, packageName -> vm.addCustomMonitoredApp(name, packageName) },
                    onDeleteApp = { vm.deleteCustomMonitoredApp(it) },
                    onReloadApps = { vm.reloadInstalledApps() },
                    onAddCategory = { type, primary, secondary -> vm.addCustomCategory(type, primary, secondary) },
                    onDeleteCategory = { type, primary, secondary -> vm.deleteCustomCategory(type, primary, secondary) },
                    onHideCategory = { type, primary, secondary -> vm.hideCategory(type, primary, secondary) },
                    onRestoreCategory = { vm.restoreCategory(it) },
                    onAddRule = { keyword, primary, secondary, type -> vm.addRule(keyword, primary, secondary, type) },
                    onDeleteRule = { vm.deleteRule(it) },
                    onExportCsv = {
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.CHINA).format(Date())
                        csvExportLauncher.launch("${context.getString(R.string.app_name)}-$stamp.csv")
                    },
                    onImportCsv = {
                        csvImportLauncher.launch(
                            arrayOf(
                                "text/csv",
                                "text/comma-separated-values",
                                "text/plain",
                                "application/octet-stream"
                            )
                        )
                    },
                    importResultMessage = importResultMessage,
                    importInProgress = importInProgress,
                    onRefresh = { refreshAllWithFeedback() }
                )
            }
            composable("notificationDiagnostics") {
                NotificationDiagnosticsScreen(
                    diagnostics = notificationDiagnostics,
                    mode = mode,
                    onRefresh = { refreshAllWithFeedback() },
                    onClear = { vm.clearNotificationDiagnostics() },
                    onBack = { nav.popBackStack() }
                )
            }
        }
        }
    }

    if (showPermissionGuide) {
        PermissionGuideDialog(
            status = permissionStatus,
            onDismiss = {
                showPermissionGuide = false
                refreshPermissionStatus()
            },
            onOpenNotificationListener = { openNotificationListenerSettings(context) },
            onRequestPostNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onBootChange = { requestBootKeepAlive(it) },
            onOpenAppSettings = { openAppSettings(context) },
            onOpenBatterySettings = { openBatterySettings(context) },
            onRefresh = { refreshPermissionStatus() }
        )
    }

    if (showBootKeepAliveGuide) {
        BootKeepAliveGuideDialog(
            onDismiss = { showBootKeepAliveGuide = false },
            onOpenSettings = {
                showBootKeepAliveGuide = false
                openAppSettings(context)
            }
        )
    }

    pendingImportUri?.let { importUri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("确认导入") },
            text = { Text("将从 CSV 导入流水，已存在的疑似重复记录会自动跳过。") },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("取消") }
            },
            confirmButton = {
                Button(
                    enabled = !importInProgress,
                    onClick = {
                        pendingImportUri = null
                        importInProgress = true
                        coroutineScope.launch {
                            val message = runCatching {
                                withContext(Dispatchers.IO) {
                                    val parsed = readTransactionsCsv(context, importUri)
                                    val imported = vm.importCsvTransactions(parsed.rows)
                                    csvImportMessage(
                                        imported = imported.importedCount,
                                        skipped = imported.skippedDuplicateCount,
                                        failed = parsed.failedCount,
                                        reasons = parsed.failureReasons
                                    )
                                }
                            }.getOrElse { error ->
                                "导入失败：${error.message ?: "CSV 解析失败"}"
                            }
                            importResultMessage = message
                            importInProgress = false
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                ) { Text("开始导入") }
            }
        )
    }

    if (showManual) {
        ManualTransactionDialog(
            initial = null,
            categories = categories,
            accountOptions = accountOptions,
            onAddAccountOption = { vm.addAccountOption(it) },
            onDismiss = { showManual = false },
            onSave = { amount, type, primary, secondary, merchant, account, note, transactionTime ->
                vm.addManual(amount, type, primary, secondary, merchant, account, note, transactionTime)
                showManual = false
            }
        )
    }

    editing?.let { tx ->
        ManualTransactionDialog(
            initial = tx,
            categories = categories,
            accountOptions = accountOptions,
            onAddAccountOption = { vm.addAccountOption(it) },
            onDismiss = { editing = null },
            onSave = { amount, type, primary, secondary, merchant, account, note, transactionTime ->
                vm.updateTransaction(
                    tx.copy(amount = amount, type = type, categoryPrimary = primary, categorySecondary = secondary, merchant = merchant, account = account, note = note, transactionTime = transactionTime)
                )
                editing = null
            }
        )
    }
}

@Composable
private fun OnboardingScreen(vm: LedgerViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MascotPairPreview(modifier = Modifier.height(108.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "选择你的记账伙伴", style = MaterialTheme.typography.headlineMedium)
        Text(text = "主题之后可以随时在设置里切换。", textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
        ThemePartnerButton(
            title = "清浅主题",
            description = "清爽温柔，白色系",
            optionMode = ThemeMode.YIER,
            selected = false,
            onClick = { vm.finishOnboarding(ThemeMode.YIER) }
        )
        Spacer(modifier = Modifier.height(10.dp))
        ThemePartnerButton(
            title = "暖色主题",
            description = "暖萌陪伴，棕色系",
            optionMode = ThemeMode.BUBU,
            selected = false,
            onClick = { vm.finishOnboarding(ThemeMode.BUBU) }
        )
    }
}

@Composable
private fun DashboardScreen(
    transactions: List<TransactionEntity>,
    pending: List<PendingTransactionEntity>,
    homeUiState: HomeUiState,
    mode: ThemeMode,
    permissionStatus: PermissionStatus,
    onRangeChange: (TimeRangeType) -> Unit,
    onCustomStartChange: (String) -> Unit,
    onCustomEndChange: (String) -> Unit,
    onCustomRangeApply: (DateRange?, String?) -> Unit,
    onContinuePermissions: () -> Unit,
    onPendingClick: () -> Unit,
    onBudgetAnalysisClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val now = System.currentTimeMillis()
    val selectedRange = homeUiState.selectedRange
    val customStart = homeUiState.customStart
    val customEnd = homeUiState.customEnd
    val customRange = homeUiState.customRange
    val customError = homeUiState.customError
    val activeRange = DateRangeCalculator.range(selectedRange, now, customRange)
    val currentRows = DateRangeCalculator.filter(transactions, activeRange)
    val today = DateRangeCalculator.filter(transactions, DateRangeCalculator.today(now))
    val week = DateRangeCalculator.filter(transactions, DateRangeCalculator.week(now))
    val month = DateRangeCalculator.filter(transactions, DateRangeCalculator.month(now))
    val budgetAnalysis = remember(transactions) { calculateBudgetAnalysis(transactions) }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            MascotHeader(
                title = "今日账本",
                subtitle = "看看今天的收支情况",
                mode = mode,
                page = MascotPage.HOME,
                action = { OutlinedButton(onClick = onRefresh) { Text("刷新") } }
            )
        }
        if (!permissionStatus.allReady) {
            item {
                WarmPanel {
                    Text("自动记账权限未完全开启", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("通知读取、通知提醒和后台运行设置会影响自动识别稳定性。")
                    Button(onClick = onContinuePermissions, modifier = Modifier.fillMaxWidth()) { Text("继续设置") }
                }
            }
        }
        item {
            WarmPanel {
                Text("当前区间", style = MaterialTheme.typography.titleLarge)
                ButtonRow(TimeRangeType.values().map { it.label }, selectedRange.label) { label ->
                    onRangeChange(TimeRangeType.values().first { it.label == label })
                }
                if (selectedRange == TimeRangeType.CUSTOM) {
                    DateInputField(value = customStart, label = "开始日期", onValueChange = onCustomStartChange)
                    DateInputField(value = customEnd, label = "结束日期", onValueChange = onCustomEndChange)
                    Button(
                        onClick = {
                            val result = DateRangeCalculator.custom(customStart, customEnd)
                            onCustomRangeApply(result.range, result.error)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("应用") }
                    customError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                val incomeLabel = if (selectedRange == TimeRangeType.ALL) "全部收入" else "区间收入"
                val expenseLabel = if (selectedRange == TimeRangeType.ALL) "全部支出" else "区间支出"
                val balanceLabel = if (selectedRange == TimeRangeType.ALL) "累计结余" else "区间结余"
                MetricGrid(
                    listOf(
                        incomeLabel to money(currentRows.sumIncome()),
                        expenseLabel to money(currentRows.sumExpense()),
                        balanceLabel to money(currentRows.sumIncome() - currentRows.sumExpense())
                    )
                )
            }
        }
        item {
            WarmPanel {
                Text("快速概览", style = MaterialTheme.typography.titleLarge)
                OverviewRow("今日", today)
                Divider()
                OverviewRow("本周", week)
                Divider()
                OverviewRow("本月", month)
            }
        }
        item {
            BudgetAnalysisCard(analysis = budgetAnalysis, onClick = onBudgetAnalysisClick)
        }
        item {
            WarmPanel {
                Text("待处理入口", style = MaterialTheme.typography.titleLarge)
                Text("待处理数量：${pending.size}")
                if (pending.isEmpty()) {
                    Text("暂无待处理")
                } else {
                    Button(onClick = onPendingClick, modifier = Modifier.fillMaxWidth()) { Text("去处理") }
                }
            }
        }
        if (transactions.isEmpty() && pending.isEmpty()) {
            item {
                MascotEmptyState(
                    title = "暂无流水",
                    subtitle = "记录第一笔收支吧",
                    mode = mode,
                    type = MascotEmptyType.HOME
                )
            }
        }
    }
}

@Composable
private fun BudgetAnalysisCard(
    analysis: BudgetAnalysis,
    onClick: () -> Unit
) {
    WarmPanel {
        Text("智能预算分析", style = MaterialTheme.typography.titleLarge)
        Text("根据历史收支习惯，预测后续消费压力")
        if (!analysis.hasEnoughData) {
            Text("数据不足，继续记录几笔后可生成更准确分析。")
        } else {
            MetricGrid(
                listOf(
                    "风险等级" to analysis.riskLevel,
                    "预计到账前还需支出" to money(analysis.expectedExpenseBeforeIncome),
                    "建议保留余额" to money(analysis.recommendedReserve)
                )
            )
        }
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("查看分析") }
    }
}

@Composable
private fun BudgetAnalysisDetailScreen(
    transactions: List<TransactionEntity>,
    mode: ThemeMode,
    categories: List<CategoryGroup>,
    budgetSettings: BudgetSettingsPreference,
    onSaveBudgetSettings: (String, Double, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var nextIncomeDateText by remember { mutableStateOf(budgetSettings.nextIncomeDate) }
    var minimumSafeBalanceText by remember { mutableStateOf("%.2f".format(budgetSettings.safeBalance)) }
    var budgetError by remember { mutableStateOf<String?>(null) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    val expenseCategories = remember(categories) {
        categories.filter { it.type == TransactionType.EXPENSE }.map { it.primary }.distinct()
    }
    var productCategory by remember { mutableStateOf(expenseCategories.firstOrNull() ?: "其他") }
    var wantAdvice by remember { mutableStateOf<WantPurchaseAdvice?>(null) }
    LaunchedEffect(budgetSettings) {
        nextIncomeDateText = budgetSettings.nextIncomeDate
        minimumSafeBalanceText = "%.2f".format(budgetSettings.safeBalance)
        budgetError = null
    }
    LaunchedEffect(expenseCategories) {
        if (expenseCategories.isNotEmpty() && productCategory !in expenseCategories) {
            productCategory = expenseCategories.first()
        }
    }
    val analyzedIncomeDate = if (DateRangeCalculator.parseDateTime(nextIncomeDateText, "00:00").millis != null) {
        nextIncomeDateText
    } else {
        budgetSettings.nextIncomeDate
    }
    val minimumSafeBalance = minimumSafeBalanceText.toDoubleOrNull()?.takeIf { it >= 0.0 } ?: budgetSettings.safeBalance
    val settings = BudgetAnalysisSettings(
        nextIncomeDate = analyzedIncomeDate,
        minimumSafeBalance = minimumSafeBalance
    )
    val analysis = remember(transactions, analyzedIncomeDate, minimumSafeBalance) {
        calculateBudgetAnalysis(transactions, settings)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            MascotHeader(
                title = "智能预算分析",
                subtitle = "根据历史流水，预测未来一段时间的消费压力。",
                mode = mode,
                page = MascotPage.STATS,
                action = { OutlinedButton(onClick = onBack) { Text("返回") } }
            )
        }
        item {
            WarmPanel {
                Text("预算分析设置", style = MaterialTheme.typography.titleLarge)
                DateInputField(
                    value = nextIncomeDateText,
                    label = "下次收入日期",
                    onValueChange = {
                        nextIncomeDateText = it
                        budgetError = null
                        saveFeedback = null
                    }
                )
                OutlinedTextField(
                    value = minimumSafeBalanceText,
                    onValueChange = {
                        minimumSafeBalanceText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12)
                        budgetError = null
                        saveFeedback = null
                    },
                    label = { Text("最低安全余额（元）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("必要支出分类：${budgetSettings.essentialCategories}")
                budgetError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                saveFeedback?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                Button(
                    onClick = {
                        val parsedDate = DateRangeCalculator.parseDateTime(nextIncomeDateText, "00:00")
                        val safeBalance = minimumSafeBalanceText.toDoubleOrNull()
                        when {
                            parsedDate.millis == null -> budgetError = parsedDate.error ?: "请选择正确的下次收入日期"
                            safeBalance == null -> budgetError = "请输入正确的最低安全余额"
                            safeBalance < 0.0 -> budgetError = "最低安全余额不能为负数"
                            else -> {
                                onSaveBudgetSettings(nextIncomeDateText, safeBalance, budgetSettings.essentialCategories)
                                budgetError = null
                                saveFeedback = "预算设置已保存"
                                Toast.makeText(context, "预算设置已保存", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存设置") }
            }
        }
        item {
            WarmPanel {
                Text("到账前预测", style = MaterialTheme.typography.titleLarge)
                MetricGrid(
                    listOf(
                        "距离下次收入还有" to "${analysis.daysUntilIncome} 天",
                        "最近 7 天日均支出" to money(analysis.recent7DailyExpense),
                        "最近 30 天日均支出" to money(analysis.recent30DailyExpense),
                        "预计到账前还需支出" to money(analysis.expectedExpenseBeforeIncome),
                        "当前结余" to money(analysis.currentBalance),
                        "建议保留余额" to money(analysis.recommendedReserve),
                        "风险等级" to analysis.riskLevel
                    )
                )
                if (!analysis.hasEnoughData) {
                    Text("数据不足，继续记录几笔后可生成更准确分析。")
                }
            }
        }
        item {
            WarmPanel {
                Text("消费习惯分析", style = MaterialTheme.typography.titleLarge)
                MetricGrid(
                    listOf(
                        "本月支出速度" to analysis.monthExpenseSpeed,
                        "支出最多分类" to analysis.topExpenseCategory,
                        "最近 7 天相比 30 天" to analysis.recentTrend,
                        "异常增加分类" to analysis.anomalyCategory
                    )
                )
            }
        }
        item {
            WarmPanel {
                Text("想买分析", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
                        wantAdvice = null
                    },
                    label = { Text("商品名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = productPrice,
                    onValueChange = {
                        productPrice = it
                        wantAdvice = null
                    },
                    label = { Text("预计价格") },
                    modifier = Modifier.fillMaxWidth()
                )
                ExpenseCategoryDropdownField(
                    value = productCategory,
                    onValueChange = {
                        productCategory = it
                        wantAdvice = null
                    },
                    categories = expenseCategories
                )
                Button(
                    onClick = {
                        wantAdvice = analyzeWantedPurchase(
                            analysis = analysis,
                            productName = productName,
                            priceText = productPrice,
                            category = productCategory
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("帮我分析") }
                wantAdvice?.let { advice ->
                    Text(advice.conclusion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(advice.reason)
                }
            }
        }
    }
}

@Composable
private fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    mode: ThemeMode,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("全部") }
    val filteredTransactions = remember(transactions, query, typeFilter) {
        transactions
            .filter { tx ->
                when (typeFilter) {
                    "支出" -> tx.type == TransactionType.EXPENSE
                    "收入" -> tx.type == TransactionType.INCOME
                    else -> true
                }
            }
            .filter { tx -> query.isBlank() || tx.matchesTransactionQuery(query) }
            .sortedByDescending { it.transactionTime }
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            MascotHeader(
                title = "流水记录",
                subtitle = "查找、筛选和编辑账目",
                mode = mode,
                page = MascotPage.RECORDS,
                action = { OutlinedButton(onClick = onRefresh) { Text("刷新") } }
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索商户、备注、账户或金额") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            ButtonRow(
                options = listOf("全部", "支出", "收入"),
                selected = typeFilter,
                onSelected = { typeFilter = it }
            )
        }
        if (transactions.isEmpty()) {
            item {
                MascotEmptyState(
                    title = "暂无流水",
                    subtitle = "记录第一笔收支吧",
                    mode = mode,
                    type = MascotEmptyType.RECORDS
                )
            }
        } else if (filteredTransactions.isEmpty()) {
            item {
                MascotEmptyState(
                    title = "没有匹配结果",
                    subtitle = "换个关键词或筛选条件",
                    mode = mode,
                    type = MascotEmptyType.SEARCH,
                    imageSize = 112.dp
                )
            }
        }
        filteredTransactions.groupBy { date(it.transactionTime) }.forEach { (day, rows) ->
            item { Text(text = day, style = MaterialTheme.typography.titleMedium) }
            items(items = rows, key = { it.id }) { tx ->
                WarmPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${tx.categoryPrimary} / ${tx.categorySecondary}", fontWeight = FontWeight.SemiBold)
                            Text(text = tx.merchant)
                            Text(text = tx.account)
                            Text(text = time(tx.transactionTime))
                            if (tx.note.isNotBlank()) Text("备注：${tx.note}")
                            if (tx.relatedSourceCount > 1) AssistChip(onClick = {}, label = { Text("已合并 ${tx.relatedSourceCount} 个来源") })
                        }
                        Text(text = if (tx.type == TransactionType.EXPENSE) "-${money(tx.amount)}" else "+${money(tx.amount)}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onEdit(tx) }) { Icon(imageVector = Icons.Default.Edit, contentDescription = "编辑") }
                        IconButton(onClick = { onDelete(tx.id) }) { Icon(imageVector = Icons.Default.Delete, contentDescription = "删除") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingScreen(
    pending: List<PendingTransactionEntity>,
    mode: ThemeMode,
    categories: List<CategoryGroup>,
    accountOptions: List<String>,
    onAddAccountOption: (String) -> Unit,
    onComplete: (PendingTransactionEntity, Double, TransactionType, String, String, String, String, String, Long) -> Unit,
    onNew: (PendingTransactionEntity) -> Unit,
    onMerge: (PendingTransactionEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    var completing by remember { mutableStateOf<PendingTransactionEntity?>(null) }
    val hidden = pending.filter { it.isHiddenAmount() }
    val confirmations = pending.filterNot { it.isHiddenAmount() }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            MascotHeader(
                title = "待处理提醒",
                subtitle = "补录和确认都在这里",
                mode = mode,
                page = MascotPage.PENDING,
                action = { OutlinedButton(onClick = onRefresh) { Text("刷新") } }
            )
        }
        item {
            WarmPanel {
                Text("待补录：金额被隐藏，需要手动补金额")
                Text("待确认：疑似重复或低置信度交易，需要确认")
            }
        }
        if (pending.isEmpty()) {
            item {
                MascotEmptyState(
                    title = "暂无待处理内容",
                    subtitle = "需要补录时会出现在这里",
                    mode = mode,
                    type = MascotEmptyType.PENDING
                )
            }
        } else {
            item { Text("待补录", style = MaterialTheme.typography.titleMedium) }
            if (hidden.isEmpty()) item { Text("暂无待补录通知。") }
            items(items = hidden, key = { it.id }) { p ->
                WarmPanel {
                    Text("${p.sourceApp} - ${p.rawTitle}", fontWeight = FontWeight.SemiBold)
                    Text(p.rawText.ifBlank { p.rawBigText })
                    Text(dateTime(p.transactionTime))
                    Text(p.reason.ifBlank { "金额被隐藏，请手动补录" })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { completing = p }) { Text("补录") }
                        IconButton(onClick = { onDelete(p.id) }) { Icon(imageVector = Icons.Default.Delete, contentDescription = "删除") }
                    }
                }
            }
            item { Divider() }
            item { Text("待确认", style = MaterialTheme.typography.titleMedium) }
            if (confirmations.isEmpty()) item { Text("暂无待确认交易。") }
            items(items = confirmations, key = { it.id }) { p ->
                WarmPanel {
                    Text("${p.merchant} ${money(p.amount)} - ${p.categoryPrimary}/${p.categorySecondary}", fontWeight = FontWeight.SemiBold)
                    Text("${p.sourceApp} - ${p.reason} - 置信度 ${"%.0f".format(p.confidence * 100)}%")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onNew(p) }) { Text("确认为新流水") }
                        OutlinedButton(onClick = { onMerge(p) }, enabled = p.possibleDuplicateTransactionId != null) { Text("合并") }
                        IconButton(onClick = { onDelete(p.id) }) { Icon(imageVector = Icons.Default.Delete, contentDescription = "删除") }
                    }
                }
            }
        }
    }
    completing?.let { pendingTx ->
        PendingCompletionDialog(
            pending = pendingTx,
            categories = categories,
            accountOptions = accountOptions,
            onAddAccountOption = onAddAccountOption,
            onDismiss = { completing = null },
            onSave = { amount, type, primary, secondary, merchant, account, note, transactionTime ->
                onComplete(pendingTx, amount, type, primary, secondary, merchant, account, note, transactionTime)
                completing = null
            }
        )
    }
}

@Composable
private fun StatsScreen(
    transactions: List<TransactionEntity>,
    mode: ThemeMode,
    statsUiState: StatsUiState,
    onRangeChange: (TimeRangeType) -> Unit,
    onDateChange: (Long) -> Unit,
    onCustomStartChange: (String) -> Unit,
    onCustomEndChange: (String) -> Unit,
    onCustomRangeApply: (DateRange?, String?) -> Unit,
    onRefresh: () -> Unit
) {
    val selectedRange = statsUiState.selectedRange
    val selectedDate = statsUiState.selectedDate
    val customStart = statsUiState.customStart
    val customEnd = statsUiState.customEnd
    val customRange = statsUiState.customRange
    val customError = statsUiState.customError
    var showSubRanks by remember { mutableStateOf(false) }
    val activeRange = if (selectedRange == TimeRangeType.CUSTOM) {
        customRange ?: StatsRangeDefaults.initial().customRange
    } else {
        DateRangeCalculator.range(selectedRange, selectedDate, customRange)
    }
    val filtered = DateRangeCalculator.filter(transactions, activeRange).sortedByDescending { it.transactionTime }
    val expense = filtered.filter { it.type == TransactionType.EXPENSE }
    val income = filtered.filter { it.type == TransactionType.INCOME }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            MascotHeader(
                title = "统计分析",
                subtitle = "按时间查看收支变化",
                mode = mode,
                page = MascotPage.STATS,
                action = { OutlinedButton(onClick = onRefresh) { Text("刷新") } }
            )
        }
        item {
            ButtonRow(TimeRangeType.values().map { it.label }, selectedRange.label) { label ->
                onRangeChange(TimeRangeType.values().first { it.label == label })
            }
        }
        if (selectedRange != TimeRangeType.ALL && selectedRange != TimeRangeType.CUSTOM) {
            item {
                WarmPanel {
                    Text(activeRange.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { onDateChange(shiftDate(selectedDate, selectedRange, -1)) }, modifier = Modifier.weight(1f)) { Text(previousLabel(selectedRange)) }
                        OutlinedButton(onClick = { onDateChange(shiftDate(selectedDate, selectedRange, 1)) }, modifier = Modifier.weight(1f)) { Text(nextLabel(selectedRange)) }
                    }
                }
            }
        }
        if (selectedRange == TimeRangeType.CUSTOM) {
            item {
                WarmPanel {
                    Text("自定义时间区间", style = MaterialTheme.typography.titleMedium)
                    DateInputField(value = customStart, label = "开始日期", onValueChange = onCustomStartChange)
                    DateInputField(value = customEnd, label = "结束日期", onValueChange = onCustomEndChange)
                    Button(
                        onClick = {
                            val result = DateRangeCalculator.custom(customStart, customEnd)
                            onCustomRangeApply(result.range, result.error)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("应用") }
                    customError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        if (selectedRange == TimeRangeType.MONTH) {
            item {
                MonthCalendar(
                    monthMillis = selectedDate,
                    transactions = transactions,
                    onDayClick = {
                        onDateChange(it)
                        onRangeChange(TimeRangeType.TODAY)
                    }
                )
            }
        }
        item {
            MetricGrid(
                listOf(
                    "收入" to money(filtered.sumIncome()),
                    "支出" to money(filtered.sumExpense()),
                    "结余" to money(filtered.sumIncome() - filtered.sumExpense())
                )
            )
        }
        item { RankingPanel(title = "支出分类排行", rows = expense.rankBy { it.categoryPrimary }) }
        item { RankingPanel(title = "收入分类排行", rows = income.rankBy { it.categoryPrimary }) }
        item {
            OutlinedButton(onClick = { showSubRanks = !showSubRanks }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showSubRanks) "收起细类排行" else "查看细类排行")
            }
        }
        if (showSubRanks) {
            item { RankingPanel(title = "支出细类排行", rows = expense.rankBy { it.categorySecondary }) }
            item { RankingPanel(title = "收入细类排行", rows = income.rankBy { it.categorySecondary }) }
        }
        item { Text("当前范围流水", style = MaterialTheme.typography.titleMedium) }
        if (filtered.isEmpty()) {
            item {
                MascotEmptyState(
                    title = "当前区间暂无数据",
                    subtitle = "调整时间范围再看看",
                    mode = mode,
                    type = MascotEmptyType.STATS
                )
            }
        }
        items(filtered, key = { it.id }) { tx ->
            WarmPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dateTime(tx.transactionTime), fontWeight = FontWeight.SemiBold)
                        Text("${tx.categoryPrimary} / ${tx.categorySecondary}", fontWeight = FontWeight.SemiBold)
                        Text(tx.merchant)
                        Text(tx.account)
                        if (tx.note.isNotBlank()) Text("备注：${tx.note}")
                    }
                    Text(if (tx.type == TransactionType.EXPENSE) "-${money(tx.amount)}" else "+${money(tx.amount)}")
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    mode: ThemeMode,
    permissionStatus: PermissionStatus,
    rules: List<CategoryRuleEntity>,
    monitoredApps: List<MonitoredApp>,
    installedApps: List<LaunchableApp>,
    customCategories: List<CategoryGroup>,
    hiddenCategories: Set<String>,
    visibleCategories: List<CategoryGroup>,
    notificationDiagnostics: List<NotificationDiagnosticEntity>,
    onTheme: (ThemeMode) -> Unit,
    onBoot: (Boolean) -> Unit,
    onOpenPermissionGuide: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenNotificationListener: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onWhitelist: (String, Boolean) -> Unit,
    onToggleInstalledApp: (LaunchableApp, Boolean) -> Unit,
    onAddApp: (String, String) -> Unit,
    onDeleteApp: (String) -> Unit,
    onReloadApps: () -> Unit,
    onAddCategory: (TransactionType, String, String) -> Unit,
    onDeleteCategory: (TransactionType, String, String?) -> Unit,
    onHideCategory: (TransactionType, String, String?) -> Unit,
    onRestoreCategory: (String) -> Unit,
    onAddRule: (String, String, String, TransactionType) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    importResultMessage: String?,
    importInProgress: Boolean,
    onRefresh: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            MascotHeader(
                title = "设置中心",
                subtitle = "主题、权限、监听和数据",
                mode = mode,
                page = MascotPage.SETTINGS,
                action = { OutlinedButton(onClick = onRefresh) { Text("刷新") } }
            )
        }
        item {
            WarmPanel {
                Text("主题伙伴", style = MaterialTheme.typography.titleLarge)
                Text("选择界面配色和后续伙伴素材风格。")
                ThemePartnerButton(
                    title = "清浅主题",
                    description = "清爽温柔，白色系",
                    optionMode = ThemeMode.YIER,
                    selected = supportedThemeMode(mode) == ThemeMode.YIER,
                    onClick = { onTheme(ThemeMode.YIER) }
                )
                ThemePartnerButton(
                    title = "暖色主题",
                    description = "暖萌陪伴，棕色系",
                    optionMode = ThemeMode.BUBU,
                    selected = supportedThemeMode(mode) == ThemeMode.BUBU,
                    onClick = { onTheme(ThemeMode.BUBU) }
                )
            }
        }
        item {
            PermissionSettingsPanel(
                status = permissionStatus,
                onBoot = onBoot,
                onOpenGuide = onOpenPermissionGuide,
                onOpenDiagnostics = onOpenDiagnostics,
                diagnosticCount = notificationDiagnostics.size,
                onOpenNotificationListener = onOpenNotificationListener,
                onRequestPostNotifications = onRequestPostNotifications,
                onOpenAppSettings = onOpenAppSettings,
                onOpenBatterySettings = onOpenBatterySettings
            )
        }
        item {
            AppManagerPanel(
                mode = mode,
                monitoredApps = monitoredApps,
                installedApps = installedApps,
                onWhitelist = onWhitelist,
                onToggleInstalledApp = onToggleInstalledApp,
                onAddApp = onAddApp,
                onDeleteApp = onDeleteApp,
                onReloadApps = onReloadApps,
                onRefresh = onRefresh
            )
        }
        item {
            WarmPanel {
                Text("数据导入与导出", style = MaterialTheme.typography.titleLarge)
                Text("导出全部流水为 CSV，方便备份或用表格查看。")
                OutlinedButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                    Text("导出流水 CSV")
                }
                Text("从 CSV 导入流水，支持本 APP 导出的 CSV；其他 APP 的 CSV 会尽量识别。")
                Button(
                    onClick = onImportCsv,
                    enabled = !importInProgress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (importInProgress) "正在导入..." else "导入流水 CSV")
                }
                importResultMessage?.let { Text(it) }
            }
        }
        item {
            CategoryManagerPanel(
                customCategories = customCategories,
                hiddenCategories = hiddenCategories,
                visibleCategories = visibleCategories,
                onAddCategory = onAddCategory,
                onDeleteCategory = onDeleteCategory,
                onHideCategory = onHideCategory,
                onRestoreCategory = onRestoreCategory
            )
        }
        item {
            RulesPanel(rules = rules, categories = visibleCategories, onAdd = onAddRule, onDelete = onDeleteRule)
        }
    }
}

@Composable
private fun NotificationDiagnosticsScreen(
    diagnostics: List<NotificationDiagnosticEntity>,
    mode: ThemeMode,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空诊断记录？") },
            text = { Text("这只会清空通知诊断日志，不会删除正式流水、待补录或账户数据。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        confirmClear = false
                    }
                ) { Text("确认清空") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            }
        )
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("通知监听诊断", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onBack) { Text("返回") }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onRefresh) { Text("刷新") }
            }
        }
        item {
            WarmPanel {
                Text("最近通知记录", fontWeight = FontWeight.SemiBold)
                Text("显示最近 30 条，用于排查识别、待补录或忽略原因。")
                OutlinedButton(
                    onClick = { confirmClear = true },
                    enabled = diagnostics.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("清空记录") }
            }
        }
        if (diagnostics.isEmpty()) {
            item {
                MascotEmptyState(
                    title = "暂无通知诊断记录",
                    subtitle = "开启通知读取后会记录结果",
                    mode = mode,
                    type = MascotEmptyType.DIAGNOSTICS,
                    imageSize = 104.dp
                )
            }
        } else {
            items(items = diagnostics, key = { it.id }) { row ->
                WarmPanel {
                    Text(dateTime(row.timeMillis), fontWeight = FontWeight.SemiBold)
                    Text("${row.appName.ifBlank { "未知 App" }}  ${row.packageName}")
                    if (row.title.isNotBlank()) Text("标题：${row.title}")
                    if (row.text.isNotBlank()) Text("内容：${row.text}")
                    Text("监听状态：${if (row.isMonitoredApp) "已监听" else "未监听"}")
                    Text("解析结果：${row.parserResult}")
                    Text("处理结果：${row.finalAction}", fontWeight = FontWeight.SemiBold)
                    if (row.ignoreReason.isNotBlank()) Text("原因：${row.ignoreReason}")
                }
            }
        }
    }
}

@Composable
private fun PermissionGuideDialog(
    status: PermissionStatus,
    onDismiss: () -> Unit,
    onOpenNotificationListener: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onBootChange: (Boolean) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onRefresh: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("开启自动记账权限", style = MaterialTheme.typography.titleLarge)
                Text("开启通知读取后，可在后台识别交易通知。数据只保存在本机。")
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        PermissionGuideItem(
                            title = "通知读取权限",
                            description = "用于读取交易通知，自动生成收支流水。",
                            enabled = status.notificationListenerEnabled,
                            button = "去开启",
                            onClick = onOpenNotificationListener
                        )
                    }
                    item {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionGuideItem(
                                title = "通知提醒权限",
                                description = "用于提醒你有待补录、待确认交易。",
                                enabled = status.postNotificationsGranted,
                                button = "允许通知",
                                onClick = onRequestPostNotifications
                            )
                        } else {
                            PermissionGuideStaticItem("通知提醒权限", "Android 13 以下无需单独授权。", "无需单独授权")
                        }
                    }
                    item {
                        PermissionGuideSwitchItem(
                            title = "开机后恢复后台任务",
                            description = "启用 APP 侧恢复任务；厂商自启动需你手动允许。",
                            checked = status.bootKeepAliveEnabled,
                            onCheckedChange = onBootChange
                        )
                    }
                    item {
                        PermissionGuideStaticItem(
                            title = "后台运行 / 电池优化",
                            description = "建议允许后台运行，并关闭电池优化限制。",
                            status = when (status.batteryOptimizationIgnored) {
                                true -> "已忽略电池优化"
                                false -> "需按系统提示设置"
                                null -> "需按系统提示设置"
                            }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.weight(1f)) { Text("应用设置") }
                            OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.weight(1f)) { Text("电池优化") }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("刷新状态") }
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("稍后再说") }
                }
            }
        }
    }
}

@Composable
private fun PermissionGuideItem(title: String, description: String, enabled: Boolean, button: String, onClick: () -> Unit) {
    WarmPanel {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(description)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (enabled) "已开启" else "未开启", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Button(onClick = onClick) { Text(button) }
        }
    }
}

@Composable
private fun PermissionGuideSwitchItem(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    WarmPanel {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(description)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (checked) "APP 侧已启用；请检查系统自启动和后台运行" else "APP 侧未启用",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun BootKeepAliveGuideDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("开机恢复需要系统允许") },
        text = {
            Text("APP 已启用开机后恢复记账任务。vivo、小米、OPPO、华为等系统的自启动、后台运行和电池优化仍需你手动允许，APP 无法自动替你开启。")
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        confirmButton = {
            Button(onClick = onOpenSettings) { Text("去设置") }
        }
    )
}

@Composable
private fun PermissionGuideStaticItem(title: String, description: String, status: String) {
    WarmPanel {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(description)
        Text(status, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PermissionSettingsPanel(
    status: PermissionStatus,
    onBoot: (Boolean) -> Unit,
    onOpenGuide: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    diagnosticCount: Int,
    onOpenNotificationListener: () -> Unit,
    onRequestPostNotifications: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    WarmPanel {
        Text("权限与后台运行", style = MaterialTheme.typography.titleLarge)
        Text("通知读取用于识别交易。自启动、后台运行和电池优化需在系统里手动允许。")
        PermissionRow("通知读取", if (status.notificationListenerEnabled) "已开启" else "未开启", "去开启", onOpenNotificationListener)
        PermissionRow("通知提醒", if (status.postNotificationsGranted) "已开启" else "未开启", "允许通知", onRequestPostNotifications)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "开机恢复：${if (status.bootKeepAliveEnabled) "APP 侧已启用，系统权限需确认" else "APP 侧未启用"}",
                modifier = Modifier.weight(1f)
            )
            Switch(checked = status.bootKeepAliveEnabled, onCheckedChange = onBoot)
        }
        PermissionRow(
            "后台运行 / 电池优化",
            when (status.batteryOptimizationIgnored) {
                true -> "已忽略电池优化"
                false -> "查看设置"
                null -> "查看设置"
            },
            "应用设置",
            onOpenAppSettings
        )
        OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) { Text("电池优化") }
        OutlinedButton(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) { Text("通知监听诊断（最近 $diagnosticCount 条）") }
        Button(onClick = onOpenGuide, modifier = Modifier.fillMaxWidth()) { Text("权限引导") }
    }
}

@Composable
private fun PermissionRow(title: String, status: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("$title：$status", modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onClick) { Text(action) }
    }
}

@Composable
private fun AppManagerPanel(
    mode: ThemeMode,
    monitoredApps: List<MonitoredApp>,
    installedApps: List<LaunchableApp>,
    onWhitelist: (String, Boolean) -> Unit,
    onToggleInstalledApp: (LaunchableApp, Boolean) -> Unit,
    onAddApp: (String, String) -> Unit,
    onDeleteApp: (String) -> Unit,
    onReloadApps: () -> Unit,
    onRefresh: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val enabledApps = monitoredApps.filter { it.enabled }
    val preview = enabledApps.take(5).joinToString("、") { it.name }
    WarmPanel {
        Text("监听 App 管理", style = MaterialTheme.typography.titleLarge)
        Text("已监听 ${enabledApps.size} 个 App")
        if (enabledApps.isEmpty()) {
            Text("尚未选择监听 App")
        } else {
            Text(preview + if (enabledApps.size > 5) "等 ${enabledApps.size} 个" else "")
        }
        Button(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) { Text("选择监听 App") }
    }
    if (showPicker) {
        AppPickerDialog(
            mode = mode,
            monitoredApps = monitoredApps,
            installedApps = installedApps,
            onWhitelist = onWhitelist,
            onToggleInstalledApp = onToggleInstalledApp,
            onAddApp = onAddApp,
            onDeleteApp = onDeleteApp,
            onReloadApps = onReloadApps,
            onRefresh = onRefresh,
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun AppPickerDialog(
    mode: ThemeMode,
    monitoredApps: List<MonitoredApp>,
    installedApps: List<LaunchableApp>,
    onWhitelist: (String, Boolean) -> Unit,
    onToggleInstalledApp: (LaunchableApp, Boolean) -> Unit,
    onAddApp: (String, String) -> Unit,
    onDeleteApp: (String) -> Unit,
    onReloadApps: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualPackage by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    val monitoredByPackage = remember(monitoredApps) { monitoredApps.associateBy { it.packageName } }
    val iconCache = remember { mutableStateMapOf<String, Drawable?>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val indexLetters = remember {
        listOf(
            "#",
            "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X", "Y", "Z"
        )
    }
    val filtered = remember(installedApps, query, showSystemApps) {
        InstalledAppRepository.displayApps(installedApps, query, showSystemApps)
    }
    val grouped = remember(filtered) {
        filtered.groupBy { AppInitialHelper.groupKey(it.appName, it.packageName) }
            .toSortedMap(compareBy<String> { if (it == "#") 0 else 1 }.thenBy { it })
    }
    val groupHeaderIndexMap = remember(grouped) {
        val starts = mutableMapOf<String, Int>()
        var index = 0
        grouped.forEach { (group, apps) ->
            starts[group] = index
            index += 1 + apps.size
        }
        starts
    }
    var activeIndexLetter by remember { mutableStateOf<String?>(null) }
    var lastDraggedLetter by remember { mutableStateOf<String?>(null) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    fun nearestAvailableGroup(targetLetter: String): String? {
        groupHeaderIndexMap[targetLetter]?.let { return targetLetter }
        val targetIndex = indexLetters.indexOf(targetLetter)
        if (targetIndex < 0) return groupHeaderIndexMap.keys.firstOrNull()
        return indexLetters.drop(targetIndex + 1).firstOrNull { it in groupHeaderIndexMap }
            ?: indexLetters.take(targetIndex).asReversed().firstOrNull { it in groupHeaderIndexMap }
    }
    fun jumpToIndex(letter: String, force: Boolean = false, animated: Boolean = true) {
        if (!force && activeIndexLetter == letter) return
        activeIndexLetter = letter
        val group = nearestAvailableGroup(letter) ?: return
        groupHeaderIndexMap[group]?.let { target ->
            scrollJob?.cancel()
            scrollJob = coroutineScope.launch {
                if (animated) {
                    listState.animateScrollToItem(target)
                } else {
                    listState.scrollToItem(target)
                }
            }
        }
    }
    fun jumpDraggedIndex(letter: String, force: Boolean = false) {
        if (!force && lastDraggedLetter == letter) return
        lastDraggedLetter = letter
        jumpToIndex(letter = letter, force = true, animated = false)
    }
    fun indexLetterAtOffset(y: Float, height: Int): String? {
        if (height <= 0) return null
        val index = ((y.coerceIn(0f, (height - 1).toFloat()) / height) * indexLetters.size)
            .toInt()
            .coerceIn(0, indexLetters.lastIndex)
        return indexLetters[index]
    }
    val selectedCount = remember(monitoredApps) { monitoredApps.count { it.enabled } }
    LaunchedEffect(filtered) {
        filtered.take(24)
            .map { it.packageName }
            .filterNot { iconCache.containsKey(it) }
            .forEach { packageName ->
                val icon = withContext(Dispatchers.IO) {
                    runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
                }
                iconCache[packageName] = icon
            }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxSize().padding(4.dp)) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("选择要监听的 App", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("完成") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("搜索 App 或包名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "找不到时开启系统应用，或手动输入包名。",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("已选择 $selectedCount 个", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("显示系统应用", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(checked = showSystemApps, onCheckedChange = { showSystemApps = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("刷新") }
                    TextButton(onClick = onReloadApps, modifier = Modifier.weight(1f)) { Text("重新加载") }
                }
                feedback?.let { Text(it, color = if (it == "保存成功") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                if (installedApps.isEmpty()) {
                    Text("未读取到可选择应用，请点击重新加载")
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filtered.isEmpty() && query.isNotBlank()) {
                            item {
                                MascotEmptyState(
                                    title = "没有匹配结果",
                                    subtitle = "可搜索包名或手动输入",
                                    mode = mode,
                                    type = MascotEmptyType.SEARCH,
                                    imageSize = 92.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        grouped.forEach { (group, apps) ->
                            item(key = "header_$group", contentType = "header") {
                                Text(group, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                            }
                            items(items = apps, key = { it.packageName }, contentType = { "app" }) { app ->
                                val monitored = monitoredByPackage[app.packageName]
                                val checked = monitored?.enabled == true
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (app.packageName in DefaultAppWhitelist.packages || app.packageName in monitoredByPackage) {
                                            onWhitelist(app.packageName, !checked)
                                        } else {
                                            onToggleInstalledApp(app, !checked)
                                        }
                                    }.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(packageName = app.packageName, fallback = app.appName, iconCache = iconCache)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.appName, fontWeight = FontWeight.SemiBold)
                                        Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Checkbox(checked = checked, onCheckedChange = { enabled ->
                                        if (app.packageName in DefaultAppWhitelist.packages || app.packageName in monitoredByPackage) {
                                            onWhitelist(app.packageName, enabled)
                                        } else {
                                            onToggleInstalledApp(app, enabled)
                                        }
                                    })
                                }
                                Divider()
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(24.dp)
                            .pointerInput(indexLetters, groupHeaderIndexMap) {
                                detectTapGestures { offset ->
                                    indexLetterAtOffset(offset.y, size.height)?.let { jumpToIndex(it, force = true) }
                                }
                            }
                            .pointerInput(indexLetters, groupHeaderIndexMap) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        lastDraggedLetter = null
                                        indexLetterAtOffset(offset.y, size.height)?.let { jumpDraggedIndex(it, force = true) }
                                    },
                                    onDragEnd = {
                                        activeIndexLetter = null
                                        lastDraggedLetter = null
                                    },
                                    onDragCancel = {
                                        activeIndexLetter = null
                                        lastDraggedLetter = null
                                    },
                                    onDrag = { change, _ ->
                                        indexLetterAtOffset(change.position.y, size.height)?.let { jumpDraggedIndex(it) }
                                        change.consume()
                                    }
                                )
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        indexLetters.forEach { letter ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clickable {
                                        jumpToIndex(letter, force = true)
                                        activeIndexLetter = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 9.sp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = { showAdvanced = !showAdvanced }) { Text(if (showAdvanced) "收起高级入口" else "手动输入包名") }
                if (showAdvanced) {
                    OutlinedTextField(value = manualName, onValueChange = { manualName = it }, label = { Text("App 名称") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = manualPackage, onValueChange = { manualPackage = it }, label = { Text("包名") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (manualPackage.isBlank()) {
                                feedback = "请输入包名"
                            } else {
                                onAddApp(manualName.ifBlank { manualPackage }, manualPackage)
                                manualName = ""
                                manualPackage = ""
                                feedback = "保存成功"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("保存监听 App") }
                    monitoredApps.filterNot { it.isDefault }.forEach { app ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${app.name}\n${app.packageName}", modifier = Modifier.weight(1f))
                            Switch(checked = app.enabled, onCheckedChange = { onWhitelist(app.packageName, it) })
                            IconButton(onClick = { onDeleteApp(app.packageName) }) { Icon(imageVector = Icons.Default.Delete, contentDescription = "删除") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, fallback: String, iconCache: MutableMap<String, Drawable?>) {
    val context = LocalContext.current
    val drawable by produceState<Drawable?>(initialValue = iconCache[packageName], context, packageName) {
        if (!iconCache.containsKey(packageName)) {
            val icon = withContext(Dispatchers.IO) {
                runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
            }
            iconCache[packageName] = icon
            value = icon
        } else {
            value = iconCache[packageName]
        }
    }
    if (drawable != null) {
        AndroidView(
            factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
            update = { it.setImageDrawable(drawable) },
            modifier = Modifier.width(40.dp).height(40.dp)
        )
    } else {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.width(40.dp).height(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(fallback.take(1).ifBlank { "#" }, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CategoryManagerPanel(
    customCategories: List<CategoryGroup>,
    hiddenCategories: Set<String>,
    visibleCategories: List<CategoryGroup>,
    onAddCategory: (TransactionType, String, String) -> Unit,
    onDeleteCategory: (TransactionType, String, String?) -> Unit,
    onHideCategory: (TransactionType, String, String?) -> Unit,
    onRestoreCategory: (String) -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var primary by remember { mutableStateOf("") }
    var secondary by remember { mutableStateOf("") }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    WarmPanel {
        Text("分类管理", style = MaterialTheme.typography.titleLarge)
        CategorySection("支出分类", TransactionType.EXPENSE, visibleCategories, customCategories, expanded, onHideCategory, onDeleteCategory)
        Divider()
        CategorySection("收入分类", TransactionType.INCOME, visibleCategories, customCategories, expanded, onHideCategory, onDeleteCategory)
        Divider()
        Text("已隐藏分类", style = MaterialTheme.typography.titleMedium)
        if (hiddenCategories.isEmpty()) Text("暂无隐藏分类")
        hiddenCategories.sorted().forEach { key ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(hiddenCategoryLabel(key), modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { onRestoreCategory(key) }) { Text("恢复") }
            }
        }
        Divider()
        Text("添加分类", style = MaterialTheme.typography.titleMedium)
        ButtonRow(listOf("支出", "收入"), if (type == TransactionType.EXPENSE) "支出" else "收入") {
            type = if (it == "支出") TransactionType.EXPENSE else TransactionType.INCOME
        }
        OutlinedTextField(value = primary, onValueChange = { primary = it }, label = { Text("大类") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = secondary, onValueChange = { secondary = it }, label = { Text("细类") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            onAddCategory(type, primary, secondary)
            primary = ""
            secondary = ""
        }, modifier = Modifier.fillMaxWidth()) { Text("保存分类") }
    }
}

@Composable
private fun CategorySection(
    title: String,
    type: TransactionType,
    visibleCategories: List<CategoryGroup>,
    customCategories: List<CategoryGroup>,
    expanded: MutableMap<String, Boolean>,
    onHideCategory: (TransactionType, String, String?) -> Unit,
    onDeleteCategory: (TransactionType, String, String?) -> Unit
) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    val customKeys = customCategories.filter { it.type == type }.associateBy { it.primary }
    visibleCategories.filter { it.type == type }.forEach { group ->
        val key = categoryKey(group.type, group.primary)
        val open = expanded[key] ?: false
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(group.primary, modifier = Modifier.weight(1f).clickable { expanded[key] = !open }, fontWeight = FontWeight.SemiBold)
            if (customKeys.containsKey(group.primary) && !CategoryCatalog.defaults.any { it.type == type && it.primary == group.primary }) {
                OutlinedButton(onClick = { onDeleteCategory(group.type, group.primary, null) }) { Text("删除") }
            } else {
                OutlinedButton(onClick = { onHideCategory(group.type, group.primary, null) }) { Text("隐藏") }
            }
        }
        if (open) {
            group.secondary.forEach { secondary ->
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(secondary, modifier = Modifier.weight(1f))
                    if (customKeys[group.primary]?.secondary?.contains(secondary) == true && !CategoryCatalog.defaults.any { it.type == type && it.primary == group.primary && secondary in it.secondary }) {
                        TextButton(onClick = { onDeleteCategory(group.type, group.primary, secondary) }) { Text("删除") }
                    } else {
                        TextButton(onClick = { onHideCategory(group.type, group.primary, secondary) }) { Text("隐藏") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RulesPanel(
    rules: List<CategoryRuleEntity>,
    categories: List<CategoryGroup>,
    onAdd: (String, String, String, TransactionType) -> Unit,
    onDelete: (Long) -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var primary by remember { mutableStateOf(firstPrimary(type, categories)) }
    var secondary by remember { mutableStateOf(firstSecondary(type, primary, categories)) }
    var message by remember { mutableStateOf<String?>(null) }
    CategorySync(type, primary, categories, onPrimary = { primary = it }, onSecondary = { secondary = it })
    WarmPanel {
        Text("自动分类规则", style = MaterialTheme.typography.titleLarge)
        Text("关键词匹配后自动分类。")
        OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text("关键词") }, modifier = Modifier.fillMaxWidth())
        CategorySelector(type, primary, secondary, categories, onType = { type = it }, onPrimary = { primary = it }, onSecondary = { secondary = it })
        Button(onClick = {
            if (keyword.isBlank()) {
                message = "请输入关键词"
            } else if (rules.any { it.keyword == keyword.trim() }) {
                message = "关键词已存在"
            } else {
                onAdd(keyword.trim(), primary, secondary, type)
                keyword = ""
                message = "规则已保存"
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("保存规则") }
        message?.let { Text(it, fontWeight = FontWeight.SemiBold) }
        if (rules.isEmpty()) Text("暂无规则")
        rules.forEach { rule ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.keyword, fontWeight = FontWeight.SemiBold)
                    Text("${typeLabel(rule.transactionType)}  ${rule.categoryPrimary} / ${rule.categorySecondary}")
                }
                IconButton(onClick = {
                    onDelete(rule.id)
                    message = "规则已删除"
                }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "删除")
                }
            }
            Divider()
        }
    }
}

@Composable
private fun ManualTransactionDialog(
    initial: TransactionEntity?,
    categories: List<CategoryGroup>,
    accountOptions: List<String>,
    onAddAccountOption: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Double, TransactionType, String, String, String, String, String, Long) -> Unit
) {
    val defaultTime = initial?.transactionTime ?: System.currentTimeMillis()
    var amount by remember { mutableStateOf(initial?.amount?.toString().orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: TransactionType.EXPENSE) }
    var primary by remember { mutableStateOf(initial?.categoryPrimary ?: firstPrimary(type, categories)) }
    var secondary by remember { mutableStateOf(initial?.categorySecondary ?: firstSecondary(type, primary, categories)) }
    var merchant by remember { mutableStateOf(initial?.merchant.orEmpty()) }
    var account by remember { mutableStateOf(initial?.account.orEmpty()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var transactionDate by remember { mutableStateOf(DateRangeCalculator.dateString(defaultTime)) }
    var transactionClock by remember { mutableStateOf(DateRangeCalculator.timeString(defaultTime)) }
    var error by remember { mutableStateOf<String?>(null) }
    CategorySync(type, primary, categories, onPrimary = { primary = it }, onSecondary = { secondary = it })
    EntryDialogScaffold(
        title = if (initial == null) "新增流水" else "编辑流水",
        onDismiss = onDismiss,
        onSave = {
            val finalAmount = amount.toDoubleOrNull()
            if (finalAmount == null) {
                error = "请输入正确金额"
                return@EntryDialogScaffold
            }
            val parsedTime = DateRangeCalculator.parseDateTime(transactionDate, transactionClock)
            if (parsedTime.error != null || parsedTime.millis == null) {
                error = parsedTime.error
                return@EntryDialogScaffold
            }
            onSave(finalAmount, type, primary, secondary, merchant, account, note, parsedTime.millis)
        }
    ) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额") }, modifier = Modifier.fillMaxWidth())
        DateInputField(value = transactionDate, label = "交易日期", onValueChange = { transactionDate = it })
        OutlinedTextField(value = transactionClock, onValueChange = { transactionClock = it }, label = { Text("交易时间") }, modifier = Modifier.fillMaxWidth())
        CategorySelector(type, primary, secondary, categories, onType = { type = it }, onPrimary = { primary = it }, onSecondary = { secondary = it })
        OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("商户/对方") }, modifier = Modifier.fillMaxWidth())
        AccountDropdownField(
            value = account,
            options = accountOptions,
            onValueChange = { account = it },
            onAddOption = onAddAccountOption
        )
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PendingCompletionDialog(
    pending: PendingTransactionEntity,
    categories: List<CategoryGroup>,
    accountOptions: List<String>,
    onAddAccountOption: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Double, TransactionType, String, String, String, String, String, Long) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var primary by remember { mutableStateOf(firstPrimary(type, categories)) }
    var secondary by remember { mutableStateOf(firstSecondary(type, primary, categories)) }
    var merchant by remember { mutableStateOf(pending.merchant) }
    var account by remember { mutableStateOf(pending.account) }
    var note by remember { mutableStateOf("") }
    var transactionDate by remember { mutableStateOf(DateRangeCalculator.dateString(pending.transactionTime)) }
    var transactionClock by remember { mutableStateOf(DateRangeCalculator.timeString(pending.transactionTime)) }
    var error by remember { mutableStateOf<String?>(null) }
    CategorySync(type, primary, categories, onPrimary = { primary = it }, onSecondary = { secondary = it })
    EntryDialogScaffold(
        title = "补录金额",
        onDismiss = onDismiss,
        onSave = {
            val finalAmount = amount.toDoubleOrNull()
            if (finalAmount == null) {
                error = "请输入正确金额"
                return@EntryDialogScaffold
            }
            val parsedTime = DateRangeCalculator.parseDateTime(transactionDate, transactionClock)
            if (parsedTime.error != null || parsedTime.millis == null) {
                error = parsedTime.error
                return@EntryDialogScaffold
            }
            onSave(finalAmount, type, primary, secondary, merchant, account, note, parsedTime.millis)
        }
    ) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("${pending.sourceApp} - ${pending.rawTitle}")
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额") }, modifier = Modifier.fillMaxWidth())
        DateInputField(value = transactionDate, label = "交易日期", onValueChange = { transactionDate = it })
        OutlinedTextField(value = transactionClock, onValueChange = { transactionClock = it }, label = { Text("交易时间") }, modifier = Modifier.fillMaxWidth())
        CategorySelector(type, primary, secondary, categories, onType = { type = it }, onPrimary = { primary = it }, onSecondary = { secondary = it })
        OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("商户/对方") }, modifier = Modifier.fillMaxWidth())
        AccountDropdownField(
            value = account,
            options = accountOptions,
            onValueChange = { account = it },
            onAddOption = onAddAccountOption
        )
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateInputField(value: String, label: String, onValueChange: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = { showPicker = true }) { Text("选择") }
    }
    if (showPicker) {
        val initialMillis = remember(value) {
            DateRangeCalculator.parseDateTime(value, "08:00").millis ?: System.currentTimeMillis()
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onValueChange(DateRangeCalculator.dateString(it)) }
                        showPicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdownField(
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    onAddOption: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val cleanOptions = remember(options) { options.map { it.trim() }.filter { it.isNotBlank() }.distinct() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("账户") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            cleanOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
            Divider()
            DropdownMenuItem(
                text = { Text("+ 新增账户选项") },
                onClick = {
                    expanded = false
                    showAddDialog = true
                }
            )
        }
    }

    if (showAddDialog) {
        AddAccountOptionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name ->
                onAddOption(name)
                onValueChange(name)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseCategoryDropdownField(
    value: String,
    categories: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(categories) {
        (categories + "其他").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value.ifBlank { options.firstOrNull().orEmpty() },
            onValueChange = {},
            readOnly = true,
            label = { Text("支出分类") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddAccountOptionDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("新增账户选项", style = MaterialTheme.typography.titleMedium)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("账户名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(
                        onClick = {
                            val cleanName = name.trim()
                            if (cleanName.isBlank()) {
                                error = "请输入账户名称"
                            } else {
                                onSave(cleanName)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("保存") }
                }
            }
        }
    }
}

@Composable
private fun EntryDialogScaffold(title: String, onDismiss: () -> Unit, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("保存") }
                }
            }
        }
    }
}

@Composable
private fun CategorySelector(
    type: TransactionType,
    primary: String,
    secondary: String,
    categories: List<CategoryGroup>,
    onType: (TransactionType) -> Unit,
    onPrimary: (String) -> Unit,
    onSecondary: (String) -> Unit
) {
    val groups = categories.filter { it.type == type }
    val secondaries = groups.firstOrNull { it.primary == primary }?.secondary.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ButtonRow(listOf("支出", "收入"), if (type == TransactionType.EXPENSE) "支出" else "收入") {
            onType(if (it == "支出") TransactionType.EXPENSE else TransactionType.INCOME)
        }
        Text("大类")
        ButtonRow(groups.map { it.primary }, primary, onPrimary)
        Text("细类")
        ButtonRow(secondaries, secondary, onSecondary)
    }
}

@Composable
private fun CategorySync(
    type: TransactionType,
    primary: String,
    categories: List<CategoryGroup>,
    onPrimary: (String) -> Unit,
    onSecondary: (String) -> Unit
) {
    LaunchedEffect(type, primary, categories) {
        val groups = categories.filter { it.type == type }
        val nextPrimary = groups.firstOrNull { it.primary == primary }?.primary ?: groups.firstOrNull()?.primary
        if (nextPrimary != null && nextPrimary != primary) onPrimary(nextPrimary)
        val nextSecondary = groups.firstOrNull { it.primary == nextPrimary }?.secondary?.firstOrNull()
        if (nextSecondary != null) onSecondary(nextSecondary)
    }
}

@Composable
private fun MonthCalendar(monthMillis: Long, transactions: List<TransactionEntity>, onDayClick: (Long) -> Unit) {
    val days = daysInMonth(monthMillis)
    WarmPanel {
        Text("月历", style = MaterialTheme.typography.titleMedium)
        days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { dayMillis ->
                    val dayRows = transactions.inRange(startOfDay(dayMillis), endOfDay(dayMillis))
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { onDayClick(dayMillis) }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(SimpleDateFormat("d", Locale.CHINA).format(Date(dayMillis)), fontWeight = FontWeight.SemiBold)
                        Text("+${compactMoney(dayRows.sumIncome())}", style = MaterialTheme.typography.bodySmall)
                        Text("-${compactMoney(dayRows.sumExpense())}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ButtonRow(options: List<String>, selected: String, onSelected: (String) -> Unit) {
    if (options.isEmpty()) {
        Text("暂无可选项")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    OutlinedButton(onClick = { onSelected(option) }, modifier = Modifier.weight(1f)) {
                        Text(if (option == selected) "$option ✓" else option)
                    }
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun WarmPanel(
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun MetricGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) -> WarmMetric(label = label, value = value, modifier = Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MascotPairPreview(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MascotOnboardingTopImage(mode = ThemeMode.YIER, modifier = Modifier.height(92.dp))
        MascotOnboardingTopImage(mode = ThemeMode.BUBU, modifier = Modifier.height(92.dp))
    }
}

@Composable
private fun ThemePartnerButton(
    title: String,
    description: String,
    optionMode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val p = paletteFor(optionMode)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) p.surfaceAlt else p.surface),
        border = BorderStroke(1.dp, if (selected) p.primary.copy(alpha = 0.42f) else p.secondary.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MascotThemeCardImage(mode = optionMode, modifier = Modifier.height(56.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            Text(if (selected) "已选择" else "选择", color = p.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WarmMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(12.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RankingPanel(title: String, rows: List<Pair<String, Double>>) {
    WarmPanel {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (rows.isEmpty()) {
            Text("暂无数据")
        } else {
            val total = rows.sumOf { it.second }.coerceAtLeast(0.01)
            rows.take(5).forEach { row ->
                val pct = (row.second / total).coerceIn(0.0, 1.0)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = row.first, modifier = Modifier.weight(1f))
                    Text(text = money(row.second), modifier = Modifier.width(88.dp), textAlign = TextAlign.End)
                    Text(text = "${"%.0f".format(pct * 100)}%", modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                }
                LinearProgressIndicator(progress = pct.toFloat(), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun openNotificationListenerSettings(context: android.content.Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun openAppSettings(context: android.content.Context) {
    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(appDetails) }.onFailure {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openBatterySettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            openAppSettings(context)
        }
    }
}

private fun PendingTransactionEntity.isHiddenAmount(): Boolean =
    amount <= 0.0 && reason.contains("金额")

private fun List<TransactionEntity>.sumIncome(): Double =
    filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

private fun List<TransactionEntity>.sumExpense(): Double =
    filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

private fun List<TransactionEntity>.inRange(start: Long, end: Long): List<TransactionEntity> =
    filter { it.transactionTime in start..end }

private fun List<TransactionEntity>.rankBy(key: (TransactionEntity) -> String): List<Pair<String, Double>> =
    groupBy(key).mapValues { it.value.sumOf { tx -> tx.amount } }.toList().sortedByDescending { it.second }

private const val DayMillis: Long = 24 * 60 * 60 * 1000L

private fun calculateBudgetAnalysis(
    transactions: List<TransactionEntity>,
    settings: BudgetAnalysisSettings = BudgetAnalysisSettings(),
    now: Long = System.currentTimeMillis()
): BudgetAnalysis {
    val expenseRows = transactions.filter { it.type == TransactionType.EXPENSE }
    val recent7 = expenseRows.filter { it.transactionTime >= now - 7 * DayMillis }
    val recent30 = expenseRows.filter { it.transactionTime >= now - 30 * DayMillis }
    val recent7Daily = recent7.sumOf { it.amount } / 7.0
    val recent30Daily = recent30.sumOf { it.amount } / 30.0
    val daysUntilIncome = daysUntilNextIncome(now, settings.nextIncomeDate)
    val projectedDaily = if (recent7Daily > recent30Daily * 1.25) recent7Daily else recent30Daily
    val expectedExpense = projectedDaily * daysUntilIncome
    val recommendedReserve = maxOf(settings.minimumSafeBalance, recent7Daily * 3)
    val currentBalance = transactions.sumIncome() - transactions.sumExpense()
    val oldestExpense = expenseRows.minOfOrNull { it.transactionTime }
    val hasEnoughData = expenseRows.size >= 3 && oldestExpense != null && now - oldestExpense >= 7 * DayMillis
    val riskLevel = when {
        !hasEnoughData -> "数据不足"
        currentBalance >= expectedExpense + recommendedReserve -> "安全"
        currentBalance >= expectedExpense -> "稍紧"
        else -> "风险较高"
    }

    return BudgetAnalysis(
        hasEnoughData = hasEnoughData,
        riskLevel = riskLevel,
        daysUntilIncome = daysUntilIncome,
        recent7DailyExpense = recent7Daily,
        recent30DailyExpense = recent30Daily,
        expectedExpenseBeforeIncome = expectedExpense,
        currentBalance = currentBalance,
        recommendedReserve = recommendedReserve,
        monthExpenseSpeed = monthExpenseSpeed(expenseRows, recent30Daily, now, hasEnoughData),
        topExpenseCategory = topExpenseCategory(recent30.ifEmpty { expenseRows }),
        recentTrend = recentExpenseTrend(recent7Daily, recent30Daily, hasEnoughData),
        anomalyCategory = anomalyCategory(expenseRows, now),
        expenseRecordCount = expenseRows.size
    )
}

private fun analyzeWantedPurchase(
    analysis: BudgetAnalysis,
    productName: String,
    priceText: String,
    category: String
): WantPurchaseAdvice {
    val price = priceText.trim()
        .replace("¥", "")
        .replace("￥", "")
        .replace(",", "")
        .trim()
        .toDoubleOrNull()
    val cleanName = productName.trim().ifBlank { "该商品" }
    val cleanCategory = category.trim().ifBlank { "未分类" }
    if (price == null || price <= 0.0) {
        return WantPurchaseAdvice(
            conclusion = "请输入有效价格",
            reason = "填写预计价格后，才能结合到账前支出和建议保留余额进行判断。"
        )
    }
    if (!analysis.hasEnoughData) {
        return WantPurchaseAdvice(
            conclusion = "数据不足，仅供参考",
            reason = "当前支出记录只有 ${analysis.expenseRecordCount} 笔，历史天数或样本太少。建议继续记录几笔后，再判断 $cleanName 是否适合买。"
        )
    }

    val balanceAfterPurchase = analysis.currentBalance - price
    val requiredBalance = analysis.expectedExpenseBeforeIncome + analysis.recommendedReserve
    val conclusion = when {
        balanceAfterPurchase >= requiredBalance -> "可以买"
        balanceAfterPurchase >= analysis.expectedExpenseBeforeIncome -> "谨慎购买"
        else -> "建议推迟"
    }
    val reason = when (conclusion) {
        "可以买" -> "你最近消费速度${analysis.monthExpenseSpeed}，购买 $cleanName 后预计结余为 ${money(balanceAfterPurchase)}，仍能覆盖到账前预计支出 ${money(analysis.expectedExpenseBeforeIncome)} 和建议保留余额 ${money(analysis.recommendedReserve)}。"
        "谨慎购买" -> "购买 $cleanName 后预计结余为 ${money(balanceAfterPurchase)}，还能覆盖到账前预计支出约 ${money(analysis.expectedExpenseBeforeIncome)}，但会低于建议保留余额 ${money(analysis.recommendedReserve)}，${cleanCategory} 类支出建议控制频率。"
        else -> "根据你最近 30 天的消费习惯，到账前预计还需要支出约 ${money(analysis.expectedExpenseBeforeIncome)}。购买 $cleanName 后预计结余为 ${money(balanceAfterPurchase)}，低于到账前预计支出，因此建议推迟购买。"
    }
    return WantPurchaseAdvice(conclusion = conclusion, reason = reason)
}

private fun daysUntilNextIncome(now: Long, nextIncomeDate: String): Int {
    val todayStart = startOfDay(now)
    val target = DateRangeCalculator.parseDateTime(nextIncomeDate, "00:00").millis
        ?: DateRangeCalculator.parseDateTime(defaultBudgetNextIncomeDate(now), "00:00").millis
        ?: todayStart
    return ((target - todayStart) / DayMillis).toInt().coerceAtLeast(0)
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

private fun monthExpenseSpeed(
    expenseRows: List<TransactionEntity>,
    recent30Daily: Double,
    now: Long,
    hasEnoughData: Boolean
): String {
    if (!hasEnoughData || recent30Daily <= 0.0) return "数据不足"
    val monthExpense = expenseRows
        .filter { it.transactionTime in startOfMonth(now)..endOfMonth(now) }
        .sumOf { it.amount }
    val dayOfMonth = Calendar.getInstance(Locale.CHINA).apply { timeInMillis = now }.get(Calendar.DAY_OF_MONTH)
    val monthDaily = monthExpense / dayOfMonth.coerceAtLeast(1)
    return when {
        monthDaily < recent30Daily * 0.8 -> "偏低"
        monthDaily > recent30Daily * 1.2 -> "偏高"
        else -> "正常"
    }
}

private fun topExpenseCategory(expenseRows: List<TransactionEntity>): String =
    expenseRows
        .groupBy { it.categoryPrimary.ifBlank { "未分类" } }
        .maxByOrNull { (_, rows) -> rows.sumOf { it.amount } }
        ?.key ?: "暂无"

private fun recentExpenseTrend(recent7Daily: Double, recent30Daily: Double, hasEnoughData: Boolean): String =
    when {
        !hasEnoughData || recent30Daily <= 0.0 -> "数据不足"
        recent7Daily < recent30Daily * 0.85 -> "降低"
        recent7Daily > recent30Daily * 1.15 -> "升高"
        else -> "接近"
    }

private fun anomalyCategory(expenseRows: List<TransactionEntity>, now: Long): String {
    val recent7 = expenseRows.filter { it.transactionTime >= now - 7 * DayMillis }
    val previous23 = expenseRows.filter { it.transactionTime >= now - 30 * DayMillis && it.transactionTime < now - 7 * DayMillis }
    val previousByCategory = previous23.groupBy { it.categoryPrimary.ifBlank { "未分类" } }.mapValues { (_, rows) -> rows.sumOf { it.amount } / 23.0 }
    return recent7
        .groupBy { it.categoryPrimary.ifBlank { "未分类" } }
        .map { (category, rows) ->
            val recentDaily = rows.sumOf { it.amount } / 7.0
            val previousDaily = previousByCategory[category] ?: 0.0
            category to (recentDaily to previousDaily)
        }
        .firstOrNull { (_, pair) ->
            val (recentDaily, previousDaily) = pair
            (previousDaily > 0.0 && recentDaily > previousDaily * 1.6 && recentDaily - previousDaily >= 10.0) ||
                (previousDaily == 0.0 && recentDaily >= 30.0)
        }
        ?.first ?: "暂无明显异常"
}

@Composable
private fun OverviewRow(label: String, rows: List<TransactionEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Text("收入 ${money(rows.sumIncome())}", modifier = Modifier.weight(1f))
            Text("支出 ${money(rows.sumExpense())}", modifier = Modifier.weight(1f))
            Text("结余 ${money(rows.sumIncome() - rows.sumExpense())}", modifier = Modifier.weight(1f))
        }
    }
}

private fun startOfDay(value: Long): Long = Calendar.getInstance(Locale.CHINA).apply {
    timeInMillis = value
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun endOfDay(value: Long): Long = Calendar.getInstance(Locale.CHINA).apply {
    timeInMillis = startOfDay(value)
    add(Calendar.DAY_OF_MONTH, 1)
    add(Calendar.MILLISECOND, -1)
}.timeInMillis

private fun startOfWeek(value: Long): Long = Calendar.getInstance(Locale.CHINA).apply {
    timeInMillis = value
    firstDayOfWeek = Calendar.MONDAY
    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun endOfWeek(value: Long): Long = Calendar.getInstance(Locale.CHINA).apply {
    timeInMillis = startOfWeek(value)
    add(Calendar.DAY_OF_MONTH, 7)
    add(Calendar.MILLISECOND, -1)
}.timeInMillis

private fun startOfMonth(value: Long): Long = Calendar.getInstance(Locale.CHINA).apply {
    timeInMillis = value
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun endOfMonth(value: Long): Long = Calendar.getInstance(Locale.CHINA).apply {
    timeInMillis = startOfMonth(value)
    add(Calendar.MONTH, 1)
    add(Calendar.MILLISECOND, -1)
}.timeInMillis

private fun shiftDate(value: Long, range: TimeRangeType, amount: Int): Long =
    Calendar.getInstance(Locale.CHINA).apply {
        timeInMillis = value
        when (range) {
            TimeRangeType.TODAY -> add(Calendar.DAY_OF_MONTH, amount)
            TimeRangeType.WEEK -> add(Calendar.WEEK_OF_YEAR, amount)
            TimeRangeType.MONTH -> add(Calendar.MONTH, amount)
            TimeRangeType.YEAR -> add(Calendar.YEAR, amount)
            TimeRangeType.ALL, TimeRangeType.CUSTOM -> Unit
        }
    }.timeInMillis

private fun daysInMonth(value: Long): List<Long> {
    val cal = Calendar.getInstance(Locale.CHINA).apply {
        timeInMillis = startOfMonth(value)
    }
    val count = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return (1..count).map { day ->
        (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.timeInMillis
    }
}

private fun previousLabel(range: TimeRangeType): String = when (range) {
    TimeRangeType.TODAY -> "前一天"
    TimeRangeType.WEEK -> "上一周"
    TimeRangeType.MONTH -> "上一月"
    TimeRangeType.YEAR -> "上一年"
    TimeRangeType.ALL, TimeRangeType.CUSTOM -> ""
}

private fun nextLabel(range: TimeRangeType): String = when (range) {
    TimeRangeType.TODAY -> "后一天"
    TimeRangeType.WEEK -> "下一周"
    TimeRangeType.MONTH -> "下一月"
    TimeRangeType.YEAR -> "下一年"
    TimeRangeType.ALL, TimeRangeType.CUSTOM -> ""
}

private fun firstPrimary(type: TransactionType, categories: List<CategoryGroup>): String =
    categories.firstOrNull { it.type == type }?.primary ?: CategoryCatalog.firstPrimary(type)

private fun firstSecondary(type: TransactionType, primary: String, categories: List<CategoryGroup>): String =
    categories.firstOrNull { it.type == type && it.primary == primary }?.secondary?.firstOrNull()
        ?: CategoryCatalog.firstSecondary(type, primary)

private fun hiddenCategoryLabel(key: String): String {
    val parts = key.split("|")
    val type = runCatching { TransactionType.valueOf(parts.getOrElse(0) { "" }) }.getOrNull()
    val primary = parts.getOrElse(1) { "" }
    val secondary = parts.getOrElse(2) { "" }
    return listOfNotNull(type?.let(::typeLabel), primary.ifBlank { null }, secondary.ifBlank { null }).joinToString(" / ")
}

private fun typeLabel(type: TransactionType): String =
    if (type == TransactionType.EXPENSE) "支出" else "收入"

private fun money(value: Double): String = "${if (value < 0) "-¥" else "¥"}${"%.2f".format(abs(value))}"

private fun TransactionEntity.matchesTransactionQuery(query: String): Boolean {
    val keyword = query.trim().lowercase(Locale.CHINA)
    if (keyword.isBlank()) return true
    return listOf(
        merchant,
        note,
        account,
        categoryPrimary,
        categorySecondary,
        money(amount),
        "%.2f".format(amount),
        amount.toString()
    ).any { it.lowercase(Locale.CHINA).contains(keyword) }
}

private fun writeTransactionsCsv(context: android.content.Context, uri: Uri, transactions: List<TransactionEntity>) {
    val output = context.contentResolver.openOutputStream(uri) ?: error("Cannot open CSV output")
    output.bufferedWriter(Charsets.UTF_8).use { writer ->
        writer.write(transactions.toCsv())
    }
}

private fun List<TransactionEntity>.toCsv(): String {
    val header = listOf("日期时间", "类型", "金额", "大类", "细类", "商户/对方", "账户", "备注", "来源")
    val rows = sortedByDescending { it.transactionTime }.map { tx ->
        listOf(
            dateTime(tx.transactionTime),
            typeLabel(tx.type),
            "%.2f".format(tx.amount),
            tx.categoryPrimary,
            tx.categorySecondary,
            tx.merchant,
            tx.account,
            tx.note,
            tx.sourceApp.ifBlank { tx.sourcePackage }
        )
    }
    return (listOf(header) + rows).joinToString("\n") { row ->
        row.joinToString(",") { csvEscape(it) }
    }
}

private fun csvEscape(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"$escaped\""
    } else {
        escaped
    }
}

private data class CsvParseResult(
    val rows: List<CsvTransactionDraft>,
    val failedCount: Int,
    val failureReasons: List<String>
)

private fun readTransactionsCsv(context: android.content.Context, uri: Uri): CsvParseResult {
    val input = context.contentResolver.openInputStream(uri) ?: error("无法读取选择的 CSV 文件")
    val text = input.use { String(it.readBytes(), Charsets.UTF_8) }.removePrefix("\uFEFF")
    val records = parseCsvRecords(text).filter { row -> row.any { it.isNotBlank() } }
    if (records.isEmpty()) return CsvParseResult(emptyList(), 0, emptyList())
    val headers = records.first().map { normalizeCsvHeader(it) }
    val rows = mutableListOf<CsvTransactionDraft>()
    var failed = 0
    val reasons = mutableListOf<String>()
    records.drop(1).forEachIndexed { index, record ->
        val lineNumber = index + 2
        val parsed = parseCsvTransactionRow(headers, record)
        if (parsed == null) {
            failed += 1
            if (reasons.size < 3) reasons += "第 $lineNumber 行缺少必要字段或格式不正确"
        } else {
            rows += parsed
        }
    }
    return CsvParseResult(rows, failed, reasons)
}

private fun parseCsvTransactionRow(headers: List<String>, row: List<String>): CsvTransactionDraft? {
    fun value(vararg names: String): String =
        names.firstNotNullOfOrNull { name ->
            val index = headers.indexOf(normalizeCsvHeader(name))
            row.getOrNull(index)?.trim()?.takeIf { it.isNotBlank() }
        }.orEmpty()

    val explicitDateTime = value("日期时间", "交易时间", "记账时间", "datetime")
    val explicitDate = value("日期", "date")
    val genericTime = value("时间", "time")
    val dateText = explicitDateTime.ifBlank { explicitDate.ifBlank { genericTime } }
    val timeText = if (explicitDateTime.isBlank() && explicitDate.isNotBlank()) genericTime else ""
    val amountText = value("金额", "交易金额", "money", "amount")
    val amount = parseCsvAmount(amountText) ?: return null
    val type = parseCsvType(value("类型", "收支类型", "收入支出", "type"), amountText) ?: return null
    val transactionTime = parseCsvDateMillis(dateText, timeText) ?: return null
    val primary = value("大类", "分类", "category", "一级分类").ifBlank { "其他" }
    val secondary = value("细类", "子分类", "subcategory", "二级分类").ifBlank { "未分类" }
    return CsvTransactionDraft(
        transactionTime = transactionTime,
        type = type,
        amount = kotlin.math.abs(amount),
        categoryPrimary = primary,
        categorySecondary = secondary,
        merchant = value("商户/对方", "商户", "对方", "交易对象", "merchant", "payee").ifBlank { "未知商户" },
        account = value("账户", "account", "支付账户").ifBlank { "未知" },
        note = value("备注", "note", "remark", "说明"),
        source = value("来源", "source").ifBlank { "CSV导入" }
    )
}

private fun parseCsvRecords(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val cell = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        when {
            ch == '"' && inQuotes && i + 1 < text.length && text[i + 1] == '"' -> {
                cell.append('"')
                i += 1
            }
            ch == '"' -> inQuotes = !inQuotes
            ch == ',' && !inQuotes -> {
                row += cell.toString()
                cell.clear()
            }
            (ch == '\n' || ch == '\r') && !inQuotes -> {
                if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i += 1
                row += cell.toString()
                rows += row.toList()
                row.clear()
                cell.clear()
            }
            else -> cell.append(ch)
        }
        i += 1
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) {
        row += cell.toString()
        rows += row.toList()
    }
    return rows
}

private fun normalizeCsvHeader(value: String): String =
    value.trim().removePrefix("\uFEFF").lowercase(Locale.CHINA).replace(Regex("\\s+"), "")

private fun parseCsvAmount(value: String): Double? =
    value.replace("¥", "")
        .replace("￥", "")
        .replace("元", "")
        .replace(",", "")
        .trim()
        .toDoubleOrNull()

private fun parseCsvType(value: String, amountText: String): TransactionType? {
    val clean = value.trim().lowercase(Locale.CHINA)
    return when {
        clean.contains("支出") || clean.contains("expense") || clean.contains("out") || clean.contains("debit") || clean.contains("消费") ->
            TransactionType.EXPENSE
        clean.contains("收入") || clean.contains("income") || clean.contains("in") || clean.contains("credit") || clean.contains("收款") ->
            TransactionType.INCOME
        amountText.trim().startsWith("-") -> TransactionType.EXPENSE
        amountText.trim().startsWith("+") -> TransactionType.INCOME
        else -> null
    }
}

private fun parseCsvDateMillis(dateText: String, timeText: String): Long? {
    val cleanDate = dateText.trim()
    if (cleanDate.isBlank()) return null
    val candidates = mutableListOf<String>().apply {
        if (timeText.isNotBlank() && !cleanDate.contains(":")) add("$cleanDate ${timeText.trim()}")
        add(cleanDate)
    }.distinct()
    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy.MM.dd HH:mm:ss",
        "yyyy.MM.dd HH:mm",
        "yyyy年M月d日 HH:mm:ss",
        "yyyy年M月d日 HH:mm",
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "yyyy.MM.dd",
        "yyyy年M月d日"
    )
    candidates.forEach { candidate ->
        patterns.forEach { pattern ->
            val format = SimpleDateFormat(pattern, Locale.CHINA).apply { isLenient = false }
            runCatching { format.parse(candidate)?.time }.getOrNull()?.let { return it }
        }
    }
    return null
}

private fun csvImportMessage(imported: Int, skipped: Int, failed: Int, reasons: List<String>): String {
    val base = "成功导入 $imported 条，跳过重复 $skipped 条，解析失败 $failed 条"
    return if (reasons.isEmpty()) base else base + "；" + reasons.joinToString("；")
}

private fun compactMoney(value: Double): String = if (value == 0.0) "0" else "%.0f".format(value)

private fun date(value: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(value))

private fun time(value: Long): String = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(value))

private fun dateTime(value: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(value))

private fun supportedThemeMode(mode: ThemeMode): ThemeMode = when (mode) {
    ThemeMode.BUBU -> ThemeMode.BUBU
    // COUPLE can still arrive from old stored preferences; the current UI maps it to YIER.
    ThemeMode.YIER, ThemeMode.COUPLE -> ThemeMode.YIER
}
