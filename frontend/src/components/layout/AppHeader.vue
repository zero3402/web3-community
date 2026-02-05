<template>
  <header class="app-header">
    <div class="header-container">
      <!-- 로고 및 브랜드 -->
      <div class="header-brand">
        <router-link to="/dashboard" class="brand-link">
          <img src="/logo.svg" alt="Web3 Community" class="brand-logo" />
          <span class="brand-name">Web3 Community</span>
        </router-link>
      </div>

      <!-- 네비게이션 메뉴 -->
      <nav class="header-nav">
        <ul class="nav-list">
          <li class="nav-item">
            <router-link 
              to="/community/posts" 
              class="nav-link"
              :class="{ active: isActiveRoute('/community') }"
            >
              <Icon icon="mdi:forum" class="nav-icon" />
              <span class="nav-text">커뮤니티</span>
            </router-link>
          </li>
          <li class="nav-item">
            <router-link 
              to="/community/members" 
              class="nav-link"
              :class="{ active: isActiveRoute('/community/members') }"
            >
              <Icon icon="mdi:account-group" class="nav-icon" />
              <span class="nav-text">멤버</span>
            </router-link>
          </li>
          <li class="nav-item">
            <router-link 
              to="/messages" 
              class="nav-link"
              :class="{ active: isActiveRoute('/messages') }"
            >
              <Icon icon="mdi:message" class="nav-icon" />
              <span class="nav-text">메시지</span>
              <el-badge 
                v-if="unreadMessagesCount > 0" 
                :value="unreadMessagesCount" 
                class="nav-badge"
              />
            </router-link>
          </li>
        </ul>
      </nav>

      <!-- 검색 -->
      <div class="header-search">
        <el-input
          v-model="searchQuery"
          placeholder="검색..."
          :prefix-icon="Search"
          class="search-input"
          @keyup.enter="handleSearch"
          @focus="showSearchDropdown = true"
          @blur="hideSearchDropdown"
        />
        
        <!-- 검색 드롭다운 -->
        <transition name="dropdown">
          <div 
            v-if="showSearchDropdown && searchQuery" 
            class="search-dropdown"
          >
            <div class="search-header">
              <span class="search-title">검색 결과</span>
              <span class="search-count">({{ searchResults.length }})</span>
            </div>
            <div v-if="searchResults.length === 0" class="search-empty">
              <Icon icon="mdi:magnify" class="empty-icon" />
              <span class="empty-text">검색 결과가 없습니다</span>
            </div>
            <ul v-else class="search-results">
              <li 
                v-for="result in searchResults" 
                :key="result.id"
                class="search-result-item"
                @click="handleSearchResultClick(result)"
              >
                <Icon :icon="getSearchResultIcon(result.type)" class="result-icon" />
                <div class="result-content">
                  <span class="result-title">{{ result.title }}</span>
                  <span class="result-type">{{ result.type }}</span>
                </div>
              </li>
            </ul>
          </div>
        </transition>
      </div>

      <!-- 사용자 메뉴 -->
      <div class="header-user">
        <!-- 알림 -->
        <el-popover placement="bottom-end" :width="320" trigger="click">
          <template #reference>
            <el-button class="notification-btn" :icon="Bell" circle />
          </template>
          <NotificationDropdown />
        </el-popover>

        <!-- 테마 토글 -->
        <el-button 
          class="theme-btn" 
          :icon="isDarkMode ? Sunny : Moon" 
          circle 
          @click="toggleTheme"
        />

        <!-- 사용자 프로필 -->
        <el-dropdown trigger="click" @command="handleUserMenuCommand">
          <div class="user-profile">
            <el-avatar 
              :src="authStore.userAvatar" 
              :size="40"
              class="user-avatar"
            >
              <Icon icon="mdi:account" />
            </el-avatar>
            <Icon icon="mdi:chevron-down" class="dropdown-icon" />
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <Icon icon="mdi:account-circle" class="menu-icon" />
                내 프로필
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <Icon icon="mdi:cog" class="menu-icon" />
                설정
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <Icon icon="mdi:logout" class="menu-icon" />
                로그아웃
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <!-- 모바일 메뉴 버튼 -->
      <el-button 
        class="mobile-menu-btn"
        :icon="Menu" 
        circle 
        @click="toggleMobileMenu"
      />
    </div>

    <!-- 모바일 메뉴 -->
    <transition name="slide">
      <div v-if="isMobileMenuOpen" class="mobile-menu-overlay" @click="closeMobileMenu">
        <div class="mobile-menu" @click.stop>
          <div class="mobile-menu-header">
            <span class="mobile-menu-title">메뉴</span>
            <el-button 
              :icon="Close" 
              circle 
              @click="closeMobileMenu"
            />
          </div>
          <nav class="mobile-nav">
            <router-link 
              v-for="item in mobileMenuItems" 
              :key="item.path"
              :to="item.path"
              class="mobile-nav-item"
              @click="closeMobileMenu"
            >
              <Icon :icon="item.icon" class="mobile-nav-icon" />
              {{ item.title }}
            </router-link>
          </nav>
        </div>
      </div>
    </transition>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Search, Bell, Moon, Sunny, Menu, Close 
} from '@element-plus/icons-vue'
import { Icon } from '@iconify/vue'
import { useAuthStore } from '@/stores/auth'
import NotificationDropdown from '@/components/notification/NotificationDropdown.vue'

