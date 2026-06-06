/**
 * quiz-engine.js — 答题引擎
 * 管理答题会话生命周期：乱序、导航、判题、计分
 */

import { createLogger } from './logger'

const log = createLogger('quiz-engine')

/**
 * Fisher-Yates 洗牌算法
 * @param {Array} arr
 * @returns {Array} 新数组（不修改原数组）
 */
function shuffle(arr) {
  const result = [...arr]
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[result[i], result[j]] = [result[j], result[i]]
  }
  return result
}

/**
 * 生成唯一 ID
 */
function generateId() {
  return 'sess_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 8)
}

/**
 * 判题：比较用户答案和正确答案
 * @param {number[]} userAnswer - 用户选择的选项索引数组
 * @param {number[]} correctAnswer - 正确答案索引数组
 * @returns {boolean}
 */
function judge(userAnswer, correctAnswer) {
  if (!userAnswer || !correctAnswer) return false
  if (userAnswer.length !== correctAnswer.length) return false
  const sortedUser = [...userAnswer].sort()
  const sortedCorrect = [...correctAnswer].sort()
  return sortedUser.every((v, i) => v === sortedCorrect[i])
}

/**
 * 创建答题会话
 * @param {object[]} questions - 题库中的题目数组
 * @param {object} [options]
 * @param {boolean} [options.random=true] - 是否乱序
 * @param {string} [options.mode='practice'] - 'practice' | 'exam'
 * @param {number|null} [options.timeLimit=null] - 限时（秒）
 * @returns {object} session 对象
 */
function createSession(questions, { random = true, mode = 'practice', timeLimit = null } = {}) {
  const ordered = random ? shuffle(questions) : [...questions]
  const session = {
    id: generateId(),
    questions: ordered,
    currentIndex: 0,
    answers: {},
    startTime: Date.now(),
    endTime: null,
    isFinished: false,
    mode,
    timeLimit
  }
  log.info('创建答题会话', {
    id: session.id,
    total: questions.length,
    mode,
    singles: questions.filter(q => q.type === 'single').length,
    multiples: questions.filter(q => q.type === 'multiple').length,
    booleans: questions.filter(q => q.type === 'boolean').length
  })
  return session
}

/**
 * 获取当前题目
 * @param {object} session
 * @returns {object|null} 当前题目对象
 */
function getCurrentQuestion(session) {
  if (!session) return null
  if (session.currentIndex >= session.questions.length) return null
  return session.questions[session.currentIndex]
}

/**
 * 判断当前题目是否已回答
 * @param {object} session
 * @returns {boolean}
 */
function isCurrentAnswered(session) {
  if (!session) return false
  const q = getCurrentQuestion(session)
  if (!q) return false
  return !!session.answers[q.id]
}

/**
 * 提交当前题目的答案并判题
 * @param {object} session
 * @param {number[]} userAnswer - 用户选择的选项索引数组
 * @returns {object} { isCorrect, correctAnswer }
 */
function submitAnswer(session, userAnswer) {
  const q = getCurrentQuestion(session)
  if (!q) {
    log.error('提交答案失败: 当前无题目')
    throw new Error('当前没有题目')
  }

  const isCorrect = judge(userAnswer, q.answer)

  session.answers[q.id] = {
    userAnswer: [...userAnswer],
    isCorrect
  }

  log.info(`提交答案 #${session.currentIndex + 1}`, {
    questionId: q.id,
    type: q.type,
    userAnswer,
    correctAnswer: q.answer,
    isCorrect
  })

  return {
    isCorrect,
    correctAnswer: q.answer
  }
}

/**
 * 跳转到指定题号
 * @param {object} session
 * @param {number} index - 目标题号（0-based）
 * @returns {boolean} 是否成功
 */
function goToQuestion(session, index) {
  if (index < 0 || index >= session.questions.length) {
    log.debug('跳转越界', { index, total: session.questions.length })
    return false
  }
  log.debug('跳转题目', { from: session.currentIndex + 1, to: index + 1 })
  session.currentIndex = index
  return true
}

/**
 * 上一题
 */
function prevQuestion(session) {
  return goToQuestion(session, session.currentIndex - 1)
}

/**
 * 下一题
 */
function nextQuestion(session) {
  return goToQuestion(session, session.currentIndex + 1)
}

/**
 * 计算得分
 * @param {object} session
 * @returns {object} { totalCount, correctCount, score, duration }
 */
function computeScore(session) {
  const answeredArr = Object.values(session.answers)
  const answeredCount = answeredArr.length
  const correctCount = answeredArr.filter(a => a.isCorrect).length
  // 正确率基于已答题数，而非全题库数
  const score = answeredCount > 0 ? Math.round((correctCount / answeredCount) * 100) : 0
  const endTime = session.endTime || Date.now()
  const duration = Math.floor((endTime - session.startTime) / 1000) // 秒

  return { totalCount: session.questions.length, answeredCount, correctCount, score, duration }
}

/**
 * 交卷：结束会话并返回成绩
 * @param {object} session
 * @returns {object} 成绩数据
 */
function finishSession(session) {
  session.endTime = Date.now()
  session.isFinished = true
  const score = computeScore(session)
  log.info('交卷', {
    id: session.id,
    correctCount: score.correctCount,
    totalCount: score.totalCount,
    score: score.score,
    duration: score.duration
  })
  return score
}

