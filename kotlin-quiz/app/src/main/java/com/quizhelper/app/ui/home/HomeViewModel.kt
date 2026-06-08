package com.quizhelper.app.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizhelper.app.data.db.AppDatabase
import com.quizhelper.app.data.model.ImportResult
import com.quizhelper.app.data.model.QuestionBankMeta
import com.quizhelper.app.data.repository.QuizRepository
import com.quizhelper.app.util.Logger
import com.quizhelper.app.util.PracticeProgressStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizRepository(application)
    private val log = Logger.create("HomeVM")

    val bankMeta: StateFlow<QuestionBankMeta?> = repository.bankMeta
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val questionsCount = bankMeta.map { it?.totalCount ?: 0 }
    val singleCount = bankMeta.map { it?.singleCount ?: 0 }
    val multipleCount = bankMeta.map { it?.multipleCount ?: 0 }
    val booleanCount = bankMeta.map { it?.booleanCount ?: 0 }
    val wrongCount: StateFlow<Int> = repository.allWrongQuestions
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _importResult = MutableSharedFlow<ImportResult>()
    val importResult: SharedFlow<ImportResult> = _importResult.asSharedFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _sequentialProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val sequentialProgress: StateFlow<Pair<Int, Int>?> = _sequentialProgress.asStateFlow()

    fun refreshSequentialProgress() {
        val app = getApplication<android.app.Application>()
        _sequentialProgress.value = PracticeProgressStore.getProgressSummary(app)
    }

    init { refreshSequentialProgress() }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = repository.importExcel(uri)
                _importResult.emit(result)
            } catch (e: Exception) {
                log.e("导入异常", e)
                _importResult.emit(ImportResult(false, "导入失败: ${e.message}"))
            } finally {
                _isImporting.value = false
            }
        }
    }
}
