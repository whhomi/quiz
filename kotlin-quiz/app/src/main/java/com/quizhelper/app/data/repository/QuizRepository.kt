package com.quizhelper.app.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.quizhelper.app.data.db.AppDatabase
import com.quizhelper.app.data.model.*
import com.quizhelper.app.data.parser.ExcelParser
import com.quizhelper.app.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class QuizRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val questionDao = db.questionDao()
    private val historyDao = db.historyDao()
    private val gson = Gson()
    private val log = Logger.create("Repository")

    val bankMeta: Flow<QuestionBankMeta?> = questionDao.getBankMetaFlow()
    val allQuestions: Flow<List<Question>> = questionDao.getAllQuestions()

    suspend fun getBankMeta(): QuestionBankMeta? = questionDao.getBankMeta()
    suspend fun getAllQuestionsList(): List<Question> = questionDao.getAllQuestionsList()
    suspend fun getQuestionCount(): Int = questionDao.getQuestionCount()

    suspend fun importExcel(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val parseResult = ExcelParser.parse(context, uri)
            val questions = parseResult.questions

            if (questions.isNotEmpty()) {
                val singleCount = questions.count { it.type == QuestionType.SINGLE }
                val multipleCount = questions.count { it.type == QuestionType.MULTIPLE }
                val booleanCount = questions.count { it.type == QuestionType.BOOLEAN }
                val meta = QuestionBankMeta(
                    totalCount = questions.size,
                    singleCount = singleCount,
                    multipleCount = multipleCount,
                    booleanCount = booleanCount
                )

                questionDao.replaceAll(questions, meta)
                historyDao.deleteAllHistory()

                log.i("题库导入完成: ${questions.size} 题")
            }

            ImportResult(
                success = true,
                message = "成功导入 ${questions.size} 道题",
                totalCount = questions.size,
                singleCount = questions.count { it.type == QuestionType.SINGLE },
                multipleCount = questions.count { it.type == QuestionType.MULTIPLE },
                booleanCount = questions.count { it.type == QuestionType.BOOLEAN },
                warnings = parseResult.warnings
            )
        } catch (e: Exception) {
            log.e("导入失败", e)
            ImportResult(false, "导入失败: ${e.message ?: "未知错误"}")
        }
    }

    suspend fun clearAllData() {
        questionDao.deleteAllQuestions()
        questionDao.deleteBankMeta()
        historyDao.deleteAllHistory()
        log.i("全部数据已清除")
    }

    // ---- History ----

    val allHistory: Flow<List<HistoryRecord>> = historyDao.getAllHistory()

    suspend fun getHistoryList(): List<HistoryRecord> = historyDao.getHistoryList()
    suspend fun getHistoryById(id: String): HistoryRecord? = historyDao.getHistoryById(id)
    suspend fun getHistoryDetails(historyId: String): List<HistoryDetail> =
        historyDao.getDetailsForHistory(historyId)

    suspend fun saveResult(result: QuizResult, questions: List<Question>) {
        val record = HistoryRecord(
            id = result.sessionId,
            mode = if (result.mode == QuizMode.EXAM) "exam" else "practice",
            timestamp = result.timestamp,
            totalCount = result.totalCount,
            correctCount = result.correctCount,
            answeredCount = result.answeredCount,
            score = result.score,
            maxScore = result.maxScore,
            duration = result.durationSeconds,
            breakdown = result.breakdown?.let { gson.toJson(it) }
        )
        val details = result.details.map { d ->
            HistoryDetail(
                historyId = result.sessionId,
                questionId = d.questionId,
                userAnswer = d.userAnswer.joinToString(","),
                isCorrect = d.isCorrect
            )
        }
        historyDao.insertFullRecord(record, details)
    }

    suspend fun clearHistory() { historyDao.deleteAllHistory() }

    suspend fun clearEverything() {
        questionDao.deleteAllQuestions()
        questionDao.deleteBankMeta()
        historyDao.deleteAllHistory()
    }
}
