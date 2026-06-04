<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuizStore } from '../composables/useQuizStore'
import { getStorageUsage } from '../utils/storage'

const router = useRouter()
const {
  state,
  hasBank,
  totalCount,
  singleCount,
  multipleCount,
  importTime,
  historyList,
  clearHistoryData,
  clearAllData
} = useQuizStore()

const showClearHistoryConfirm = ref(false)
const showClearAllConfirm = ref(false)

function confirmClearHistory() {
  clearHistoryData()
  showClearHistoryConfirm.value = false
}

function confirmClearAll() {
  clearAllData()
  showClearAllConfirm.value = false
}

function formatTime(isoStr) {
  if (!isoStr) return ''
  return new Date(isoStr).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

function formatStorage(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

const storageUsage = computed(() => formatStorage(getStorageUsage()))
</script>

<template>
  <div class="max-w-lg mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-800 mb-6">⚙️ 设置</h1>

    <!-- 题库信息 -->
    <div class="bg-white rounded-xl p-5 border border-gray-100 mb-6">
      <h2 class="text-lg font-semibold text-gray-700 mb-4">📋 当前题库</h2>

      <div v-if="hasBank" class="space-y-3">
        <div class="flex justify-between text-sm">
          <span class="text-gray-500">总题数</span>
          <span class="text-gray-700 font-medium">{{ totalCount }}</span>
        </div>
        <div class="flex justify-between text-sm">
          <span class="text-gray-500">单选题</span>
          <span class="text-gray-700">{{ singleCount }}</span>
        </div>
        <div class="flex justify-between text-sm">
          <span class="text-gray-500">多选题</span>
          <span class="text-gray-700">{{ multipleCount }}</span>
        </div>
        <div class="flex justify-between text-sm">
          <span class="text-gray-500">导入时间</span>
          <span class="text-gray-700">{{ formatTime(importTime) }}</span>
        </div>
        <div class="flex justify-between text-sm">
          <span class="text-gray-500">历史记录</span>
          <span class="text-gray-700">{{ historyList.length }} 条</span>
        </div>
      </div>
      <div v-else class="text-center py-4 text-gray-400 text-sm">
        暂未导入题库
      </div>
    </div>

    <!-- 存储用量 -->
    <div class="bg-white rounded-xl p-5 border border-gray-100 mb-6">
      <div class="flex justify-between items-center">
        <span class="text-sm text-gray-500">本地存储用量</span>
        <span class="text-sm text-gray-700 font-medium">{{ storageUsage }}</span>
      </div>
      <div class="w-full bg-gray-200 rounded-full h-1.5 mt-3">
        <div
          class="bg-blue-500 h-1.5 rounded-full"
          :style="{ width: `${Math.min((getStorageUsage() / (5 * 1024 * 1024)) * 100, 100)}%` }"
        />
      </div>
      <p class="text-xs text-gray-400 mt-1">上限 5MB</p>
    </div>

    <!-- 数据管理 -->
    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <h2 class="text-lg font-semibold text-gray-700 px-5 pt-5 pb-3">🗑 数据管理</h2>

      <!-- 清空历史记录 -->
      <div class="px-5 py-4 border-t border-gray-50">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-700">清空历史记录</p>
            <p class="text-xs text-gray-400">保留题库，仅删除练习记录</p>
          </div>
          <button
            @click="showClearHistoryConfirm = true"
            :disabled="historyList.length === 0"
            class="px-4 py-2 text-sm text-red-500 border border-red-200 rounded-lg hover:bg-red-50 transition disabled:opacity-30 disabled:cursor-not-allowed"
          >
            清空
          </button>
        </div>
      </div>

      <!-- 清除全部数据 -->
      <div class="px-5 py-4 border-t border-gray-50">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-700">清除全部数据</p>
            <p class="text-xs text-gray-400">删除题库和所有历史记录</p>
          </div>
          <button
            @click="showClearAllConfirm = true"
            class="px-4 py-2 text-sm text-red-500 border border-red-200 rounded-lg hover:bg-red-50 transition"
          >
            清除
          </button>
        </div>
      </div>
    </div>

    <!-- 清空历史确认弹窗 -->
    <div
      v-if="showClearHistoryConfirm"
      class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
      @click.self="showClearHistoryConfirm = false"
    >
      <div class="bg-white rounded-2xl shadow-xl p-6 max-w-sm w-full">
        <h3 class="text-lg font-semibold text-gray-800 mb-2">清空历史记录</h3>
        <p class="text-gray-500 text-sm mb-6">确定要清空所有练习记录吗？题库将保留。此操作不可恢复。</p>
        <div class="flex gap-3">
          <button
            @click="showClearHistoryConfirm = false"
            class="flex-1 py-2.5 bg-gray-100 text-gray-600 rounded-xl font-medium hover:bg-gray-200 transition"
          >
            取消
          </button>
          <button
            @click="confirmClearHistory"
            class="flex-1 py-2.5 bg-red-500 text-white rounded-xl font-medium hover:bg-red-600 transition"
          >
            确认清空
          </button>
        </div>
      </div>
    </div>

    <!-- 清除全部确认弹窗 -->
    <div
      v-if="showClearAllConfirm"
      class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4"
      @click.self="showClearAllConfirm = false"
    >
      <div class="bg-white rounded-2xl shadow-xl p-6 max-w-sm w-full">
        <h3 class="text-lg font-semibold text-gray-800 mb-2">清除全部数据</h3>
        <p class="text-gray-500 text-sm mb-6">
          确定要清除<strong class="text-red-500">题库和所有历史记录</strong>吗？此操作不可恢复。
        </p>
        <div class="flex gap-3">
          <button
            @click="showClearAllConfirm = false"
            class="flex-1 py-2.5 bg-gray-100 text-gray-600 rounded-xl font-medium hover:bg-gray-200 transition"
          >
            取消
          </button>
          <button
            @click="confirmClearAll"
            class="flex-1 py-2.5 bg-red-500 text-white rounded-xl font-medium hover:bg-red-600 transition"
          >
            确认清除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
