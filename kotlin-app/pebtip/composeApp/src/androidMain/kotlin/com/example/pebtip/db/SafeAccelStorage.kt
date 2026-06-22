package com.example.pebtip.db

import com.example.pebtip.AccelRecord
import com.example.pebtip.AccelStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/*
 * Wraps AccelStorage to handle database errors by providing fallback empty values instead of crashing.
 */
class SafeAccelStorage(private val delegate: AccelStorage) : AccelStorage {

    override val totalCount: Flow<Int> = delegate.totalCount
        .catch { emit(0) }

    override val recentSamples: Flow<List<AccelRecord>> = delegate.recentSamples
        .catch { emit(emptyList()) }

    override suspend fun insertAll(records: List<AccelRecord>) {
        delegate.insertAll(records)
    }

    override suspend fun clearAll() {
        delegate.clearAll()
    }
}
