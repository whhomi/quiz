package com.quizhelper.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizhelper.app.ui.components.*
import com.quizhelper.app.ui.theme.*
import com.quizhelper.app.ui.navigation.Screen
import com.quizhelper.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val bankMeta by viewModel.bankMeta.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val wrongCount by viewModel.wrongCount.collectAsState()
    val hasBank = bankMeta != null

    var message by remember { mutableStateOf<String?>(null) }
    var messageType by remember { mutableStateOf("success") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Handle import results
    LaunchedEffect(Unit) {
        viewModel.importResult.collect { result ->
            if (result.success) {
                messageType = "success"
                message = "成功导入 ${result.totalCount} 道题" +
                        "（单选 ${result.singleCount}，" +
                        "多选 ${result.multipleCount}" +
                        if (result.booleanCount > 0) "，判断 ${result.booleanCount}" else "" +
                        "）"
            } else {
                messageType = "error"
                message = result.message
            }
        }
    }

    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFile(uri)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasBank) {
            // Empty state
            Text("📚", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "刷题助手",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Gray800
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "导入 Excel 题库，开始高效刷题练习\n支持单选、多选，自动判分，记录历史成绩",
                fontSize = 14.sp,
                color = Gray500,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(32.dp))

            PrimaryButton(
                text = if (isImporting) "正在解析..." else "📥 导入题库",
                onClick = {
                    if (hasBank) showConfirmDialog = true
                    else filePickerLauncher.launch(arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel"
                    ))
                },
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Blue600
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "支持 .xlsx / .xls 格式 · 所有数据仅存储于本地",
                fontSize = 11.sp,
                color = Gray400
            )
        } else {
            // Has question bank
            Text(
                "刷题助手",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Gray800
            )
            Spacer(Modifier.height(20.dp))

            // Bank stats card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("📋 当前题库", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("总题数", "${bankMeta?.totalCount ?: 0}", Blue600, Blue50)
                        StatItem("单选", "${bankMeta?.singleCount ?: 0}", Green600, Green50)
                        StatItem("多选", "${bankMeta?.multipleCount ?: 0}", Purple600, Purple50)
                        StatItem("判断", "${bankMeta?.booleanCount ?: 0}", Amber600, Amber50)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        TimeUtils.formatTimestamp(bankMeta?.importTime ?: 0) + " 导入",
                        fontSize = 11.sp,
                        color = Gray400,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Practice mode selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("📖 练习模式", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SmallButton(
                            text = "📖 顺序练习",
                            onClick = {
                                navController.navigate(Screen.Quiz.createRoute("practice", "sequential")) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = Blue600
                        )
                        SmallButton(
                            text = "🔀 随机练习",
                            onClick = {
                                navController.navigate(Screen.Quiz.createRoute("practice", "random")) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = Green600
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Exam section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("📝 模拟考试", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                    Spacer(Modifier.height(8.dp))
                    Text("限时 100 分钟 · 满分 100 分", fontSize = 12.sp, color = Gray400)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SmallButton(
                            text = "🔀 全随机考试",
                            onClick = {
                                navController.navigate(Screen.Quiz.createRoute("exam", examType = "full_random")) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = Amber600,
                            fontSize = 14
                        )
                        SmallButton(
                            text = "📋 分类考试",
                            onClick = {
                                navController.navigate(Screen.Quiz.createRoute("exam", examType = "grouped")) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = Amber600,
                            fontSize = 14
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Wrong questions section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📕 错题集", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                        if (wrongCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Red50
                            ) {
                                Text(
                                    "$wrongCount 题待复习",
                                    fontSize = 12.sp,
                                    color = Red600,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SmallButton(
                        text = if (wrongCount > 0) "📕 查看错题" else "暂无错题",
                        onClick = {
                            navController.navigate(Screen.WrongQuestions.route) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = if (wrongCount > 0) Red500 else Gray200,
                        textColor = if (wrongCount > 0) White else Gray500
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Secondary buttons
            SecondaryButton(
                text = "📊 历史记录",
                onClick = { navController.navigate("history") { launchSingleTop = true } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("导入新题库", color = Gray500, fontSize = 14.sp)
            }
        }

        // Message display
        if (message != null) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (messageType == "success") Green50 else Red50
                )
            ) {
                Text(
                    "${if (messageType == "success") "✅" else "❌"} $message",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = if (messageType == "success") Green700 else Red600
                )
            }
        }
    }

    // Replace bank confirmation
    if (showConfirmDialog) {
        ConfirmDialog(
            title = "导入新题库",
            message = "导入新题库将清空所有历史练习记录，是否继续？",
            confirmText = "确认导入",
            onConfirm = {
                showConfirmDialog = false
                filePickerLauncher.launch(arrayOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel"
                ))
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, textColor: androidx.compose.ui.graphics.Color, bgColor: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
        Text(label, fontSize = 11.sp, color = Gray500)
    }
}
