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

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(
            route = Screen.Quiz.route,
            arguments = listOf(navArgument("mode") {
                type = NavType.StringType
                defaultValue = "practice"
            })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "practice"
            QuizScreen(navController = navController, mode = mode)
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
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
