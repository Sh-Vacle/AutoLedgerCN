package com.autoledgercn.domain.model

enum class TransactionType { EXPENSE, INCOME }
enum class SourceType { NOTIFICATION, MANUAL, IMPORT }
enum class ThemeMode { YIER, BUBU, COUPLE }
enum class DedupAction { IGNORE_RAW, CREATE, MERGE_EXACT, MERGE_FUZZY, PENDING_CONFIRMATION }

enum class SourceKind {
    BANK, PAYMENT, ECOMMERCE, FOOD_DELIVERY, OTHER
}
