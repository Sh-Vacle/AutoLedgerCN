package com.autoledgercn.data.repository

import android.util.Log
import com.autoledgercn.data.entity.CategoryRuleEntity
import com.autoledgercn.data.entity.NotificationDiagnosticEntity
import com.autoledgercn.data.entity.PendingTransactionEntity
import com.autoledgercn.data.entity.RawNotificationEntity
import com.autoledgercn.data.entity.TransactionEntity
import com.autoledgercn.data.local.LedgerDao
import com.autoledgercn.domain.classifier.LocalRuleClassifier
import com.autoledgercn.domain.dedup.DeduplicationEngine
import com.autoledgercn.domain.model.DedupAction
import com.autoledgercn.domain.model.ParsedTransaction
import com.autoledgercn.domain.model.SourceKind
import com.autoledgercn.domain.model.SourceType
import com.autoledgercn.domain.model.TransactionType
import com.autoledgercn.domain.parser.Fingerprint
import com.autoledgercn.domain.parser.NotificationParser
import com.autoledgercn.domain.parser.SourceAppMapper
import com.autoledgercn.domain.parser.transactionCandidateDecision
import kotlinx.coroutines.flow.Flow

data class ProcessResult(
    val action: DedupAction,
    val transactionId: Long? = null,
    val reason: String = "",
    val finalAction: String = NotificationFinalAction.RECEIVED_ONLY
)

data class CsvTransactionDraft(
    val transactionTime: Long,
    val type: TransactionType,
    val amount: Double,
    val categoryPrimary: String,
    val categorySecondary: String,
    val merchant: String,
    val account: String,
    val note: String,
    val source: String = "CSV导入"
)

data class ImportTransactionsResult(
    val importedCount: Int,
    val skippedDuplicateCount: Int
)

object NotificationFinalAction {
    const val RECEIVED_ONLY = "RECEIVED_ONLY"
    const val IGNORED_NOT_MONITORED = "IGNORED_NOT_MONITORED"
    const val IGNORED_EMPTY_CONTENT = "IGNORED_EMPTY_CONTENT"
    const val IGNORED_MARKETING = "IGNORED_MARKETING"
    const val IGNORED_NON_TRANSACTION = "IGNORED_NON_TRANSACTION"
    const val PARSE_FAILED = "PARSE_FAILED"
    const val CREATED_TRANSACTION = "CREATED_TRANSACTION"
    const val CREATED_PENDING_COMPLETION = "CREATED_PENDING_COMPLETION"
    const val CREATED_PENDING_CONFIRMATION = "CREATED_PENDING_CONFIRMATION"
    const val MERGED_DUPLICATE = "MERGED_DUPLICATE"
    const val ERROR = "ERROR"
}

