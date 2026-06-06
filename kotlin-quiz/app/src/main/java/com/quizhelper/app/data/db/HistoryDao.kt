package com.quizhelper.app.data.db

import androidx.room.*
import com.quizhelper.app.data.model.HistoryDetail
import com.quizhelper.app.data.model.HistoryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history_records ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryRecord>>

    @Query("SELECT * FROM history_records ORDER BY timestamp DESC")
    suspend fun getHistoryList(): List<HistoryRecord>

    @Query("SELECT * FROM history_records WHERE id = :id")
    suspend fun getHistoryById(id: String): HistoryRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(record: HistoryRecord)

    @Query("DELETE FROM history_records")
    suspend fun deleteAllHistory()

    @Query("SELECT COUNT(*) FROM history_records")
    suspend fun getHistoryCount(): Int

    @Query("SELECT * FROM history_details WHERE history_id = :historyId")
    suspend fun getDetailsForHistory(historyId: String): List<HistoryDetail>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<HistoryDetail>)

    @Transaction
    suspend fun insertFullRecord(record: HistoryRecord, details: List<HistoryDetail>) {
        insertHistory(record)
        insertDetails(details)
    }
}
