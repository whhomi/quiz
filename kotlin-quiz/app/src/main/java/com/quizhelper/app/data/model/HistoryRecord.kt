package com.quizhelper.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "history_records")
data class HistoryRecord(
    @PrimaryKey val id: String,
    val mode: String,
    val timestamp: Long,
    @ColumnInfo(name = "total_count") val totalCount: Int,
    @ColumnInfo(name = "correct_count") val correctCount: Int,
    @ColumnInfo(name = "answered_count") val answeredCount: Int,
    val score: Double,
    @ColumnInfo(name = "max_score") val maxScore: Double? = null,
    val duration: Int,
    val breakdown: String? = null
)

@Entity(
    tableName = "history_details",
    foreignKeys = [
        ForeignKey(
            entity = HistoryRecord::class,
            parentColumns = ["id"],
            childColumns = ["history_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("history_id")]
)
data class HistoryDetail(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "history_id") val historyId: String,
    @ColumnInfo(name = "question_id") val questionId: Long,
    @ColumnInfo(name = "user_answer") val userAnswer: String,
    @ColumnInfo(name = "is_correct") val isCorrect: Boolean
) {
    fun getUserAnswerList(): List<Int> =
        if (userAnswer.isBlank()) emptyList() else userAnswer.split(",").mapNotNull { it.trim().toIntOrNull() }
}