// =============================================================================
// 🎯 컴포넌트 상태
// =============================================================================
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 검색 관련
const searchQuery = ref('')
const searchResults = ref([])
const showSearchDropdown = ref(false)

// UI 상태
const isMobileMenuOpen = ref(false)
const isDarkMode = ref(false)
const unreadMessagesCount = ref(3)

// =============================================================================
// 🎯 컴퓨티드 프로퍼티
// =============================================================================
const mobileMenuItems = computed(() => [
  { path: '/dashboard', title: '대시보드', icon: 'mdi:view-dashboard' },
  { path: '/community/posts', title: '커뮤니티', icon: 'mdi:forum' },
  { path: '/community/members', title: '멤버', icon: 'mdi:account-group' },
  { path: '/messages', title: '메시지', icon: 'mdi:message' },
  { path: '/notifications', title: '알림', icon: 'mdi:bell' },
  { path: '/profile', title: '프로필', icon: 'mdi:account-circle' },
])

// =============================================================================
// 🔍 검색 기능
// =============================================================================
const handleSearch = (): void => {
  if (!searchQuery.value.trim()) return
  
  router.push({
    path: '/community/search',
    query: { q: searchQuery.value.trim() }
  })
  
  showSearchDropdown.value = false
  searchQuery.value = ''
}

const hideSearchDropdown = (): void => {
  setTimeout(() => {
    showSearchDropdown.value = false
  }, 200)
}

const handleSearchResultClick = (result: any): void => {
  router.push(result.url)
  showSearchDropdown.value = false
  searchQuery.value = ''
}

const getSearchResultIcon = (type: string): string => {
  const icons = {
    post: 'mdi:file-document-outline',
    user: 'mdi:account',
    tag: 'mdi:tag',
    category: 'mdi:folder-outline'
  }
  return icons[type as keyof typeof icons] || 'mdi:magnify'
}

// =============================================================================
// 🎨 테마 관리
// =============================================================================
const toggleTheme = (): void => {
  isDarkMode.value = !isDarkMode.value
  
  const html = document.documentElement
  if (isDarkMode.value) {
    html.classList.add('dark')
    localStorage.setItem('theme', 'dark')
  } else {
    html.classList.remove('dark')
    localStorage.setItem('theme', 'light')
  }
}

// =============================================================================
// 📱 모바일 메뉴
// =============================================================================
const toggleMobileMenu = (): void => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}

const closeMobileMenu = (): void => {
  isMobileMenuOpen.value = false
}

// =============================================================================
// 👤 사용자 메뉴
// =============================================================================
const handleUserMenuCommand = (command: string): void => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/profile/settings')
      break
    case 'logout':
      authStore.logout()
      break
  }
}

// =============================================================================
// 🔍 라우트 활성화 확인
// =============================================================================
const isActiveRoute = (path: string): boolean => {
  return route.path.startsWith(path)
}

// =============================================================================
// 🎯 라이프사이클
// =============================================================================
onMounted(() => {
  // 테마 초기화
  const savedTheme = localStorage.getItem('theme')
  isDarkMode.value = savedTheme === 'dark' || 
    (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)
  
  if (isDarkMode.value) {
    document.documentElement.classList.add('dark')
  }
  
  // 이스케이프 키로 모바일 메뉴 닫기
  const handleEscape = (e: KeyboardEvent) => {
    if (e.key === 'Escape' && isMobileMenuOpen.value) {
      closeMobileMenu()
    }
  }
  
  document.addEventListener('keydown', handleEscape)
  
  onUnmounted(() => {
    document.removeEventListener('keydown', handleEscape)
  })
})

// =============================================================================
// 🔍 검색 디바운싱
// =============================================================================
let searchTimeout: NodeJS.Timeout

watch(searchQuery, (newValue) => {
  clearTimeout(searchTimeout)
  
  if (newValue.trim()) {
    searchTimeout = setTimeout(() => {
      // TODO: 실제 검색 API 호출
      searchResults.value = [
        { id: 1, title: 'Web3 커뮤니티 시작하기', type: 'post', url: '/community/posts/1' },
        { id: 2, title: '사용자 가이드', type: 'post', url: '/community/posts/2' },
        { id: 3, title: 'John Doe', type: 'user', url: '/profile/john' },
      ]
    }, 300)
  } else {
    searchResults.value = []
  }
})
</script>

