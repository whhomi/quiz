package com.quizhelper.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizhelper.app.data.model.HistoryDetail
import com.quizhelper.app.data.model.HistoryRecord
import com.quizhelper.app.data.model.Question
import com.quizhelper.app.data.repository.QuizRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizRepository(application)

    val historyList: StateFlow<List<HistoryRecord>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }
}

class HistoryDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizRepository(application)

    private val _record = MutableStateFlow<HistoryRecord?>(null)
    val record: StateFlow<HistoryRecord?> = _record.asStateFlow()

    private val _details = MutableStateFlow<List<HistoryDetail>>(emptyList())
    val details: StateFlow<List<HistoryDetail>> = _details.asStateFlow()

    private val _questions = MutableStateFlow<Map<Long, Question>>(emptyMap())
    val questions: StateFlow<Map<Long, Question>> = _questions.asStateFlow()

    private val _filter = MutableStateFlow<String>("all")
    val filter: StateFlow<String> = _filter.asStateFlow()

    val filteredDetails: StateFlow<List<HistoryDetail>> = combine(_details, _filter) { all, f ->
        if (f == "all") all
        else all.filter { d ->
            val q = _questions.value[d.questionId]
            q?.type?.name?.lowercase() == f.lowercase()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(recordId: String) {
        viewModelScope.launch {
            _record.value = repository.getHistoryById(recordId)
            _details.value = repository.getHistoryDetails(recordId)
            val allQuestions = repository.getAllQuestionsList()
            _questions.value = allQuestions.associateBy { it.id }
        }
    }

    fun setFilter(f: String) {
        _filter.value = f
    }
}
