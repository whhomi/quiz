package com.quizhelper.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Quiz : Screen("quiz/{mode}") {
        fun createRoute(mode: String = "practice") = "quiz/$mode"
    }
    data object Result : Screen("result/{sessionId}") {
        fun createRoute(sessionId: String) = "result/$sessionId"
    }
    data object History : Screen("history")
    data object HistoryDetail : Screen("history/{id}") {
        fun createRoute(id: String) = "history/$id"
    }
    data object Settings : Screen("settings")
}
