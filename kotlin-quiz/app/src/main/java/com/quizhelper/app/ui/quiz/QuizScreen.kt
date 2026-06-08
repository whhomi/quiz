package com.quizhelper.app.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizhelper.app.data.model.*
import com.quizhelper.app.ui.components.*
import com.quizhelper.app.ui.navigation.Screen
import com.quizhelper.app.ui.theme.*
import com.quizhelper.app.util.ProgressInfo
import com.quizhelper.app.util.TimeUtils
import com.quizhelper.app.util.Encouragement
import com.quizhelper.app.util.PracticeProgressStore
import com.quizhelper.app.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    navController: NavController,
    mode: String = "practice",
    practiceType: String = "random",
    source: String = "all",
    examType: String = "full_random",
    viewModel: QuizViewModel = viewModel()
) {
    val question by viewModel.currentQuestion.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val judgment by viewModel.judgment.collectAsState()
    val selected by viewModel.selectedAnswers.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()
    val result by viewModel.result.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val showTimeWarning by viewModel.showTimeWarning.collectAsState()
    val isReady by viewModel.isReady.collectAsState()

    var showExitConfirm by remember { mutableStateOf(false) }
    var exitMessage by remember { mutableStateOf(Encouragement.randomRetention()) }
    var showResumeDialog by remember { mutableStateOf(false) }

    // Start session based on mode
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!started) {
            started = true
            if (mode == "exam") viewModel.startExam(examType)
            else if (mode == "practice" && practiceType == "sequential") {
                // Check if there is a saved session to restore
                showResumeDialog = PracticeProgressStore.hasProgress(navController.context)
                if (!showResumeDialog) {
                    viewModel.startPractice(random = false, source = source)
                }
            } else {
                viewModel.startPractice(random = practiceType == "random", source = source)
            }
        }
    }

    // Show resume dialog
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = {
                showResumeDialog = false
                viewModel.startPractice(random = false, source = source)
            },
            shape = RoundedCornerShape(20.dp),
            title = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📖", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("发现上次练习进度", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Gray800)
                }
            },
            text = {
                Text("检测到您有未完成的顺序练习，是否继续上次的进度？", fontSize = 14.sp, color = Gray600, textAlign = TextAlign.Center)
            },
            confirmButton = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    SmallButton(
                        text = "继续练习",
                        onClick = {
                            showResumeDialog = false
                            viewModel.startPractice(random = false, source = source, resume = true)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Green600,
                        textColor = White,
                        fontSize = 14
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showResumeDialog = false
                            PracticeProgressStore.clear(navController.context)
                            viewModel.startPractice(random = false, source = source)
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Gray300),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray500)
                    ) {
                        Text("重新开始", fontSize = 14.sp)
                    }
                }
            }
        )
    }

    if (!isReady) {
        Box(Modifier.fillMaxSize().background(Gray50), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Blue600)
                Spacer(Modifier.height(16.dp))
                Text("加载题库中...", color = Gray500, fontSize = 14.sp)
            }
        }
        return
    }

    if (isFinished && result != null) {
        var showResultDialog by remember { mutableStateOf(true) }
        var navigateTo by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(navigateTo) {
            navigateTo?.let { route ->
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        }

        if (showResultDialog) {
            EncouragementDialog(
                result = result!!,
                onViewDetail = {
                    showResultDialog = false
                    navigateTo = Screen.HistoryDetail.createRoute(result!!.sessionId)
                },
                onRetry = {
                    showResultDialog = false
                    if (result!!.mode == QuizMode.EXAM) viewModel.startExam(examType)
                    else viewModel.startPractice(random = practiceType == "random", source = source)
                },
                onHome = {
                    showResultDialog = false
                    showExitConfirm = false
                    navigateTo = Screen.Home.route
                }
            )
        }
        return
    }

    if (showTimeWarning) {
        TimeWarningDialog(onDismiss = { viewModel.dismissTimeWarning() })
    }

    if (question == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val session = viewModel.session ?: return
    val isExam = session.mode == QuizMode.EXAM

    if (showExitConfirm) {
        if (isExam) {
            ConfirmDialog(
                title = "退出考试",
                message = "确定要退出考试吗？退出后本次答题记录将不会保存。",
                confirmText = "确定退出",
                confirmColor = Amber600,
                onConfirm = {
                    showExitConfirm = false
                    navController.popBackStack()
                },
                onDismiss = { showExitConfirm = false }
            )
        } else {
            ExitConfirmDialog(
                message = exitMessage,
                confirmText = "确定退出",
                onConfirm = {
                    showExitConfirm = false
                    navController.popBackStack()
                },
                onDismiss = { showExitConfirm = false }
            )
        }
    }

    BackHandler {
        if (!isExam) viewModel.saveProgress()
        showExitConfirm = true
    }
    var showGrid by remember { mutableStateOf(false) }
    val isAnswered = judgment != null
    val options = question?.getOptionsList() ?: emptyList()
    val isMulti = question?.type == QuestionType.MULTIPLE
    val correctAnswer = question?.getAnswerList() ?: emptyList()
    val isCorrect = judgment?.isCorrect

    Column(Modifier.fillMaxSize().background(Gray50)) {
        // Top bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        "第 ${progress.current}/${progress.total} 题  (已答 ${progress.answered})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray700
                    )
                }
            },
            actions = {
                if (isExam && timeRemaining != null) {
                    val isDanger = timeRemaining!! <= 300
                    val isWarning = timeRemaining!! <= 600 && timeRemaining!! > 300
                    Text(
                        TimeUtils.formatCountdown(timeRemaining!!),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = when {
                            isDanger -> Red600
                            isWarning -> Amber600
                            else -> Gray700
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                if (isExam) {
                    SmallButton(
                        text = "交卷",
                        onClick = { viewModel.finishQuiz() },
                        containerColor = Amber500,
                        textColor = White,
                        fontSize = 13,
                        modifier = Modifier.height(36.dp)
                    )
                }
                SmallButton(
                    text = if (showGrid) "收起" else "题号",
                    onClick = { showGrid = !showGrid },
                    containerColor = Gray100,
                    textColor = Gray600,
                    fontSize = 13,
                    modifier = Modifier.height(36.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
        )

        // Progress bar
        ProgressBar(
            answered = progress.answered,
            total = progress.total,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))

        // Question grid
        AnimatedVisibility(visible = showGrid) {
            QuestionGrid(
                questions = session.questions,
                answers = session.answers,
                currentIndex = session.currentIndex,
                onJump = { index ->
                    viewModel.jumpToQuestion(index)
                    showGrid = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .background(White)
                    .padding(12.dp)
            )
        }

        // Question content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            QuestionTypeTag(question!!.type)
            Spacer(Modifier.height(8.dp))

            Text(
                question!!.stem,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gray800,
                lineHeight = 28.sp
            )
            Spacer(Modifier.height(16.dp))

            // Options
            options.forEachIndexed { idx, opt ->
                OptionButton(
                    index = idx,
                    text = opt,
                    isSelected = idx in selected,
                    isCorrect = if (isAnswered) correctAnswer.contains(idx) else null,
                    showResult = isAnswered,
                    isMultipleChoice = isMulti,
                    onClick = {
                        if (!isAnswered || (!isExam && !isMulti)) {
                            viewModel.toggleOption(idx)
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Judgment feedback
            if (isAnswered && !isExam) {
                Spacer(Modifier.height(12.dp))
                val feedBg = when {
                    correctAnswer.isEmpty() -> Amber50
                    isCorrect == true -> Green50
                    else -> Red50
                }
                val feedColor = when {
                    correctAnswer.isEmpty() -> Amber700
                    isCorrect == true -> Green700
                    else -> Red700
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = feedBg)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            when {
                                correctAnswer.isEmpty() -> "📝 本题暂无标准答案，请自行判断"
                                isCorrect == true -> "✅ 回答正确！"
                                else -> "❌ 回答错误！正确答案是：" +
                                        correctAnswer.map { ('A' + it) }.joinToString("、")
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = feedColor
                        )
                        if (question!!.analysis.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "💡 解析",
                                fontSize = 11.sp,
                                color = Blue500,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                question!!.analysis,
                                fontSize = 14.sp,
                                color = Gray700,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

        }

        // Bottom bar
        QuizBottomBar(
            isExam = isExam,
            progress = progress,
            isAnswered = isAnswered,
            isSingle = question!!.type == QuestionType.SINGLE || question!!.type == QuestionType.BOOLEAN,
            isMulti = question!!.type == QuestionType.MULTIPLE,
            canSubmit = selected.isNotEmpty(),
            onPrev = { viewModel.goPrev() },
            onNext = { viewModel.goNext() },
            onSubmit = { viewModel.submitAnswer() }
        )
    }
}

@Composable
private fun QuestionGrid(
    questions: List<Question>,
    answers: Map<Long, List<Int>>,
    currentIndex: Int,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(questions.size) { idx ->
            val q = questions[idx]
            val isAnswered = q.id in answers
            val isCurrent = idx == currentIndex
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isCurrent -> Blue50
                            isAnswered -> Green500
                            else -> Gray100
                        }
                    )
                    .border(
                        width = if (isCurrent) 2.dp else 0.dp,
                        color = if (isCurrent) Blue500 else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onJump(idx) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${idx + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isAnswered -> White
                        isCurrent -> Blue700
                        else -> Gray500
                    }
                )
            }
        }
    }
}

@Composable
private fun QuizBottomBar(
    isExam: Boolean,
    progress: com.quizhelper.app.util.ProgressInfo,
    isAnswered: Boolean,
    isSingle: Boolean,
    isMulti: Boolean,
    canSubmit: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Prev button (left)
            OutlinedButton(
                onClick = onPrev,
                enabled = progress.current > 1,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("← 上一题", fontSize = 13.sp)
            }

            // Center action button
            if (isExam && !isMulti) {
                Spacer(Modifier.width(1.dp))
            } else if (isExam && isMulti && !isAnswered) {
                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple600)
                ) {
                    Text("提交答案", fontSize = 13.sp)
                }
            } else if (isMulti && !isAnswered) {
                Button(
                    onClick = onSubmit,
                    enabled = canSubmit,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple600)
                ) {
                    Text("提交答案", fontSize = 13.sp)
                }
            } else if (isAnswered) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSingle) Green600 else Purple600
                    )
                ) {
                    Text(if (progress.current >= progress.total) "完成" else "下一题 →", fontSize = 13.sp)
                }
            } else {
                Button(
                    onClick = onNext,
                    enabled = canSubmit,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                ) {
                    Text(if (progress.current >= progress.total) "完成" else "下一题 →", fontSize = 13.sp)
                }
            }

            // Next button (right)
            OutlinedButton(
                onClick = onNext,
                enabled = progress.current < progress.total && !(isExam && !isAnswered) && !(isMulti && !isAnswered) && !(isExam && isMulti && !isAnswered),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("下一题 →", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ResultContent(
    result: QuizResult,
    onViewDetail: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray50)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        val titleIcon = if (result.mode == QuizMode.EXAM) "🏆" else "🎉"
        Text(
            titleIcon,
            fontSize = 40.sp
        )
        Text(
            if (result.mode == QuizMode.EXAM) "考试详情" else "练习详情",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Gray800
        )
        Spacer(Modifier.height(16.dp))

        val isPass = result.correctRate >= 80
        ScoreCircle(
            score = result.score,
            maxScore = result.maxScore ?: if (result.mode == QuizMode.EXAM) 100.0 else result.totalCount.toDouble()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "正确率 ${result.correctRate.toInt()}%",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isPass) Green600 else Red500
        )

        Spacer(Modifier.height(16.dp))


        // Exam breakdown
        if (result.mode == QuizMode.EXAM && result.breakdown != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BreakdownItem("单选题", result.breakdown.single, Blue600, Blue50)
                BreakdownItem("多选题", result.breakdown.multiple, Purple600, Purple50)
                BreakdownItem("判断题", result.breakdown.boolean, Amber600, Amber50)
            }
            Spacer(Modifier.height(16.dp))
        }

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScoreStat("正确", "${result.correctCount}")
            ScoreStat("已答", "${result.answeredCount}")
            ScoreStat("总题", "${result.totalCount}")
            ScoreStat("用时", TimeUtils.formatDuration(result.durationSeconds))
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton(text = "📋 查看详情", onClick = onViewDetail)
        Spacer(Modifier.height(10.dp))
        SecondaryButton(text = "🔄 再练一次", onClick = onRetry, textColor = Blue600)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Gray200),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray400)
        ) {
            Text("返回首页", fontSize = 14.sp)
        }
    }
}

@Composable
private fun BreakdownItem(label: String, breakdown: TypeBreakdown, color: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${breakdown.correct}/${breakdown.total}", fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        Text(label, color = Gray400, fontSize = 11.sp)
    }
}
