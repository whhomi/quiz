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
    var showChangelog by remember { mutableStateOf(false) }

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
            Text("墨答 · v2.2.8", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gray800)
            Text("优雅刷题，从容作答", fontSize = 13.sp, color = Gray500)
            Spacer(Modifier.height(16.dp))

            AboutClickableItem("📋", "更新日志", "查看各版本变更") { showChangelog = true }
            Spacer(Modifier.height(6.dp))
            AboutClickableItem("📧", "邮箱", "littleboy@example.com") {
                Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:littleboy@example.com") }.also { context.startActivity(it) }
            }
            Spacer(Modifier.height(6.dp))
            AboutClickableItem("🐙", "GitHub", "github.com/littleboy") {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/littleboy")).also { context.startActivity(it) }
            }
            Spacer(Modifier.height(6.dp))
            AboutClickableItem("📦", "项目地址", "github.com/littleboy/quiz-helper") {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/littleboy/quiz-helper")).also { context.startActivity(it) }
            }
            Spacer(Modifier.height(6.dp))
            AboutItem("👤", "作者", "littleboy")
            Spacer(Modifier.height(6.dp))
            AboutItem("📜", "开源协议", "MIT License")
            Spacer(Modifier.height(16.dp))
            Text("© 2026 littleboy", fontSize = 12.sp, color = Gray300)
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showChangelog) {
        ChangelogDialog(onDismiss = { showChangelog = false })
    }
}

