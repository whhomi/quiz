package com.quizhelper.app.ui.settings

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
import com.quizhelper.app.ui.components.ConfirmDialog
import com.quizhelper.app.ui.theme.*
import com.quizhelper.app.util.TimeUtils

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val bankMeta by viewModel.bankMeta.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "⚙️ 设置",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Gray800
        )
        Spacer(Modifier.height(20.dp))

        // Question bank info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("📋 当前题库", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                Spacer(Modifier.height(12.dp))

                if (bankMeta != null) {
                    SettingsRow("总题数", "${bankMeta!!.totalCount}")
                    SettingsRow("单选题", "${bankMeta!!.singleCount}")
                    SettingsRow("多选题", "${bankMeta!!.multipleCount}")
                    SettingsRow("判断题", "${bankMeta!!.booleanCount}")
                    SettingsRow("历史记录", "${historyList.size} 条")
                    SettingsRow("导入时间", TimeUtils.formatTimestamp(bankMeta!!.importTime))
                } else {
                    Text(
                        "暂未导入题库",
                        fontSize = 14.sp,
                        color = Gray400,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Data management
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("🗑 数据管理", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                Spacer(Modifier.height(12.dp))

                // Clear history
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("清空历史记录", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray700)
                        Text("保留题库，仅删除练习记录", fontSize = 12.sp, color = Gray400)
                    }
                    Button(
                        onClick = { showClearHistoryDialog = true },
                        enabled = historyList.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red50,
                            contentColor = Red500,
                            disabledContainerColor = Gray100,
                            disabledContentColor = Gray300
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("清空")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Gray100)

                // Clear all
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("清除全部数据", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray700)
                        Text("删除题库和所有历史记录", fontSize = 12.sp, color = Gray400)
                    }
                    Button(
                        onClick = { showClearAllDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red50,
                            contentColor = Red500
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("清除")
                    }
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        ConfirmDialog(
            title = "清空历史记录",
            message = "确定要清空所有练习记录吗？题库将保留。此操作不可恢复。",
            confirmText = "确认清空",
            onConfirm = {
                viewModel.clearHistory()
                showClearHistoryDialog = false
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }

    if (showClearAllDialog) {
        ConfirmDialog(
            title = "清除全部数据",
            message = "确定要清除题库和所有历史记录吗？此操作不可恢复。",
            confirmText = "确认清除",
            onConfirm = {
                viewModel.clearAllData()
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Gray500)
        Text(value, fontSize = 14.sp, color = Gray700, fontWeight = FontWeight.Medium)
    }
}
