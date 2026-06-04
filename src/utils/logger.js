/**
 * logger.js — 集中式日志工具
 * 带命名空间、级别过滤、可选持久化
 */

const LEVELS = { debug: 0, info: 1, warn: 2, error: 3 }
const LEVEL_LABELS = { debug: 'DEBUG', info: 'INFO', warn: 'WARN', error: 'ERROR' }
const LEVEL_COLORS = {
  debug: '#6b7280',
  info: '#2563eb',
  warn: '#d97706',
  error: '#dc2626'
}

const LOG_STORAGE_KEY = '__quiz_logs__'
const MAX_STORED_LOGS = 200

let _level = import.meta.env.DEV ? 'debug' : 'warn'
let _storageEnabled = false

function getStoredLogs() {
  try {
    return JSON.parse(localStorage.getItem(LOG_STORAGE_KEY) || '[]')
  } catch {
    return []
  }
}

function persistLog(entry) {
  if (!_storageEnabled) return
  try {
    const logs = getStoredLogs()
    logs.push(entry)
    if (logs.length > MAX_STORED_LOGS) {
      logs.splice(0, logs.length - MAX_STORED_LOGS)
    }
    localStorage.setItem(LOG_STORAGE_KEY, JSON.stringify(logs))
  } catch {
    // 存储满了就放弃
  }
}

function shouldLog(level) {
  return LEVELS[level] >= LEVELS[_level]
}

function createLogger(namespace) {
  function log(level, message, data) {
    if (!shouldLog(level)) return

    const timestamp = new Date().toISOString()
    const prefix = `%c[${timestamp.slice(11, 23)}] [${namespace}] [${LEVEL_LABELS[level]}]`
    const style = `color: ${LEVEL_COLORS[level]}; font-weight: bold`

    const args = [prefix, style, message]
    if (data !== undefined) {
      args.push(data)
    }

    const method = level === 'error' ? 'error' : level === 'warn' ? 'warn' : 'log'
    console[method](...args)

    // 持久化
    if (_storageEnabled) {
      persistLog({
        ts: timestamp,
        ns: namespace,
        level,
        msg: message,
        data: data !== undefined ? (typeof data === 'object' ? JSON.stringify(data) : String(data)) : undefined
      })
    }
  }

  function time(label) {
    if (!shouldLog('debug')) return () => {}
    const start = performance.now()
    return () => {
      const elapsed = (performance.now() - start).toFixed(1)
      log('debug', `${label} ⏱ ${elapsed}ms`)
    }
  }

  return {
    debug: (msg, data) => log('debug', msg, data),
    info: (msg, data) => log('info', msg, data),
    warn: (msg, data) => log('warn', msg, data),
    error: (msg, data) => log('error', msg, data),
    time
  }
}

function setLogLevel(level) {
  if (LEVELS[level] !== undefined) {
    _level = level
    console.log(`[logger] 日志级别设为: ${level}`)
  }
}

function enableStorage(enabled = true) {
  _storageEnabled = enabled
}

function getLogs() {
  return getStoredLogs()
}

function clearLogs() {
  localStorage.removeItem(LOG_STORAGE_KEY)
}

export { createLogger, setLogLevel, enableStorage, getLogs, clearLogs }
