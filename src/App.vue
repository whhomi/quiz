<script setup>
import { computed } from 'vue'
import { useRoute, RouterView } from 'vue-router'

const route = useRoute()

const showNav = computed(() => !['Quiz', 'Result'].includes(route.name))
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex flex-col">
    <!-- PC 顶部导航栏 -->
    <nav
      v-if="showNav"
      class="hidden md:flex sticky top-0 z-50 bg-white border-b border-gray-200 shadow-sm"
    >
      <div class="max-w-lg mx-auto w-full flex items-center gap-1 px-4 py-2">
        <router-link
          to="/"
          class="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition"
          active-class="text-blue-600 bg-blue-50"
          :class="$route.path === '/' ? '' : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
          </svg>
          首页
        </router-link>
        <router-link
          to="/history"
          class="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition"
          active-class="text-blue-600 bg-blue-50"
          :class="$route.path.startsWith('/history') ? '' : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          历史记录
        </router-link>
        <router-link
          to="/settings"
          class="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium transition"
          active-class="text-blue-600 bg-blue-50"
          :class="$route.path === '/settings' ? '' : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
          设置
        </router-link>
      </div>
    </nav>

    <!-- 主体内容区域 -->
    <main class="flex-1">
      <router-view />
    </main>

    <!-- 移动端底部 TabBar -->
    <nav
      v-if="showNav"
      class="md:hidden sticky bottom-0 z-50 bg-white border-t border-gray-200 shadow-lg safe-bottom"
    >
      <div class="flex items-center justify-around max-w-lg mx-auto">
        <router-link
          to="/"
          class="flex flex-col items-center gap-0.5 py-2 px-6 text-xs font-medium transition"
          active-class="text-blue-600"
          :class="$route.path === '/' ? '' : 'text-gray-400 hover:text-gray-600'"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
          </svg>
          首页
        </router-link>
        <router-link
          to="/history"
          class="flex flex-col items-center gap-0.5 py-2 px-6 text-xs font-medium transition"
          active-class="text-blue-600"
          :class="$route.path.startsWith('/history') ? '' : 'text-gray-400 hover:text-gray-600'"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          历史
        </router-link>
        <router-link
          to="/settings"
          class="flex flex-col items-center gap-0.5 py-2 px-6 text-xs font-medium transition"
          active-class="text-blue-600"
          :class="$route.path === '/settings' ? '' : 'text-gray-400 hover:text-gray-600'"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
          设置
        </router-link>
      </div>
    </nav>
  </div>
</template>

<style>
/* iOS Safari 安全区域适配 */
.safe-bottom {
  padding-bottom: env(safe-area-inset-bottom, 0px);
}
</style>
