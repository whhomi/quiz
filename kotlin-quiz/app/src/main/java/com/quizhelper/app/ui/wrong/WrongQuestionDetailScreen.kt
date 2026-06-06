package com.quizhelper.app.ui.wrong

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizhelper.app.data.model.Question
import com.quizhelper.app.ui.components.*
import com.quizhelper.app.ui.theme.*

@Composable
fun WrongQuestionDetailScreen(
    navController: NavController,
    questionId: Long,
    viewModel: WrongQuestionsViewModel = viewModel()
) {
    // Get the question from the loaded list
    val questions by viewModel.questions.collectAsState()
    val question = questions.find { it.id == questionId }

    if (question == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔍", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text("题目未找到", color = Gray400, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                SecondaryButton(
                    text = "返回错题集",
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    textColor = Blue600
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Back button
        BackButton(onClick = { navController.popBackStack() })

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                // Type tag
                QuestionTypeTag(type = question.type)
                Spacer(Modifier.height(8.dp))

                // Stem
                Text(
                    question.stem,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray800,
                    lineHeight = 28.sp
                )
                Spacer(Modifier.height(16.dp))

                // Options with correct answer highlighted
                val options = question.getOptionsList()
                val correctAnswer = question.getAnswerList()

                options.forEachIndexed { idx, opt ->
                    val isCorrect = correctAnswer.contains(idx)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) Green50 else Gray50
                        ),
                        border = if (isCorrect) CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Green500),
                            width = 1.5.dp
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (isCorrect) Green500 else Gray200,
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isCorrect) "✓" else "${('A' + idx)}",
                                    color = if (isCorrect) White else Gray500,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                opt,
                                fontSize = 14.sp,
                                color = Gray700,
                                fontWeight = if (isCorrect) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Correct answer summary
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Green50
                ) {
                    Text(
                        "正确答案：${correctAnswer.map { ('A' + it) }.joinToString("、")}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Green700,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Remove button
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    text = "从错题集中移除",
                    onClick = { viewModel.remove(question.id); navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                    textColor = Red500
                )

                // Analysis
                if (question.analysis.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Blue50.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("💡 解析", fontSize = 11.sp, color = Blue500, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                question.analysis,
                                fontSize = 14.sp,
                                color = Gray700,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SecondaryButton(
            text = "返回错题集",
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            textColor = Blue600
        )
    }
}
