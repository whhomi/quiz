/**
 * excel-parser.js — 浏览器端 Excel 解析
 * 使用 SheetJS 读取 .xlsx/.xls 并转换为题库 JSON
 */

import * as XLSX from 'xlsx'
import { createLogger } from './logger'

const log = createLogger('excel-parser')

/**
 * 将选项字母/索引转换为数字索引数组
 * 支持格式:
 *   单选: "A", "B"
 *   多选: "AB", "A,B", "A、B", "A B", "A，B"
 *   判断: "正确"→0, "错误"→1, "正确（A）"→0, "错误（B）"→1
 *   数字: "0", "0,1"
 * @param {string|number} raw - 原始答案值
 * @param {number} optionCount - 选项总数
 * @returns {number[]} 答案索引数组
 */
function parseAnswer(raw, optionCount) {
  if (raw === undefined || raw === null || raw === '') {
    throw new Error('正确答案不能为空')
  }

  const str = String(raw).trim()
  log.debug('解析答案', { raw, cleaned: str, optionCount })

  // 尝试解析（可能带括号注释）
  function tryParse(s) {
    if (!s) return null

    // 判断题: "正确" → 0, "错误" → 1
    const boolMatch = s.match(/^(正确|对|true)/i)
    if (boolMatch) return [0]
    const falseMatch = s.match(/^(错误|错|false)/i)
    if (falseMatch) return [1]

    const upper = s.toUpperCase()

    // 连续字母: "AB", "ABCD"
    if (/^[A-Z]+$/.test(upper)) {
      return upper.split('').map(ch => {
        const idx = ch.charCodeAt(0) - 65
        if (idx < 0 || idx >= optionCount) return null
        return idx
      }).filter(v => v !== null)
    }

    // 带分隔符: "A,B", "A、B", "A B", "A，B，C, D"
    const separatorPattern = /^[A-Z]([\s,，、]+[A-Z])+$/i
    if (separatorPattern.test(s)) {
      return s.split(/[\s,，、]+/).map(ch => {
        const idx = ch.trim().toUpperCase().charCodeAt(0) - 65
        if (idx < 0 || idx >= optionCount) return null
        return idx
      }).filter(v => v !== null)
    }

    // 数字: "0", "0,1"
    if (/^[\d,，、\s]+$/.test(s.replace(/[,，、\s]/g, ''))) {
      return s.split(/[,，、\s]+/).map(p => {
        const idx = parseInt(p, 10)
        if (isNaN(idx) || idx < 0 || idx >= optionCount) return null
        return idx
      }).filter(v => v !== null)
    }

    return null
  }

  // 第一次尝试：原文解析
  let result = tryParse(str)
  if (result && result.length > 0) return result

  // 第二次尝试：剥离括号注释后再试
  const stripped = str.replace(/[（(][^）)]*[）)]/g, '').replace(/[（(].*$/, '').trim()
  if (stripped && stripped !== str) {
    log.debug('剥离注释重试', { original: str, stripped })
    result = tryParse(stripped)
    if (result && result.length > 0) return result
  }

  // 第三次尝试：从文本中提取连续字母序列
  // 处理 "题目存疑 BCD"、"答案: ABD" 等残余格式
  const letterMatch = stripped ? stripped.match(/[A-Z]{1,6}([\s,，、]+[A-Z])*/i) : null
  if (letterMatch) {
    log.debug('提取字母重试', { original: str, extracted: letterMatch[0] })
    result = tryParse(letterMatch[0])
    if (result && result.length > 0) return result
  }

  throw new Error(`无法解析答案格式: "${str}"`)
}

/**
 * 根据表头识别列映射
 * 支持中英文表头
 */
