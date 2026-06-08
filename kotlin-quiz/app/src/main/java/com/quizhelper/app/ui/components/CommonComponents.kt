package com.quizhelper.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizhelper.app.data.model.QuestionType
import com.quizhelper.app.data.model.QuizResult
import com.quizhelper.app.ui.theme.*
import com.quizhelper.app.util.Encouragement

@Composable
fun QuestionTypeTag(type: QuestionType, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (type) {
        QuestionType.SINGLE -> Triple("单选题", Blue100, Blue700)
        QuestionType.MULTIPLE -> Triple("多选题", Purple100, Purple600)
        QuestionType.BOOLEAN -> Triple("判断题", Amber100, Amber700)
    }
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = fg,
        modifier = modifier
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun OptionButton(
    index: Int,
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean?,
    showResult: Boolean,
    isMultipleChoice: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val optionLetter = ('A' + index)
    val borderColor = when {
        showResult && isCorrect == true -> Green500
        showResult && isCorrect == false && isSelected -> Red500
        isSelected -> Blue500
        else -> Gray200
    }
    val bgColor = when {
        showResult && isCorrect == true -> Green50
        showResult && isCorrect == false && isSelected -> Red50
        isSelected -> Blue50
        else -> White
    }
    val indicatorColor = when {
        showResult && isCorrect == true -> Green500
        showResult && isCorrect == false && isSelected -> Red500
        isSelected -> Blue500
        else -> Gray200
    }
    val indicatorTextColor = when {
        showResult && isCorrect == true -> White
        showResult && isCorrect == false && isSelected -> White
        isSelected -> White
        else -> Gray600
    }
    val indicatorShape = if (isMultipleChoice) RoundedCornerShape(4.dp) else RoundedCornerShape(12.dp)

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected || showResult) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(indicatorShape)
                    .background(indicatorColor),
                contentAlignment = Alignment.Center
            ) {
                if (showResult && isCorrect == true) {
                    Text("✓", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                } else if (showResult && isCorrect == false && isSelected) {
                    Text("✗", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                } else {
                    Text(
                        optionLetter.toString(),
                        color = indicatorTextColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = text.ifBlank { "(空)" },
                fontSize = 15.sp,
                color = Gray700,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ProgressBar(answered: Int, total: Int, modifier: Modifier = Modifier) {
    val progress = if (total > 0) answered.toFloat() / total else 0f
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Gray100)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(Blue500)
            )
        }
    }
}

@Composable
fun ScoreCircle(
    score: Double,
    maxScore: Double? = null,
    modifier: Modifier = Modifier
) {
    val percentage = if (maxScore != null && maxScore > 0) (score / maxScore * 100) else score
    val isPass = percentage >= 80
    val displayText = if (maxScore != null && maxScore > 0) {
        "${score.toInt()}分"
    } else {
        "${score.toInt()}%"
    }

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { (percentage / 100f).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = if (isPass) Green500 else Red500,
            trackColor = Gray200,
            strokeWidth = 8.dp
        )
        Text(
            text = displayText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPass) Green600 else Red500,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ScoreStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gray800)
        Text(label, fontSize = 12.sp, color = Gray400)
    }
}

// ═══════════════════════════════════════════════
//  统一按钮组件
// ═══════════════════════════════════════════════

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Blue600,
    textColor: Color = White,
    fontSize: Int = 15
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = textColor,
            disabledContainerColor = Gray200,
            disabledContentColor = Gray400
        ),
        content = { Text(text, fontSize = fontSize.sp) }
    )
}

@Composable
fun SmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Blue600,
    textColor: Color = White,
    fontSize: Int = 13
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = textColor,
            disabledContainerColor = Gray200,
            disabledContentColor = Gray400
        ),
        content = { Text(text, fontSize = fontSize.sp) }
    )
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = Gray700,
    fontSize: Int = 14
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) Gray300 else Gray100),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
        content = { Text(text, fontSize = fontSize.sp) }
    )
}

@Composable
fun BackButton(
    text: String = "← 返回",
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(text, fontSize = 14.sp, color = Gray500)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    confirmColor: Color = Red500,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(4.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Gray800)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(message, fontSize = 14.sp, color = Gray600, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SmallButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = confirmColor,
                    textColor = White,
                    fontSize = 14
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Gray200),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray500)
                ) {
                    Text("取消", fontSize = 14.sp)
                }
            }
        }
    )
}

@Composable
fun TimeWarningDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏰", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text("时间提醒", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Gray800)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "距离考试结束还剩 5 分钟",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Amber600,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text("请抓紧时间作答！", fontSize = 14.sp, color = Gray600, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SmallButton(
                    text = "知道了",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Amber600,
                    textColor = White,
                    fontSize = 14
                )
            }
        }
    )
}

@Composable
fun ExitConfirmDialog(
    message: String,
    confirmText: String = "确定退出",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("😢 再坚持一下", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Gray800)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(4.dp))
                Text(message, fontSize = 15.sp, color = Gray700, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("确定要退出吗？", fontSize = 14.sp, color = Gray500, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SmallButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Red500,
                    textColor = White,
                    fontSize = 14
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Gray200),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray500)
                ) {
                    Text("我再练练", fontSize = 14.sp)
                }
            }
        }
    )
}

@Composable
fun EncouragementDialog(
    result: QuizResult,
    onViewDetail: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val encouragement = remember { if (Encouragement.rollEasterEgg()) Encouragement.EASTER_EGG_MESSAGE else Encouragement.random() }
    val isPass = result.correctRate >= 80
    val isEasterEgg = encouragement == Encouragement.EASTER_EGG_MESSAGE

    AlertDialog(
        onDismissRequest = onHome,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (result.mode.name == "EXAM") "🏆" else "🎉",
                    fontSize = 48.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (result.mode.name == "EXAM") "考试完成！" else "练习完成！",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray800
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScoreCircle(
                    score = result.score,
                    maxScore = result.maxScore ?: if (result.mode.name == "EXAM") 100.0 else result.totalCount.toDouble(),
                    modifier = Modifier.size(100.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "正确率 ${result.correctRate.toInt()}%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPass) Green600 else Red500
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Blue50)
                ) {
                    Text(
                        "💪 $encouragement",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isEasterEgg) Amber700 else Blue700,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
                confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimaryButton(
                    text = "📋 查看详情",
                    onClick = onViewDetail,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 15
                )
                Spacer(Modifier.height(8.dp))
                SmallButton(
                    text = "🔄 再练一次",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Green600,
                    textColor = White,
                    fontSize = 14
                )

                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Gray300),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray600)
                ) {
                    Text("返回首页", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    )
}
