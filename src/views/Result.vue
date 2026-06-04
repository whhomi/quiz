<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuizStore } from '../composables/useQuizStore'

const router = useRouter()
const { historyList } = useQuizStore()

const lastRecord = computed(() => historyList.value?.[0] || null)

function goHome() {
  router.push('/')
}

function goDetail() {
  if (lastRecord.value) {
    router.push(`/history/${lastRecord.value.id}`)
  }
}

function formatTime(ts) {
  return new Date(ts).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex items-center justify-center p-4">
    <div v-if="lastRecord" class="bg-white rounded-2xl shadow-xl p-8 max-w-sm w-full text-center">
      <div class="text-6xl mb-4">📊</div>
      <h2 class="text-2xl font-bold text-gray-800 mb-1">最近一次练习</h2>
      <p class="text-xs text-gray-400 mb-6">{{ formatTime(lastRecord.timestamp) }}</p>

      <!-- 得分圆环 -->
      <div class="relative w-28 h-28 mx-auto my-6">
        <svg class="w-full h-full -rotate-90" viewBox="0 0 100 100">
          <circle cx="50" cy="50" r="42" fill="none" stroke="#e5e7eb" stroke-width="8"/>
          <circle
            cx="50" cy="50" r="42" fill="none"
            :stroke="lastRecord.score >= 60 ? '#22c55e' : '#ef4444'"
            stroke-width="8"
            stroke-linecap="round"
            :stroke-dasharray="`${lastRecord.score * 2.64} 264`"
          />
        </svg>
        <div class="absolute inset-0 flex items-center justify-center">
          <span class="text-2xl font-bold" :class="lastRecord.score >= 60 ? 'text-green-600' : 'text-red-500'">
            {{ lastRecord.score }}%
          </span>
        </div>
      </div>

      <div class="flex justify-center gap-8 mb-6 text-sm">
        <div>
          <div class="text-2xl font-bold text-green-600">{{ lastRecord.correctCount }}</div>
          <div class="text-gray-400">正确</div>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-600">{{ lastRecord.answeredCount || lastRecord.details?.length || 0 }}</div>
          <div class="text-gray-400">已答</div>
        </div>
        <div>
          <div class="text-2xl font-bold text-blue-600">{{ formatDuration(lastRecord.duration) }}</div>
          <div class="text-gray-400">用时</div>
        </div>
      </div>

      <div class="flex flex-col gap-3">
        <button
          @click="goDetail"
          class="w-full py-3 bg-blue-600 text-white rounded-xl font-medium hover:bg-blue-700 transition"
        >
          查看详情
        </button>
        <button
          @click="goHome"
          class="w-full py-3 text-gray-400 rounded-xl font-medium hover:text-gray-600 transition text-sm"
        >
          返回首页
        </button>
      </div>
    </div>

    <div v-else class="text-center">
      <p class="text-gray-400 mb-4">暂无练习记录</p>
      <button
        @click="goHome"
        class="px-6 py-2 bg-blue-600 text-white rounded-xl font-medium hover:bg-blue-700 transition"
      >
        返回首页
      </button>
    </div>
  </div>
</template>
