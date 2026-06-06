<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuizStore } from '../composables/useQuizStore'

const router = useRouter()
const { historyList, hasBank } = useQuizStore()

function viewDetail(id) {
  router.push(`/history/${id}`)
}

function formatDate(ts) {
  return new Date(ts).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}
</script>

<template>
  <div class="max-w-lg mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-800 mb-6">📊 历史记录</h1>

    <!-- 空状态 -->
    <div v-if="historyList.length === 0" class="text-center py-16">
      <div class="text-5xl mb-4">📭</div>
      <p class="text-gray-400 mb-2">暂无练习记录</p>
      <button
        v-if="hasBank"
        @click="router.push('/quiz')"
        class="mt-4 px-6 py-2 bg-blue-600 text-white rounded-xl font-medium hover:bg-blue-700 transition"
      >
        去练习
      </button>
      <button
        v-else
        @click="router.push('/')"
        class="mt-4 px-6 py-2 bg-blue-600 text-white rounded-xl font-medium hover:bg-blue-700 transition"
      >
        导入题库
      </button>
    </div>

    <!-- 历史列表 -->
    <div v-else class="space-y-3">
      <div
        v-for="record in historyList"
        :key="record.id"
        @click="viewDetail(record.id)"
        class="bg-white rounded-xl p-4 border border-gray-100 hover:border-blue-200 hover:shadow-sm cursor-pointer transition"
      >
        <div class="flex items-center justify-between mb-2">
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-400">{{ formatDate(record.timestamp) }}</span>
            <span
              v-if="record.mode === 'exam'"
              class="px-1.5 py-0.5 rounded text-xs font-medium bg-amber-100 text-amber-700"
            >考试</span>
          </div>
          <span
            :class="[
              'px-2 py-0.5 rounded-full text-xs font-medium',
              record.mode === 'exam'
                ? (record.score >= 60 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700')
                : (record.score >= 80 ? 'bg-green-100 text-green-700' :
                   record.score >= 60 ? 'bg-yellow-100 text-yellow-700' :
                   'bg-red-100 text-red-700')
            ]"
          >
            <template v-if="record.mode === 'exam'">{{ record.score }} / {{ record.maxScore || 100 }}分</template>
            <template v-else>{{ record.score }}%</template>
          </span>
        </div>
        <div class="flex items-center gap-6 text-sm text-gray-500">
          <span>正确 {{ record.correctCount }}/{{ record.answeredCount || record.details?.length || 0 }}</span>
          <span>⏱ {{ formatDuration(record.duration) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
