package com.quizhelper.app.ui.wrong

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizhelper.app.data.model.Question
import com.quizhelper.app.data.model.QuestionType
import com.quizhelper.app.ui.components.*
import com.quizhelper.app.ui.navigation.Screen
import com.quizhelper.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongQuestionsScreen(
    navController: NavController,
    viewModel: WrongQuestionsViewModel = viewModel()
) {
    val questions by viewModel.questions.collectAsState()
    val count by viewModel.count.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "📕 错题集",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Gray800
            )
            if (count > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Red50
                ) {
                    Text(
                        "共 $count 题",
                        fontSize = 12.sp,
                        color = Red600,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (count == 0) {
            // Empty state
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("暂无错题记录", color = Gray400, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("继续保持！", color = Gray300, fontSize = 12.sp)
                }
            }
        } else {
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallButton(
                    text = "🔁 练习错题",
                    onClick = {
                        navController.navigate(Screen.Quiz.createRoute(
                            mode = "practice",
                            practiceType = "random",
                            source = "wrong"
                        )) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = Red500
                )
                SmallButton(
                    text = "📖 顺序练习",
                    onClick = {
                        navController.navigate(Screen.Quiz.createRoute(
                            mode = "practice",
                            practiceType = "sequential",
                            source = "wrong"
                        )) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = Blue600
                )
            }

            Spacer(Modifier.height(8.dp))

            // Clear button
            TextButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🗑 清空错题集", color = Red500, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            // Question list
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(questions, key = { it.id }) { question ->
                    WrongQuestionCard(
                        question = question,
                        onClick = {
                            navController.navigate(Screen.WrongQuestionDetail.createRoute(question.id))
                        },
                        onRemove = { viewModel.remove(question.id) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "清空错题集",
            message = "确定要清空所有错题记录吗？此操作不可恢复。",
            confirmText = "确认清空",
            onConfirm = {
                viewModel.clearAll()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }
}

@Composable
private fun WrongQuestionCard(
    question: Question,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Wrong icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Red500, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("✗", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuestionTypeTag(type = question.type)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    question.stem,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Gray800,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                // Show correct answer
                val answerLetters = question.getAnswerList().map { ('A' + it) }
                Text(
                    "正确答案: ${answerLetters.joinToString("")}",
                    fontSize = 12.sp,
                    color = Green600,
                    fontWeight = FontWeight.Medium
                )
                if (question.analysis.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "💡 ${question.analysis}",
                        fontSize = 12.sp,
                        color = Gray400,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "移除",
                    tint = Gray300,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
