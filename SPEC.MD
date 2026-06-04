# 刷题助手 Web 版 — 实现规格说明书

> 纯前端 SPA，Vue 3 + Vite + Tailwind CSS。导入 Excel 题库 → 单选/多选练习 → 历史记录。
> 部署目标：Cloudflare Pages（或任何静态托管），零后端。

---

## 1. 技术栈

| 层 | 选型 | 版本 |
|---|------|------|
| 框架 | Vue 3 (Composition API + `<script setup>`) | ^3.5 |
| 构建 | Vite | ^8.0 |
| 路由 | Vue Router (Hash 模式) | ^4.6 |
| 样式 | Tailwind CSS v4 (Vite 插件) | ^4.3 |
| Excel 解析 | SheetJS (xlsx) | ^0.18 |

---

## 2. 项目结构

```
quiz-web/
├── public/
│   ├── favicon.svg
│   ├── _redirects          # Cloudflare Pages SPA fallback
│   └── _headers            # 静态资源长缓存
├── src/
│   ├── main.js             # 入口：挂载 Vue + Router
│   ├── App.vue             # 根组件：响应式导航（PC顶部 / 移动端底部）
│   ├── style.css           # Tailwind 入口
│   ├── router/index.js     # 6 条路由（Hash 模式）
│   ├── composables/
│   │   └── useQuizStore.js # 全局响应式状态管理（单例）
│   ├── utils/
│   │   ├── storage.js      # localStorage CRUD 封装
│   │   ├── excel-parser.js # 主线程 Excel 解析（fallback）
│   │   ├── quiz-engine.js  # 答题引擎（洗牌/判题/计分）
│   │   └── logger.js       # 集中式日志（带命名空间）
│   ├── workers/
│   │   └── excel-worker.js # Web Worker 后台解析（优先路径）
│   └── views/
│       ├── Home.vue         # 首页（空引导 / 题库概览 / 导入 / 模式选择）
│       ├── Quiz.vue         # 答题页（核心）
│       ├── Result.vue       # 成绩单独立页（最近一次记录）
│       ├── History.vue      # 历史列表（倒序）
│       ├── HistoryDetail.vue # 历史逐题回顾（已答题 + 筛选）
│       └── Settings.vue     # 设置（存储用量 / 清空数据）
├── index.html
├── vite.config.js
└── package.json
```

---

## 3. 路由表

| 路径 | 名称 | 说明 |
|------|------|------|
| `/#/` | Home | 首页 |
| `/#/quiz` | Quiz | 答题（沉浸模式，无导航栏） |
| `/#/result` | Result | 成绩单独立页 |
| `/#/history` | History | 历史记录列表 |
| `/#/history/:id` | HistoryDetail | 历史详情逐题回顾 |
| `/#/settings` | Settings | 设置页 |

> Router 使用 `createWebHashHistory`，兼容 Cloudflare Pages 无服务端路由配置。

---

## 4. 数据模型

### 4.1 题库（localStorage key: `questionBank`）

```json
{
  "version": 1,
  "importTime": "2026-06-03T10:30:00.000Z",
  "total": 5044,
  "questions": [
    {
      "id": "q1",
      "type": "single | multiple",
      "stem": "题干",
      "options": ["A", "B", "C", "D"],
      "answer": [0],
      "analysis": "解析文本（可为空）"
    }
  ]
}
```

### 4.2 历史记录（localStorage key: `historyList`）

```json
[
  {
    "id": "sess_xxx",
    "timestamp": 1640000000000,
    "totalCount": 5044,
    "answeredCount": 50,
    "correctCount": 42,
    "score": 84,
    "duration": 600,
    "details": [
      { "questionId": "q1", "userAnswer": [0], "isCorrect": true }
    ]
  }
]
```

> **关键设计**：`details` 只存已答题，未答题不存储。correctCount 按已答题计算，score = correctCount / answeredCount × 100。

### 4.3 答题会话（内存，不持久化）

```json
{
  "id": "sess_xxx",
  "questions": [...],       // 乱序或顺序排列的题目数组
  "currentIndex": 0,
  "answers": {              // key: questionId
    "q1": { "userAnswer": [0], "isCorrect": true }
  },
  "startTime": 1640000000000,
  "endTime": null,
  "isFinished": false
}
```

---

## 5. 核心模块详解

### 5.1 storage.js — localStorage 封装

