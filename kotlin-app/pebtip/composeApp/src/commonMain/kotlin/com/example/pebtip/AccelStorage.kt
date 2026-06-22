package com.example.pebtip

import kotlinx.coroutines.flow.Flow

interface AccelStorage {
    val totalCount: Flow<Int>
    val recentSamples: Flow<List<AccelRecord>>
    suspend fun insertAll(records: List<AccelRecord>)
    suspend fun clearAll()
}
