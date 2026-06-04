/**
 * useQuizStore.js — Vue 3 Composable
 * 全局响应式题库和答题状态管理
 */

import { reactive, computed, readonly, toRaw, markRaw } from 'vue'
import * as storage from '../utils/storage'
import { parseExcelFile } from '../utils/excel-parser'
import {
  createSession,
  submitAnswer,
  nextQuestion,
  prevQuestion,
  goToQuestion,
  finishSession,
  getUnansweredCount,
  getProgress,
  getCurrentQuestion,
  isCurrentAnswered,
  buildHistoryRecord
} from '../utils/quiz-engine'
import { createLogger } from '../utils/logger'

const log = createLogger('store')

// ---- 全局响应式状态 ----
const state = reactive({
  // 题库
  questionBank: storage.getQuestionBank(),

  // 历史记录
  historyList: storage.getHistoryList(),

  // 当前答题会话（仅答题期间存在）
  session: null,

  // 当前判题结果（最近一次提交）
  lastJudgment: null
})

// ---- 题库相关 ----
const hasBank = computed(() => state.questionBank !== null && state.questionBank.questions?.length > 0)
const totalCount = computed(() => state.questionBank?.total || 0)
const singleCount = computed(() =>
  state.questionBank?.questions?.filter(q => q.type === 'single').length || 0
)
const multipleCount = computed(() =>
  state.questionBank?.questions?.filter(q => q.type === 'multiple').length || 0
)
const importTime = computed(() => state.questionBank?.importTime || '')

/**
 * 导入题库
 * @param {File} file
 * @returns {Promise<{success: boolean, message: string, warnings: string[]}>}
 */
/**
 * 导入题库 — 优先使用 Web Worker 后台线程解析
 * @param {File} file
 * @returns {Promise<{success: boolean, message: string, warnings: string[]}>}
 */
async function importBank(file) {
  try {
    log.info('开始导入题库', { name: file.name, size: file.size })
    const endTimer = log.time('导入题库')

    // 优先尝试 Worker 线程解析（不阻塞 UI）
    const result = await tryWorkerParse(file)
    if (!result) {
      // Worker 不可用，降级到主线程 chunked 解析
      log.info('Worker 不可用，使用主线程解析')
      const { bank, warnings } = await parseExcelFile(file)
      return finalizeImport(bank, warnings, endTimer)
    }

    if (result.type === 'error') {
      log.error('Worker 解析失败', result.message)
      endTimer()
      return { success: false, message: result.message, warnings: [] }
    }

    return finalizeImport(result.bank, result.warnings, endTimer)
  } catch (e) {
    log.error('导入失败', e.message)
    return { success: false, message: e.message || '导入失败', warnings: [] }
  }
}

function tryWorkerParse(file) {
  return new Promise((resolve) => {
    if (typeof Worker === 'undefined') {
      resolve(null)
      return
    }

    // 先主线程读文件为 ArrayBuffer，避免 File 对象跨线程传输问题
    const reader = new FileReader()
    reader.onload = () => {
      const buffer = reader.result  // ArrayBuffer

      let settled = false
      try {
        const worker = new Worker(new URL('../workers/excel-worker.js', import.meta.url), { type: 'module' })

        const timeout = setTimeout(() => {
          if (!settled) { settled = true; worker.terminate(); log.warn('Worker 超时，降级主线程'); resolve(null) }
        }, 20000)

        worker.onmessage = (e) => {
          if (settled) return
          settled = true
          clearTimeout(timeout)
          worker.terminate()
          if (e.data.type === 'done' && typeof e.data.json === 'string') {
            try {
              const bank = JSON.parse(e.data.json)
              resolve({ type: 'done', bank, warnings: e.data.warnings || [] })
            } catch (err) {
              log.error('Worker JSON 解析失败', err.message)
              resolve(null)
            }
          } else {
            resolve(e.data)
          }
        }

        worker.onerror = (err) => {
          if (settled) return
          settled = true
          clearTimeout(timeout)
          worker.terminate()
          log.warn('Worker 错误，降级主线程', err.message || 'unknown')
          resolve(null)
        }

        // 用 transferable 传 ArrayBuffer，零拷贝
        worker.postMessage({ buffer }, [buffer])
      } catch (e) {
        log.warn('Worker 创建失败，降级主线程', e.message)
        resolve(null)
      }
    }

    reader.onerror = () => {
      log.warn('文件读取失败，降级主线程')
      resolve(null)
    }

    reader.readAsArrayBuffer(file)
  })
}

