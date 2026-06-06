<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuizStore } from '../composables/useQuizStore'

const router = useRouter()
const { hasBank, totalCount, singleCount, multipleCount, booleanCount, importTime, importBank, startQuiz, startExam, examQuotaStatus } = useQuizStore()

const randomMode = ref(true)  // 默认随机

const fileInput = ref(null)
const loading = ref(false)
const message = ref('')
const messageType = ref('success') // 'success' | 'error'
const warnings = ref([])
const showConfirm = ref(false)

function triggerImport() {
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  loading.value = true
  message.value = ''

  const result = await importBank(file)
  loading.value = false

  if (result.success) {
    messageType.value = 'success'
    message.value = result.message
    warnings.value = result.warnings || []
    showConfirm.value = false
  } else {
    messageType.value = 'error'
    message.value = result.message
    warnings.value = []
  }

  // 重置 file input 以允许重复导入同一文件
  e.target.value = ''
}

function confirmReplace() {
  showConfirm.value = true
  message.value = ''
  warnings.value = []
}

function cancelReplace() {
  showConfirm.value = false
}

function confirmAndImport() {
  showConfirm.value = false
  triggerImport()
}

function triggerStart() {
  startQuiz(randomMode.value)
  router.push('/quiz')
}

function triggerExam() {
  const ok = startExam()
  if (ok) router.push('/quiz')
}

