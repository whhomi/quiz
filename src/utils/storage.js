/**
 * storage.js — localStorage 封装层
 * 管理 questionBank 和 historyList 的读写
 */

import { createLogger } from './logger'

const log = createLogger('storage')
const KEY_BANK = 'questionBank'
const KEY_HISTORY = 'historyList'
const MAX_STORAGE = 5 * 1024 * 1024 // 5MB localStorage limit

/**
 * 估算已用存储空间（字节）
 */
function getStorageUsage() {
  let total = 0
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    total += (key.length + (localStorage.getItem(key) || '').length) * 2 // UTF-16
  }
  log.debug('存储用量查询', { bytes: total, kb: (total / 1024).toFixed(1) })
  return total
}

/**
 * 获取题库
 * @returns {object|null} 题库对象，无数据时返回 null
 */
function getQuestionBank() {
  try {
    const raw = localStorage.getItem(KEY_BANK)
    const bank = raw ? JSON.parse(raw) : null
    log.debug('读取题库', { exists: !!bank, total: bank?.total || 0 })
    return bank
  } catch (e) {
    log.error('读取题库异常', e.message)
    return null
  }
}

/**
 * 覆盖写入题库（替换旧数据）
 * @param {object} bank - 题库对象
 */
function setQuestionBank(bank) {
  const json = JSON.stringify(bank)
  if (json.length * 2 > MAX_STORAGE * 0.9) {
    log.error('题库过大', { size: json.length, questions: bank.total })
    throw new Error('题库数据过大，可能超出 localStorage 限制 (5MB)')
  }
  localStorage.setItem(KEY_BANK, json)
  log.info('题库已写入', { total: bank.total, size: json.length })
}

/**
 * 检查是否有题库
 * @returns {boolean}
 */
function hasQuestionBank() {
  const exists = localStorage.getItem(KEY_BANK) !== null
  log.debug('检查题库', { exists })
  return exists
}

/**
 * 获取历史记录列表
 * @returns {Array} 历史记录数组
 */
function getHistoryList() {
  try {
    const raw = localStorage.getItem(KEY_HISTORY)
    const list = raw ? JSON.parse(raw) : []
    log.debug('读取历史记录', { count: list.length })
    return list
  } catch (e) {
    log.error('读取历史记录异常', e.message)
    return []
  }
}

/**
 * 添加一条历史记录（插入到列表头部）
 * @param {object} record - 单次练习记录
 */
function addHistory(record) {
  const list = getHistoryList()
  list.unshift(record)
  localStorage.setItem(KEY_HISTORY, JSON.stringify(list))
  log.info('添加历史记录', { id: record.id, score: record.score, total: list.length })
}

/**
 * 根据 ID 获取一条历史记录
 * @param {string} id
 * @returns {object|null}
 */
function getHistoryById(id) {
  const list = getHistoryList()
  return list.find(r => r.id === id) || null
}

/**
 * 清空历史记录（保留题库）
 */
function clearHistory() {
  localStorage.setItem(KEY_HISTORY, JSON.stringify([]))
  log.info('历史记录已清空')
}

/**
 * 清空全部数据（题库 + 历史）
 */
function clearAll() {
  localStorage.removeItem(KEY_BANK)
  localStorage.removeItem(KEY_HISTORY)
  log.info('全部数据已清空')
}

/**
 * 导入新题库：覆盖旧题库、清空历史
 * @param {object} bank - 新题库对象
 */
function replaceBank(bank) {
  setQuestionBank(bank)
  clearHistory()
  log.info('题库已替换，历史已清空', { total: bank.total })
}

export {
  getStorageUsage,
  getQuestionBank,
  setQuestionBank,
  hasQuestionBank,
  getHistoryList,
  addHistory,
  getHistoryById,
  clearHistory,
  clearAll,
  replaceBank
}
