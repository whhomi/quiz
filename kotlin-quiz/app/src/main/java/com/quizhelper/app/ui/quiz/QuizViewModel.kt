package com.quizhelper.app.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizhelper.app.data.model.*
import com.quizhelper.app.data.repository.QuizRepository
import com.quizhelper.app.util.Logger
import com.quizhelper.app.util.ProgressInfo
import com.quizhelper.app.util.PracticeProgressStore
import com.quizhelper.app.util.QuizEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizRepository(application)
    private val log = Logger.create("QuizVM")

    private var _session: QuizSession? = null
    val session: QuizSession? get() = _session

    private val _currentQuestion = MutableStateFlow<Question?>(null)
    val currentQuestion: StateFlow<Question?> = _currentQuestion.asStateFlow()

    private val _progress = MutableStateFlow(ProgressInfo(1, 0, 0, 0))
    val progress: StateFlow<ProgressInfo> = _progress.asStateFlow()

    private val _judgment = MutableStateFlow<Judgment?>(null)
    val judgment: StateFlow<Judgment?> = _judgment.asStateFlow()

    private val _selectedAnswers = MutableStateFlow<List<Int>>(emptyList())
    val selectedAnswers: StateFlow<List<Int>> = _selectedAnswers.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    private val _result = MutableStateFlow<QuizResult?>(null)
    val result: StateFlow<QuizResult?> = _result.asStateFlow()

    private val _timeRemaining = MutableStateFlow<Int?>(null)
    val timeRemaining: StateFlow<Int?> = _timeRemaining.asStateFlow()

    private val _isReady = MutableStateFlow(false)

    private val _showTimeWarning = MutableStateFlow(false)
    val showTimeWarning: StateFlow<Boolean> = _showTimeWarning.asStateFlow()
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /** 当前练习来源: "all" 全部题目, "wrong" 错题集 */
    private var questionSource: String = "all"

    fun startPractice(random: Boolean = true, source: String = "all", resume: Boolean = false) {
        questionSource = source
        _isFinished.value = false
        _result.value = null
        _isReady.value = false
        viewModelScope.launch {
            val allQuestions = repository.getAllQuestionsList()
            if (allQuestions.isEmpty()) { _isReady.value = true; return@launch }
            if (resume && !random && source == "all") {
                val restored: com.quizhelper.app.data.model.QuizSession? = PracticeProgressStore.restoreSession(getApplication(), allQuestions)
                if (restored != null) {
                    _session = restored
                    refreshFromSession()
                    _isReady.value = true
                    return@launch
                }
            }
            val questions = if (source == "wrong") {
                repository.getWrongQuestionsList()
            } else {
                allQuestions
            }
            if (questions.isEmpty()) { _isReady.value = true; return@launch }
            _session = QuizEngine.createSession(questions, random = random, mode = QuizMode.PRACTICE)
            refreshFromSession()
            _isReady.value = true
        }
    }

    fun startExam(examType: String = "full_random") {
        _isFinished.value = false
        _result.value = null
        _isReady.value = false
        viewModelScope.launch {
            val allQuestions = repository.getAllQuestionsList()
            if (allQuestions.isEmpty()) { _isReady.value = true; return@launch }
            val examQuestions = QuizEngine.selectExamQuestions(allQuestions, examType)
            _session = QuizEngine.createSession(examQuestions, random = false, mode = QuizMode.EXAM, timeLimit = 6000)
            refreshFromSession()
            _isReady.value = true
            timeWarningShown = false; startTimer()
        }
    }

    private var timeWarningShown = false

    private fun startTimer() {
        viewModelScope.launch {
            val limit = _session?.timeLimitSeconds ?: return@launch
            _timeRemaining.value = limit
            while (_timeRemaining.value ?: 0 > 0 && !_isFinished.value) {
                delay(1000L)
                _timeRemaining.value = (_timeRemaining.value ?: 0) - 1
                if (_timeRemaining.value ?: 0 <= 300 && !timeWarningShown && !_showTimeWarning.value) {
                    timeWarningShown = true
                    _showTimeWarning.value = true
                }
                if (_timeRemaining.value ?: 0 <= 0) {
                    finishQuiz()
                }
            }
        }
    }

    fun toggleOption(index: Int) {
        val q = _currentQuestion.value ?: return
        // 考试模式下已答题不可修改
        if (_session?.mode == QuizMode.EXAM && _judgment.value != null) return
        val current = _selectedAnswers.value.toMutableList()
        if (q.type == QuestionType.SINGLE || q.type == QuestionType.BOOLEAN) {
            // 单选/判断题：点击即自动提交并显示反馈
            _selectedAnswers.value = listOf(index)
            val s = _session ?: return
            QuizEngine.submitAnswer(s, listOf(index))
            val isCorrect = QuizEngine.judge(listOf(index), q.getAnswerList())
            _judgment.value = Judgment(
                isCorrect = isCorrect,
                correctAnswer = q.getAnswerList(),
                questionId = q.id
            )
            refreshFromSession()
            recordWrongQuestion(q.id, isCorrect)
        } else {
            // 多选题：先记录选中状态，等待点击"提交答案"
            if (index in current) current.remove(index) else current.add(index)
            _selectedAnswers.value = current
        }
    }

    fun submitAnswer() {
        val s = _session ?: return
        val selected = _selectedAnswers.value
        if (selected.isEmpty()) return
        val result = QuizEngine.submitAnswer(s, selected)
        _judgment.value = result
        refreshFromSession()
        recordWrongQuestion(result.questionId, result.isCorrect)
    }

    fun goNext() {
        val s = _session ?: return
        val hasMore = QuizEngine.nextQuestion(s)
        if (!hasMore) {
            finishQuiz()
            return
        }
        refreshFromSession()
    }

    fun goPrev() {
        val s = _session ?: return
        QuizEngine.prevQuestion(s)
        refreshFromSession()
    }

    fun jumpToQuestion(index: Int) {
        val s = _session ?: return
        QuizEngine.goToQuestion(s, index)
        refreshFromSession()
    }

    fun finishQuiz() {
        val s = _session ?: return
        s.endTime = System.currentTimeMillis()
        s.isFinished = true
        val result = if (s.mode == QuizMode.EXAM) QuizEngine.buildExamResult(s) else QuizEngine.buildResult(s)
        if (s.mode == QuizMode.PRACTICE) {
            PracticeProgressStore.clear(getApplication())
        }
        _result.value = result
        _isFinished.value = true

        viewModelScope.launch {
            repository.saveResult(result, s.questions)
        }
    }

    /**
     * 自动记录错题：答错时加入错题集；在错题集练习中答对则移出错题集
     */
    fun dismissTimeWarning() {
        _showTimeWarning.value = false
    }

    private fun recordWrongQuestion(questionId: Long, isCorrect: Boolean) {
        viewModelScope.launch {
            if (!isCorrect) {
                repository.addToWrongQuestions(questionId)
            } else if (questionSource == "wrong") {
                repository.removeFromWrongQuestions(questionId)
            }
        }
    }

    fun saveProgress() {
        if (_session?.mode == QuizMode.PRACTICE && questionSource == "all") {
            _session?.let { PracticeProgressStore.saveSession(getApplication(), it) }
        }
    }

    private fun refreshFromSession() {
        val s = _session ?: return
        _currentQuestion.value = QuizEngine.getCurrentQuestion(s)
        _progress.value = QuizEngine.getProgress(s)

        val q = QuizEngine.getCurrentQuestion(s)
        if (q != null) {
            val saved = s.answers[q.id]
            _selectedAnswers.value = saved ?: emptyList()
            if (saved != null) {
                _judgment.value = Judgment(
                    isCorrect = QuizEngine.judge(saved, q.getAnswerList()),
                    correctAnswer = q.getAnswerList(),
                    questionId = q.id
                )
            } else {
                _judgment.value = null
            }
        }
    }
}