function detectColumns(headers) {
  const map = {}
  const headerMap = {
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

  headers.forEach((h, i) => {
    const clean = String(h || '').trim()
    for (const [key, aliases] of Object.entries(headerMap)) {
      if (aliases.some(a => a.toLowerCase() === clean.toLowerCase())) {
        // 先匹配到的列优先，后面的同名别名不覆盖
        if (map[key] === undefined) map[key] = i
        break
      }
    }
  })

  log.debug('表头识别', { headers, mapping: map })
  return map
}

/**
 * 智能查找真正的表头行
 * 扫描前 10 行，找到匹配列名最多的一行作为表头
 * 这能处理合并标题行、空行等常见格式
 */
function findHeaderRow(rows, maxScan = 10) {
  const knownKeywords = [
    '题型', '题干', '选项', '答案', '解析', '备注',
    'type', 'stem', 'option', 'answer', 'analysis',
    '题目', '问题'
  ]

  let bestRowIndex = 0
  let bestScore = 0

  const scanLimit = Math.min(maxScan, rows.length)
  for (let i = 0; i < scanLimit; i++) {
    const row = rows[i]
    const score = row.reduce((count, cell) => {
      const str = String(cell || '').trim().toLowerCase()
      return count + knownKeywords.filter(kw => str.includes(kw.toLowerCase())).length
    }, 0)
    if (score > bestScore) {
      bestScore = score
      bestRowIndex = i
    }
  }

  log.debug('表头行定位', { bestRowIndex, bestScore, scanned: scanLimit })
  return bestRowIndex
}

/**
 * 解析 Excel 文件，返回题库格式 JSON
 * @param {File} file - 用户选择的文件对象
 * @returns {Promise<object>} 题库对象 { version, importTime, total, questions }
 */
function parseExcelFile(file) {
  return new Promise((resolve, reject) => {
    if (!file) {
      reject(new Error('未选择文件'))
      return
    }

    log.info('开始解析文件', { name: file.name, size: file.size })

    // 验证文件类型
    const validTypes = [
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-excel'
    ]
    const validExts = ['.xlsx', '.xls']
    const name = file.name.toLowerCase()
    if (!validTypes.includes(file.type) && !validExts.some(ext => name.endsWith(ext))) {
      log.warn('文件类型不支持', { name, type: file.type })
      reject(new Error('文件格式不支持，请上传 .xlsx 或 .xls 文件'))
      return
    }

    const reader = new FileReader()

    reader.onload = (e) => {
      try {
        log.debug('文件读取完成，开始解析工作表')
        const data = new Uint8Array(e.target.result)
        const workbook = XLSX.read(data, { type: 'array' })

        // 读取第一个工作表
        const sheetName = workbook.SheetNames[0]
        if (!sheetName) {
          reject(new Error('Excel 文件中没有工作表'))
          return
        }

        log.debug('工作表', { sheetName, totalSheets: workbook.SheetNames.length })

        const sheet = workbook.Sheets[sheetName]
        const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '' })

        if (!rows || rows.length < 2) {
          log.warn('数据行不足', { rows: rows?.length || 0 })
          reject(new Error('Excel 文件中没有足够的数据（至少需要表头和一行数据）'))
          return
        }

        log.info('行数统计', { totalRows: rows.length, dataRows: rows.length - 1 })

        // 智能定位真正的表头行（处理合并标题行等情况）
        const headerRowIndex = findHeaderRow(rows)
        const headers = rows[headerRowIndex]
        const colMap = detectColumns(headers)

        // 验证必填列
        const required = ['type', 'stem', 'optionA', 'optionB', 'answer']
        const missing = required.filter(k => colMap[k] === undefined)
        if (missing.length > 0) {
          const nameMap = {
            type: '题型', stem: '题干', optionA: '选项A', optionB: '选项B', answer: '正确答案'
          }
          const missingNames = missing.map(k => nameMap[k]).join('、')
          reject(new Error(`Excel 缺少必填列: ${missingNames}。\n请确保表头包含: 题型、题干、选项A、选项B、正确答案`))
          return
        }

        // 同步解析（主线程 fallback，正常由 Worker 处理）
        const questions = []
        const errors = []
        let skippedEmpty = 0

        for (let i = headerRowIndex + 1; i < rows.length; i++) {
          const row = rows[i]
          if (row.every(cell => String(cell).trim() === '')) {
            skippedEmpty++
            continue
          }

          try {
            const stem = String(row[colMap.stem] || '').trim()
            if (!stem) {
              errors.push(`第 ${i + 1} 行: 题干为空`)
              continue
            }

            const optionKeys = ['optionA', 'optionB', 'optionC', 'optionD', 'optionE', 'optionF']
            const options = []
            let optionColumnCount = 0  // 有列头定义的选项数
            let filledOptions = 0       // 实际有内容的选项数
            for (const key of optionKeys) {
              if (colMap[key] !== undefined) {
                optionColumnCount++
                const opt = String(row[colMap[key]] || '').trim()
                options.push(opt)       // 保留空位，保证索引正确
                if (opt) filledOptions++
              }
            }

            if (filledOptions < 2) {
              errors.push(`第 ${i + 1} 行: 至少需要两个非空选项`)
              continue
            }

            const rawAnswer = row[colMap.answer]
            let answer = []
            try {
              answer = parseAnswer(rawAnswer, optionColumnCount)
            } catch (e) {
              // 答案无法解析也导入，answer 留空让用户自己判断
              errors.push(`第 ${i + 1} 行: ${e.message}（已导入，答案为空）`)
            }

            const typeRaw = String(row[colMap.type] || '').trim()
            const type = /判断/.test(typeRaw) ? 'boolean'
              : /多选/.test(typeRaw) ? 'multiple'
              : answer.length > 1 ? 'multiple' : 'single'

            const analysis = colMap.analysis !== undefined
              ? String(row[colMap.analysis] || '').trim()
              : ''

            questions.push({
              id: `q${questions.length + 1}`,
              type,
              stem,
              options,
              answer,
              analysis
            })
          } catch (e) {
            errors.push(`第 ${i + 1} 行: ${e.message}`)
          }
        }

        if (questions.length === 0) {
          const errMsg = errors.length > 0
            ? `解析失败，所有行都存在错误:\n${errors.join('\n')}`
            : '未解析到有效题目，请检查 Excel 格式'
          reject(new Error(errMsg))
          return
        }

        if (questions.length > 10000) {
          reject(new Error(`题库超过 10000 题限制（当前 ${questions.length} 题）。\n请拆分 Excel 文件后分批导入。`))
          return
        }

        const bank = {
          version: 1,
          importTime: new Date().toISOString(),
          total: questions.length,
          questions
        }

        log.info('解析完成', {
          total: questions.length,
          errors: errors.length,
          skipped: skippedEmpty,
          singles: questions.filter(q => q.type === 'single').length,
          multiples: questions.filter(q => q.type === 'multiple').length,
          booleans: questions.filter(q => q.type === 'boolean').length
        })

        if (errors.length > 0) {
          log.warn('部分行解析失败', { count: errors.length, errors: errors.slice(0, 20) })
        }

        resolve({ bank, warnings: errors.length > 0 ? errors : [] })
      } catch (e) {
        log.error('解析异常', e.message)
        reject(e)
      }
    }

    reader.onerror = () => {
      log.error('文件读取失败')
      reject(new Error('文件读取失败，请重试'))
    }

    reader.readAsArrayBuffer(file)
  })
}

export { parseExcelFile }
