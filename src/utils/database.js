/**
 * database.js — SQLite 题库存储层
 * 仅在 Android 环境下使用 @capacitor-community/sqlite
 * 非 Android 环境所有函数静默返回 null/false
 */

import { CapacitorSQLite, SQLiteConnection } from '@capacitor-community/sqlite'
import { createLogger } from './logger'

const log = createLogger('database')

const DB_NAME = 'quiz_bank'
const TABLE_NAME = 'question_bank'
const ROW_KEY = 'bank'

/** @type {import('@capacitor-community/sqlite').SQLiteDBConnection|null} */
let _db = null

/**
 * 检查是否运行在 Capacitor 原生环境
 */
function isCapacitorNative() {
  return typeof window !== 'undefined' && window.Capacitor?.isNativePlatform()
}

/**
 * 初始化数据库连接并创建表（如不存在）
 * @returns {Promise<import('@capacitor-community/sqlite').SQLiteDBConnection|null>}
 */
async function initDatabase() {
  if (!isCapacitorNative()) return null
  if (_db) return _db

  try {
    const sqlite = new SQLiteConnection(CapacitorSQLite)
    _db = await sqlite.createConnection(DB_NAME, false, 'no-encryption', 1, false)
    await _db.open()

    // 创建 key-value 表
    await _db.execute(
      `CREATE TABLE IF NOT EXISTS ${TABLE_NAME} (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      )`
    )

    log.info('SQLite 数据库初始化完成', { db: DB_NAME })
    return _db
  } catch (e) {
    log.error('SQLite 初始化失败', e.message)
    _db = null
    return null
  }
}

/**
 * 获取数据库连接（初始化缓存）
 * @returns {Promise<import('@capacitor-community/sqlite').SQLiteDBConnection|null>}
 */
async function getDb() {
  return _db || (await initDatabase())
}

/**
 * 关闭数据库连接
 */
async function closeDatabase() {
  if (_db) {
    try {
      await _db.close()
      const sqlite = new SQLiteConnection(CapacitorSQLite)
      await sqlite.closeConnection(DB_NAME, false)
    } catch (e) {
      log.warn('关闭数据库连接异常', e.message)
    }
    _db = null
  }
}

/**
 * 从 SQLite 读取题库
 * @returns {Promise<object|null>}
 */
async function dbGetQuestionBank() {
  try {
    const db = await getDb()
    if (!db) return null

    const ret = await db.query(`SELECT value FROM ${TABLE_NAME} WHERE key = ?`, [ROW_KEY])
    const rows = ret.values || []
    if (rows.length === 0) return null

    const raw = rows[0].value
    if (!raw) return null

    const bank = JSON.parse(raw)
    log.info('从 SQLite 读取题库', { total: bank.total || 0 })
    return bank
  } catch (e) {
    log.error('从 SQLite 读取题库失败', e.message)
    return null
  }
}

/**
 * 写入题库到 SQLite
 * @param {object} bank
 */
async function dbSetQuestionBank(bank) {
  const json = JSON.stringify(bank)

  try {
    const db = await getDb()
    if (!db) throw new Error('数据库未初始化')

    await db.run(
      `INSERT OR REPLACE INTO ${TABLE_NAME} (key, value) VALUES (?, ?)`,
      [ROW_KEY, json]
    )
    log.info('题库已写入 SQLite', { total: bank.total, size: json.length })
  } catch (e) {
    log.error('写入 SQLite 失败', e.message)
    throw e
  }
}

/**
 * 检查 SQLite 中是否有题库
 * @returns {Promise<boolean>}
 */
async function dbHasQuestionBank() {
  try {
    const db = await getDb()
    if (!db) return false

    const ret = await db.query(`SELECT COUNT(*) AS cnt FROM ${TABLE_NAME} WHERE key = ?`, [ROW_KEY])
    const cnt = ret.values?.[0]?.cnt ?? 0
    return cnt > 0
  } catch (e) {
    log.error('检查 SQLite 题库失败', e.message)
    return false
  }
}

/**
 * 清空 SQLite 中的题库数据
 */
async function dbClearAll() {
  try {
    const db = await getDb()
    if (!db) return

    await db.run(`DELETE FROM ${TABLE_NAME} WHERE key = ?`, [ROW_KEY])
    log.info('SQLite 题库已清空')
  } catch (e) {
    log.error('清空 SQLite 题库失败', e.message)
  }
}

/**
 * 将 localStorage 中的题库迁移到 SQLite
 * 迁移后清除 localStorage 中的旧数据
 * @returns {Promise<boolean>} 是否执行了迁移
 */
async function migrateFromLocalStorage() {
  try {
    const raw = localStorage.getItem('questionBank')
    if (!raw) return false

    let bank
    try {
      bank = JSON.parse(raw)
    } catch {
      return false
    }
    if (!bank || !bank.questions) return false

    // 写入 SQLite
    await dbSetQuestionBank(bank)
    // 清除 localStorage 旧数据（保留历史记录）
    localStorage.removeItem('questionBank')
    log.info('题库已从 localStorage 迁移到 SQLite', { total: bank.total })
    return true
  } catch (e) {
    log.warn('题库迁移失败，保留 localStorage 数据', e.message)
    return false
  }
}

export {
  isCapacitorNative,
  initDatabase,
  closeDatabase,
  dbGetQuestionBank,
  dbSetQuestionBank,
  dbHasQuestionBank,
  dbClearAll,
  migrateFromLocalStorage
}
