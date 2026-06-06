package com.quizhelper.app.ui.about

import android.content.Intent
import android.net.Uri
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
import com.quizhelper.app.ui.components.BackButton
import com.quizhelper.app.ui.theme.*

@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        BackButton(text = "← 返回", onClick = { navController.popBackStack() })

        Spacer(Modifier.height(16.dp))

        // App header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📖", fontSize = 52.sp)
                Spacer(Modifier.height(8.dp))
                Text("墨答", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Gray800)
                Text("v2.1.0", fontSize = 14.sp, color = Gray400)
                Spacer(Modifier.height(6.dp))
                Text("优雅刷题，从容作答", fontSize = 14.sp, color = Gray500, textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(16.dp))

        // All info in one card (author + links + copyright)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(20.dp)) {

                // Author section
                Text("👤 作者", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                Spacer(Modifier.height(12.dp))
                InfoRow("作者", "littleboy")
                Spacer(Modifier.height(8.dp))

                // Clickable email
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:littleboy@example.com")
                        }
                        context.startActivity(intent)
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("邮箱", fontSize = 14.sp, color = Gray500)
                    Text("littleboy@example.com →", fontSize = 14.sp, color = Blue600, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))

                // Clickable GitHub
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/littleboy"))
                        context.startActivity(intent)
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GitHub", fontSize = 14.sp, color = Gray500)
                    Text("github.com/littleboy →", fontSize = 14.sp, color = Blue600, fontWeight = FontWeight.Medium)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Gray100)

                // Links section
                Text("🔗 项目", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/littleboy/quiz-helper"))
                        context.startActivity(intent)
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("项目地址", fontSize = 14.sp, color = Gray500)
                    Text("GitHub →", fontSize = 14.sp, color = Blue600, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                InfoRow("开源协议", "MIT License")

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Gray100)

                // Copyright section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "© 2026 littleboy. All rights reserved.",
                        fontSize = 13.sp,
                        color = Gray400,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "本软件仅供学习交流使用",
                        fontSize = 12.sp,
                        color = Gray300,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Gray500)
        Text(value, fontSize = 14.sp, color = Gray700, fontWeight = FontWeight.Medium)
    }
}
