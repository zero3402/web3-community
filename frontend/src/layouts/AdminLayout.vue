<template>
  <div class="admin-layout">
    <!-- 관리자 헤더 -->
    <AdminHeader />
    
    <!-- 관리자 메인 컨텐츠 -->
    <div class="admin-content">
      <!-- 사이드바 네비게이션 -->
      <AdminSidebar />
      
      <!-- 페이지 컨텐츠 -->
      <main class="admin-main">
        <router-view v-slot="{ Component, route }">
          <transition :name="route.meta.transition || 'fade'" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import AdminHeader from '@/components/admin/AdminHeader.vue'
import AdminSidebar from '@/components/admin/AdminSidebar.vue'

onMounted(() => {
  document.body.classList.add('admin-layout')
})

onUnmounted(() => {
  document.body.classList.remove('admin-layout')
})
</script>

<style scoped>
/* =============================================================================
// 🎨 관리자 레이아웃 스타일
// ============================================================================= */
.admin-layout {
  @apply min-h-screen bg-gray-50 dark:bg-gray-900;
}

.admin-content {
  @apply flex pt-16; /* 헤더 높이만큼 패딩 */
}

.admin-main {
  @apply flex-1 ml-64 p-6 transition-all duration-300;
}

/* 모바일에서 사이드어 축소 시 */
@media (max-width: 1024px) {
  .admin-main {
    @apply ml-0 p-4;
  }
}
</style>