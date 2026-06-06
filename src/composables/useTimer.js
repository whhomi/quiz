/**
 * useTimer.js — 倒计时 composable
 * @param {number} totalSeconds - 总倒计时秒数
 * @param {Function} onExpire - 归零时的回调
 */
import { ref, computed, onUnmounted } from 'vue'

export function useTimer(totalSeconds, onExpire) {
  const remaining = ref(totalSeconds)
  const isRunning = ref(false)
  let intervalId = null

  const formatted = computed(() => {
    const m = Math.floor(remaining.value / 60)
    const s = remaining.value % 60
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  })

  const isWarning = computed(() => remaining.value <= 600 && remaining.value > 300)
  const isDanger = computed(() => remaining.value <= 300)

  function start() {
    if (isRunning.value) return
    isRunning.value = true
    intervalId = setInterval(() => {
      remaining.value--
      if (remaining.value <= 0) {
        remaining.value = 0
        stop()
        if (onExpire) onExpire()
      }
    }, 1000)
  }

  function stop() {
    isRunning.value = false
    if (intervalId !== null) {
      clearInterval(intervalId)
      intervalId = null
    }
  }

  function reset(seconds) {
    stop()
    remaining.value = seconds
  }

  onUnmounted(() => stop())

  return { remaining, isRunning, formatted, isWarning, isDanger, start, stop, reset }
}
