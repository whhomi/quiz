package com.quizhelper.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizhelper.app.data.model.QuestionType
import com.quizhelper.app.ui.theme.*

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
    val isPass = percentage >= 60
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

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message, fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = Red500)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Gray500)
            }
        }
    )
}
