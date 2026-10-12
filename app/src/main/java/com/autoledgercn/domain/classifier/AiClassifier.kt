package com.autoledgercn.domain.classifier

import com.autoledgercn.domain.model.ClassifiedTransaction
import com.autoledgercn.domain.model.ParsedTransaction

interface AiClassifier {
    suspend fun classify(parsed: ParsedTransaction): ClassifiedTransaction?
}

class NoopAiClassifier : AiClassifier {
    override suspend fun classify(parsed: ParsedTransaction): ClassifiedTransaction? {
        return null
    }
}
