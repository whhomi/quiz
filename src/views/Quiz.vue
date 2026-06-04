<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { useQuizStore } from '../composables/useQuizStore'

const router = useRouter()
const {
  state,
  hasBank,
  startQuiz,
  answerCurrent,
  goNext,
  goPrev,
  jumpToQuestion,
  finishQuiz,
  endQuiz,
  getCurrentQuestion,
  isCurrentAnswered,
  getUnansweredCount,
  getProgress
} = useQuizStore()

// 选中的选项索引
const selected = ref([])
const showResult = ref(false)
const finalScore = ref(null)
const finalRecordId = ref(null)
const sessionEnded = ref(false)
const answeredCount = ref(0)  // 显式已答题计数，驱动进度条

// 当前题目
const question = computed(() => getCurrentQuestion(state.session))

// 安全的题目对象（确保 answer/options 始终是数组）
const safeQuestion = computed(() => {
  const q = question.value
  if (!q) return null
  return {
    ...q,
    answer: Array.isArray(q.answer) ? q.answer : [],
    options: Array.isArray(q.options) ? q.options : []
  }
})

const progress = computed(() => state.session ? getProgress(state.session) : { current: 0, total: 0, answered: 0, unanswered: 0 })
const isAnswered = computed(() => state.session ? isCurrentAnswered(state.session) : false)
const lastJudgment = computed(() => state.lastJudgment)

// 是否可以在当前题修改答案（未答或未判定）
const canModify = computed(() => !isAnswered.value)

// 已选择的答案索引
const savedAnswer = computed(() => {
  if (!state.session || !question.value) return []
  const ans = state.session.answers[question.value.id]
  return ans ? ans.userAnswer : []
})

// 初始化答题会话
onMounted(() => {
  if (!hasBank.value) {
    router.replace('/')
    return
  }
  if (!state.session) {
    startQuiz()
  }
  // 恢复当前题的已选答案
  if (savedAnswer.value.length > 0) {
    selected.value = [...savedAnswer.value]
  }
})

// 路由离开守卫
onBeforeRouteLeave((to, from, next) => {
  if (sessionEnded.value) {
    next()
    return
  }
  const unanswered = state.session ? getUnansweredCount(state.session) : 0
  if (unanswered === 0 || state.session?.isFinished) {
    next()
    return
  }
  const ok = confirm(`还有 ${unanswered} 道题未作答，确定要离开吗？\n离开后本次练习将丢失。`)
  if (ok) {
    endQuiz()
    next()
  } else {
    next(false)
  }
})

// 监听题目切换，恢复 saved answer
watch(() => state.session?.currentIndex, () => {
  if (savedAnswer.value.length > 0) {
    selected.value = [...savedAnswer.value]
  } else {
    selected.value = []
  }
})

function toggleOption(index) {
  if (!canModify.value) return // 已判定不可修改

  if (safeQuestion.value && safeQuestion.value.type === 'single') {
    selected.value = [index]
  } else {
    const pos = selected.value.indexOf(index)
    if (pos >= 0) {
      selected.value.splice(pos, 1)
    } else {
      selected.value.push(index)
    }
  }
}

function handleNext() {
  // 单选题未答：提交答案并展示对错，不立即跳题
  if (safeQuestion.value && safeQuestion.value.type === 'single' && !isAnswered.value) {
    if (selected.value.length === 0) {
      goNext()      // 没选就跳过
      return
    }
    const ok = answerCurrent(selected.value)
    if (ok) answeredCount.value++
    return          // 停在当前题，展示判题反馈
  }

  const hasMore = goNext()
  if (!hasMore) {
    finishAndShow()
  }
}

function handleSubmit() {
  if (selected.value.length === 0) return
  const ok = answerCurrent(selected.value)
  if (ok) answeredCount.value++
}

function handlePrev() {
  goPrev()
}

function handleFinish() {
  const unanswered = state.session ? getUnansweredCount(state.session) : 0
  if (unanswered > 0) {
    const ok = confirm(`还有 ${unanswered} 道题未作答，确定提前交卷吗？\n未答题将计为错误。`)
    if (!ok) return
  }
  finishAndShow()
}

