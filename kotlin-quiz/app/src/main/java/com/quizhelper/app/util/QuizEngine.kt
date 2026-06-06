package com.quizhelper.app.util

import com.quizhelper.app.data.model.*
import kotlin.math.roundToInt
import kotlin.random.Random

object QuizEngine {

    fun <T> shuffle(list: List<T>): List<T> {
        val result = list.toMutableList()
        for (i in result.size - 1 downTo 1) {
            val j = Random.nextInt(i + 1)
            val temp = result[i]; result[i] = result[j]; result[j] = temp
        }
        return result
    }

    fun judge(userAnswer: List<Int>, correctAnswer: List<Int>): Boolean {
        if (userAnswer.size != correctAnswer.size) return false
        return userAnswer.sorted() == correctAnswer.sorted()
    }

    fun createSession(
        questions: List<Question>,
        random: Boolean = true,
        mode: QuizMode = QuizMode.PRACTICE,
        timeLimit: Int? = null
    ): QuizSession {
        val ordered = if (random) shuffle(questions) else questions.toList()
        return QuizSession(questions = ordered, mode = mode, timeLimitSeconds = timeLimit, randomOrder = random)
    }

    fun getCurrentQuestion(session: QuizSession): Question? = session.questions.getOrNull(session.currentIndex)

    fun submitAnswer(session: QuizSession, selectedAnswers: List<Int>): Judgment {
        val q = getCurrentQuestion(session) ?: throw IllegalStateException("No question")
        val isCorrect = judge(selectedAnswers.sorted(), q.getAnswerList().sorted())
        session.answers[q.id] = selectedAnswers
        return Judgment(isCorrect = isCorrect, correctAnswer = q.getAnswerList(), questionId = q.id)
    }

    fun goToQuestion(session: QuizSession, index: Int): Boolean {
        if (index < 0 || index >= session.questions.size) return false
        session.currentIndex = index; return true
    }

    fun nextQuestion(session: QuizSession): Boolean = goToQuestion(session, session.currentIndex + 1)
    fun prevQuestion(session: QuizSession): Boolean = goToQuestion(session, session.currentIndex - 1)

    fun getProgress(session: QuizSession): ProgressInfo {
        val total = session.questions.size
        val answered = session.answers.size
        return ProgressInfo(session.currentIndex + 1, total, answered, total - answered)
    }

    private fun countCorrect(session: QuizSession): Int {
        return session.answers.count { (qId, userAns) ->
            val q = session.questions.find { it.id == qId } ?: return@count false
            judge(userAns, q.getAnswerList())
        }
    }

    fun computeScore(session: QuizSession): ScoreResult {
        val answeredCount = session.answers.size
        val correctCount = countCorrect(session)
        val points = correctCount.toDouble()
        val maxPoints = session.questions.size.toDouble()
        val correctRate = if (answeredCount > 0) (correctCount.toDouble() / answeredCount * 100).roundToInt().toDouble() else 0.0
        val duration = ((session.endTime ?: System.currentTimeMillis()) - session.startTime) / 1000
        return ScoreResult(session.questions.size, answeredCount, correctCount, points, maxPoints, correctRate, duration.toInt())
    }

    fun buildResult(session: QuizSession): QuizResult {
        val score = computeScore(session)
        val details = session.answers.map { (qId, userAns) ->
            val q = session.questions.find { it.id == qId }!!
            AnswerDetail(qId, userAns, judge(userAns, q.getAnswerList()))
        }
        return QuizResult(
            sessionId = session.id, mode = QuizMode.PRACTICE,
            totalCount = score.totalCount, answeredCount = score.answeredCount,
            correctCount = score.correctCount, score = score.score,
            maxScore = score.maxScore, correctRate = score.correctRate,
            durationSeconds = score.durationSeconds, details = details
        )
    }

    fun selectExamQuestions(questions: List<Question>, examType: String = "full_random"): List<Question> {
        val selected = mutableListOf<Question>()
        var counter = 1

        fun pickAndTag(type: QuestionType, count: Int) {
            val pool = shuffle(questions.filter { it.type == type })
            pool.take(minOf(count, pool.size)).forEach { q ->
                selected.add(q.copy(questionId = "eq${counter++}"))
            }
        }

        if (examType == "grouped") {
            pickAndTag(QuestionType.SINGLE, 60)
            pickAndTag(QuestionType.MULTIPLE, 100)
            pickAndTag(QuestionType.BOOLEAN, 40)
        } else {
            pickAndTag(QuestionType.SINGLE, 60)
            pickAndTag(QuestionType.MULTIPLE, 100)
            pickAndTag(QuestionType.BOOLEAN, 40)
            return shuffle(selected)
        }
        return selected
    }

    private fun computeTypeBreakdown(session: QuizSession, type: QuestionType, pts: Double): TypeBreakdown {
        val typeQs = session.questions.filter { it.type == type }
        val correct = typeQs.count { q -> session.answers[q.id]?.let { judge(it, q.getAnswerList()) } ?: false }
        return TypeBreakdown(typeQs.size, correct, correct * pts)
    }

    fun computeExamScore(session: QuizSession): ExamScoreResult {
        val pts = 0.5
        val answeredCount = session.answers.size
        val totalCount = session.questions.size
        val maxScore = totalCount * pts
        val correctCount = countCorrect(session)
        val examScore = correctCount * pts
        val breakdown = ExamBreakdown(
            single = computeTypeBreakdown(session, QuestionType.SINGLE, pts),
            multiple = computeTypeBreakdown(session, QuestionType.MULTIPLE, pts),
            boolean = computeTypeBreakdown(session, QuestionType.BOOLEAN, pts)
        )
        val duration = ((session.endTime ?: System.currentTimeMillis()) - session.startTime) / 1000
        return ExamScoreResult(totalCount, answeredCount, correctCount, examScore, maxScore, duration.toInt(), breakdown)
    }

    fun buildExamResult(session: QuizSession): QuizResult {
        val score = computeExamScore(session)
        val details = session.answers.map { (qId, userAns) ->
            val q = session.questions.find { it.id == qId }!!
            AnswerDetail(qId, userAns, judge(userAns, q.getAnswerList()))
        }
        val correctRate = if (score.totalCount > 0) (score.correctCount.toDouble() / score.totalCount * 100).roundToInt().toDouble() else 0.0
        return QuizResult(
            sessionId = session.id, mode = QuizMode.EXAM,
            totalCount = score.totalCount, answeredCount = score.answeredCount,
            correctCount = score.correctCount, score = score.examScore,
            maxScore = score.maxScore, correctRate = correctRate,
            durationSeconds = score.durationSeconds,
            breakdown = score.breakdown, details = details
        )
    }
}

data class ProgressInfo(val current: Int, val total: Int, val answered: Int, val unanswered: Int)
data class ScoreResult(val totalCount: Int, val answeredCount: Int, val correctCount: Int, val score: Double, val maxScore: Double, val correctRate: Double, val durationSeconds: Int)
data class ExamScoreResult(val totalCount: Int, val answeredCount: Int, val correctCount: Int, val examScore: Double, val maxScore: Double, val durationSeconds: Int, val breakdown: ExamBreakdown)
