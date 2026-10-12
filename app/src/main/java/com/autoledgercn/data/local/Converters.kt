package com.autoledgercn.data.local

import androidx.room.TypeConverter
import com.autoledgercn.domain.model.SourceType
import com.autoledgercn.domain.model.TransactionType

class Converters {
    @TypeConverter fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
    @TypeConverter fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter fun toSourceType(value: String): SourceType = SourceType.valueOf(value)
    @TypeConverter fun fromSourceType(value: SourceType): String = value.name
}
