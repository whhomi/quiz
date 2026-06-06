package com.quizhelper.app.ui.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizhelper.app.data.model.*
import com.quizhelper.app.data.repository.QuizRepository
import com.quizhelper.app.util.Logger
import com.quizhelper.app.util.ProgressInfo
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
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun startPractice(random: Boolean = true) {
        _isReady.value = false
        viewModelScope.launch {
            val questions = repository.getAllQuestionsList()
            if (questions.isEmpty()) { _isReady.value = true; return@launch }
            _session = QuizEngine.createSession(questions, random = random, mode = QuizMode.PRACTICE)
            refreshFromSession()
            _isReady.value = true
        }
    }

    fun startExam() {
        _isReady.value = false
        viewModelScope.launch {
            val allQuestions = repository.getAllQuestionsList()
            if (allQuestions.isEmpty()) { _isReady.value = true; return@launch }
            val examQuestions = QuizEngine.selectExamQuestions(allQuestions)
            _session = QuizEngine.createSession(examQuestions, random = false, mode = QuizMode.EXAM, timeLimit = 6000)
            refreshFromSession()
            _isReady.value = true
            startTimer()
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            val limit = _session?.timeLimitSeconds ?: return@launch
            _timeRemaining.value = limit
            while (_timeRemaining.value ?: 0 > 0 && !_isFinished.value) {
                delay(1000L)
                _timeRemaining.value = (_timeRemaining.value ?: 0) - 1
                if (_timeRemaining.value ?: 0 <= 0) {
                    finishQuiz()
                }
            }
        }
    }

    fun toggleOption(index: Int) {
        val q = _currentQuestion.value ?: return
        val current = _selectedAnswers.value.toMutableList()
        if (q.type == QuestionType.SINGLE || q.type == QuestionType.BOOLEAN) {
            _selectedAnswers.value = listOf(index)
            if (_session?.mode == QuizMode.EXAM) {
                val s = _session ?: return
                QuizEngine.submitAnswer(s, listOf(index))
                _judgment.value = Judgment(
                    isCorrect = QuizEngine.judge(listOf(index), q.getAnswerList()),
                    correctAnswer = q.getAnswerList(),
                    questionId = q.id
                )
                refreshFromSession()
            }
        } else {
            if (index in current) current.remove(index) else current.add(index)
            _selectedAnswers.value = current
            if (_session?.mode == QuizMode.EXAM) {
                val s = _session ?: return
                QuizEngine.submitAnswer(s, current)
                _judgment.value = Judgment(
                    isCorrect = QuizEngine.judge(current, q.getAnswerList()),
                    correctAnswer = q.getAnswerList(),
                    questionId = q.id
                )
                refreshFromSession()
            }
        }
    }

    fun submitAnswer() {
        val s = _session ?: return
        val selected = _selectedAnswers.value
        if (selected.isEmpty()) return
        val result = QuizEngine.submitAnswer(s, selected)
        _judgment.value = result
        refreshFromSession()
    }

    fun goNext() {
        val s = _session ?: return
        val q = QuizEngine.getCurrentQuestion(s)
        if (q != null && s.mode == QuizMode.PRACTICE && (q.type == QuestionType.SINGLE || q.type == QuestionType.BOOLEAN)) {
            val selected = _selectedAnswers.value
            if (selected.isNotEmpty() && !s.answers.containsKey(q.id)) {
                QuizEngine.submitAnswer(s, selected)
                _judgment.value = Judgment(
                    isCorrect = QuizEngine.judge(selected, q.getAnswerList()),
                    correctAnswer = q.getAnswerList(),
                    questionId = q.id
                )
            }
        }
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
        _result.value = result
        _isFinished.value = true

        viewModelScope.launch {
            repository.saveResult(result, s.questions)
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
