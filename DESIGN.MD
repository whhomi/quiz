# 刷题助手 Web 版 — 详细设计文档

> 面向复刻和二次开发，涵盖数据流、状态管理、关键算法、边界处理。

---

## 1. 组件树与数据流

```
App.vue (路由出口 + 响应式导航)
├── Home.vue ──── importBank() ──→ useQuizStore ──→ localStorage
│                  startQuiz() ──→ router.push('/quiz')
├── Quiz.vue ──── useQuizStore (session/answers/judgment)
│                  finishQuiz() ──→ localStorage (history)
├── Result.vue ── useQuizStore (historyList[0])
├── History.vue ─ useQuizStore (historyList)
├── HistoryDetail ─ useQuizStore (getHistoryById + questionBank)
└── Settings.vue ─ useQuizStore (questionBank + clear*)
```

**单向数据流**：
- 所有状态变更统一通过 `useQuizStore`（单例 Composable）
- 视图只读 `state`（`readonly()` 包装），变更调用 store 方法
- 持久化层（`localStorage`）只在 `storage.js` 中操作

---

## 2. 状态管理架构

### 2.1 模块级单例

```js
// useQuizStore.js — 模块顶层
const state = reactive({ ... })  // 全局唯一实例

export function useQuizStore() {
  return { state: readonly(state), ... }
}
```

每次调用 `useQuizStore()` 返回同一个 `state` 的只读代理。所有视图共享一份数据。

### 2.2 响应式优化：markRaw

**问题**：`state.questionBank.questions` 有 5000+ 个题目对象，Vue `reactive()` 会深度代理每个对象的每个属性（id, type, stem, options[], answer[], analysis），产生 3.5 万+ Proxy。

**方案**：
```js
// 导入时
bank.questions = markRaw(bank.questions)
state.questionBank = bank

// 创建会话时
const session = createSession(rawBank.questions)
markRaw(session.questions)
state.session = session
```

- `markRaw()` 标记对象永不被代理
- 题目数据只读不变，不需要响应式
- `session.answers` 和 `session.currentIndex` 保持响应式（它们会变）
- `toRaw()` 取值时剥离外层代理，确保传给 `createSession` 的是原始对象

### 2.3 answeredCount：显式进度驱动

**问题**：`progress.answered` 依赖 `Object.keys(state.session.answers).length`，但 5000 题时深层响应式追踪可能丢失。

**方案**：Quiz.vue 维护独立的 `answeredCount` ref，每次 `answerCurrent()` 成功后显式 +1。进度条直接读 `answeredCount`。

```js
const answeredCount = ref(0)

function handleNext() {
  const ok = answerCurrent(selected.value)
  if (ok) answeredCount.value++  // 显式递增
}
```

---

## 3. 导入流程（全路径）

### 3.1 决策树

```
importBank(file)
  │
  ├─ tryWorkerParse(file) 成功？
  │   ├─ YES → finalizeImport(bank, warnings)
  │   └─ NO  → parseExcelFile(file)  // 主线程 fallback
  │             └─ finalizeImport(bank, warnings)
  │
  └─ finalizeImport:
       ├─ storage.replaceBank(bank)   // JSON.stringify → localStorage
       ├─ markRaw(bank.questions)     // 禁止 Vue 深度代理
       ├─ state.questionBank = bank   // 触发视图更新
       └─ state.historyList = []      // 清空旧历史
```

### 3.2 Worker 路径详解

```
主线程                          Worker 线程
───────                         ──────────
FileReader.readAsArrayBuffer
  ↓
ArrayBuffer (772KB)
  ↓
postMessage({buffer}, [buffer])  → 零拷贝转移
                                   ↓
                                  new Uint8Array(buffer)
                                   ↓
                                  XLSX.read(data, {type:'array'})
                                   ↓
                                  逐行解析（同步）
                                   ↓
                                  JSON.stringify(bank)
                                   ↓
                                  postMessage({json, warnings})
  ← 接收 JSON                   ←
  ↓
JSON.parse → bank 对象
  ↓
finalizeImport
```

**关键设计**：
- `[buffer]` 是 Transferable 列表，ArrayBuffer 所有权转移（零拷贝），主线程不再持有
- Worker 回传 JSON 字符串而非对象，避免 structured clone 遍历 5000 对象
- 20 秒超时：`setTimeout` 触发 → `worker.terminate()` → `resolve(null)` → 降级
- Worker `onerror`：模块加载失败等原因 → 降级

### 3.3 Fallback 路径