<style scoped>
/* =============================================================================
// 🎨 헤더 스타일
// ============================================================================= */
.app-header {
  @apply bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-40;
  backdrop-filter: blur(8px);
  background-color: rgba(255, 255, 255, 0.95);
}

.dark .app-header {
  background-color: rgba(17, 24, 39, 0.95);
}

.header-container {
  @apply container-responsive flex items-center justify-between h-16 px-4;
}

/* =============================================================================
// 🎯 브랜드
// ============================================================================= */
.header-brand {
  @apply flex-shrink-0;
}

.brand-link {
  @apply flex items-center gap-2 hover:opacity-80 transition-opacity duration-200;
}

.brand-logo {
  @apply w-8 h-8;
}

.brand-name {
  @apply text-lg font-bold text-gray-900 dark:text-gray-100 hidden sm:block;
}

/* =============================================================================
// 🧭 네비게이션
// ============================================================================= */
.header-nav {
  @apply hidden lg:block;
}

.nav-list {
  @apply flex items-center gap-6;
}

.nav-link {
  @apply flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-800 transition-all duration-200;
}

.nav-link.active {
  @apply text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/20;
}

.nav-icon {
  @apply w-4 h-4;
}

.nav-text {
  @apply hidden md:block;
}

.nav-badge {
  @apply ml-2;
}

/* =============================================================================
// 🔍 검색
// ============================================================================= */
.header-search {
  @apply relative hidden md:block;
}

.search-input {
  @apply w-64 lg:w-80;
}

.search-input :deep(.el-input__wrapper) {
  @apply rounded-full;
}

.search-dropdown {
  @apply absolute top-full left-0 right-0 mt-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg overflow-hidden z-50;
}

.search-header {
  @apply flex items-center justify-between px-4 py-2 bg-gray-50 dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700;
}

.search-title {
  @apply text-sm font-medium text-gray-900 dark:text-gray-100;
}

.search-count {
  @apply text-sm text-gray-500 dark:text-gray-400;
}

.search-empty {
  @apply flex flex-col items-center justify-center py-8 text-gray-500 dark:text-gray-400;
}

.empty-icon {
  @apply w-8 h-8 mb-2;
}

.empty-text {
  @apply text-sm;
}

.search-results {
  @apply max-h-64 overflow-y-auto;
}

.search-result-item {
  @apply flex items-center gap-3 px-4 py-3 hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer transition-colors duration-200;
}

.result-icon {
  @apply w-4 h-4 text-gray-400;
}

.result-content {
  @apply flex-1 flex items-center justify-between;
}

.result-title {
  @apply text-sm text-gray-900 dark:text-gray-100;
}

.result-type {
  @apply text-xs text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded;
}

/* =============================================================================
// 👤 사용자 메뉴
// ============================================================================= */
.header-user {
  @apply flex items-center gap-2;
}

.notification-btn,
.theme-btn {
  @apply p-2 text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-800;
}

.user-profile {
  @apply flex items-center gap-2 p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors duration-200;
}

.user-avatar {
  @apply border-2 border-transparent hover:border-primary-500 transition-colors duration-200;
}

.dropdown-icon {
  @apply w-4 h-4 text-gray-400;
}

.menu-icon {
  @apply w-4 h-4 mr-2;
}

/* =============================================================================
// 📱 모바일 메뉴
// ============================================================================= */
.mobile-menu-btn {
  @apply lg:hidden p-2 text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100;
}

.mobile-menu-overlay {
  @apply fixed inset-0 bg-black bg-opacity-50 z-50 lg:hidden;
}

.mobile-menu {
  @apply absolute top-0 left-0 w-80 h-full bg-white dark:bg-gray-900 shadow-xl;
}

.mobile-menu-header {
  @apply flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700;
}

.mobile-menu-title {
  @apply text-lg font-semibold text-gray-900 dark:text-gray-100;
}

.mobile-nav {
  @apply p-4 space-y-2;
}

.mobile-nav-item {
  @apply flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-800 transition-all duration-200;
}

.mobile-nav-icon {
  @apply w-5 h-5;
}

/* =============================================================================
// 🎭 트랜지션
// ============================================================================= */
.dropdown-enter-active,
.dropdown-leave-active {
  @apply transition-all duration-200 ease-in-out;
}

.dropdown-enter-from {
  @apply opacity-0 transform -translate-y-2;
}

.dropdown-leave-to {
  @apply opacity-0 transform -translate-y-2;
}

.slide-enter-active,
.slide-leave-active {
  @apply transition-all duration-300 ease-in-out;
}

.slide-enter-from {
  @apply transform -translate-x-full;
}

.slide-leave-to {
  @apply transform -translate-x-full;
}

/* =============================================================================
// 📱 반응형 디자인
// ============================================================================= */
@media (max-width: 1024px) {
  .header-search {
    @apply hidden;
  }
}

@media (max-width: 640px) {
  .header-container {
    @apply px-2;
  }
  
  .brand-name {
    @apply hidden;
  }
}
</style>