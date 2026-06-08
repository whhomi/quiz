package com.quizhelper.app.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quizhelper.app.ui.components.*
import com.quizhelper.app.ui.theme.*

@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray50)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Gray50)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BackButton(onClick = { navController.popBackStack() })
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📖", fontSize = 40.sp)
            Spacer(Modifier.height(4.dp))
            Text("墨答 · v2.3.5", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gray800)
            Text("优雅刷题，从容作答", fontSize = 13.sp, color = Gray500)
            Spacer(Modifier.height(16.dp))

            AboutClickableItem("📋", "更新日志", "CHANGELOG") {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/whhomi/quiz/blob/main/CHANGELOG.md")).also { context.startActivity(it) }
            }
            Spacer(Modifier.height(6.dp))
            AboutClickableItem("📧", "Email", "gantalbravexv@gmail.com") {
                Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:gantalbravexv@gmail.com") }.also { context.startActivity(it) }
            }
            Spacer(Modifier.height(6.dp))
            AboutClickableItem("🐙", "GitHub", "github.com/whhomi") {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/whhomi")).also { context.startActivity(it) }
            }
            Spacer(Modifier.height(6.dp))
            AboutClickableItem("📦", "项目地址", "github.com/whhomi/quiz") {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/whhomi/quiz")).also { context.startActivity(it) }
            }
            Spacer(Modifier.height(6.dp))
            AboutClickableItem("🌐", "网页版", "https://quiz.gantalbravexv.workers.dev") {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://quiz.gantalbravexv.workers.dev/")).also { context.startActivity(it) }
            }
            Spacer(Modifier.height(6.dp))
            AboutItem("👤", "作者", "whhomi")
            Spacer(Modifier.height(6.dp))
            AboutItem("📜", "开源协议", "MIT License")
            Spacer(Modifier.height(12.dp))
            SmallButton(
                text = "📥 检查更新",
                onClick = {
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/whhomi/quiz/releases")).also { context.startActivity(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = Blue600,
                textColor = White,
                fontSize = 14
            )
            Spacer(Modifier.height(16.dp))
            Text("© 2026 whhomi", fontSize = 12.sp, color = Gray300)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AboutItem(emoji: String, label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Gray50
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 13.sp, color = Gray500, modifier = Modifier.width(60.dp))
            Spacer(Modifier.width(8.dp))
            Text(value, fontSize = 13.sp, color = Gray700, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AboutClickableItem(emoji: String, label: String, value: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Blue50
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 13.sp, color = Gray500, modifier = Modifier.width(60.dp))
            Spacer(Modifier.width(8.dp))
            Text(value, fontSize = 13.sp, color = Blue600, fontWeight = FontWeight.Medium)
        }
    }
}
