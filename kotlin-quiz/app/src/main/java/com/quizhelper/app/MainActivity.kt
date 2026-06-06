package com.quizhelper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.quizhelper.app.ui.components.MainScreen
import com.quizhelper.app.ui.theme.QuizHelperTheme
import com.quizhelper.app.util.Logger

class MainActivity : ComponentActivity() {
    private val log = Logger.create("MainActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        log.i("MainActivity onCreate")

        setContent {
            QuizHelperTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}
