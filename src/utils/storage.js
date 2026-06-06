/**
 * storage.js — 存储封装层
 * 管理 questionBank 和 historyList 的读写
 * Android 环境下题库使用 SQLite，历史记录仍用 localStorage
 */

import { createLogger } from './logger'
import {
  isCapacitorNative,
  dbGetQuestionBank,
  dbSetQuestionBank,
  dbHasQuestionBank,
  dbClearAll,
  migrateFromLocalStorage
} from './database'

const log = createLogger('storage')
const KEY_BANK = 'questionBank'
const KEY_HISTORY = 'historyList'
const MAX_STORAGE = 5 * 1024 * 1024 // 5MB localStorage limit

// ---- Android 缓存 ----
/** @type {object|null} Android 环境下的题库内存缓存 */
let _androidBank = null
/** @type {boolean} Android 环境是否已初始化 */
let _androidReady = false

/**
 * 初始化 Android 存储（从 SQLite 加载题库到缓存）
 * 在模块加载时由 store 调用
 */
async function initAndroidStorage() {
  if (!isCapacitorNative()) return
  if (_androidReady) return
  _androidReady = true

  // 首次启动尝试从 localStorage 迁移到 SQLite
  const migrated = await migrateFromLocalStorage()
  if (migrated) {
    _androidBank = await dbGetQuestionBank()
    log.info('Android 存储初始化完成（已迁移）', { hasBank: !!_androidBank })
    return
  }

  // 直接从 SQLite 加载
  _androidBank = await dbGetQuestionBank()
  log.info('Android 存储初始化完成', { hasBank: !!_androidBank })
}

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
  // Android 路径：返回内存缓存
  if (isCapacitorNative()) {
    return _androidBank
  }

  // localStorage 路径
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
  // Android 路径：写入内存缓存 + 异步写入 SQLite
  if (isCapacitorNative()) {
    const json = JSON.stringify(bank)
    _androidBank = bank
    dbSetQuestionBank(bank).catch(e => log.error('SQLite 写入失败', e.message))
    log.info('题库已写入 SQLite', { total: bank.total, size: json.length })
    return
  }

  // localStorage 路径
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
  // Android 路径
  if (isCapacitorNative()) {
    return _androidBank !== null
  }

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
  // Android 路径
  if (isCapacitorNative()) {
    _androidBank = null
    dbClearAll().catch(e => log.error('SQLite 清空失败', e.message))
    localStorage.removeItem(KEY_HISTORY)
    log.info('全部数据已清空（SQLite + localStorage）')
    return
  }

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
  initAndroidStorage,
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
