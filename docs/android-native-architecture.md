# 刷题助手 — Android 原生客户端架构文档

> 基于 Kotlin + Jetpack Compose 构建的原生刷题应用  
> 包名：`com.quizhelper.app` | 版本：`2.0.0` | minSdk: 26 | targetSdk: 36

---

## 目录

1. [项目概览](#1-项目概览)
2. [技术栈](#2-技术栈)
3. [项目结构](#3-项目结构)
4. [功能清单](#4-功能清单)
5. [架构分层](#5-架构分层)
   - 5.1 [数据层 (Data Layer)](#51-数据层)
   - 5.2 [业务逻辑层 (Repository + ViewModel)](#52-业务逻辑层)
   - 5.3 [UI 层 (Jetpack Compose)](#53-ui-层)
   - 5.4 [工具层 (Utilities)](#54-工具层)
6. [核心流程](#6-核心流程)
   - 6.1 [题库导入流程](#61-题库导入流程)
   - 6.2 [练习模式流程](#62-练习模式流程)
   - 6.3 [考试模式流程](#63-考试模式流程)
   - 6.4 [历史记录流程](#64-历史记录流程)
7. [导航与路由](#7-导航与路由)
8. [数据模型详解](#8-数据模型详解)
9. [关键设计决策](#9-关键设计决策)
10. [依赖清单](#10-依赖清单)

---

## 1. 项目概览

| 项目 | 说明 |
|------|------|
| **项目目录** | `kotlin-quiz/` |
| **应用名称** | 墨答 |
| **应用 ID** | `com.quizhelper.app` |
| **当前版本** | 2.0.0 (versionCode 2) |
| **最低系统** | Android 8.0 (API 26) |
| **目标系统** | Android 16 (API 36) |
| **开发语言** | Kotlin 100% |
| **UI 框架** | Jetpack Compose (Material 3) |
| **数据库** | Room (SQLite, 5 张表) |
| **架构模式** | MVVM (Model-View-ViewModel) |
| **鼓励语系统** | 内置 100 句 + 彩蛋彩蛋 |

### 与其他模块的关系

- **Capacitor `android/` 目录**：这是项目的 WebView 壳工程（Capacitor 构建产物），用于将 Web 版打包成 APK。两者是**独立并存**的 Android 项目，可分别构建。
- **`src/`（Web 前端）**：Vue 3 Web 版源码，与本原生客户端功能平行但代码独立。

---

## 2. 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.0.0 |
| UI | Jetpack Compose + Material 3 | BOM 2024.02.00 |
| 导航 | Navigation Compose | 2.7.7 |
| 数据库 | Room (SQLite) | 2.6.1 |
| ORM 编译 | KSP (Kotlin Symbol Processing) | 2.0.0-1.0.21 |
| 异步 | Kotlin Coroutines / Flow | 1.8.0 |
| JSON | Gson | 2.10.1 |
| Excel 解析 | Android 原生 SAX Parser (无 Apache POI) | 内置 |
| 构建 | Gradle + Android Gradle Plugin | 8.4.0 |
| 签名 | Android Debug Keystore | 本地 |

### 无外部依赖的特性

- **Excel (.xlsx) 解析**：未使用 Apache POI，而是利用 xlsx = ZIP + XML 的特性，用 `ZipInputStream` + `SAXParser` 直接解析。
- **主题与颜色**：纯自定义颜色系统，未使用 Material 预设调色板。

---

## 3. 项目结构

```
kotlin-quiz/
├── build.gradle.kts              # 根构建配置（AGP 8.4, Kotlin 2.0, KSP）
├── settings.gradle.kts           # 项目设置
├── gradle.properties             # Gradle 属性
├── gradle/wrapper/               # Gradle Wrapper
├── local.properties              # 本地 Android SDK 路径
│
└── app/
    ├── build.gradle.kts          # 模块构建配置
    ├── proguard-rules.pro        # 混淆规则
    └── src/main/
        ├── AndroidManifest.xml   # 清单文件（权限、Activity、intent-filter）
        ├── res/                  # 资源文件（图标、主题样式、字符串）
        │
        └── java/com/quizhelper/app/
            ├── MainActivity.kt           # 入口 Activity (ComponentActivity)
            ├── QuizHelperApp.kt          # Application 类（数据库单例宿主）
            │
            ├── data/
            │   ├── db/
            │   │   ├── AppDatabase.kt    # Room 数据库定义（4 张表）
            │   │   ├── QuestionDao.kt    # 题目 & 题库元数据 DAO
            │   │   └── HistoryDao.kt     # 历史记录 & 答题详情 DAO
            │   │
            │   ├── model/
            │   │   ├── Question.kt       # 题目实体 + 题库元数据
            │   │   ├── HistoryRecord.kt  # 历史记录 & 答题详情实体
            │   │   ├── QuizSession.kt    # 答题会话 + 结果 + 评判
            │   │   └── ImportResult.kt   # 导入结果
            │   │
            │   ├── parser/
            │   │   └── ExcelParser.kt    # xlsx 解析器（SAX + ZIP）
            │   │
            │   └── repository/
            │       └── QuizRepository.kt # 统一数据仓库
            │
            ├── ui/
            │   ├── components/
            │   │   ├── MainScreen.kt     # 根 Scaffold（底部导航框架）
            │   │   ├── BottomNavBar.kt   # 底部导航栏（首页/历史/设置）
            │   │   └── CommonComponents.kt # 通用 UI 组件
            │   │
            │   ├── navigation/
            │   │   ├── Screen.kt         # 路由定义 sealed class
            │   │   └── NavGraph.kt       # 导航图定义
            │   │
            │   ├── home/
            │   │   ├── HomeScreen.kt     # 首页（题库导入 + 模式选择）
            │   │   └── HomeViewModel.kt  # 首页 ViewModel
            │   │
            │   ├── quiz/
            │   │   ├── QuizScreen.kt     # 答题主界面
            │   │   ├── QuizViewModel.kt  # 答题 ViewModel（核心逻辑）
            │   │   └── ResultScreen.kt   # 结果展示页（独立路由）
            │   │
            │   ├── history/
            │   │   ├── HistoryScreen.kt       # 历史列表页
            │   │   ├── HistoryDetailScreen.kt # 历史详情页
            │   │   └── HistoryViewModel.kt    # 历史 ViewModel
            │   │
            │   ├── wrong/
            │   │   ├── WrongQuestionsScreen.kt   # 错题集页面
            │   │   └── WrongQuestionsViewModel.kt # 错题集 ViewModel
            │   │
            │   ├── settings/
            │   │   ├── SettingsScreen.kt      # 设置页
            │   │   └── SettingsViewModel.kt   # 设置 ViewModel
            │   │
            │   └── theme/
            │       ├── Color.kt         # 颜色系统（蓝/绿/红/紫/琥珀/灰）
            │       └── Theme.kt         # Material 3 主题配置
            │
            └── util/
                ├── Encouragement.kt     # 鼓励语系统（100 句 + 彩蛋）
                ├── Logger.kt            # 日志工具（封装 Android Log）
                ├── QuizEngine.kt        # 题冧引擎（核心业务逻辑）
                └── TimeUtils.kt         # 时间格式化工具
```

---

## 4. 功能清单

### 4.1 题库导入

| 功能 | 描述 |
|------|------|
| Excel 导入 | 支持 `.xlsx` / `.xls` 格式，通过系统文件选择器选取 |
| 自动列识别 | 智能匹配表头（中文/英文别名），自动检测列位置 |
| 题型推导 | 根据"判断"/"多选"关键词或答案数量自动判断题型 |
| 数据验证 | 空题干/不足2选项/答案格式错误 → 跳过并警告 |
| 上限保护 | 最多 10000 题，超出报错 |
| 覆盖导入 | 导入新题库时自动清空旧题库 + 历史记录 |
| 兼容模式 | Android 10+ 使用 scoped storage，旧版本请求 `READ_EXTERNAL_STORAGE` |

### 4.2 练习模式 (Practice)

| 功能 | 描述 |
|------|------|
| 顺序练习 | 按照题库原有顺序逐题练习 |
| 随机练习 | 全部题目随机打乱顺序练习 |
| 即时反馈 | 提交答案后立即显示对错和正确答案 |
| 自动提交 | 单/判断题选择后直接判对错 |
| 提交答案 | 多选题需手动点击「提交答案」后判对错 |
| 解析查看 | 每道题可查看解析说明 |
| 答题网格 | 题号网格，已答绿色/未答灰色/当前蓝色高亮 |
| 跳题 | 通过网格点击任意跳转（未答多选题不可跳） |
| 自由导航 | 上一题/下一题，可反复修改已答题 |
| 进度条 | 顶部进度条显示答题进度 |
| **得分展示** | 分开显示**得分**（正确题数）和**正确率**（百分比） |

### 4.3 考试模式 (Exam)

| 功能 | 描述 |
|------|------|
| 标准化组卷 | 单选 60 题 + 多选 100 题 + 判断 40 题 = 200 题 |
| 限时机制 | 总计 6000 秒（100 分钟），倒计时显示 |
| 超时自动交卷 | 时间耗尽自动提交 |
| 选后即判 | 选择答案立即记录并显示对错（不可改） |
| 分数换算 | 每题 0.5 分，满分 = 总题数 × 0.5 |
| 分项统计 | 按题型展示正确率（单选/多选/判断） |
| 限时视觉反馈 | <10 分钟橙色警告，<5 分钟红色危险 |
| **得分展示** | 分开显示**得分**（分数制）和**正确率**（百分比） |

### 4.4 错题集

| 功能 | 描述 |
|------|------|
| 自动记录 | 答题时自动将答错的题目加入错题集 |
| 错题练习 | 支持随机练习和顺序练习错题 |
| 逐题移除 | 可在错题集中单独移除题目 |
| 清空操作 | 一键清空全部错题记录 |
| 练习自动移出 | 在错题集练习中答对后自动移出错题集 |
| 底部导航 | 底部栏"错题"入口直达错题集 |
| 首页入口 | 首页显示错题数量概览及入口 |
| 数据联动 | 重新导入题库时自动清空错题集（外键级联） |

### 4.5 历史记录

| 功能 | 描述 |
|------|------|
| 历史列表 | 按时间倒序展示所有练习记录 |
| 练习/考试标识 | 考试记录带"考试"标签 |
| 分数着色 | 绿色及格 / 黄色中等 / 红色不及格 |
| 详情查看 | 逐题展示用户答案 vs 正确答案 |
| 题目筛选 | 全部 / 单选 / 多选 / 判断 分类查看 |
| 解析回顾 | 详情页每道题都可查看解析 |

### 4.6 设置

| 功能 | 描述 |
|------|------|
| 题库信息 | 显示总题数、各题型数量、导入时间 |
| 历史统计 | 历史记录条数 |
| 清空历史 | 删除所有练习记录，保留题库 |
| 清除全部 | 删除题库 + 所有历史记录 |

---

## 5. 架构分层

### 5.1 数据层

#### Room 数据库 — `AppDatabase.kt`

5 张表，版本 2，使用 `fallbackToDestructiveMigration()`（降级破坏式迁移）。

| 表名 | 实体 | 主键 | 说明 |
|------|------|------|------|
| `questions` | `Question` | `id` (自增) | 题目数据 |
| `question_bank_meta` | `QuestionBankMeta` | `id=1` | 题库元数据（单行表） |
| `history_records` | `HistoryRecord` | `id` (UUID) | 练习/考试历史 |
| `history_details` | `HistoryDetail` | `id` (自增) | 每道题的答题详情 |
| `wrong_questions` | `WrongQuestion` | `id` (自增) | 错题记录（FK→questions，CASCADE） |

#### DAO — `QuestionDao.kt`

主要操作：
- **查询**：全部题目（Flow/一次）、按类型查询、按 ID 查询、统计总数/各类型数
- **写入**：批量插入（REPLACE 冲突策略）、单条插入
- **删除**：全部删除
- **题库元数据**：插入/查询/Flow 监听
- **事务**：`replaceAll()` — 全量替换题库（删除旧题 → 写入新题 → 删除旧元数据 → 写入新元数据）

#### DAO — `HistoryDao.kt`

主要操作：
- **查询**：全部历史（Flow）、按 ID 查询、详情列表
- **写入**：单条历史记录、批量答题详情
- **事务**：`insertFullRecord()` — 写入记录 + 明细
- **删除**：全部清空

### 5.2 业务逻辑层

#### QuizRepository — 统一数据仓库

```kotlin
class QuizRepository(private val context: Context)
```

职责：
- 持有 DB、DAO、Gson、Logger
- 对外暴露：`bankMeta: Flow`、`allQuestions: Flow`、`allHistory: Flow`
- 导入流程编排：解析 Excel → 统计题型 → `questionDao.replaceAll()` → `historyDao.deleteAllHistory()`
- 保存结果：`HistoryRecord` + `HistoryDetail` 构建 → 事务写入
- 提供获取/清除数据的统一入口

#### ViewModel 层

共 5 个 ViewModel，均继承 `AndroidViewModel`（持有 `Application` 引用）：

| ViewModel | 对应的 Screen | 核心职责 |
|-----------|---------------|----------|
| `HomeViewModel` | HomeScreen | 题库元数据监听、Excel 文件导入 |
| `QuizViewModel` | QuizScreen/ResultScreen | 答题会话管理、模式切换、提交判分 |
| `HistoryViewModel` | HistoryScreen | 历史列表监听 |
| `HistoryDetailViewModel` | HistoryDetailScreen | 单条历史加载、详情筛选 |
| `SettingsViewModel` | SettingsScreen | 数据清除操作 |

所有 ViewModel 共享 `QuizRepository` 实例。

### 5.3 UI 层

采用 **Jetpack Compose** + **Material 3**，遵循无状态组件模式：

- **状态提升**：所有可变状态在 ViewModel 中管理，Screen 组件通过 `collectAsState()` 订阅
- **导航**：`NavHost` + `composable()` 路由
- **底部导航**：三级导航（首页/历史/设置），答题和结果页隐藏底部栏

#### UI 组件层次

```
MainScreen (Scaffold + 底部导航)
├── NavGraph
│   ├── HomeScreen          // 首页（无题库: 引导导入；有题库: 练习/考试入口）
│   ├── QuizScreen          // 答题界面（题号栏 + 题目 + 选项 + 反馈 + 底部操作栏）
│   ├── ResultScreen        // 结果界面（分数 + 统计 + 操作按钮）
│   ├── HistoryScreen       // 历史列表
│   ├── HistoryDetailScreen // 历史详情（逐题展示 + 筛选）
│   └── SettingsScreen      // 设置页
```

#### 通用 UI 组件 (`CommonComponents.kt`)

| 组件 | 用途 |
|------|------|
| `QuestionTypeTag` | 题型标签（单选蓝/多选紫/判断琥珀） |
| `OptionButton` | 选项按钮（选中/正确/错误状态，各有颜色区分） |
| `ProgressBar` | 答题进度条 |
| `ScoreCircle` | 圆形分数展示（60分以上绿色） |
| `ScoreStat` | 统计数字组件 |
| `ConfirmDialog` | 通用确认弹窗（圆角20dp + 全宽按钮 + 可自定义颜色） |
| `TimeWarningDialog` | 考试5分钟倒计时提醒弹窗（琥珀色主题） |
| `EncouragementDialog` | 完成鼓励弹窗（分数 + 鼓励语 + 三个直达按钮） |
| `PrimaryButton` | 主操作按钮（蓝底白字，48dp高，圆角12dp） |
| `SmallButton` | 次要操作按钮（40dp高，圆角10dp） |
| `SecondaryButton` | 轮廓按钮（灰边框，48dp高，圆角12dp） |
| `BackButton` | 返回按钮（灰色文字，无背景） |

### 5.4 工具层

#### QuizEngine — 题冧引擎（核心）

无状态工具类（`object`），不依赖 Android 环境：

| 方法 | 说明 |
|------|------|
| `shuffle()` | Fisher-Yates 洗牌算法 |
| `judge()` | 比较用户答案与正确答案（排序后逐元素比较） |
| `createSession()` | 创建答题会话（可选随机顺序 + 限时） |
| `getCurrentQuestion()` | 获取当前题目 |
| `submitAnswer()` | 提交答案 → 生成评判结果 |
| `goToQuestion/next/prev()` | 题目导航 |
| `getProgress()` | 进度统计 |
| `computeScore()` | 练习模式计分 |
| `buildResult()` | 构建练习结果 |
| `selectExamQuestions()` | 按配额抽题（单60/多100/判40） |
| `computeExamScore()` | 考试模式计分（每题0.5分） |
| `buildExamResult()` | 构建考试结果 |

#### Encouragement — 鼓励语系统

```kotlin
object Encouragement
```

100 句内置鼓励语，分 6 类（学霸、轻松、燃系、禅意、考试、通用），每次练习/考试完成后随机展示一条。

| 方法 | 说明 |
|------|------|
| `random()` | 随机返回一条鼓励语 |
| `checkEasterEgg()` | 彩蛋检测（连续调用 7 次返回 true） |
| `EASTER_EGG_MESSAGE` | 彩蛋内容："嘻嘻，你打出彩蛋了，你会考过的！ 🎉" |

彩蛋触发方式：在结果页连续点击 🏆/🎉 图标 7 次。

#### Logger — 日志封装

```kotlin
Logger.create("QuizVM") -> Log tag: "QuizHelper/QuizVM"
```

包装 Android `Log.{d,i,w,e}` 方法，统一日志前缀。

#### TimeUtils — 时间工具

| 方法 | 输出格式 |
|------|----------|
| `formatTimestamp()` | `2026/06/06 21:30` |
| `formatTimestampFull()` | `2026/06/06 21:30:15` |
| `formatDuration()` | `5分30秒` / `30秒` |
| `formatCountdown()` | `05:30` (MM:SS) |

---

## 6. 核心流程

### 6.1 题库导入流程

```
用户点击"导入题库"
    → 系统文件选择器 (OpenDocument)
    → 选择 .xlsx 文件
    → HomeViewModel.importFile(uri)
        → QuizRepository.importExcel(uri) [Dispatchers.IO]
            → ExcelParser.parse(context, uri)
                → ZipInputStream 读取 xlsx
                → 解压到内存 (entries: Map<name, bytes>)
                → 解析 xl/sharedStrings.xml → 共享字符串表
                → 解析 xl/worksheets/sheet1.xml → SAX 逐行解析
                → findHeaderRow(): 前 10 行扫描匹配表头
                → detectColumns(): 匹配列别名
                → 逐行读取数据 → 构建 Question 对象
                → 验证: 题干、选项、答案格式检查
                → 返回 ParseResult
            → 统计各题型数量
            → questionDao.replaceAll() (@Transaction)
            → historyDao.deleteAllHistory()
            → 返回 ImportResult
    → HomeScreen 显示导入结果消息
```

**Excel 解析亮点**：
- 无需 Apache POI，直接操作 XML
- 智能表头匹配，支持中英文列名
- 自动查找表头行（前 10 行评分最高者）
- 答案格式兼容：`A/B/C`、`A,B,C`、`正确/错误`、数字索引

### 6.2 练习模式流程

```
首页 → 点击"顺序练习"或"随机练习"
    → navController.navigate("quiz/practice?practiceType=sequential&source=all")
    → QuizScreen 加载
    → QuizViewModel.startPractice(random= 是否随机, source="all")
        → repository.getAllQuestionsList()
        → QuizEngine.createSession(questions, random=是否随机, mode=PRACTICE)
    → 逐题展示
        → 单选题/判断题: 点击选项 → 自动提交 → 显示对错 + 解析 → 可自由翻页
        → 多选题: 点击勾选选项 → 点击「提交答案」 → 显示对错 + 解析 → 可自由翻页
    → 用户可: 上一题 / 下一题(自动提交) / 题号网格跳转
    → 点击"完成"
        → QuizEngine.buildResult()
        → repository.saveResult()
    → 显示 ResultContent 结果页（含随机鼓励语 + 分数 + 正确率 + 操作按钮）
```

### 6.3 考试模式流程

```
首页 → 点击"开始考试"
    → navController.navigate("quiz/exam")
    → QuizViewModel.startExam()
        → repository.getAllQuestionsList()
        → QuizEngine.selectExamQuestions() → 抽题（单60/多100/判40）
        → QuizEngine.createSession(questions, random=false, mode=EXAM, timeLimit=6000)
        → startTimer() → 起计时协程
    → 考试中
        → 倒计时显示在顶栏
        → 选择任意选项 → 自动提交 → 显示对错 → 锁定答案（不可改）
        → 可上下翻页或跳题
    → 超时 / 点击"交卷"
        → finishQuiz()
        → QuizEngine.buildExamResult()
        → repository.saveResult()
    → 显示分数（满分制）、分项统计、各按钮
```

### 6.4 历史记录流程

```
底部导航 → "历史"
    → HistoryViewModel: repository.allHistory (Flow)
    → LazyColumn 展示历史列表

点击某条记录 → navigate("history/{id}")
    → HistoryDetailScreen
    → HistoryDetailViewModel.load(id)
        → repository.getHistoryById()
        → repository.getHistoryDetails()
        → repository.getAllQuestionsList() (用于展示题目内容)
    → 显示分数概览 + 逐题答案对比 + 解析
    → 支持按题型筛选
```

---

## 7. 导航与路由

### 路由定义 (`Screen.kt`)

```kotlin
sealed class Screen(val route: String) {
    data object Home              : Screen("home")
    data object Quiz              : Screen("quiz/{mode}?practiceType={practiceType}&source={source}")
    data object Result            : Screen("result/{sessionId}")
    data object History           : Screen("history")
    data object HistoryDetail     : Screen("history/{id}")
    data object WrongQuestions    : Screen("wrong_questions")
    data object WrongQuestionDetail : Screen("wrong_detail/{questionId}")
    data object Settings          : Screen("settings")
}
```

| 参数 | 说明 | 取值 |
|------|------|------|
| `mode` | 答题模式 | `practice` / `exam` |
| `practiceType` | 练习排序方式 | `random` / `sequential` |
| `source` | 题目来源 | `all` / `wrong` |

### 导航图 (`NavGraph.kt`)

```
home (startDestination)
├── quiz/{mode}?practiceType={practiceType}&source={source}
│   ├── practice/random/all   → 随机练习（全部题）
│   ├── practice/sequential/all → 顺序练习（全部题）
│   ├── practice/random/wrong → 随机练习（错题）
│   ├── practice/sequential/wrong → 顺序练习（错题）
│   └── exam                  → 模拟考试
│       └── result/{sessionId}
├── history
│   └── history/{id}
├── wrong_questions            → 错题集
│   └── wrong_detail/{questionId} → 错题详情（正确选项高亮 + 解析）
└── settings
```

### 底部导航显示逻辑

在 `home`、`wrong_questions`、`history`、`settings` 四个一级页面显示底部栏。答题页、结果页、详情页自动隐藏。

```kotlin
val showBottomBar = currentRoute in listOf(
    Screen.Home.route, Screen.History.route,
    Screen.WrongQuestions.route, Screen.Settings.route
)
```

---

## 8. 数据模型详解

### 8.1 `Question`（题目）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long (PK, 自增) | 数据库主键 |
| `questionId` | String | 业务题号（"q1", "eq1" 等） |
| `type` | `QuestionType` | 枚举: SINGLE / MULTIPLE / BOOLEAN |
| `stem` | String | 题干内容 |
| `options` | String | 选项，以 `"; "` 分隔（如 "A选项; B选项; C选项"） |
| `answer` | String | 正确答案索引，以 `","` 分隔（如 "0,2"） |
| `analysis` | String | 题目解析 |
| `importTime` | Long | 导入时间戳 |

辅助方法：
- `getOptionsList()` → `"; "` 分割为 List
- `getAnswerList()` → `","` 分割为 `List<Int>`

### 8.2 `QuestionBankMeta`（题库元数据）

单行表（`id = 1`），记录题库统计摘要。

| 字段 | 说明 |
|------|------|
| `totalCount` | 总题数 |
| `singleCount` | 单选题数 |
| `multipleCount` | 多选题数 |
| `booleanCount` | 判断题数 |
| `importTime` | 导入时间 |
| `version` | 版本号 |

### 8.3 `HistoryRecord`（历史记录）

| 字段 | 说明 |
|------|------|
| `id` (PK) | 会话 ID（格式: `sess_{timestamp}_{counter}`） |
| `mode` | `"practice"` 或 `"exam"` |
| `timestamp` | 完成时间 |
| `totalCount` | 总题数 |
| `correctCount` | 正确数 |
| `answeredCount` | 已答数 |
| `score` | 得分（练习: 百分比；考试: 原始分数） |
| `maxScore` | 满分（仅考试模式） |
| `duration` | 用时（秒） |
| `breakdown` | JSON 字符串（考试分项统计，用 Gson 序列化） |

### 8.4 `HistoryDetail`（答题详情）

外键关联 `history_records.id`，级联删除。

| 字段 | 说明 |
|------|------|
| `id` (PK, 自增) | |
| `historyId` (FK) | 关联历史记录 |
| `questionId` | 题目 ID |
| `userAnswer` | 用户答案，`","` 分隔的索引字符串 |
| `isCorrect` | 是否正确 |

### 8.5 `WrongQuestion`（错题记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long (PK, 自增) | 主键 |
| `questionId` | Long (FK) | 关联 `questions.id`，CASCADE 删除 |
| `wrongCount` | Int | 答错次数累积 |
| `lastWrongTime` | Long | 最近一次答错时间 |

外键级联：题目重新导入（删除旧题）时，关联的错题记录自动清除。

### 8.6 `QuizSession`（答题会话）

内存数据（不持久化），驱动答题逻辑：

| 字段 | 说明 |
|------|------|
| `id` | 会话 ID |
| `questions` | 题目列表（有序） |
| `currentIndex` | 当前题目索引 |
| `answers` | `MutableMap<问题ID, 答案List>` |
| `startTime/endTime` | 起止时间戳 |
| `isFinished` | 是否已完成 |
| `mode` | PRACTICE / EXAM |
| `timeLimitSeconds` | 限时（秒，仅考试模式） |
| `randomOrder` | 是否随机顺序 |

### 8.7 辅助数据类型

| 类型 | 用途 |
|------|------|
| `ImportResult` | 导入结果（成功/失败 + 各题型计数 + 警告） |
| `QuizResult` | 答题结果（含分项统计、详情列表） |
| `AnswerDetail` | 单题答题详情 |
| `ExamBreakdown` | 考试分项（三个 `TypeBreakdown`） |
| `TypeBreakdown` | 题型统计（total/correct/score） |
| `Judgment` | 单题评判结果（isCorrect + correctAnswer） |
| `ProgressInfo` | 进度信息（current/total/answered/unanswered） |
| `ScoreResult` | 练习计分结果 |
| `ExamScoreResult` | 考试计分结果 |

---

## 9. 关键设计决策

### 9.1 不依赖 Apache POI

选型理由：POI 体积大（~5MB），增加 APK 体积。xlsx 本质是 ZIP + XML，Android 原生 `ZipInputStream` + `SAXParser` 足以高效解析，且不增加依赖。

### 9.2 题型自动识别

不再依赖 Excel 中的"题型"列，而是根据实际数据推导：

| 条件 | 判定题型 |
|------|----------|
| 有效选项数 ≤ 2 | 判断题 (BOOLEAN) |
| 有效选项数 > 2 且只有 1 个正确答案 | 单选题 (SINGLE) |
| 有效选项数 > 2 且多个正确答案 | 多选题 (MULTIPLE) |

此策略兼容各种 Excel 格式，即使题型列缺失也能正确识别。

### 9.3 题库元数据独立表

将 `QuestionBankMeta` 与 `Question` 分离：
- 避免每次统计都需要 `COUNT` 查询
- 可通过 Flow 实时监听题库状态变化
- 单行设计简化查询

### 9.4 导入即覆盖

导入新题库时自动清空历史记录和错题集：
- 保证题目 ID 与历史记录/错题的引用一致性
- 题目更新后旧记录失去意义
- 错题集通过外键 CASCADE 自动清理
- 用户操作前有确认弹窗

### 9.5 考试模式逻辑

- 抽题策略：从全部题目中按类型配额随机抽取（单 60 / 多 100 / 判 40）
- 每题 0.5 分（使满分接近传统 100 分制）
- 选即提交：点击选项立即记录答案并评判
- 不可修改：模拟真实考试环境
- **交卷按钮**位于右上角 TopAppBar，底部栏不再显示

### 9.6 错题集机制

- 采用独立表 (`wrong_questions`) + 外键引用题目表
- 答错自动加入（未存在则插入，已存在则递增 `wrongCount`）
- 在错题集练习中答对自动移出（`questionSource == "wrong"` 时）
- 正常练习中答对不移出，保留错题记录直至用户主动移除
- CASCADE 外键保证题库重新导入时错题集自动清理

### 9.7 分数分离展示

`QuizResult` 增加 `correctRate` 字段，与 `score` 分开：

| 字段 | 练习模式 | 考试模式 |
|------|----------|----------|
| `score` | 正确题数（每题1分） | 正确题数 × 0.5 |
| `maxScore` | 总题数 | 总题数 × 0.5 |
| `correctRate` | 正确数/总题数 × 100% | 正确数/总题数 × 100% |

UI 中同时展示"得分 X 分"和"正确率 X%"，不再混用。

### 9.8 按钮 UI 统一

设计 4 个统一按钮组件，确保全应用按钮风格一致：

| 组件 | 高度 | 圆角 | 典型用途 |
|------|------|------|----------|
| `PrimaryButton` | 48dp | 12dp | 导入题库、查看详情 |
| `SmallButton` | 40dp | 10dp | 顺序练习、随机练习、开始考试 |
| `SecondaryButton` | 48dp | 12dp | 历史记录、再练一次 |
| `BackButton` | — | — | 返回 |

所有按钮统一：`shape = RoundedCornerShape(12dp/10dp)`、禁用态为灰色 (`Gray200`)、正文大小 13-15sp。

### 9.9 鼓励语系统 + 彩蛋

- 内置 100 句分类鼓励语（6 类：学霸/轻松/燃系/禅意/考试/通用）
- 每次练习/考试完成后从结果页随机展示一条
- **彩蛋机制**：连续点击结果页 🏆/🎉 图标 7 次 → 触发特殊鼓励语：
  > "嘻嘻，你打出彩蛋了，你会考过的！ 🎉"
- 触发后鼓励卡片变为琥珀色背景高亮

### 9.10 多选题交互逻辑

练习模式中多选题与其他题型采用不同交互：

| 题型 | 操作 | 翻页 |
|------|------|------|
| 单选题 | 点击选项 → 自动提交 → 显示反馈 | 可自由翻页 |
| 判断题 | 点击选项 → 自动提交 → 显示反馈 | 可自由翻页 |
| 多选题 | 勾选选项 → 点击「提交答案」→ 显示反馈 | 未提交不可翻页，禁止跳过 |

右侧「下一题」按钮在考试未答或多选题（练习）未提交时禁用，防止跳过。考试全部为选即提交，无「提交答案」按钮。

### 9.11 颜色系统

自定义 6 色系（蓝/绿/红/紫/琥珀/灰），每色系 4-8 个色阶，确保：
- 红绿色盲友好（辅以图标 ✓✗）
- 各题型有明确区分色（单选蓝/多选紫/判断琥珀）
- 正确/错误状态视觉明确

---

## 10. 依赖清单

```kotlin
// AndroidX Core
androidx.core:core-ktx                    // 1.13.1

// Lifecycle
androidx.lifecycle:lifecycle-runtime-ktx  // 2.7.0
androidx.lifecycle:lifecycle-runtime-compose // 2.7.0
androidx.lifecycle:lifecycle-viewmodel-compose // 2.7.0

// Activity Compose
androidx.activity:activity-compose        // 1.9.0

// Compose BOM
androidx.compose:compose-bom              // 2024.02.00
    ├── ui
    ├── ui-graphics
    ├── ui-tooling-preview
    ├── material3
    ├── material-icons-extended
    └── animation

// Navigation
androidx.navigation:navigation-compose    // 2.7.7

// Room
androidx.room:room-runtime                // 2.6.1
androidx.room:room-ktx                    // 2.6.1
androidx.room:room-compiler (KSP)         // 2.6.1

// Coroutines
kotlinx-coroutines-android                // 1.8.0

// JSON
com.google.code.gson:gson                 // 2.10.1
```

**无网络依赖**：所有数据完全离线，本地存储。
