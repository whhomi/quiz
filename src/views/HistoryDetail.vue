<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuizStore } from '../composables/useQuizStore'

const route = useRoute()
const router = useRouter()
const { getHistoryById, state } = useQuizStore()

const recordId = route.params.id
const record = computed(() => getHistoryById(recordId))

const filter = ref('all') // 'all' | 'single' | 'multiple' | 'boolean'

const filteredDetails = computed(() => {
  if (!record.value) return []
  const details = record.value.details || []
  if (filter.value === 'all') return details
  return details.filter(d => {
    const q = findQuestion(d.questionId)
    return q?.type === filter.value
  })
})

function findQuestion(questionId) {
  if (!state.questionBank) return null
  return state.questionBank.questions.find(q => q.id === questionId) || null
}

function formatDate(ts) {
  return new Date(ts).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

const optionLetters = ['A', 'B', 'C', 'D', 'E', 'F']

function goBack() {
  router.push('/history')
}
</script>

<template>
  <div class="max-w-lg mx-auto px-4 py-8" v-if="record">
    <!-- 返回 + 标题 -->
    <div class="flex items-center gap-3 mb-4">
      <button @click="goBack" class="text-gray-400 hover:text-gray-600 transition">
        ← 返回
      </button>
    </div>

    <!-- 成绩概览 -->
    <div class="bg-white rounded-2xl p-6 border border-gray-100 mb-6">
      <h1 class="text-xl font-bold text-gray-800 mb-1">练习详情</h1>
      <p class="text-xs text-gray-400 mb-4">{{ formatDate(record.timestamp) }}</p>

      <div class="flex justify-center gap-8 mb-4">
        <div class="text-center">
          <div class="text-3xl font-bold" :class="record.score >= 60 ? 'text-green-600' : 'text-red-500'">
            <template v-if="record.mode === 'exam'">{{ record.score }} / {{ record.maxScore || 100 }} 分</template>
            <template v-else>{{ record.score }}%</template>
          </div>
          <div class="text-xs text-gray-400 mt-1">正确率</div>
        </div>
        <div class="text-center">
          <div class="text-3xl font-bold text-gray-700">{{ record.correctCount }}/{{ record.totalCount }}</div>
          <div class="text-xs text-gray-400 mt-1">正确 / 总题</div>
        </div>
        <div class="text-center">
          <div class="text-3xl font-bold text-blue-600">{{ formatDuration(record.duration) }}</div>
          <div class="text-xs text-gray-400 mt-1">用时</div>
        </div>
      </div>

      <!-- 考试模式：各题型细分 -->
      <div v-if="record.mode === 'exam' && record.breakdown" class="grid grid-cols-3 gap-2 mb-4 text-xs">
        <div class="bg-blue-50 rounded-lg p-2 text-center">
          <div class="font-bold text-blue-600">{{ record.breakdown.single?.correct || 0 }}/{{ record.breakdown.single?.total || 0 }}</div>
          <div class="text-gray-400">单选题</div>
        </div>
        <div class="bg-purple-50 rounded-lg p-2 text-center">
          <div class="font-bold text-purple-600">{{ record.breakdown.multiple?.correct || 0 }}/{{ record.breakdown.multiple?.total || 0 }}</div>
          <div class="text-gray-400">多选题</div>
        </div>
        <div class="bg-amber-50 rounded-lg p-2 text-center">
          <div class="font-bold text-amber-600">{{ record.breakdown.boolean?.correct || 0 }}/{{ record.breakdown.boolean?.total || 0 }}</div>
          <div class="text-gray-400">判断题</div>
        </div>
      </div>

      <div class="text-center text-xs text-gray-400 mb-4">
        已答 {{ record.answeredCount || record.details.length }} 题，跳过 {{ record.totalCount - (record.answeredCount || record.details.length) }} 题
      </div>

      <!-- 过滤按钮 -->
      <div class="flex gap-2 justify-center">
        <button
          v-for="f in [{k:'all',l:'全部'},{k:'single',l:'单选'},{k:'multiple',l:'多选'},{k:'boolean',l:'判断'}]"
          :key="f.k"
          @click="filter = f.k"
          :class="[
            'px-4 py-1.5 rounded-full text-xs font-medium transition',
            filter === f.k ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
          ]"
        >
          {{ f.l }}
        </button>
      </div>
    </div>

    <!-- 逐题详情 -->
    <div v-if="filteredDetails.length === 0" class="text-center py-8 text-gray-400">
      该分类下无已答题目（跳过的题目不记录详情）
    </div>

    <div v-else class="space-y-4">
      <div
        v-for="(detail, i) in filteredDetails"
        :key="detail.questionId"
        class="bg-white rounded-xl border p-4"
        :class="detail.isCorrect ? 'border-green-200' : 'border-red-200'"
      >
        <div class="flex items-start gap-3">
          <span
            :class="[
              'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5',
              detail.isCorrect ? 'bg-green-500 text-white' : 'bg-red-500 text-white'
            ]"
          >
            {{ detail.isCorrect ? '✓' : '✗' }}
          </span>
          <div class="flex-1 min-w-0">
            <!-- 题干 -->
            <p class="text-gray-800 font-medium mb-2 break-words">
              {{ findQuestion(detail.questionId)?.stem || '(题目已删除)' }}
            </p>

            <template v-if="findQuestion(detail.questionId)">
              <!-- 选项 -->
              <div class="space-y-1 mb-3">
                <div
                  v-for="(opt, oi) in findQuestion(detail.questionId).options"
                  :key="oi"
                  :class="[
                    'text-sm py-1 px-2 rounded break-words',
                    findQuestion(detail.questionId).answer.includes(oi)
                      ? 'bg-green-50 text-green-700'
                      : detail.userAnswer.includes(oi) && !findQuestion(detail.questionId).answer.includes(oi)
                        ? 'bg-red-50 text-red-700'
                        : 'text-gray-500'
                  ]"
                >
                  {{ optionLetters[oi] }}. {{ opt }}
                  <span v-if="findQuestion(detail.questionId).answer.includes(oi)" class="text-green-500 ml-1">✓</span>
                  <span v-if="detail.userAnswer.includes(oi) && !findQuestion(detail.questionId).answer.includes(oi)" class="text-red-500 ml-1">✗ 你的选择</span>
                </div>
              </div>

              <!-- 解析 -->
              <div v-if="findQuestion(detail.questionId).analysis" class="mt-2 p-3 rounded-lg border-l-4 border-blue-400 bg-blue-50/50">
                <p class="text-xs text-blue-500 font-medium mb-1">💡 解析</p>
                <p class="text-sm text-gray-700 leading-relaxed break-words">{{ findQuestion(detail.questionId).analysis }}</p>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-8 text-center">
      <button @click="goBack" class="text-blue-600 hover:text-blue-700 text-sm">
        返回历史列表
      </button>
    </div>
  </div>

  <!-- 记录不存在 -->
  <div v-else class="max-w-lg mx-auto px-4 py-16 text-center">
    <div class="text-5xl mb-4">🔍</div>
    <p class="text-gray-400 mb-4">找不到该练习记录</p>
    <button @click="router.push('/history')" class="text-blue-600 hover:text-blue-700">
      返回历史列表
    </button>
  </div>
</template>