| 函数 | 说明 |
|------|------|
| `getQuestionBank()` | 读取题库，无数据返回 null |
| `setQuestionBank(bank)` | 写入题库，超 5MB 抛异常 |
| `replaceBank(bank)` | 替换题库 + 清空历史（导入新题库用） |
| `getHistoryList()` | 读取历史记录数组 |
| `addHistory(record)` | 头部插入一条记录 |
| `getHistoryById(id)` | 按 ID 查找单条记录 |
| `clearHistory()` | 清空历史（保留题库） |
| `clearAll()` | 清空全部数据 |
| `getStorageUsage()` | 估算已用空间（字节） |

### 5.2 excel-parser.js — Excel 解析

**表头识别**：

| 内部键 | Excel 列名（自动匹配） |
|--------|----------------------|
| `type` | 题型 / type / 题目类型 |
| `stem` | 题干 / stem / 题目 |
| `optionA~F` | 选项A~F / A~F |
| `answer` | 正确答案 / 答案 / answer |
| `analysis` | 解析 / 新题依据 / 备注 / analysis |

**智能表头定位**：`findHeaderRow()` 扫描前 10 行，找匹配列名最多的一行，处理合并标题行。

**答案解析三级尝试**：
1. 原文解析：支持 `A` / `AB` / `A,B` / `A、B` / `正确`/`错误` 等格式
2. 剥离括号注释：`BCD(BC)` → `BCD` / `A（争议）` → `A`
3. 提取字母序列：`题目存疑 BCD` → `BCD`

**选项列索引保护**：保留所有选项列占位（空列也不剔除），以列数为准校验答案范围，避免 `D` 超出 A-C 问题。

**性能**：主线程 fallback 改用同步一次性解析（Worker 是真正的异步路径）。

### 5.3 excel-worker.js — Web Worker 后台解析

解析逻辑与 `excel-parser.js` 一致，但运行在独立 Worker 线程：
- 主线程 `FileReader` 读为 `ArrayBuffer` → `postMessage(buffer, [buffer])` 零拷贝转移
- Worker 直接 `new Uint8Array(buffer)` + `XLSX.read()` 解析
- 结果 `JSON.stringify` 回传 → 主线程 `JSON.parse`
- 15s 超时自动降级到主线程 fallback
- Worker 不可用（老浏览器）自动降级

### 5.4 quiz-engine.js — 答题引擎

| 函数 | 说明 |
|------|------|
| `shuffle(arr)` | Fisher-Yates 洗牌 |
| `createSession(questions, {random})` | 创建会话，random=false 保持顺序 |
| `getCurrentQuestion(session)` | 获取当前题，session 为 null 返回 null |
| `submitAnswer(session, userAnswer)` | 提交答案并判题 |
| `goToQuestion(session, idx)` | 跳转到指定题号 |
| `prevQuestion/nextQuestion(session)` | 上下题导航 |
| `computeScore(session)` | 计算得分（正确数/已答题数） |
| `finishSession(session)` | 结束会话 |
| `buildHistoryRecord(session)` | 生成历史记录对象（仅含已答题详情） |
| `getProgress/getUnansweredCount` | 进度查询 |

**判题逻辑**：`judge(userAnswer, correctAnswer)` — 排序后逐项对比，长度不同直接 false。

### 5.5 useQuizStore.js — 全局状态管理

Vue 3 Composable 单例模式，`reactive()` 管理全局状态：

```js
state = {
  questionBank,   // 题库（questions 数组已 markRaw，免深度代理）
  historyList,    // 历史记录
  session,        // 当前答题会话
  lastJudgment    // 最近一次判题结果
}
```

**关键优化**：
- `markRaw(questions)` 标记题库只读，避免 Vue 对 5000+ 题目对象深度代理
- `toRaw(state.questionBank)` 取原始对象给 `createSession`
- Worker 优先 → 主线程同步 fallback

### 5.6 logger.js — 日志系统

命名空间日志，5 个模块：`storage` / `excel-parser` / `quiz-engine` / `store` / app。

- 开发模式默认 `debug`，生产构建默认 `warn`
- Console 彩色输出 + 可选 localStorage 持久化
- `log.time(label)` 性能计时

---

## 6. 视图说明

### 6.1 Home.vue — 首页

**无题库**：显示导入引导 + "导入题库"按钮。

**有题库**：题库卡片（总题数/单选数/多选数/导入时间）+ 模式切换 + "开始练习"按钮。

**模式切换**：`🎲 随机` / `📋 顺序`，随机默认开启。

