package com.quizhelper.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Quiz : Screen("quiz/{mode}?practiceType={practiceType}&source={source}&examType={examType}") {
        fun createRoute(mode: String = "practice", practiceType: String = "random", source: String = "all", examType: String = "full_random") =
            "quiz/$mode?practiceType=$practiceType&source=$source&examType=$examType"
    }
    data object Result : Screen("result/{sessionId}") {
        fun createRoute(sessionId: String) = "result/$sessionId"
    }
    data object History : Screen("history")
    data object HistoryDetail : Screen("history/{id}") {
        fun createRoute(id: String) = "history/$id"
    }
    data object WrongQuestions : Screen("wrong_questions")
    data object WrongQuestionDetail : Screen("wrong_detail/{questionId}") {
        fun createRoute(questionId: Long) = "wrong_detail/$questionId"
    }
    data object Settings : Screen("settings")
}