function finalizeImport(bank, warnings, endTimer) {
  storage.replaceBank(bank)
  bank.questions = markRaw(bank.questions)
  state.questionBank = bank
  state.historyList = []
  endTimer()
  log.info('导入完成', {
    total: bank.total,
    singles: bank.questions.filter(q => q.type === 'single').length,
    multiples: bank.questions.filter(q => q.type === 'multiple').length,
    warnings: warnings.length
  })
  return {
    success: true,
    message: `成功导入 ${bank.total} 道题（单选 ${bank.questions.filter(q => q.type === 'single').length}，多选 ${bank.questions.filter(q => q.type === 'multiple').length}）`,
    warnings: warnings || []
  }
}

// ---- 历史记录相关 ----
function refreshHistory() {
  state.historyList = storage.getHistoryList()
}

function clearHistoryData() {
  storage.clearHistory()
  state.historyList = []
  log.info('历史记录已清除')
}

function clearAllData() {
  storage.clearAll()
  state.questionBank = null
  state.historyList = []
  log.info('全部数据已清除')
}

function getHistoryById(id) {
  return storage.getHistoryById(id)
}

// ---- 答题会话相关 ----
function startQuiz(random = true) {
  if (!state.questionBank) {
    log.warn('开始答题失败: 无题库')
    return false
  }
  const rawBank = toRaw(state.questionBank)
  const session = createSession(rawBank.questions, { random })
  markRaw(session.questions)
  state.session = session
  state.lastJudgment = null
  log.info('开始答题', { sessionId: session.id, total: session.questions.length })
  return true
}

function answerCurrent(selectedAnswers) {
  if (!state.session) {
    log.warn('答题失败: 无会话')
    return null
  }
  const result = submitAnswer(state.session, selectedAnswers)
  state.lastJudgment = {
    ...result,
    questionId: getCurrentQuestion(state.session)?.id
  }
  return result
}

function goNext() {
  if (!state.session) return false
  state.lastJudgment = null
  return nextQuestion(state.session)
}

function goPrev() {
  if (!state.session) return false
  const prev = prevQuestion(state.session)
  // 恢复之前的判题结果
  if (prev && state.session) {
    const q = getCurrentQuestion(state.session)
    const ans = q ? state.session.answers[q.id] : null
    state.lastJudgment = ans ? {
      isCorrect: ans.isCorrect,
      correctAnswer: q?.answer || [],
      questionId: q?.id
    } : null
  }
  return prev
}

function jumpToQuestion(index) {
  if (!state.session) return false
  state.lastJudgment = null
  const ok = goToQuestion(state.session, index)
  if (ok && state.session) {
    const q = getCurrentQuestion(state.session)
    const ans = q ? state.session.answers[q.id] : null
    state.lastJudgment = ans ? {
      isCorrect: ans.isCorrect,
      correctAnswer: q?.answer || [],
      questionId: q?.id
    } : null
  }
  return ok
}

function finishQuiz() {
  if (!state.session) {
    log.warn('交卷失败: 无会话')
    return null
  }
  const result = finishSession(state.session)
  const record = buildHistoryRecord(state.session)
  storage.addHistory(record)
  state.historyList = storage.getHistoryList()
  const session = state.session
  state.session = null
  state.lastJudgment = null
  log.info('答题结束', { correct: result.correctCount, total: result.totalCount, score: result.score })
  return { score: result, record }
}

function endQuiz() {
  state.session = null
  state.lastJudgment = null
}

// ---- 导出 ----
export function useQuizStore() {
  return {
    // 状态（只读）
    state: readonly(state),
    hasBank,
    totalCount,
    singleCount,
    multipleCount,
    importTime,

    // 题库操作
    importBank,

    // 历史记录
    historyList: computed(() => state.historyList),
    refreshHistory,
    clearHistoryData,
    clearAllData,
    getHistoryById,

    // 答题会话
    startQuiz,
    answerCurrent,
    goNext,
    goPrev,
    jumpToQuestion,
    finishQuiz,
    endQuiz,

    // 引擎函数透传（视图可直接用）
    getCurrentQuestion,
    isCurrentAnswered,
    getUnansweredCount,
    getProgress
  }
}