**导入流程**：Worker 优先 → 降级主线程 → `markRaw` 免代理 → `localStorage` 写入。

### 6.2 Quiz.vue — 答题页（核心）

- **顶部**：进度条 + 题号 + 已答数 + "提前交卷"按钮
- **主体**：一次一道题，题型标签 + 题干 + 选项列表
- **单选**：点选项 → 点「下一题」→ 显示对错反馈 + 解析 → 点「继续」→ 下一题
- **多选**：点选项 → 点「提交答案」→ 显示对错反馈 + 解析 → 点「下一题」→ 下一题
- **无答案题目**：显示琥珀色 "📝 本题暂无标准答案，请自行判断"
- **底部**：`上一题` | `◀◀ [输入框] / 5044 ▶▶` | `下一题/继续/提交答案`
- **交卷**：成绩弹窗（圆环 + 正确/已答/总题 + 用时），可查看详情或再练一次

**关键交互**：
- 已判定的题目不可修改答案
- 离开页面有未答题确认
- 进度条用显式 `answeredCount` ref 驱动（不依赖深层响应式追踪）

### 6.3 History.vue — 历史列表

按时间倒序，每条显示：日期 / 正确率标签 / 正确数/已答数 / 用时。点击进入详情。

### 6.4 HistoryDetail.vue — 历史逐题回顾

- 顶部：正确率 + 正确/已答/总题 + 已答/跳过统计
- 筛选项：全部 / 单选 / 多选
- 逐题：题干 + 选项（绿=正确答案，红=选错）+ 解析
- 只展示已答题，跳过的题目不记录详情

### 6.5 Settings.vue — 设置

- 题库信息展示
- 存储用量条（上限 5MB）
- 清空历史（保留题库）
- 清除全部数据（题库 + 历史）

### 6.6 Result.vue — 成绩单独立页

显示最近一次历史记录的成绩，复用成绩弹窗 UI。

---

## 7. 响应式导航

- **PC（≥768px）**：顶部固定导航栏（首页 | 历史 | 设置）
- **移动端（<768px）**：底部固定 TabBar（带 SVG 图标）
- **答题页/成绩页**：沉浸模式，不显示导航

---

## 8. 构建与部署

### 本地开发
```bash
cd quiz-web
npm install
npm run dev        # → http://localhost:5173
```

### 生产构建
```bash
npm run build      # 输出到 dist/
npx serve dist     # 预览: http://localhost:3000
```

### Cloudflare Pages

1. 代码推送到 GitHub/GitLab
2. Cloudflare Pages 关联仓库
3. 框架预设：`Vite`，构建命令：`npm run build`，输出目录：`dist`

`public/_redirects` 确保 SPA 路由 fallback：
```
/*    /index.html   200
```

`public/_headers` 设置静态资源长缓存：
```
/assets/*
  Cache-Control: public, max-age=31536000, immutable
```

---

## 9. 性能设计

| 策略 | 说明 |
|------|------|
| Web Worker 解析 | Excel 解析不阻塞 UI 主线程 |
| ArrayBuffer 零拷贝 | `postMessage(buffer, [buffer])` transferable |
| markRaw | 题库 5000+ 题目对象不创建 Vue Proxy |
| 显式 answeredCount | 进度条不依赖深层响应式追踪 |
| 历史记录精简 | 仅存已答题详情，未答题不存储 |
| 无进度点 DOM | 5000 题不用 v-for 渲染点，改用数字跳转器 |

---

## 10. Excel 格式兼容性

支持以下答案格式自动识别：

| 原始值 | 解析结果 |
|--------|---------|
| `A` | `[0]` |
| `AB` / `A,B` / `A、B` / `A，B` | `[0, 1]` |
| `正确` / `正确（A）` | `[0]` |
| `错误` / `错误（B）` | `[1]` |
| `B,D（争议）` → 剥离括号 → | `[1, 3]` |
| `题目存疑 BCD(BC)` → 提取字母 → | `[1, 2, 3]` |
| `？` / 空 / 纯备注 | `[]`（导入但无答案，用户自行判断） |

表头兼容：`题型/题干/选项A~D/正确答案/答案/解析/新题依据/备注` 等中英文列名。

---

## 11. 已知局限

- 题库 ≤ 10000 题（硬限制，可调整）
- localStorage 5MB 上限（约 5000 题含解析约 3-4MB）
- 选项列最多 A-F（6 个）
- 不支持图片/富文本题干