function formatTime(isoStr) {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <div class="max-w-lg mx-auto px-4 py-8 md:py-12">
    <!-- 无题库时的引导页 -->
    <div v-if="!hasBank" class="text-center">
      <div class="text-6xl mb-6">📚</div>
      <h1 class="text-2xl md:text-3xl font-bold text-gray-800 mb-3">刷题助手</h1>
      <p class="text-gray-500 mb-8 leading-relaxed">
        导入 Excel 题库，开始高效刷题练习<br />
        支持单选、多选，自动判分，记录历史成绩
      </p>

      <button
        @click="triggerImport"
        :disabled="loading"
        class="inline-flex items-center gap-2 px-8 py-4 bg-blue-600 text-white rounded-xl font-medium hover:bg-blue-700 active:bg-blue-800 transition disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-blue-200"
      >
        <svg v-if="loading" class="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
        </svg>
        <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/>
        </svg>
        {{ loading ? '正在解析...' : '导入题库' }}
      </button>

      <!-- 隐藏的文件输入 -->
      <input
        ref="fileInput"
        type="file"
        accept=".xlsx,.xls"
        class="hidden"
        @change="handleFileChange"
      />

      <p class="text-xs text-gray-400 mt-6">
        支持 .xlsx / .xls 格式 · 所有数据仅存储于本地
      </p>
    </div>

    <!-- 有题库时的首页 -->
    <div v-else>
      <h1 class="text-2xl md:text-3xl font-bold text-gray-800 mb-6 text-center">刷题助手</h1>

      <!-- 题库详情卡片 -->
      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-6">
        <h2 class="text-lg font-semibold text-gray-700 mb-4">📋 当前题库</h2>
        <div class="grid grid-cols-4 gap-3 mb-4">
          <div class="bg-blue-50 rounded-xl p-3 text-center">
            <div class="text-2xl font-bold text-blue-600">{{ totalCount }}</div>
            <div class="text-xs text-gray-500 mt-0.5">总题数</div>
          </div>
          <div class="bg-green-50 rounded-xl p-3 text-center">
            <div class="text-2xl font-bold text-green-600">{{ singleCount }}</div>
            <div class="text-xs text-gray-500 mt-0.5">单选</div>
          </div>
          <div class="bg-purple-50 rounded-xl p-3 text-center">
            <div class="text-2xl font-bold text-purple-600">{{ multipleCount }}</div>
            <div class="text-xs text-gray-500 mt-0.5">多选</div>
          </div>
          <div class="bg-amber-50 rounded-xl p-3 text-center">
            <div class="text-2xl font-bold text-amber-600">{{ booleanCount }}</div>
            <div class="text-xs text-gray-500 mt-0.5">判断</div>
          </div>
        </div>
        <div class="text-center text-xs text-gray-400">{{ formatTime(importTime) }} 导入</div>
      </div>

      <!-- 练习模式切换 -->
      <div class="flex items-center justify-center gap-2 mb-2">
        <button
          @click="randomMode = true"
          :class="randomMode ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-500'"
          class="px-4 py-2 rounded-lg text-sm font-medium transition"
        >🎲 随机</button>
        <button
          @click="randomMode = false"
          :class="!randomMode ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-500'"
          class="px-4 py-2 rounded-lg text-sm font-medium transition"
        >📋 顺序</button>
      </div>

      <!-- 模拟考试 -->
      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 mb-6">
        <h2 class="text-lg font-semibold text-gray-700 mb-4">📝 模拟考试</h2>
        <div class="grid grid-cols-3 gap-3 mb-4 text-center text-sm">
          <div class="bg-blue-50 rounded-xl p-3">
            <div class="font-bold text-blue-600">{{ examQuotaStatus?.available?.single || 0 }}/60</div>
            <div class="text-xs text-gray-500 mt-0.5">单选题 × 0.5分</div>
          </div>
          <div class="bg-purple-50 rounded-xl p-3">
            <div class="font-bold text-purple-600">{{ examQuotaStatus?.available?.multiple || 0 }}/100</div>
            <div class="text-xs text-gray-500 mt-0.5">多选题 × 0.5分</div>
          </div>
          <div class="bg-amber-50 rounded-xl p-3">
            <div class="font-bold text-amber-600">{{ examQuotaStatus?.available?.boolean || 0 }}/40</div>
            <div class="text-xs text-gray-500 mt-0.5">判断题 × 0.5分</div>
          </div>
        </div>
        <div v-if="examQuotaStatus?.warnings?.length > 0" class="mb-4 space-y-1">
          <p v-for="(w, i) in examQuotaStatus.warnings" :key="i" class="text-xs text-amber-600">⚠️ {{ w }}</p>
        </div>
        <div class="text-center text-xs text-gray-400 mb-4">限时 100 分钟，满分 100 分</div>
        <button
          @click="triggerExam"
          :disabled="!examQuotaStatus?.canStart"
          class="w-full py-4 bg-amber-600 text-white rounded-xl font-medium text-lg hover:bg-amber-700 active:bg-amber-800 transition disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-amber-200"
        >
          🏆 开始考试
        </button>
      </div>

      <!-- 操作按钮 -->
      <div class="flex flex-col gap-3">
        <button
          @click="triggerStart"
          class="w-full py-4 bg-blue-600 text-white rounded-xl font-medium text-lg hover:bg-blue-700 active:bg-blue-800 transition shadow-lg shadow-blue-200"
        >
          🚀 开始练习
        </button>
        <button
          @click="router.push('/history')"
          class="w-full py-3 bg-white border border-gray-200 text-gray-700 rounded-xl font-medium hover:bg-gray-50 active:bg-gray-100 transition"
        >
          📊 历史记录
        </button>
        <button
          @click="confirmReplace"
          class="w-full py-3 bg-white border border-gray-200 text-gray-500 rounded-xl font-medium hover:bg-gray-50 active:bg-gray-100 transition text-sm"
        >
          导入新题库
        </button>
      </div>
    </div>

    <!-- 消息提示 -->
    <div
      v-if="message"
      :class="[
        'mt-6 p-4 rounded-xl text-sm',
        messageType === 'success' ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-red-50 text-red-700 border border-red-200'
      ]"
    >
      <p class="font-medium">{{ messageType === 'success' ? '✅' : '❌' }} {{ message }}</p>
      <ul v-if="warnings.length > 0" class="mt-3 space-y-1 text-yellow-700">
        <li v-for="(w, i) in warnings" :key="i" class="text-xs">⚠️ {{ w }}</li>
      </ul>
    </div>

    <!-- 导入确认弹窗 -->
    <div
      v-if="showConfirm"
      class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
      @click.self="cancelReplace"
    >
      <div class="bg-white rounded-2xl shadow-xl p-6 max-w-sm w-full">
        <h3 class="text-lg font-semibold text-gray-800 mb-2">导入新题库</h3>
        <p class="text-gray-500 text-sm mb-6">
          导入新题库将<strong class="text-red-500">清空所有历史练习记录</strong>，是否继续？
        </p>
        <div class="flex gap-3">
          <button
            @click="cancelReplace"
            class="flex-1 py-2.5 bg-gray-100 text-gray-600 rounded-xl font-medium hover:bg-gray-200 transition"
          >
            取消
          </button>
          <button
            @click="confirmAndImport"
            class="flex-1 py-2.5 bg-red-500 text-white rounded-xl font-medium hover:bg-red-600 transition"
          >
            确认导入
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
