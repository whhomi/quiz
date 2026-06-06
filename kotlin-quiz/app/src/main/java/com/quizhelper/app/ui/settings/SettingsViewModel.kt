package com.quizhelper.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizhelper.app.data.db.AppDatabase
import com.quizhelper.app.data.model.QuestionBankMeta
import com.quizhelper.app.data.repository.QuizRepository
import com.quizhelper.app.util.Logger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizRepository(application)
    private val log = Logger.create("SettingsVM")

    val bankMeta: StateFlow<QuestionBankMeta?> = repository.bankMeta
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val historyList = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            log.i("历史记录已清除")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearEverything()
            log.i("全部数据已清除")
        }
    }
}
