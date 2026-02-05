<template>
  <div class="default-layout">
    <!-- 네비게이션 헤더 -->
    <AppHeader />
    
    <!-- 메인 컨텐츠 -->
    <main class="main-content">
      <router-view v-slot="{ Component, route }">
        <transition :name="route.meta.transition || 'fade'" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>
    
    <!-- 푸터 -->
    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'

onMounted(() => {
  document.body.classList.add('default-layout')
})

onUnmounted(() => {
  document.body.classList.remove('default-layout')
})
</script>

<style scoped>
/* =============================================================================
// 🎨 기본 레이아웃 스타일
// ============================================================================= */
.default-layout {
  @apply min-h-screen flex flex-col;
}

.main-content {
  @apply flex-1 container-responsive py-6;
}
</style>