@Composable
private fun ChangelogDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(4.dp))
                Text("📋 更新日志", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Gray800)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val changelog = getFullChangelog()
                Text(
                    text = changelog,
                    fontSize = 12.sp,
                    color = Gray600,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SmallButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Blue600,
                    textColor = White,
                    fontSize = 14
                )
            }
        }
    )
}




    
    private fun getFullChangelog(): String = """# 变更日志

[2.2.8] — 2026-06-07 02:47

🚀 新增功能
- 关于页新增「更新日志」按钮，弹窗展示完整CHANGELOG

🔧 CI
- 修复GitHub Actions release构建失败：生成debug keystore后签名

[2.2.7] — 2026-06-07 02:44

🎨 UI
- 更换为小红书风格App图标设计PNG
- 导入页彩蛋提示改为「想要彩蛋就要多练习或者多考试哦！」

[2.2.6] — 2026-06-07 02:36

🎨 UI
- App图标更换为AI生成的PNG图片

[2.2.5] — 2026-06-07 02:31

🎨 UI
- App图标：小红书风格（#FF2442暖红底 + 白色大对勾✓ + 右上墨滴点缀）

[2.2.4] — 2026-06-07 02:26

🎨 UI
- 关于页背景色统一为Gray50
- 全新图标：小红书风格，纯白对勾✓剪影+深海蓝纯色背景

[2.2.3] — 2026-06-07 02:16

🎨 UI
- 图标配色修正：极光蓝→科技紫渐变背景（aapt:attr gradient实现）

[2.2.2] — 2026-06-07 02:15

🎨 UI
- 图标重新设计：极简扁平风，打开的书+对勾+答题卡方格+悬浮钢笔尖+金色星芒

[2.2.1] — 2026-06-07 02:10

🎨 UI
- 关于页紧凑化重构：缩减图标/间距/行高，高度减少约50%
- 全新极简图标：蓝底白圈+蓝色对勾 ✓（象征答对）

[2.2.0] — 2026-06-07 01:52

🚀 新增功能
- 关于页面：作者信息、版本号、邮箱/GitHub可点击跳转、版权声明
- APK命名自定义：输出格式为「墨答-{版本号}-{构建类型}.apk」
- 历史记录清空按钮：标题旁新增🗑清空按钮（带二次确认）

🎨 UI
- 二次元风格App图标：粉色薰衣草底+星星+闪光+粉色书页+金色王冠
- 关于页重构成统一页面：无分模块、整行可点击、粉蓝二次元风
- 设置页简化：直接显示「💡 关于墨答」按钮
- 导航栏高度优化：缩小图标和文字，更紧凑

🐛 修复
- 考试详情页「返回历史列表」按钮无响应
- 完成弹窗导航卡顿（先关弹窗再导航）
- 预留给版本号递增提醒机制

[2.1.0] — 2026-06-07 00:56

🎨 UI 一致性大重构

🚀 新增功能
#### 两种考试模式
- 全随机考试：抽题后全部打乱顺序，题目完全随机
- 分类考试：先40单选 → 再100多选 → 最后60判断，按题型分组
- 考试题数修正：单选40题 / 多选100题 / 判断60题

#### 练习退出挽留弹窗
- 练习模式下按返回键弹出挽留鼓励弹窗
- 内置100句挽留语，随机展示
- 提供「确定退出」和「我再练练」两个选项

#### 彩蛋随机触发
- 彩蛋改为每次完成练习/考试后约8%概率随机触发
- 触发后在鼓励弹窗中显示彩蛋话语（琥珀色高亮）

📝 应用命名
- app 正式命名为 「墨答」（Mò Dá）
- 更新 AndroidManifest、strings.xml、HomeScreen 中的显示名称

🐛 修复
- 考试多选题提交按钮改为等用户点击「提交答案」后才提交
- 考试详情页「返回历史列表」按钮无响应
- 完成弹窗点击按钮卡顿（先关弹窗再导航）
- 练习模式返回按钮无确认弹窗
- 导航栏高度优化：缩小图标和文字大小
- 历史记录页新增清空按钮（带二次确认弹窗）
- 彩蛋提示词明确告知8%概率
- 导入题库按钮图标统一为书卷风格

#### 弹窗系统统一
- 所有弹窗统一视觉风格：RoundedCornerShape(20.dp)、居中标题、全宽按钮布局
- ConfirmDialog 重构：圆角弹窗、确认按钮全宽红底白字、取消按钮灰边框
- 新增 TimeWarningDialog：统一风格的时间提醒弹窗
- 鼓励语弹窗按钮使用 PrimaryButton + SecondaryButton + OutlinedButton 三级布局

#### 完成弹窗交互优化
- 考试/练习完成后先弹鼓励语弹窗（分数圈 + 正确率 + 随机鼓励语）
- 弹窗内提供三个直达按钮：「查看详情」/「再练一次」/「返回首页」
- 点击「查看详情」直接跳转到考试/练习详情页，无需二次点击

#### 底部导航栏统一
- 选中态：蓝色主题（蓝图标 + 蓝文字 + 浅蓝指示器）
- 未选中态：灰色图标 + 灰色文字
- 白底、无阴影，与全应用风格统一

#### 首页布局调整
- 模拟考试卡片去掉题型统计数据，改为简洁布局（限时说明 + 开始按钮）
- 与练习模式卡片视觉统一

#### 按钮全局统一
- 所有按钮统一使用 CommonComponents 中的规范组件
- 高度规格：40dp（小）/ 48dp（大）、圆角规格：10dp / 12dp
- 禁用态统一：灰色背景 Gray200 + 灰色文字

🚀 新增功能

#### 考试退出二次确认
- 考试模式下按返回键 → 弹出确认弹窗：「确定要退出考试吗？退出后本次答题记录将不会保存。」
- 提供「确定退出」和「取消」两个选项

#### 5 分钟倒计时提醒
- 考试剩余 5 分钟时自动弹窗提醒
- 统一风格的 TimeWarningDialog，琥珀色主题
- 提醒后继续作答，不影响答题状态

#### 鼓励语系统 + 彩蛋
- 内置 100 句分类鼓励语（6 类）
- 每次完成练习/考试弹窗随机展示
- 彩蛋：详情页连续点击 🏆/🎉 图标 7 次 → 触发隐藏鼓励语

🔧 技术改进
- 新增 BackHandler 拦截考试返回键
- ViewModel 新增 showTimeWarning / dismissTimeWarning()
- 新增 confirmColor 参数支持弹窗按钮颜色自定义

[2.0.0] — 2026-06-06

✨ 新增功能

#### 错题集系统
- 自动记录错题：练习和考试中答错的题目自动加入错题集
- 错题集页面：底部导航新增「错题」入口，首页显示错题数量概览
- 错题详情：点击错题可查看详情（题目、正确选项高亮、答案、解析）
- 错题练习：支持随机练习和顺序练习错题，答对自动移出错题集
- 数据联动：重新导入题库时，外键 CASCADE 自动清空错题集

#### 练习模式拆分
- 顺序练习：按照题库原始顺序逐题练习
- 随机练习：题目随机打乱顺序练习
- 首页两个独立入口按钮，路由带 practiceType 参数

#### 分数体系重构
- 得分与正确率分离：QuizResult 新增 correctRate 字段
- 练习：显示「X分」（正确题数）+ 下方「正确率 X%」
- 考试：显示「X分」（分数制）+ 下方「正确率 X%」
- 历史记录列表/详情同步展示得分和正确率
- ScoreCircle 组件改为显示「X分」而非「X/Y」

#### 考试交互优化
- 交卷按钮：从底部栏移至右上角 TopAppBar 区域
- 不可改选项：考试中作答后锁定，不可修改（ViewModel + UI 双层守卫）
- 不显示解析：考试模式不展示题目解析，仅练习模式显示

#### 练习交互优化
- 题目解析：顺序/随机练习均展示对错反馈 + 答案解析
- 自动提交：单选题/判断题点击选项即自动提交并显示反馈
- 简化底部栏：移除「提交答案」按钮，多选题需先选后提交
- 多选题点「下一题」自动提交后再翻页

🎯 题型识别改进

不再依赖 Excel 中的「题型」列，改为纯数据驱动：

🔧 技术改进

- 新增 WrongQuestion Room 实体表（FK→questions CASCADE）
- 数据库版本 1 → 2（fallbackToDestructiveMigration）
- 导航系统：新增 wrong_questions 和 wrong_detail/{questionId} 路由
- 底部导航：从 3 栏扩展为 4 栏（首页/错题/历史/设置）
- GitHub Actions：新增原生 Android 构建 job + 自动 Release

📄 文档

- [docs/android-native-architecture.md](docs/android-native-architecture.md) — 完整架构文档，覆盖：
  - 错题集模块（Entity/DAO/ViewModel/Screen）
  - 题型自动识别策略
  - 分数分离展示设计
  - 路由参数说明（practiceType、source）
  - 5 张 Room 表结构

[1.0.0] — 2026-06-04

初始发布

- Kotlin + Jetpack Compose 原生 Android 客户端
- MVVM 架构（AndroidViewModel + Flow + Compose）
- Room 本地数据库（4 张表：questions、question_bank_meta、history_records、history_details）
- 自研 Excel 解析器（ZipInputStream + SAXParser，无 Apache POI）
- 练习模式（随机出题、即时反馈、答题网格跳转）
- 考试模式（单 60 + 多 100 + 判 40 组卷、100 分钟限时、每题 0.5 分）
- 历史记录（列表/详情/题型筛选）
- 自定义 Material 3 主题（6 色系）
- 底部 3 栏导航（首页/历史/设置）
- 支持 Android 8.0+ (API 26)
- Capacitor Android WebView 壳工程同步构建""".trimIndent()

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
