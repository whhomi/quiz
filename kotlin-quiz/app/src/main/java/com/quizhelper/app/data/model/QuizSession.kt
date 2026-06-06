package com.quizhelper.app.data.model

data class QuizSession(
    val id: String = generateSessionId(),
    val questions: List<Question>,
    var currentIndex: Int = 0,
    val answers: MutableMap<Long, List<Int>> = mutableMapOf(),
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    var isFinished: Boolean = false,
    val mode: QuizMode = QuizMode.PRACTICE,
    val timeLimitSeconds: Int? = null,
    val randomOrder: Boolean = true
) {
    companion object {
        private var counter = 0L
        fun generateSessionId(): String {
            counter++
            return "sess_${System.currentTimeMillis()}_$counter"
        }
    }
}

enum class QuizMode { PRACTICE, EXAM }

data class QuizResult(
    val sessionId: String,
    val mode: QuizMode,
    val timestamp: Long = System.currentTimeMillis(),
    val totalCount: Int,
    val answeredCount: Int,
    val correctCount: Int,
    val score: Double,
    val maxScore: Double? = null,
    val durationSeconds: Int,
    val breakdown: ExamBreakdown? = null,
    val details: List<AnswerDetail>
)

data class AnswerDetail(
    val questionId: Long,
    val userAnswer: List<Int>,
    val isCorrect: Boolean
)

data class ExamBreakdown(
    val single: TypeBreakdown,
    val multiple: TypeBreakdown,
    val boolean: TypeBreakdown
)

data class TypeBreakdown(
    val total: Int,
    val correct: Int,
    val score: Double
)

data class Judgment(
    val isCorrect: Boolean,
    val correctAnswer: List<Int>,
    val questionId: Long
)