主线程 `parseExcelFile()` 直接在 `FileReader.onload` 中同步解析：
1. `XLSX.read()` 解析 Workbook
2. `findHeaderRow()` 智能定位表头行（扫描前 10 行）
3. `detectColumns()` 列名匹配（先匹配先得）
4. 单层 for 循环逐行处理（无 setTimeout 分段）
5. `resolve({ bank, warnings })`

---

## 4. 答案解析算法

### 4.1 三级尝试

```
原始字符串 "题目存疑 BCD(BC)"
  │
  ├─ 第 1 级：tryParse("题目存疑 BCD(BC)")
  │   ├─ 匹配 /^(正确|对|true)/i → 否
  │   ├─ 匹配 /^(错误|错|false)/i → 否
  │   ├─ 匹配 /^[A-Z]+$/ → 否（有中文前缀）
  │   ├─ 匹配分隔符模式 → 否
  │   └─ 返回 null
  │
  ├─ 第 2 级：剥离括号 → "题目存疑 BCD"
  │   └─ tryParse("题目存疑 BCD") → 同上失败 → null
  │
  └─ 第 3 级：提取字母序列 → "BCD"
      └─ tryParse("BCD") → /^[A-Z]+$/ 匹配 → [1,2,3] ✅
```

### 4.2 支持格式

| 输入 | 级别 | 输出 |
|------|------|------|
| `A` | 1 | `[0]` |
| `AB` / `ABCD` | 1 | `[0,1]` / `[0,1,2,3]` |
| `A,B` / `A、B` / `A B` / `A，B` | 1 | `[0,1]` |
| `正确` / `对` / `true` | 1 | `[0]` |
| `错误` / `错` / `false` | 1 | `[1]` |
| `BCD(BC)` → `BCD` | 2 | `[1,2,3]` |
| `A（原题存疑）` → `A` | 2 | `[0]` |
| `B,D（争议）` → `B,D` | 2 | `[1,3]` |
| `题目存疑 BCD(BC)` → 提取 `BCD` | 3 | `[1,2,3]` |
| `？` | — | `[]`（无答案导入） |
| 空 / 纯备注 | — | `[]`（无答案导入） |

### 4.3 无答案处理

答案解析失败的题目仍然导入，`answer: []`。答题时显示：

```
📝 本题暂无标准答案，请自行判断
```

用户选择任意选项后提交，`judge([idx], [])` → 长度不同 → false。计分时该题计入已答题但不计为正确。

---

## 5. 答题会话生命周期

### 5.1 状态机

```
[未开始] ──startQuiz()──→ [答题中] ──finishQuiz()──→ [已结束]
                               │                        │
                               │ answerCurrent()         ├─ finishSession()
                               │ goNext/goPrev           ├─ buildHistoryRecord()
                               │ jumpToQuestion          ├─ storage.addHistory()
                               │                        └─ state.session = null
                               │
                               └── 每道题状态：
                                   未答 → 已答(正确/错误)
```

### 5.2 单选交互时序

```
用户点击选项
  → toggleOption(idx)
  → selected = [idx]
  → 选项高亮为蓝色边框

用户点击"下一题"
  → handleNext()
  → answerCurrent(selected.value)
    → submitAnswer(session, [idx])
    → judge([idx], question.answer)
    → session.answers[q.id] = { userAnswer, isCorrect }
    → state.lastJudgment = { isCorrect, correctAnswer, questionId }
    → answeredCount++
  → return（停在当前题）
  → 模板重新渲染
  → 判题反馈出现（✅/❌ + 解析）
  → 按钮变为绿色"继续"

用户点击"继续"
  → handleNext()
  → isAnswered = true → 跳过判题分支
  → goNext()
  → currentIndex++
  → watch 触发 → selected = []
  → 下一题渲染
```

### 5.3 多选交互时序

```
用户点击多个选项
  → toggleOption(idx) × N
  → selected = [0, 2, 3]

用户点击"提交答案"
  → handleSubmit()
  → answerCurrent(selected.value)
  → 判题 + 反馈（同上）
  → 选项变为红绿色（正确选项绿，选错的红）

用户点击"下一题"
  → goNext()
  → 下一题渲染
```

### 5.4 导航跳转

```
jumpToQuestion(500)
  → goToQuestion(session, 500)
  → currentIndex = 500
  → watch 触发
  → 检查 session.answers[q500.id]
    ├─ 有答案 → selected = savedAnswer, lastJudgment 恢复
    └─ 无答案 → selected = [], lastJudgment = null
```

---

## 6. 计分规则

```
computeScore(session):
  answered = Object.values(session.answers)  // 已答题列表
  correctCount = answered.filter(a => a.isCorrect).length
  score = answered.length > 0
    ? Math.round(correctCount / answered.length * 100)
    : 0
```

