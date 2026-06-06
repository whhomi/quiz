package com.quizhelper.app.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizhelper.app.data.model.QuizResult
import com.quizhelper.app.ui.components.EncouragementDialog
import com.quizhelper.app.ui.navigation.Screen

/**
 * Standalone result screen (used when navigating to result via deep link / sessionId).
 */
@Composable
fun ResultScreen(
    navController: NavController,
    sessionId: String,
    viewModel: QuizViewModel = viewModel()
) {
    val result by viewModel.result.collectAsState()

    if (result != null) {
        var showResultDialog by remember { mutableStateOf(true) }
        var navigateTo by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(navigateTo) {
            navigateTo?.let { route ->
                navController.navigate(route) {
                    popUpTo("home") { inclusive = true }
                }
            }
        }

        if (showResultDialog) {
            EncouragementDialog(
                result = result!!,
                onViewDetail = {
                    showResultDialog = false
                    navigateTo = "history/$sessionId"
                },
                onRetry = {
                    showResultDialog = false
                    if (result!!.mode == com.quizhelper.app.data.model.QuizMode.EXAM)
                        viewModel.startExam()
                    else
                        viewModel.startPractice()
                },
                onHome = {
                    showResultDialog = false
                    navigateTo = "home"
                }
            )
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载中...")
        }
    }
}
