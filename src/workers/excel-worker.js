/**
 * excel-worker.js — Web Worker：后台线程解析 Excel
 * 接收 File 对象，返回解析后的题库 JSON 或错误
 */

import * as XLSX from 'xlsx'

// ---- 答案解析 (与 excel-parser.js 中 tryParse 一致) ----
function tryParse(s, optionCount) {
  if (!s) return null

  const boolMatch = s.match(/^(正确|对|true)/i)
  if (boolMatch) return [0]
  const falseMatch = s.match(/^(错误|错|false)/i)
  if (falseMatch) return [1]

  const upper = s.toUpperCase()
  if (/^[A-Z]+$/.test(upper)) {
    return upper.split('').map(ch => {
      const idx = ch.charCodeAt(0) - 65
      return (idx >= 0 && idx < optionCount) ? idx : null
    }).filter(v => v !== null)
  }

  const sepPattern = /^[A-Z]([\s,，、]+[A-Z])+$/i
  if (sepPattern.test(s)) {
    return s.split(/[\s,，、]+/).map(ch => {
      const idx = ch.trim().toUpperCase().charCodeAt(0) - 65
      return (idx >= 0 && idx < optionCount) ? idx : null
    }).filter(v => v !== null)
  }

  if (/^[\d,，、\s]+$/.test(s.replace(/[,，、\s]/g, ''))) {
    return s.split(/[,，、\s]+/).map(p => {
      const idx = parseInt(p, 10)
      return (!isNaN(idx) && idx >= 0 && idx < optionCount) ? idx : null
    }).filter(v => v !== null)
  }

  return null
}

function parseAnswer(raw, optionCount) {
  if (raw === undefined || raw === null || raw === '') return { error: '正确答案不能为空' }
  const str = String(raw).trim()

  let result = tryParse(str, optionCount)
  if (result && result.length > 0) return { answer: result }

  // 剥离括号注释重试
  const stripped = str.replace(/[（(][^）)]*[）)]/g, '').replace(/[（(].*$/, '').trim()
  if (stripped && stripped !== str) {
    result = tryParse(stripped, optionCount)
    if (result && result.length > 0) return { answer: result }
  }

  // 从文本中提取字母序列
  const letterMatch = stripped ? stripped.match(/[A-Z]{1,6}([\s,，、]+[A-Z])*/i) : null
  if (letterMatch) {
    result = tryParse(letterMatch[0], optionCount)
    if (result && result.length > 0) return { answer: result }
  }

  return { error: `无法解析答案格式: "${str}"` }
}

// ---- 表头映射 ----
const HEADER_MAP = {
  type: ['题型', 'type', '题目类型', '题目类别'],
  stem: ['题干', 'stem', '题目', '问题', '题目内容'],
  optionA: ['选项A', 'optionA', 'A', '选项 A', '选项a'],
  optionB: ['选项B', 'optionB', 'B', '选项 B', '选项b'],
  optionC: ['选项C', 'optionC', 'C', '选项 C', '选项c'],
  optionD: ['选项D', 'optionD', 'D', '选项 D', '选项d'],
  optionE: ['选项E', 'optionE', 'E', '选项 E', '选项e'],
  optionF: ['选项F', 'optionF', 'F', '选项 F', '选项f'],
  answer: ['正确答案', 'answer', '答案', '正确选项', '答案选项'],
  analysis: ['解析', 'analysis', '题目解析', '答案解析', '新题依据', '备注', '备注说明']
}

function detectColumns(headers) {
  const map = {}
  headers.forEach((h, i) => {
    const clean = String(h || '').trim()
    for (const [key, aliases] of Object.entries(HEADER_MAP)) {
      if (aliases.some(a => a.toLowerCase() === clean.toLowerCase())) {
        if (map[key] === undefined) map[key] = i
        break
      }
    }
  })
  return map
}

function findHeaderRow(rows, maxScan = 10) {
  const keywords = ['题型', '题干', '选项', '答案', '解析', '备注', 'type', 'stem', 'option', 'answer', 'analysis', '题目', '问题']
  let bestIdx = 0, bestScore = 0
  const limit = Math.min(maxScan, rows.length)
  for (let i = 0; i < limit; i++) {
    const score = rows[i].reduce((c, cell) => {
      const s = String(cell || '').trim().toLowerCase()
      return c + keywords.filter(k => s.includes(k.toLowerCase())).length
    }, 0)
    if (score > bestScore) { bestScore = score; bestIdx = i }
  }
  return bestIdx
}

// ---- 主解析逻辑 ----
self.onmessage = (e) => {
  const { buffer } = e.data
  if (!buffer) {
    self.postMessage({ type: 'error', message: '未收到数据' })
    return
  }

  try {
    const data = new Uint8Array(buffer)
    const workbook = XLSX.read(data, { type: 'array' })

    const sheetName = workbook.SheetNames[0]
    const sheet = workbook.Sheets[sheetName]
    const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '' })

    if (!rows || rows.length < 2) {
      self.postMessage({ type: 'error', message: '文件中没有足够的数据' })
      return
    }

    const headerIdx = findHeaderRow(rows)
    const colMap = detectColumns(rows[headerIdx])

    const required = ['type', 'stem', 'optionA', 'optionB', 'answer']
    const missing = required.filter(k => colMap[k] === undefined)
    if (missing.length > 0) {
      const names = { type: '题型', stem: '题干', optionA: '选项A', optionB: '选项B', answer: '正确答案' }
      self.postMessage({ type: 'error', message: `缺少必填列: ${missing.map(k => names[k]).join('、')}` })
      return
    }

    const questions = []
    const errors = []
    const optionKeys = ['optionA','optionB','optionC','optionD','optionE','optionF']

    for (let i = headerIdx + 1; i < rows.length; i++) {
      const row = rows[i]
      if (row.every(cell => String(cell).trim() === '')) continue

      const stem = String(row[colMap.stem] || '').trim()
      if (!stem) { errors.push(`第 ${i+1} 行: 题干为空`); continue }

      const options = []
      let optCols = 0, filled = 0
      for (const key of optionKeys) {
        if (colMap[key] !== undefined) {
          optCols++
          const opt = String(row[colMap[key]] || '').trim()
          options.push(opt)
          if (opt) filled++
        }
      }
      if (filled < 2) { errors.push(`第 ${i+1} 行: 少于2个有效选项`); continue }

      const parsed = parseAnswer(row[colMap.answer], optCols)
      const answer = parsed.error ? [] : parsed.answer
      if (parsed.error) errors.push(`第 ${i+1} 行: ${parsed.error}（已导入，答案为空）`)

      const typeRaw = String(row[colMap.type] || '').trim()
      const type = /判断/.test(typeRaw) ? 'boolean'
        : /多选/.test(typeRaw) ? 'multiple'
        : answer.length > 1 ? 'multiple' : 'single'

      const analysis = colMap.analysis !== undefined
        ? String(row[colMap.analysis] || '').trim() : ''

      questions.push({
        id: `q${questions.length + 1}`,
        type,
        stem,
        options,
        answer,
        analysis
      })
    }

    if (questions.length === 0) {
      self.postMessage({ type: 'error', message: errors.length ? errors.join('\n') : '未解析到有效题目' })
      return
    }

    const bank = {
      version: 1,
      importTime: new Date().toISOString(),
      total: questions.length,
      questions
    }

    self.postMessage({ type: 'done', json: JSON.stringify(bank), warnings: errors })
  } catch (err) {
    self.postMessage({ type: 'error', message: err.message || '解析异常' })
  }
}
