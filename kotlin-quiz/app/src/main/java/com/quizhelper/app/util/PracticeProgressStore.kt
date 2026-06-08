package com.quizhelper.app.util

import android.content.Context
import android.content.SharedPreferences
import com.quizhelper.app.data.model.Question
import com.quizhelper.app.data.model.QuizMode
import com.quizhelper.app.data.model.QuizSession
import com.google.gson.Gson

object PracticeProgressStore {
    private const val PREFS_NAME = "practice_progress"
    private const val KEY_PROGRESS = "sequential_progress"
    private val gson = Gson()

    data class PracticeProgress(
        val questions: List<Long>,
        val currentIndex: Int,
        val answers: Map<Long, List<Int>>
    )

    fun save(context: Context, progress: PracticeProgress) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROGRESS, gson.toJson(progress)).apply()
    }

    fun load(context: Context): PracticeProgress? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PROGRESS, null) ?: return null
        return try {
            gson.fromJson(json, PracticeProgress::class.java)
        } catch (e: Exception) { null }
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_PROGRESS).apply()
    }

    fun hasProgress(context: Context): Boolean = load(context) != null

    fun getProgressSummary(context: Context): Pair<Int, Int>? {
        val progress = load(context) ?: return null
        return Pair(progress.answers.size, progress.questions.size)
    }

    fun restoreSession(context: Context, allQuestions: List<Question>): QuizSession? {
        val progress = load(context) ?: return null
        val questionMap = allQuestions.associateBy { it.id }
        val orderedQuestions = progress.questions.mapNotNull { questionMap[it] }
        if (orderedQuestions.isEmpty()) return null

        val session = QuizSession(
            questions = orderedQuestions,
            currentIndex = progress.currentIndex,
            mode = QuizMode.PRACTICE,
            randomOrder = false
        )
        progress.answers.forEach { (qId, answer) ->
            orderedQuestions.find { it.id == qId }?.let { q ->
                session.answers[q.id] = answer
            }
        }
        return session
    }

    fun saveSession(context: Context, session: QuizSession) {
        val progress = PracticeProgress(
            questions = session.questions.map { it.id },
            currentIndex = session.currentIndex,
            answers = session.answers.toMap()
        )
        save(context, progress)
    }
}
