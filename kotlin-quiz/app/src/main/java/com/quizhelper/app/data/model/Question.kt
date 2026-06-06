package com.quizhelper.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class QuestionType {
    SINGLE, MULTIPLE, BOOLEAN
}

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    val type: QuestionType,
    val stem: String,
    val options: String,
    val answer: String,
    val analysis: String = "",
    @ColumnInfo(name = "import_time")
    val importTime: Long = System.currentTimeMillis()
) {
    fun getOptionsList(): List<String> =
        if (options.isBlank()) emptyList() else options.split("; ")

    fun getAnswerList(): List<Int> =
        if (answer.isBlank()) emptyList() else answer.split(",").mapNotNull { it.trim().toIntOrNull() }

    companion object {
        fun create(
            questionId: String, type: QuestionType, stem: String,
            options: List<String>, answer: List<Int>, analysis: String = ""
        ): Question = Question(
            questionId = questionId, type = type, stem = stem,
            options = options.joinToString("; "),
            answer = answer.joinToString(","),
            analysis = analysis
        )
    }
}

@Entity(tableName = "question_bank_meta")
data class QuestionBankMeta(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "total_count") val totalCount: Int = 0,
    @ColumnInfo(name = "single_count") val singleCount: Int = 0,
    @ColumnInfo(name = "multiple_count") val multipleCount: Int = 0,
    @ColumnInfo(name = "boolean_count") val booleanCount: Int = 0,
    @ColumnInfo(name = "import_time") val importTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "version") val version: Int = 1
)