function finishAndShow() {
  const result = finishQuiz()
  if (result) {
    finalScore.value = result.score
    finalRecordId.value = result.record.id
    sessionEnded.value = true
    showResult.value = true
  }
}

function goHome() {
  router.push('/')
}

function goDetail() {
  if (finalRecordId.value) {
    router.push(`/history/${finalRecordId.value}`)
  } else {
    router.push('/history')
  }
}

function retryQuiz() {
  showResult.value = false
  finalScore.value = null
  finalRecordId.value = null
  sessionEnded.value = false
  answeredCount.value = 0
  startQuiz()
  selected.value = []
}

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

// 选项字母
const optionLetters = ['A', 'B', 'C', 'D', 'E', 'F']
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex flex-col" v-if="safeQuestion && !showResult">
    <!-- 顶部进度条 -->
    <div class="bg-white border-b border-gray-200 sticky top-0 z-40">
      <div class="max-w-lg mx-auto px-4 py-3">
        <div class="flex items-center justify-between mb-2">
          <div class="flex items-center gap-2">
            <span class="text-sm font-semibold text-gray-700">
              第 {{ progress.current }}/{{ progress.total }} 题
            </span>
            <span class="text-xs text-gray-400">
              (已答 {{ answeredCount }})
            </span>
          </div>
          <button
            @click="handleFinish"
            class="px-3 py-1.5 text-xs font-medium text-orange-600 border border-orange-300 rounded-lg hover:bg-orange-50 active:bg-orange-100 transition"
          >
            提前交卷
          </button>
        </div>
        <div class="w-full bg-gray-100 rounded-full h-2">
          <div
            class="bg-blue-500 h-2 rounded-full transition-all duration-300"
            :style="{ width: `${Math.max((answeredCount / progress.total) * 100, 2)}%` }"
          />
        </div>
      </div>
    </div>

    <!-- 题目区域 -->
    <div class="flex-1 max-w-lg mx-auto w-full px-4 py-6">
      <!-- 题型标签 -->
      <div class="mb-4">
        <span
          :class="[
            'inline-block px-3 py-1 rounded-full text-xs font-medium',
            safeQuestion.type === 'single' ? 'bg-blue-100 text-blue-700' : 'bg-purple-100 text-purple-700'
          ]"
        >
          {{ safeQuestion.type === 'single' ? '单选题' : '多选题' }}
        </span>
      </div>

      <!-- 题干 -->
      <h2 class="text-lg md:text-xl font-semibold text-gray-800 leading-relaxed mb-6 break-words overflow-hidden">
        {{ safeQuestion.stem }}
      </h2>

      <!-- 选项列表 -->
      <div class="space-y-3">
        <button
          v-for="(opt, idx) in safeQuestion.options"
          :key="idx"
          @click="toggleOption(idx)"
          :disabled="!canModify && isAnswered"
          class="w-full text-left"
        >
          <div
            :class="[
              'flex items-center gap-3 p-4 rounded-xl border-2 transition-all',
              canModify && selected.includes(idx)
                ? 'border-blue-500 bg-blue-50'
                : isAnswered && safeQuestion.answer.includes(idx)
                  ? 'border-green-400 bg-green-50'
                  : isAnswered && selected.includes(idx) && !safeQuestion.answer.includes(idx)
                    ? 'border-red-400 bg-red-50'
                    : 'border-gray-200 bg-white hover:border-gray-300',
              !canModify && isAnswered ? 'cursor-default' : 'cursor-pointer'
            ]"
          >
            <!-- 选项指示器 -->
            <div
              :class="[
                'w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold flex-shrink-0',
                safeQuestion.type === 'single' ? 'rounded-full' : 'rounded-lg',
                canModify && selected.includes(idx)
                  ? 'bg-blue-500 text-white'
                  : isAnswered && safeQuestion.answer.includes(idx)
                    ? 'bg-green-500 text-white'
                    : isAnswered && selected.includes(idx) && !safeQuestion.answer.includes(idx)
                      ? 'bg-red-500 text-white'
                      : 'bg-gray-100 text-gray-600'
              ]"
            >
              <template v-if="isAnswered && safeQuestion.answer.includes(idx)">✓</template>
              <template v-else-if="isAnswered && selected.includes(idx) && !safeQuestion.answer.includes(idx)">✗</template>
              <template v-else>{{ optionLetters[idx] }}</template>
            </div>
            <span class="flex-1 text-gray-700 break-words">{{ opt }}</span>
          </div>
        </button>
      </div>

      <!-- 判题反馈 -->
      <div
        v-if="isAnswered && lastJudgment?.questionId === safeQuestion.id"
        :class="[
          'mt-4 p-4 rounded-xl text-sm font-medium',
          safeQuestion.answer.length === 0
            ? 'bg-amber-50 text-amber-700 border border-amber-200'
            : lastJudgment.isCorrect ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-red-50 text-red-700 border border-red-200'
        ]"
      >
        <template v-if="safeQuestion.answer.length === 0">
          📝 本题暂无标准答案，请自行判断
        </template>
        <template v-else-if="lastJudgment.isCorrect">
          ✅ 回答正确！
        </template>
        <template v-else>
          ❌ 回答错误！正确答案是：
          <span class="font-bold">
            {{ safeQuestion.answer.map(i => optionLetters[i]).join('、') }}
          </span>
        </template>
        <div v-if="safeQuestion.analysis" class="mt-3 p-3 bg-white/60 rounded-lg border-l-4 border-blue-400">
          <p class="text-xs text-blue-500 font-medium mb-1">💡 解析</p>
          <p class="text-sm text-gray-700 leading-relaxed break-words">{{ safeQuestion.analysis }}</p>
        </div>
      </div>

      <!-- 解析（已回答且判题后显示） -->
      <div
        v-if="isAnswered && safeQuestion.analysis && lastJudgment?.questionId !== safeQuestion.id"
        class="mt-4 p-4 rounded-xl border-l-4 border-blue-400 bg-blue-50/50"
      >
        <p class="text-xs text-blue-500 font-medium mb-1">💡 解析</p>
        <p class="text-sm text-gray-700 leading-relaxed break-words">{{ safeQuestion.analysis }}</p>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="bg-white border-t border-gray-200 sticky bottom-0 z-40">
      <div class="max-w-lg mx-auto px-4 py-3 flex items-center justify-between">
        <!-- 上一题 -->
        <button
          @click="handlePrev"
          :disabled="progress.current <= 1"
          class="flex items-center gap-1 px-4 py-2.5 text-sm font-medium text-gray-600 bg-gray-50 rounded-xl border border-gray-200 hover:bg-gray-100 hover:border-gray-300 active:bg-gray-200 transition disabled:opacity-30 disabled:cursor-not-allowed"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
          上一题
        </button>

        <!-- 跳转 + 进度 -->
        <div class="flex items-center gap-2">
          <button
            @click="jumpToQuestion(Math.max(0, (state.session?.currentIndex || 0) - 10))"
            class="w-7 h-7 flex items-center justify-center text-gray-400 hover:text-blue-500 hover:bg-blue-50 rounded-lg transition text-xs"
            title="前10题"
          >◀◀</button>
          <div class="flex items-center gap-1">
            <input
              type="number"
              :value="progress.current"
              @change="e => jumpToQuestion(Math.max(0, Math.min(progress.total - 1, parseInt(e.target.value) - 1)))"
              class="w-14 text-center border-2 border-gray-200 rounded-lg px-2 py-1.5 text-sm font-mono font-semibold text-gray-700 focus:border-blue-400 focus:outline-none transition"
              :min="1"
              :max="progress.total"
            />
            <span class="text-sm text-gray-400 font-mono">/ {{ progress.total }}</span>
          </div>
          <button
            @click="jumpToQuestion(Math.min(progress.total - 1, (state.session?.currentIndex || 0) + 10))"
            class="w-7 h-7 flex items-center justify-center text-gray-400 hover:text-blue-500 hover:bg-blue-50 rounded-lg transition text-xs"
            title="后10题"
          >▶▶</button>
        </div>

        <!-- 下一题 / 提交 / 完成 -->
        <button
          v-if="safeQuestion.type === 'single' && !isAnswered"
          @click="handleNext"
          :disabled="selected.length === 0"
          class="flex items-center gap-1 px-5 py-2.5 text-sm font-medium text-white bg-blue-600 rounded-xl hover:bg-blue-700 active:bg-blue-800 transition disabled:opacity-40 disabled:cursor-not-allowed shadow-sm"
        >
          {{ progress.current === progress.total ? '完成' : '下一题' }}
          <svg v-if="progress.current < progress.total" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        </button>
        <button
          v-else-if="safeQuestion.type === 'single' && isAnswered"
          @click="handleNext"
          class="flex items-center gap-1 px-5 py-2.5 text-sm font-medium text-white bg-green-600 rounded-xl hover:bg-green-700 active:bg-green-800 transition shadow-sm"
        >
          {{ progress.current === progress.total ? '完成' : '继续' }}
          <svg v-if="progress.current < progress.total" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        </button>
        <button
          v-else-if="safeQuestion.type === 'multiple' && !isAnswered"
          @click="handleSubmit"
          :disabled="selected.length === 0"
          class="flex items-center gap-1 px-5 py-2.5 text-sm font-medium text-white bg-purple-600 rounded-xl hover:bg-purple-700 active:bg-purple-800 transition disabled:opacity-40 disabled:cursor-not-allowed shadow-sm"
        >
          提交答案
        </button>
        <button
          v-else-if="safeQuestion.type === 'multiple' && isAnswered"
          @click="handleNext"
          class="flex items-center gap-1 px-5 py-2.5 text-sm font-medium text-white bg-purple-600 rounded-xl hover:bg-purple-700 active:bg-purple-800 transition shadow-sm"
        >
          {{ progress.current === progress.total ? '完成' : '下一题' }}
          <svg v-if="progress.current < progress.total" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
        </button>
      </div>
    </div>
  </div>

  <!-- 成绩弹窗 -->
  <div
    v-if="showResult && finalScore"
    class="min-h-screen bg-gray-50 flex items-center justify-center p-4"
  >
    <div class="bg-white rounded-2xl shadow-xl p-8 max-w-sm w-full text-center">
      <div class="text-6xl mb-4">🎉</div>
      <h2 class="text-2xl font-bold text-gray-800 mb-2">练习完成！</h2>

      <!-- 得分圆环 -->
      <div class="relative w-28 h-28 mx-auto my-6">
        <svg class="w-full h-full -rotate-90" viewBox="0 0 100 100">
          <circle cx="50" cy="50" r="42" fill="none" stroke="#e5e7eb" stroke-width="8"/>
          <circle
            cx="50" cy="50" r="42" fill="none"
            :stroke="finalScore.score >= 60 ? '#22c55e' : '#ef4444'"
            stroke-width="8"
            stroke-linecap="round"
            :stroke-dasharray="`${finalScore.score * 2.64} 264`"
            class="transition-all duration-1000"
          />
        </svg>
        <div class="absolute inset-0 flex items-center justify-center">
          <span class="text-2xl font-bold" :class="finalScore.score >= 60 ? 'text-green-600' : 'text-red-500'">
            {{ finalScore.score }}%
          </span>
        </div>
      </div>

      <div class="flex justify-center gap-8 mb-6 text-sm">
        <div>
          <div class="text-2xl font-bold text-green-600">{{ finalScore.correctCount }}</div>
          <div class="text-gray-400">正确</div>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-600">{{ finalScore.answeredCount }}</div>
          <div class="text-gray-400">已答</div>
        </div>
        <div>
          <div class="text-2xl font-bold text-gray-600">{{ finalScore.totalCount }}</div>
          <div class="text-gray-400">总题</div>
        </div>
        <div>
          <div class="text-2xl font-bold text-blue-600">{{ formatDuration(finalScore.duration) }}</div>
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
          @click="retryQuiz"
          class="w-full py-3 bg-gray-100 text-gray-600 rounded-xl font-medium hover:bg-gray-200 transition"
        >
          再练一次
        </button>
        <button
          @click="goHome"
          class="w-full py-3 text-gray-400 rounded-xl font-medium hover:text-gray-600 transition text-sm"
        >
          返回首页
        </button>
      </div>
    </div>
  </div>

  <!-- 加载中 / 无数据 -->
  <div v-if="!safeQuestion && !showResult" class="min-h-screen bg-gray-50 flex items-center justify-center">
    <p class="text-gray-400">加载中...</p>
  </div>
</template>
