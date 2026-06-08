package com.quizhelper.app.ui.history

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizhelper.app.data.model.*
import com.quizhelper.app.ui.components.*
import com.quizhelper.app.ui.theme.*
import com.quizhelper.app.util.TimeUtils
import com.quizhelper.app.util.ShareUtil
import com.quizhelper.app.util.Encouragement

@Composable
fun HistoryDetailScreen(
    navController: NavController,
    recordId: String,
    viewModel: HistoryDetailViewModel = viewModel()
) {
    LaunchedEffect(recordId) {
        viewModel.load(recordId)
    }

    val record by viewModel.record.collectAsState()
    val filteredDetails by viewModel.filteredDetails.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()

    if (record == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔍", fontSize = 48.sp)
                Text("找不到该练习记录", color = Gray400)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { navController.navigate("history") { popUpTo("history") { inclusive = true }; launchSingleTop = true } }) {
                    Text("返回历史列表", color = Blue600)
                }
            }
        }
        return
    }

    val r = record!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Back button
        BackButton(text = "← 返回列表", onClick = { navController.navigate("history") { popUpTo("history") { inclusive = true }; launchSingleTop = true } })

        // Score overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (r.mode == "exam") "考试详情" else "练习详情",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gray800)
                        if (r.mode == "exam") {
                            SmallButton(
                                text = "📤 分享",
                                onClick = {
                                    ShareUtil.shareExamResult(
                                        context = navController.context,
                                        score = r.score,
                                        maxScore = r.maxScore ?: 100.0,
                                        correctRate = if (r.totalCount > 0) (r.correctCount.toDouble() / r.totalCount * 100) else 0.0,
                                        correctCount = r.correctCount,
                                        totalCount = r.totalCount,
                                        durationSeconds = r.duration,
                                        isPass = r.score >= 80
                                    )
                                },
                                containerColor = Purple600,
                                textColor = White,
                                fontSize = 12
                            )
                        }
                    }
                    Text(
                        TimeUtils.formatTimestampFull(r.timestamp),
                        fontSize = 12.sp,
                        color = Gray400
                    )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Score (point-based)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${r.score.toInt()} 分",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (r.score >= 80) Green600 else Red500
                        )
                        Text("得分", fontSize = 11.sp, color = Gray400)
                    }
                    // Correct rate
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val rate = if (r.totalCount > 0) (r.correctCount.toDouble() / r.totalCount * 100).toInt() else 0
                        Text(
                            "${rate}%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rate >= 80) Green600 else Red500
                        )
                        Text("正确率", fontSize = 11.sp, color = Gray400)
                    }
                    // Correct count
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${r.correctCount}/${r.totalCount}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray700
                        )
                        Text("正确 / 题", fontSize = 11.sp, color = Gray400)
                    }
                    // Duration
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            TimeUtils.formatDuration(r.duration),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue600
                        )
                        Text("用时", fontSize = 11.sp, color = Gray400)
                    }
                }

                // Exam breakdown
                if (r.mode == "exam" && r.breakdown != null) {
                    Spacer(Modifier.height(12.dp))
                    val breakdown = com.google.gson.Gson().fromJson(r.breakdown, ExamBreakdown::class.java)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BreakdownStat("单选题", breakdown.single, Blue600, Blue50)
                        BreakdownStat("多选题", breakdown.multiple, Purple600, Purple50)
                        BreakdownStat("判断题", breakdown.boolean, Amber600, Amber50)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "已答 ${r.answeredCount} 题，跳过 ${r.totalCount - r.answeredCount} 题",
                    fontSize = 11.sp,
                    color = Gray400
                )

                // Filter buttons
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val filters = listOf(
                        "all" to "全部", "single" to "单选",
                        "multiple" to "多选", "boolean" to "判断"
                    )
                    filters.forEach { (key, label) ->
                        val isSelected = currentFilter == key
                        Surface(
                            onClick = { viewModel.setFilter(key) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Blue600 else Gray100,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) White else Gray500,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (filteredDetails.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("该分类下无已答题目", color = Gray400, fontSize = 14.sp)
            }
        } else {
            filteredDetails.forEach { detail ->
                val question = questions[detail.questionId]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (detail.isCorrect) Green200 else Red200
                        ),
                        width = 1.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .let {
                                    it.then(
                                        Modifier
                                            .background(
                                                if (detail.isCorrect) Green500 else Red500,
                                                RoundedCornerShape(14.dp)
                                            )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (detail.isCorrect) "✓" else "✗",
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                question?.stem ?: "(题目已删除)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Gray800
                            )

                            if (question != null) {
                                Spacer(Modifier.height(4.dp))
                                val options = question.getOptionsList()
                                val correctAns = question.getAnswerList()
                                val userAns = detail.getUserAnswerList()

                                options.forEachIndexed { idx, opt ->
                                    val isCorrectOpt = correctAns.contains(idx)
                                    val isWrongUser = userAns.contains(idx) && !isCorrectOpt
                                    Surface(
                                        color = when {
                                            isCorrectOpt -> Green50
                                            isWrongUser -> Red50
                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                if (opt.isBlank()) "${('A' + idx)}. (空)" else "${('A' + idx)}. $opt",
                                                fontSize = 13.sp,
                                                color = if (isCorrectOpt || isWrongUser) Gray700 else Gray500,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isCorrectOpt) {
                                                Spacer(Modifier.width(4.dp))
                                                Text("\u2713", color = Green500, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            if (isWrongUser) {
                                                Spacer(Modifier.width(4.dp))
                                                Text("\u2717", color = Red500, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                if (question.analysis.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Blue50.copy(alpha = 0.5f))
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            Text("💡 解析", fontSize = 11.sp, color = Blue500, fontWeight = FontWeight.Medium)
                                            Text(
                                                question.analysis,
                                                fontSize = 13.sp,
                                                color = Gray700,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "返回历史列表",
            onClick = { navController.navigate("history") { popUpTo("history") { inclusive = true }; launchSingleTop = true } },
            modifier = Modifier.fillMaxWidth(),
            textColor = Blue600,
            fontSize = 15
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BreakdownStat(label: String, breakdown: TypeBreakdown, color: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${breakdown.correct}/${breakdown.total}", fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
        Text(label, color = Gray400, fontSize = 11.sp)
    }
}