- **分母是已答题数**，不是全题库数
- 跳过的题目不计入
- 无答案题目（answer=[]）：选择任意选项后计入已答，但不计为正确

---

## 7. 历史记录存储策略

### 7.1 精简存储

```js
buildHistoryRecord(session):
  details: session.questions
    .filter(q => session.answers[q.id])     // 只存已答题
    .map(q => ({
      questionId: q.id,
      userAnswer: session.answers[q.id].userAnswer,
      isCorrect: session.answers[q.id].isCorrect
    }))
```

- 不存题干/选项（从题库读取）
- 不存未答题（跳过的题目）
- `answeredCount` 记录实际答题数

### 7.2 详情页回显

```
HistoryDetail 渲染：
  record.details.forEach(detail =>
    题干 = state.questionBank.questions.find(q => q.id === detail.questionId).stem
    选项 = ...find(q => q.id === detail.questionId).options
    标识 = detail.isCorrect ? 绿 : 红
  )
```

> 依赖当前题库存在。如果题目被删除（导入新题库），显示"(题目已删除)"。

---

## 8. 响应式导航设计

```
App.vue:
  showNav = computed(() => !['Quiz', 'Result'].includes(route.name))

  PC (≥768px):
    <nav class="hidden md:flex ... sticky top-0">
      <router-link to="/">首页</router-link>
      <router-link to="/history">历史记录</router-link>
      <router-link to="/settings">设置</router-link>
    </nav>

  移动端 (<768px):
    <nav class="md:hidden ... sticky bottom-0">
      <router-link to="/">🏠 首页</router-link>
      <router-link to="/history">🕐 历史</router-link>
      <router-link to="/settings">⚙️ 设置</router-link>
    </nav>

  Quiz / Result: showNav = false → 不渲染导航
```

---

## 9. 边界处理清单

| 场景 | 处理方式 |
|------|---------|
| 无题库时访问 /quiz | `onMounted` 检测 → `router.replace('/')` |
| 答题中离开页面 | `onBeforeRouteLeave` → confirm 弹窗 |
| 题库超 5000 题 | `markRaw` 免代理 → 不卡顿 |
| localStorage 满 | `setQuestionBank` 估算 + 抛异常 |
| Excel 解析失败 | Worker 降级 → 主线程 fallback → 错误提示 |
| Worker 超时 | 20s 超时 → 降级 |
| 答案无法解析 | 导入但 answer=[]，显示"暂无标准答案" |
| 选项 D 为空 | 保留空位占索引 → D 选项显示为空文本 |
| 历史详情题目缺失 | 显示"(题目已删除)" |
| 重新导入题库 | 确认弹窗 → 清空历史 |
| 长文本溢出 | 全局 `break-words` |
| Vue ref 模板解包 | 模板中用 `selected` 而非 `selected.value` |

---

## 10. 文件依赖图

```
main.js
  ├── App.vue
  │   ├── router/index.js
  │   │   ├── Home.vue
  │   │   │   └── useQuizStore.js
  │   │   │       ├── storage.js ← localStorage
  │   │   │       ├── excel-parser.js ← xlsx
  │   │   │       ├── quiz-engine.js
  │   │   │       ├── excel-worker.js (Web Worker) ← xlsx
  │   │   │       └── logger.js
  │   │   ├── Quiz.vue
  │   │   │   └── useQuizStore.js
  │   │   ├── Result.vue → useQuizStore.js
  │   │   ├── History.vue → useQuizStore.js
  │   │   ├── HistoryDetail.vue → useQuizStore.js
  │   │   └── Settings.vue → useQuizStore.js ← storage.js
  │   └── style.css ← @import "tailwindcss"
  └── logger.js
```

> `excel-worker.js` 是独立入口，由 Vite 单独打包（`dist/assets/excel-worker-*.js`），通过 `new Worker(new URL(...))` 动态加载。

---

## 11. 构建产物结构

```
dist/
├── index.html                          (0.7 KB)
├── _redirects                          (Cloudflare Pages)
├── _headers                            (静态缓存策略)
├── favicon.svg
└── assets/
    ├── excel-worker-XXXXXX.js          (335 KB, Worker 独立 chunk)
    ├── index-XXXXXX.css                (23 KB, Tailwind)
    └── index-XXXXXX.js                 (469 KB, 主应用 + Vue/Router/xlsx)
```

> 总 gzip 后约 160 KB（主应用）+ 85 KB（Worker），首次访问 ~245 KB。
