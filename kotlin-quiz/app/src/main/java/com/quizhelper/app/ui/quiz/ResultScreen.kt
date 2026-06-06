package com.quizhelper.app.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizhelper.app.data.model.QuizResult

/**
 * Standalone result screen (used when navigating to result via deep link / sessionId).
 * The QuizScreen handles result internally. This is for external navigation.
 */
@Composable
fun ResultScreen(
    navController: NavController,
    sessionId: String,
    viewModel: QuizViewModel = viewModel()
) {
    val result by viewModel.result.collectAsState()

    if (result != null) {
        ResultContent(
            result = result!!,
            onViewDetail = {
                navController.navigate("history/$sessionId") {
                    popUpTo("home") { saveState = true }
                }
            },
            onRetry = {
                if (result!!.mode == com.quizhelper.app.data.model.QuizMode.EXAM)
                    viewModel.startExam()
                else
                    viewModel.startPractice()
            },
            onHome = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
        )
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载中...")
        }
    }
}