class LedgerRepository(
    private val dao: LedgerDao,
    private val parser: NotificationParser = NotificationParser(),
    private val classifier: LocalRuleClassifier = LocalRuleClassifier(),
    private val deduplicationEngine: DeduplicationEngine = DeduplicationEngine()
) {
    private companion object {
        const val TAG = "LedgerRepository"

        fun log(message: String) {
            runCatching { Log.d(TAG, message) }
        }
    }

    fun observeTransactions(): Flow<List<TransactionEntity>> = dao.observeTransactions()
    fun observePending(): Flow<List<PendingTransactionEntity>> = dao.observePending()
    fun observeCategoryRules(): Flow<List<CategoryRuleEntity>> = dao.observeCategoryRules()
    fun observeNotificationDiagnostics(limit: Int = 30): Flow<List<NotificationDiagnosticEntity>> =
        dao.observeNotificationDiagnostics(limit)

    suspend fun allTransactions(): List<TransactionEntity> = dao.getTransactions()

    suspend fun clearNotificationDiagnostics() = dao.clearNotificationDiagnostics()

    suspend fun recordNotificationDiagnostic(
        timeMillis: Long = System.currentTimeMillis(),
        appName: String,
        packageName: String,
        title: String,
        text: String,
        isMonitoredApp: Boolean,
        parserResult: String,
        finalAction: String,
        ignoreReason: String = ""
    ) {
        dao.insertNotificationDiagnostic(
            NotificationDiagnosticEntity(
                timeMillis = timeMillis,
                appName = appName.take(80),
                packageName = packageName.take(120),
                title = title.take(120),
                text = text.take(240),
                isMonitoredApp = isMonitoredApp,
                parserResult = parserResult.take(80),
                finalAction = finalAction,
                ignoreReason = ignoreReason.take(160)
            )
        )
        dao.trimNotificationDiagnostics(100)
    }

    suspend fun processNotification(
        packageName: String,
        appName: String,
        title: String,
        text: String,
        bigText: String,
        postTime: Long
    ): ProcessResult {
        log("processNotification package=$packageName appName=$appName title=$title text=$text")
        val candidate = transactionCandidateDecision(packageName, appName, title, text, bigText)
        if (!candidate.isCandidate) {
            log("ignored non-transaction notification package=$packageName reason=${candidate.diagnosticResult}")
            recordNotificationDiagnostic(
                appName = appName,
                packageName = packageName,
                title = title,
                text = text.ifBlank { bigText },
                isMonitoredApp = true,
                parserResult = candidate.diagnosticResult,
                finalAction = NotificationFinalAction.IGNORED_NON_TRANSACTION,
                ignoreReason = "非交易通知，已忽略"
            )
            return ProcessResult(
                DedupAction.IGNORE_RAW,
                reason = candidate.diagnosticResult,
                finalAction = NotificationFinalAction.IGNORED_NON_TRANSACTION
            )
        }
        val rawFingerprint = Fingerprint.raw(packageName, title, text, bigText, postTime)
        if (dao.rawFingerprintExists(rawFingerprint)) {
            log("ignored duplicate raw notification package=$packageName")
            recordNotificationDiagnostic(
                appName = appName,
                packageName = packageName,
                title = title,
                text = text.ifBlank { bigText },
                isMonitoredApp = true,
                parserResult = "RAW_DUPLICATE",
                finalAction = NotificationFinalAction.MERGED_DUPLICATE,
                ignoreReason = "Duplicate raw notification"
            )
            return ProcessResult(
                DedupAction.IGNORE_RAW,
                reason = "原始通知已存在",
                finalAction = NotificationFinalAction.MERGED_DUPLICATE
            )
        }
        val rawId = dao.insertRawNotification(
            RawNotificationEntity(
                packageName = packageName,
                appName = appName,
                title = title,
                text = text,
                bigText = bigText,
                postTime = postTime,
                rawFingerprint = rawFingerprint,
                createdAt = System.currentTimeMillis()
            )
        )
        if (title.isBlank() && text.isBlank() && bigText.isBlank()) {
            recordNotificationDiagnostic(
                appName = appName,
                packageName = packageName,
                title = title,
                text = "",
                isMonitoredApp = true,
                parserResult = "EMPTY_CONTENT",
                finalAction = NotificationFinalAction.IGNORED_EMPTY_CONTENT,
                ignoreReason = "Notification title and text are empty"
            )
            return ProcessResult(
                DedupAction.IGNORE_RAW,
                reason = "Empty notification content",
                finalAction = NotificationFinalAction.IGNORED_EMPTY_CONTENT
            )
        }
        val parsed = parser.parse(packageName, title, text, bigText, postTime, appName)
        if (parsed == null) {
            log("parser returned null package=$packageName")
            val isCustomApp = SourceAppMapper.sourceKind(packageName) == SourceKind.OTHER
            val shouldCreateHiddenAmountPending = !candidate.hasAmount &&
                parser.shouldCreateHiddenAmountPending(packageName, title, text, bigText, isCustomApp)
            log("shouldCreateHiddenAmountPending=$shouldCreateHiddenAmountPending package=$packageName isCustomApp=$isCustomApp")
            return if (shouldCreateHiddenAmountPending) {
                val now = System.currentTimeMillis()
                val pending = PendingTransactionEntity(
                    amount = 0.0,
                    type = TransactionType.EXPENSE,
                    categoryPrimary = "其他",
                    categorySecondary = "未分类",
                    merchant = appName.ifBlank { SourceAppMapper.appName(packageName) },
                    account = SourceAppMapper.accountFrom(packageName, "$title $text $bigText"),
                    sourceApp = appName.ifBlank { SourceAppMapper.appName(packageName) },
                    sourcePackage = packageName,
                    rawTitle = title,
                    rawText = text,
                    rawBigText = bigText,
                    transactionTime = postTime,
                    confidence = 0.35,
                    reason = "金额被隐藏，请手动补录",
                    possibleDuplicateTransactionId = null,
                    createdAt = now
                )
                val duplicate = findRecentDuplicatePendingCompletion(pending, now)
                if (duplicate != null) {
                    log("hidden amount pending duplicate ignored package=$packageName existing=${duplicate.id}")
                    recordNotificationDiagnostic(
                        appName = appName,
                        packageName = packageName,
                        title = title,
                        text = text.ifBlank { bigText },
                        isMonitoredApp = true,
                        parserResult = "HIDDEN_AMOUNT_PENDING_DUPLICATE",
                        finalAction = NotificationFinalAction.MERGED_DUPLICATE,
                        ignoreReason = "Duplicate pending completion notification within short window"
                    )
                    return ProcessResult(
                        DedupAction.IGNORE_RAW,
                        reason = "Duplicate pending completion notification within short window",
                        finalAction = NotificationFinalAction.MERGED_DUPLICATE
                    )
                }
                dao.insertPending(pending)
                log("hidden amount pending created package=$packageName sourceApp=${appName.ifBlank { SourceAppMapper.appName(packageName) }}")
                recordNotificationDiagnostic(
                    appName = appName,
                    packageName = packageName,
                    title = title,
                    text = text.ifBlank { bigText },
                    isMonitoredApp = true,
                    parserResult = "HIDDEN_AMOUNT_PENDING",
                    finalAction = NotificationFinalAction.CREATED_PENDING_COMPLETION,
                    ignoreReason = ""
                )
                ProcessResult(
                    DedupAction.PENDING_CONFIRMATION,
                    reason = "金额被隐藏，请手动补录",
                    finalAction = NotificationFinalAction.CREATED_PENDING_COMPLETION
                )
            } else {
                log("ignored unrecognized transaction notification package=$packageName title=$title text=$text")
                val action = if (looksLikeMarketing(title, text, bigText)) {
                    NotificationFinalAction.IGNORED_MARKETING
                } else {
                    NotificationFinalAction.PARSE_FAILED
                }
                recordNotificationDiagnostic(
                    appName = appName,
                    packageName = packageName,
                    title = title,
                    text = text.ifBlank { bigText },
                    isMonitoredApp = true,
                    parserResult = "PARSE_NULL",
                    finalAction = action,
                    ignoreReason = if (action == NotificationFinalAction.IGNORED_MARKETING) {
                        "Likely marketing or non-transaction notification"
                    } else {
                        "No parser matched and no hidden amount pending rule matched"
                    }
                )
                ProcessResult(DedupAction.IGNORE_RAW, reason = "不是可识别交易通知", finalAction = action)
            }
        }
        log("parser returned transaction package=$packageName amount=${parsed.amount} type=${parsed.type}")
        val result = saveParsed(parsed.copy(rawFingerprint = rawFingerprint), rawId)
        recordNotificationDiagnostic(
            appName = appName,
            packageName = packageName,
            title = title,
            text = text.ifBlank { bigText },
            isMonitoredApp = true,
            parserResult = "PARSED",
            finalAction = result.finalAction,
            ignoreReason = result.reason
        )
        return result
    }

    suspend fun saveParsed(parsed: ParsedTransaction, rawId: Long? = null): ProcessResult {
        val rules = dao.getEnabledCategoryRules()
        val classified = classifier.classify(parsed, rules)
        val enriched = parsed.copy(
            categoryPrimary = classified.categoryPrimary,
            categorySecondary = classified.categorySecondary,
            confidence = (parsed.confidence + classified.confidenceDelta).coerceIn(0.0, 1.0),
            transactionFingerprint = parsed.transactionFingerprint()
        )
        val exact = if (canUseTransactionFingerprint(enriched)) {
            dao.findTransactionByFingerprint(enriched.transactionFingerprint)
        } else {
            null
        }
        val candidates = dao.findCandidates(
            enriched.amount,
            enriched.type,
            enriched.transactionTime - 10 * 60_000L,
            enriched.transactionTime + 10 * 60_000L
        )
        val decision = deduplicationEngine.decide(enriched, exact, candidates)
        return when (decision.action) {
            DedupAction.MERGE_EXACT, DedupAction.MERGE_FUZZY -> {
                val target = requireNotNull(decision.target)
                dao.updateTransaction(deduplicationEngine.merge(target, enriched))
                rawId?.takeIf { it > 0 }?.let { dao.linkRawNotification(it, target.id) }
                ProcessResult(decision.action, target.id, decision.reason, NotificationFinalAction.MERGED_DUPLICATE)
            }
            DedupAction.PENDING_CONFIRMATION -> {
                dao.insertPending(enriched.toPending(decision.reason, decision.target?.id))
                ProcessResult(
                    DedupAction.PENDING_CONFIRMATION,
                    decision.target?.id,
                    decision.reason,
                    NotificationFinalAction.CREATED_PENDING_CONFIRMATION
                )
            }
            DedupAction.CREATE -> {
                val id = dao.insertTransaction(deduplicationEngine.toEntity(enriched, System.currentTimeMillis()))
                rawId?.takeIf { it > 0 }?.let { dao.linkRawNotification(it, id) }
                ProcessResult(DedupAction.CREATE, id, finalAction = NotificationFinalAction.CREATED_TRANSACTION)
            }
            DedupAction.IGNORE_RAW -> ProcessResult(DedupAction.IGNORE_RAW, finalAction = NotificationFinalAction.RECEIVED_ONLY)
        }
    }

    suspend fun addManualTransaction(
        amount: Double,
        type: TransactionType,
        primary: String,
        secondary: String,
        merchant: String,
        account: String,
        note: String,
        transactionTime: Long = System.currentTimeMillis()
    ) {
        val now = System.currentTimeMillis()
        val fingerprint = Fingerprint.transaction(amount, type, merchant, transactionTime, merchant, "MANUAL")
        dao.insertTransaction(
            TransactionEntity(
                amount = amount,
                type = type,
                categoryPrimary = primary,
                categorySecondary = secondary,
                merchant = merchant.ifBlank { "手动记录" },
                account = account.ifBlank { "未知" },
                sourceApp = "手动",
                sourcePackage = "manual",
                sourceType = SourceType.MANUAL,
                rawTitle = "",
                rawText = "",
                rawBigText = "",
                rawFingerprint = "manual-$now",
                transactionFingerprint = fingerprint,
                transactionTime = transactionTime,
                createdAt = now,
                confidence = 1.0,
                note = note
            )
        )
    }

    suspend fun deleteTransaction(id: Long) = dao.deleteTransaction(id)

    suspend fun updateTransaction(entity: TransactionEntity) = dao.updateTransaction(entity)

    suspend fun importTransactions(rows: List<CsvTransactionDraft>): ImportTransactionsResult {
        val existing = dao.getTransactions().toMutableList()
        var imported = 0
        var skippedDuplicate = 0
        val now = System.currentTimeMillis()
        rows.forEachIndexed { index, row ->
            if (!row.amount.isFinite() || row.amount <= 0.0) return@forEachIndexed
            val duplicate = existing.any { it.isLikelySameImportedRow(row) }
            if (duplicate) {
                skippedDuplicate += 1
            } else {
                val merchant = row.merchant.ifBlank { "未知商户" }
                val account = row.account.ifBlank { "未知" }
                val fingerprint = Fingerprint.transaction(
                    amount = row.amount,
                    type = row.type,
                    merchant = merchant,
                    transactionTime = row.transactionTime,
                    counterparty = merchant,
                    sourceRole = "IMPORT"
                )
                val entity = TransactionEntity(
                    amount = row.amount,
                    type = row.type,
                    categoryPrimary = row.categoryPrimary.ifBlank { "其他" },
                    categorySecondary = row.categorySecondary.ifBlank { "未分类" },
                    merchant = merchant,
                    account = account,
                    sourceApp = row.source.ifBlank { "CSV导入" },
                    sourcePackage = "csv-import",
                    sourceType = SourceType.IMPORT,
                    rawTitle = "",
                    rawText = "",
                    rawBigText = "",
                    rawFingerprint = "csv-import-$now-$index",
                    transactionFingerprint = fingerprint,
                    transactionTime = row.transactionTime,
                    createdAt = now,
                    confidence = 1.0,
                    note = row.note
                )
                val id = dao.insertTransaction(entity)
                existing += entity.copy(id = id)
                imported += 1
            }
        }
        return ImportTransactionsResult(imported, skippedDuplicate)
    }

    suspend fun confirmPendingAsNew(pending: PendingTransactionEntity) {
        if (pending.amount <= 0.0 && pending.reason.contains("金额被隐藏")) return
        val parsed = ParsedTransaction(
            amount = pending.amount,
            type = pending.type,
            merchant = pending.merchant,
            account = pending.account,
            sourceApp = pending.sourceApp,
            sourcePackage = pending.sourcePackage,
            rawTitle = pending.rawTitle,
            rawText = pending.rawText,
            rawBigText = pending.rawBigText,
            transactionTime = pending.transactionTime,
            confidence = pending.confidence,
            categoryPrimary = pending.categoryPrimary,
            categorySecondary = pending.categorySecondary,
            rawFingerprint = "pending-${pending.id}",
            transactionFingerprint = Fingerprint.transaction(
                pending.amount,
                pending.type,
                pending.merchant,
                pending.transactionTime,
                pending.merchant,
                SourceAppMapper.sourceKind(pending.sourcePackage).name
            )
        )
        dao.insertTransaction(deduplicationEngine.toEntity(parsed, System.currentTimeMillis()))
        dao.deletePending(pending.id)
    }

    suspend fun completePending(
        pending: PendingTransactionEntity,
        amount: Double,
        type: TransactionType,
        primary: String,
        secondary: String,
        merchant: String,
        account: String,
        note: String,
        transactionTime: Long = pending.transactionTime
    ) {
        if (amount <= 0.0) return
        val finalMerchant = merchant.ifBlank { pending.merchant }
        val finalAccount = account.ifBlank { pending.account }
        val fingerprint = Fingerprint.transaction(
            amount = amount,
            type = type,
            merchant = finalMerchant,
            transactionTime = transactionTime,
            counterparty = finalMerchant,
            sourceRole = SourceAppMapper.sourceKind(pending.sourcePackage).name
        )
        dao.insertTransaction(
            TransactionEntity(
                amount = amount,
                type = type,
                categoryPrimary = primary,
                categorySecondary = secondary,
                merchant = finalMerchant,
                account = finalAccount,
                sourceApp = pending.sourceApp,
                sourcePackage = pending.sourcePackage,
                sourceType = SourceType.NOTIFICATION,
                rawTitle = pending.rawTitle,
                rawText = pending.rawText,
                rawBigText = pending.rawBigText,
                rawFingerprint = "pending-${pending.id}",
                transactionFingerprint = fingerprint,
                transactionTime = transactionTime,
                createdAt = System.currentTimeMillis(),
                confidence = 0.95,
                note = note
            )
        )
        dao.deletePending(pending.id)
    }

    suspend fun mergePending(pending: PendingTransactionEntity) {
        val targetId = pending.possibleDuplicateTransactionId ?: return
        val target = dao.getTransaction(targetId) ?: return
        val parsed = pending.toParsed()
        dao.updateTransaction(deduplicationEngine.merge(target, parsed))
        dao.deletePending(pending.id)
    }

    suspend fun deletePending(id: Long) = dao.deletePending(id)

    suspend fun addCategoryRule(keyword: String, primary: String, secondary: String, type: TransactionType) {
        dao.insertCategoryRule(
            CategoryRuleEntity(
                keyword = keyword,
                categoryPrimary = primary,
                categorySecondary = secondary,
                transactionType = type,
                priority = 120,
                enabled = true,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCategoryRule(id: Long) = dao.deleteCategoryRule(id)

    private fun ParsedTransaction.toPending(reason: String, duplicateId: Long?): PendingTransactionEntity =
        PendingTransactionEntity(
            amount = amount,
            type = type,
            categoryPrimary = categoryPrimary,
            categorySecondary = categorySecondary,
            merchant = merchant,
            account = account,
            sourceApp = sourceApp,
            sourcePackage = sourcePackage,
            rawTitle = rawTitle,
            rawText = rawText,
            rawBigText = rawBigText,
            transactionTime = transactionTime,
            confidence = confidence,
            reason = reason,
            possibleDuplicateTransactionId = duplicateId,
            createdAt = System.currentTimeMillis()
        )

    private fun PendingTransactionEntity.toParsed(): ParsedTransaction =
        ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            account = account,
            sourceApp = sourceApp,
            sourcePackage = sourcePackage,
            rawTitle = rawTitle,
            rawText = rawText,
            rawBigText = rawBigText,
            transactionTime = transactionTime,
            confidence = confidence,
            categoryPrimary = categoryPrimary,
            categorySecondary = categorySecondary,
            rawFingerprint = "pending-$id",
            transactionFingerprint = Fingerprint.transaction(
                amount,
                type,
                merchant,
                transactionTime,
                merchant,
                SourceAppMapper.sourceKind(sourcePackage).name
            )
        )

    private suspend fun findRecentDuplicatePendingCompletion(
        pending: PendingTransactionEntity,
        now: Long
    ): PendingTransactionEntity? {
        val since = now - 10 * 60_000L
        return dao.findRecentHiddenAmountPending(pending.sourcePackage, since).firstOrNull { existing ->
            existing.isSameHiddenAmountNotification(pending)
        }
    }

    private fun PendingTransactionEntity.isSameHiddenAmountNotification(other: PendingTransactionEntity): Boolean {
        val closeInTime = kotlin.math.abs(transactionTime - other.transactionTime) <= 10 * 60_000L ||
            kotlin.math.abs(createdAt - other.createdAt) <= 10 * 60_000L
        return closeInTime &&
            sourcePackage == other.sourcePackage &&
            rawTitle.normalizedNotificationContent() == other.rawTitle.normalizedNotificationContent() &&
            normalizedPendingBody() == other.normalizedPendingBody()
    }

    private fun PendingTransactionEntity.normalizedPendingBody(): String {
        val textPart = rawText.normalizedNotificationContent()
        val bigTextPart = rawBigText.normalizedNotificationContent()
        return listOf(textPart, bigTextPart)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("|")
    }

    private fun String.normalizedNotificationContent(): String =
        trim().replace(Regex("\\s+"), "")

    private fun ParsedTransaction.transactionFingerprint(): String =
        Fingerprint.transaction(
            amount = amount,
            type = type,
            merchant = merchant,
            transactionTime = transactionTime,
            counterparty = merchant,
            sourceRole = SourceAppMapper.sourceKind(sourcePackage).name
        )

    private fun canUseTransactionFingerprint(parsed: ParsedTransaction): Boolean =
        parsed.type == TransactionType.EXPENSE || Fingerprint.hasConcreteCounterparty(parsed.merchant)

    private fun TransactionEntity.isLikelySameImportedRow(row: CsvTransactionDraft): Boolean {
        val closeTime = kotlin.math.abs(transactionTime - row.transactionTime) <= 5 * 60_000L
        val sameAmount = kotlin.math.abs(amount - row.amount) < 0.005
        val sameType = type == row.type
        val sameMerchant = merchant.normalizedImportText() == row.merchant.normalizedImportText()
        val sameAccount = account.normalizedImportText() == row.account.normalizedImportText()
        val sameNote = note.isNotBlank() &&
            row.note.isNotBlank() &&
            note.normalizedImportText() == row.note.normalizedImportText()
        return closeTime && sameAmount && sameType && sameMerchant && (sameAccount || sameNote)
    }

    private fun String.normalizedImportText(): String =
        trim().lowercase().replace(Regex("\\s+"), "")

    private fun looksLikeMarketing(title: String, text: String, bigText: String): Boolean {
        val content = "$title $text $bigText"
        val keywords = listOf("广告", "活动", "优惠", "红包", "会员", "秒杀", "直播", "优惠券", "促销")
        return keywords.any { content.contains(it) }
    }
}
