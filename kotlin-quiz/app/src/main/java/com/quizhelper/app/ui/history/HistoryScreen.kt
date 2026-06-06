package com.quizhelper.app.ui.history

import androidx.compose.foundation.clickable
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
import com.quizhelper.app.data.model.HistoryRecord
import com.quizhelper.app.ui.components.*
import com.quizhelper.app.ui.theme.*
import com.quizhelper.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = viewModel()
) {
    val historyList by viewModel.historyList.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "📊 历史记录",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Gray800
            )
            if (historyList.isNotEmpty()) {
                TextButton(onClick = { showClearDialog = true }) {
                    Text("🗑 清空", color = Red500, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (historyList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("暂无练习记录", color = Gray400, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(historyList) { record ->
                    HistoryCard(
                        record = record,
                        onClick = { navController.navigate("history/${record.id}") }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "清空历史记录",
            message = "确定要清空所有练习记录吗？此操作不可恢复。",
            confirmText = "确认清空",
            onConfirm = {
                showClearDialog = false
                viewModel.clearHistory()
            },
            onDismiss = { showClearDialog = false }
        )
    }
}

@Composable
private fun HistoryCard(record: HistoryRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        TimeUtils.formatTimestamp(record.timestamp),
                        fontSize = 13.sp,
                        color = Gray400
                    )
                    if (record.mode == "exam") {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "考试",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Amber700,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                val correctRate = if (record.totalCount > 0) (record.correctCount.toDouble() / record.totalCount * 100).toInt() else 0
                val scoreColor = when {
                    record.mode == "exam" -> if (correctRate >= 60) Green700 else Red600
                    correctRate >= 80 -> Green700
                    correctRate >= 60 -> Amber700
                    else -> Red600
                }
                val bgColor = when {
                    record.mode == "exam" -> if (correctRate >= 60) Green50 else Red50
                    correctRate >= 80 -> Green50
                    correctRate >= 60 -> Amber50
                    else -> Red50
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = bgColor
                ) {
                    Text(
                        "${record.score.toInt()}分",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "正确 ${record.correctCount}/${record.answeredCount}",
                    fontSize = 13.sp,
                    color = Gray500
                )
                val rate = if (record.totalCount > 0) (record.correctCount.toDouble() / record.totalCount * 100).toInt() else 0
                Text(
                    "正确率 ${rate}%",
                    fontSize = 13.sp,
                    color = Gray500
                )
                Text(
                    "⏱ ${TimeUtils.formatDuration(record.duration)}",
                    fontSize = 13.sp,
                    color = Gray500
                )
            }
        }
    }
}
