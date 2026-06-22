package com.example.pebtip.db

import com.example.pebtip.AccelRecord
import com.example.pebtip.AccelStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAccelStorage(private val dao: AccelSampleDao) : AccelStorage {

    override val totalCount: Flow<Int> = dao.observeCount()

    override val recentSamples: Flow<List<AccelRecord>> =
        dao.observeRecent().map { entities -> entities.map { it.toRecord() } }

    override suspend fun insertAll(records: List<AccelRecord>) {
        dao.insertAll(records.map { it.toEntity() })
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
