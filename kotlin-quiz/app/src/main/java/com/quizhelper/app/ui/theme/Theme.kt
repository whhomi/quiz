package com.quizhelper.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue800,
    secondary = Purple600,
    onSecondary = Color.White,
    secondaryContainer = Purple100,
    onSecondaryContainer = Purple600,
    error = Red600,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red600,
    background = Gray50,
    onBackground = Gray800,
    surface = Color.White,
    onSurface = Gray800,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
    outline = Gray200,
    outlineVariant = Gray100,
)

@Composable
fun QuizHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}