/**
 * 获取未答题数
 * @param {object} session
 * @returns {number}
 */
function getUnansweredCount(session) {
  if (!session) return 0
  return session.questions.filter(q => !session.answers[q.id]).length
}

/**
 * 获取进度信息
 * @param {object} session
 * @returns {object} { current, total, answered, unanswered }
 */
function getProgress(session) {
  if (!session) return { current: 0, total: 0, answered: 0, unanswered: 0 }
  const total = session.questions.length
  const answered = Object.keys(session.answers).length
  return {
    current: session.currentIndex + 1,
    total,
    answered,
    unanswered: total - answered
  }
}

/**
 * 生成历史记录对象
 * @param {object} session - 已结束的会话
 * @returns {object} 符合 localStorage historyList 格式的记录
 */
function buildHistoryRecord(session) {
  if (session.mode === 'exam') {
    const score = computeExamScore(session)
    return {
      id: session.id,
      mode: 'exam',
      timestamp: session.startTime,
      totalCount: score.totalCount,
      correctCount: score.correctCount,
      answeredCount: score.answeredCount,
      score: score.examScore,
      maxScore: score.maxScore,
      duration: score.duration,
      breakdown: score.breakdown,
      details: session.questions
        .filter(q => session.answers[q.id])
        .map(q => {
          const ans = session.answers[q.id]
          return {
            questionId: q.id,
            userAnswer: ans.userAnswer,
            isCorrect: ans.isCorrect
          }
        })
    }
  }
  const { totalCount, correctCount, score, duration } = computeScore(session)
  const answeredCount = Object.keys(session.answers).length
  return {
    id: session.id,
    timestamp: session.startTime,
    totalCount,
    correctCount,
    answeredCount,
    score,
    duration,
    // 仅记录已答题的详情，未答题不存储（节省空间）
    details: session.questions
      .filter(q => session.answers[q.id])
      .map(q => {
        const ans = session.answers[q.id]
        return {
          questionId: q.id,
          userAnswer: ans.userAnswer,
          isCorrect: ans.isCorrect
        }
      })
  }
}

/**
 * 考试模式：按题型配额随机抽题
 * @param {object[]} questions - 题库中的全部题目
 * @returns {object[]} 抽选后的题目数组
 */
function selectExamQuestions(questions) {
  const QUOTA = { single: 60, multiple: 100, boolean: 40 }
  const singles = shuffle(questions.filter(q => q.type === 'single')).slice(0, QUOTA.single)
  const multiples = shuffle(questions.filter(q => q.type === 'multiple')).slice(0, QUOTA.multiple)
  const booleans = shuffle(questions.filter(q => q.type === 'boolean')).slice(0, QUOTA.boolean)
  const combined = [...singles, ...multiples, ...booleans]
  // 重新编号 ID（eq = exam question）
  combined.forEach((q, i) => { q.id = `eq${i + 1}` })
  log.info('考试抽题', {
    singles: singles.length,
    multiples: multiples.length,
    booleans: booleans.length,
    total: combined.length
  })
  return combined
}

/**
 * 考试模式：计分（每题 0.5 分，满分 100）
 * @param {object} session
 * @returns {object} { totalCount, answeredCount, correctCount, examScore, maxScore, duration, breakdown }
 */
function computeExamScore(session) {
  const answeredArr = Object.values(session.answers)
  const totalCount = session.questions.length
  const answeredCount = answeredArr.length
  const correctCount = answeredArr.filter(a => a.isCorrect).length
  const pointsPerQuestion = 0.5
  const maxScore = totalCount * pointsPerQuestion
  const examScore = correctCount * pointsPerQuestion

  const breakdown = {}
  ;['single', 'multiple', 'boolean'].forEach(type => {
    const typeQuestions = session.questions.filter(q => q.type === type)
    const typeCorrect = typeQuestions.filter(q => session.answers[q.id]?.isCorrect).length
    breakdown[type] = {
      total: typeQuestions.length,
      correct: typeCorrect,
      score: typeCorrect * pointsPerQuestion
    }
  })

  const endTime = session.endTime || Date.now()
  const duration = Math.floor((endTime - session.startTime) / 1000)
  return { totalCount, answeredCount, correctCount, examScore, maxScore, duration, breakdown }
}

/**
 * 考试模式：交卷
 * @param {object} session
 * @returns {object} 计分结果
 */
function finishExamSession(session) {
  session.endTime = Date.now()
  session.isFinished = true
  const score = computeExamScore(session)
  log.info('考试交卷', {
    id: session.id,
    correctCount: score.correctCount,
    totalCount: score.totalCount,
    examScore: score.examScore,
    maxScore: score.maxScore,
    duration: score.duration
  })
  return score
}

export {
  shuffle,
  createSession,
  getCurrentQuestion,
  isCurrentAnswered,
  submitAnswer,
  goToQuestion,
  prevQuestion,
  nextQuestion,
  computeScore,
  finishSession,
  getUnansweredCount,
  getProgress,
  buildHistoryRecord,
  selectExamQuestions,
  computeExamScore,
  finishExamSession
}
