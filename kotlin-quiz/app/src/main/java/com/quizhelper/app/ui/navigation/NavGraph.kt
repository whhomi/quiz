package com.quizhelper.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.quizhelper.app.ui.home.HomeScreen
import com.quizhelper.app.ui.history.HistoryDetailScreen
import com.quizhelper.app.ui.history.HistoryScreen
import com.quizhelper.app.ui.quiz.QuizScreen
import com.quizhelper.app.ui.quiz.ResultScreen
import com.quizhelper.app.ui.settings.SettingsScreen
import com.quizhelper.app.ui.wrong.WrongQuestionDetailScreen
import com.quizhelper.app.ui.wrong.WrongQuestionsScreen

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(
            route = Screen.Quiz.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "practice" },
                navArgument("practiceType") { type = NavType.StringType; defaultValue = "random" },
                navArgument("source") { type = NavType.StringType; defaultValue = "all" },
                navArgument("examType") { type = NavType.StringType; defaultValue = "full_random" }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "practice"
            val practiceType = backStackEntry.arguments?.getString("practiceType") ?: "random"
            val source = backStackEntry.arguments?.getString("source") ?: "all"
            val examType = backStackEntry.arguments?.getString("examType") ?: "full_random"
            QuizScreen(navController = navController, mode = mode, practiceType = practiceType, source = source, examType = examType)
        }
        composable(
            route = Screen.Result.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ResultScreen(navController = navController, sessionId = sessionId)
        }
        composable(Screen.History.route) {
            HistoryScreen(navController = navController)
        }
        composable(
            route = Screen.HistoryDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            HistoryDetailScreen(navController = navController, recordId = id)
        }
        composable(Screen.WrongQuestions.route) {
            WrongQuestionsScreen(navController = navController)
        }
        composable(
            route = Screen.WrongQuestionDetail.route,
            arguments = listOf(navArgument("questionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val questionId = backStackEntry.arguments?.getLong("questionId") ?: 0L
            WrongQuestionDetailScreen(navController = navController, questionId = questionId)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
