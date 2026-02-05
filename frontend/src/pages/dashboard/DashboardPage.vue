<template>
  <div class="dashboard-page">
    <!-- 헤더 -->
    <div class="dashboard-header">
      <h1 class="page-title">대시보드</h1>
      <p class="page-subtitle">Web3 Community 활동 현황을 확인하세요</p>
    </div>

    <!-- 통계 카드 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon-wrapper primary">
          <Icon icon="mdi:forum" class="stat-icon" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalPosts }}</div>
          <div class="stat-label">전체 게시글</div>
          <div class="stat-change positive">
            <Icon icon="mdi:trending-up" class="change-icon" />
            {{ stats.postsGrowth }}%
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper success">
          <Icon icon="mdi:account-group" class="stat-icon" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalMembers }}</div>
          <div class="stat-label">전체 멤버</div>
          <div class="stat-change positive">
            <Icon icon="mdi:trending-up" class="change-icon" />
            {{ stats.membersGrowth }}%
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper warning">
          <Icon icon="mdi:message" class="stat-icon" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalMessages }}</div>
          <div class="stat-label">메시지</div>
          <div class="stat-change positive">
            <Icon icon="mdi:trending-up" class="change-icon" />
            {{ stats.messagesGrowth }}%
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper info">
          <Icon icon="mdi:calendar" class="stat-icon" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.activeToday }}</div>
          <div class="stat-label">오늘 활동</div>
          <div class="stat-change neutral">
            <Icon icon="mdi:minus" class="change-icon" />
            0%
          </div>
        </div>
      </div>
    </div>

    <!-- 메인 콘텐츠 -->
    <div class="dashboard-content">
      <!-- 좌측: 최근 활동 -->
      <div class="dashboard-left">
        <!-- 최근 게시글 -->
        <div class="content-card">
          <div class="card-header">
            <h2 class="card-title">최근 게시글</h2>
            <router-link to="/community/posts" class="card-link">
              모두 보기
            </router-link>
          </div>
          <div class="card-content">
            <div v-if="recentPosts.length === 0" class="empty-state">
              <Icon icon="mdi:file-document-outline" class="empty-icon" />
              <p class="empty-text">게시글이 없습니다</p>
            </div>
            <ul v-else class="post-list">
              <li v-for="post in recentPosts" :key="post.id" class="post-item">
                <router-link :to="`/community/posts/${post.id}`" class="post-link">
                  <div class="post-info">
                    <h3 class="post-title">{{ post.title }}</h3>
                    <p class="post-meta">
                      {{ post.author }} · {{ formatDate(post.createdAt) }}
                    </p>
                  </div>
                  <div class="post-stats">
                    <div class="post-stat">
                      <Icon icon="mdi:eye" class="stat-icon" />
                      {{ post.views }}
                    </div>
                    <div class="post-stat">
                      <Icon icon="mdi:comment" class="stat-icon" />
                      {{ post.comments }}
                    </div>
                    <div class="post-stat">
                      <Icon icon="mdi:heart" class="stat-icon" />
                      {{ post.likes }}
                    </div>
                  </div>
                </router-link>
              </li>
            </ul>
          </div>
        </div>

        <!-- 활동 피드 -->
        <div class="content-card">
          <div class="card-header">
            <h2 class="card-title">활동 피드</h2>
            <el-button size="small" @click="refreshFeed">
              <Icon icon="mdi:refresh" class="refresh-icon" />
            </el-button>
          </div>
          <div class="card-content">
            <div v-if="activities.length === 0" class="empty-state">
              <Icon icon="mdi:timeline" class="empty-icon" />
              <p class="empty-text">활동 내역이 없습니다</p>
            </div>
            <ul v-else class="activity-list">
              <li v-for="activity in activities" :key="activity.id" class="activity-item">
                <div class="activity-icon" :class="activity.type">
                  <Icon :icon="getActivityIcon(activity.type)" class="activity-type-icon" />
                </div>
                <div class="activity-content">
                  <p class="activity-text">
                    <span class="activity-user">{{ activity.user }}</span>
                    {{ getActivityText(activity) }}
                  </p>
                  <span class="activity-time">{{ formatTime(activity.createdAt) }}</span>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 우측: 차트 및 위젯 -->
      <div class="dashboard-right">
        <!-- 활동 차트 -->
        <div class="content-card">
          <div class="card-header">
            <h2 class="card-title">활동 추이</h2>
            <el-select v-model="chartPeriod" size="small">
              <el-option label="7일" value="7d" />
              <el-option label="30일" value="30d" />
              <el-option label="90일" value="90d" />
            </el-select>
          </div>
          <div class="card-content">
            <div class="chart-container">
              <canvas ref="chartCanvas" width="400" height="200"></canvas>
            </div>
          </div>
        </div>

        <!-- 인기 태그 -->
        <div class="content-card">
          <div class="card-header">
            <h2 class="card-title">인기 태그</h2>
          </div>
          <div class="card-content">
            <div class="tag-cloud">
              <span 
                v-for="tag in popularTags" 
                :key="tag.name"
                :class="['tag', `tag-${tag.size}`]"
                @click="searchTag(tag.name)"
              >
                #{{ tag.name }}
              </span>
            </div>
          </div>
        </div>

        <!-- 퀵 액션 -->
        <div class="content-card">
          <div class="card-header">
            <h2 class="card-title">퀵 액션</h2>
          </div>
          <div class="card-content">
            <div class="quick-actions">
              <el-button 
                type="primary" 
                class="action-btn"
                @click="createPost"
              >
                <Icon icon="mdi:plus" class="action-icon" />
                게시글 작성
              </el-button>
              <el-button 
                type="success" 
                class="action-btn"
                @click="openMessages"
              >
                <Icon icon="mdi:message" class="action-icon" />
                메시지 보내기
              </el-button>
              <el-button 
                type="info" 
                class="action-btn"
                @click="viewProfile"
              >
                <Icon icon="mdi:account" class="action-icon" />
                프로필 보기
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { useAuthStore } from '@/stores/auth'
import { Chart, registerables } from 'chart.js'

// =============================================================================
// 🎯 컴포넌트 상태
// =============================================================================
const router = useRouter()
const authStore = useAuthStore()

// 차트 관련
const chartCanvas = ref<HTMLCanvasElement>()
const chartPeriod = ref('7d')

// =============================================================================
// 📊 데이터 상태
// =============================================================================
const stats = ref({
  totalPosts: 156,
  postsGrowth: 12.5,
  totalMembers: 892,
  membersGrowth: 8.3,
  totalMessages: 1234,
  messagesGrowth: 15.7,
  activeToday: 45
})

const recentPosts = ref([
  {
    id: 1,
    title: 'Web3 기술의 현재와 미래',
    author: '김개발',
    createdAt: new Date('2024-01-15T10:30:00'),
    views: 234,
    comments: 12,
    likes: 45
  },
  {
    id: 2,
    title: '스마트 컨트랙트 개발 가이드',
    author: '이블록',
    createdAt: new Date('2024-01-15T09:15:00'),
    views: 189,
    comments: 8,
    likes: 32
  },
  {
    id: 3,
    title: 'NFT 프로젝트 시작하기',
    author: '박디파이',
    createdAt: new Date('2024-01-14T16:45:00'),
    views: 312,
    comments: 15,
    likes: 67
  }
])

const activities = ref([
  {
    id: 1,
    user: '김개발',
    type: 'post',
    target: 'Web3 기술의 현재와 미래',
    createdAt: new Date('2024-01-15T10:30:00')
  },
  {
    id: 2,
    user: '이블록',
    type: 'comment',
    target: '스마트 컨트랙트 개발 가이드',
    createdAt: new Date('2024-01-15T09:15:00')
  },
  {
    id: 3,
    user: '박디파이',
    type: 'like',
    target: 'Web3 기술의 현재와 미래',
    createdAt: new Date('2024-01-14T16:45:00')
  }
])

const popularTags = ref([
  { name: 'Web3', size: 'large' },
  { name: '블록체인', size: 'medium' },
  { name: '스마트컨트랙트', size: 'large' },
  { name: 'NFT', size: 'small' },
  { name: 'DeFi', size: 'medium' },
  { name: '이더리움', size: 'small' },
  { name: '솔리디티', size: 'medium' },
  { name: '탈중앙화', size: 'large' }
])

// =============================================================================
// 🔧 유틸리티 함수
// =============================================================================
const formatDate = (date: Date): string => {
  return date.toLocaleDateString('ko-KR')
}

const formatTime = (date: Date): string => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '방금'
  if (minutes < 60) return `${minutes}분 전`
  if (hours < 24) return `${hours}시간 전`
  return `${days}일 전`
}

const getActivityIcon = (type: string): string => {
  const icons = {
    post: 'mdi:file-document',
    comment: 'mdi:comment',
    like: 'mdi:heart',
    follow: 'mdi:account-plus',
    join: 'mdi:account-multiple-plus'
  }
  return icons[type as keyof typeof icons] || 'mdi:information'
}

const getActivityText = (activity: any): string => {
  const texts = {
    post: `게시글 "${activity.target}"을 작성했습니다`,
    comment: `"${activity.target}"에 댓글을 달았습니다`,
    like: `"${activity.target}"을 좋아합니다`,
    follow: `${activity.target}을 팔로우합니다`,
    join: '커뮤니티에 참여했습니다'
  }
  return texts[activity.type as keyof typeof texts] || '활동했습니다'
}

// =============================================================================
// 🔄 액션 함수
// =============================================================================
const refreshFeed = (): void => {
  // TODO: 실제 데이터 새로고침
  console.log('새로고침')
}

const createPost = (): void => {
  router.push('/community/posts/create')
}

const openMessages = (): void => {
  router.push('/messages')
}

const viewProfile = (): void => {
  router.push('/profile')
}

const searchTag = (tag: string): void => {
  router.push({
    path: '/community/search',
    query: { tag }
  })
}

// =============================================================================
// 📊 차트 초기화
// =============================================================================
const initChart = (): void => {
  if (!chartCanvas.value) return

  const ctx = chartCanvas.value.getContext('2d')
  if (!ctx) return

  Chart.register(...registerables)

  new Chart(ctx, {
    type: 'line',
    data: {
      labels: ['1월 9일', '1월 10일', '1월 11일', '1월 12일', '1월 13일', '1월 14일', '1월 15일'],
      datasets: [{
        label: '게시글',
        data: [12, 19, 15, 25, 22, 30, 28],
        borderColor: 'rgb(59, 130, 246)',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        tension: 0.4
      }, {
        label: '댓글',
        data: [8, 12, 10, 18, 15, 22, 20],
        borderColor: 'rgb(16, 185, 129)',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        tension: 0.4
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom'
        }
      },
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  })
}

// =============================================================================
// 🎯 라이프사이클
// =============================================================================
onMounted(async () => {
  await nextTick()
  initChart()
})
</script>

<style scoped>
/* =============================================================================
// 🎨 대시보드 페이지 스타일
// ============================================================================= */
.dashboard-page {
  @apply space-y-6;
}

/* =============================================================================
// 📋 헤더
// ============================================================================= */
.dashboard-header {
  @apply text-center mb-8;
}

.page-title {
  @apply text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2;
}

.page-subtitle {
  @apply text-gray-600 dark:text-gray-400;
}

/* =============================================================================
// 📊 통계 카드
// ============================================================================= */
.stats-grid {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8;
}

.stat-card {
  @apply bg-white dark:bg-gray-800 rounded-xl shadow-soft p-6 border border-gray-200 dark:border-gray-700 hover:shadow-medium transition-shadow duration-200;
}

.stat-icon-wrapper {
  @apply w-12 h-12 rounded-lg flex items-center justify-center mb-4;
}

.stat-icon-wrapper.primary {
  @apply bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400;
}

.stat-icon-wrapper.success {
  @apply bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400;
}

.stat-icon-wrapper.warning {
  @apply bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400;
}

.stat-icon-wrapper.info {
  @apply bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400;
}

.stat-icon {
  @apply w-6 h-6;
}

.stat-content {
  @apply space-y-1;
}

.stat-value {
  @apply text-2xl font-bold text-gray-900 dark:text-gray-100;
}

.stat-label {
  @apply text-sm text-gray-600 dark:text-gray-400;
}

.stat-change {
  @apply flex items-center gap-1 text-xs font-medium;
}

.stat-change.positive {
  @apply text-green-600 dark:text-green-400;
}

.stat-change.negative {
  @apply text-red-600 dark:text-red-400;
}

.stat-change.neutral {
  @apply text-gray-500 dark:text-gray-400;
}

.change-icon {
  @apply w-3 h-3;
}

/* =============================================================================
// 📋 메인 콘텐츠
// ============================================================================= */
.dashboard-content {
  @apply grid grid-cols-1 lg:grid-cols-3 gap-6;
}

.dashboard-left {
  @apply lg:col-span-2 space-y-6;
}

.dashboard-right {
  @apply space-y-6;
}

/* =============================================================================
// 📋 콘텐츠 카드
// ============================================================================= */
.content-card {
  @apply bg-white dark:bg-gray-800 rounded-xl shadow-soft border border-gray-200 dark:border-gray-700;
}

.card-header {
  @apply flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700;
}

.card-title {
  @apply text-lg font-semibold text-gray-900 dark:text-gray-100;
}

.card-link {
  @apply text-sm text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300;
}

.card-content {
  @apply p-6;
}

/* =============================================================================
// 📝 게시글 목록
// ============================================================================= */
.post-list {
  @apply space-y-4;
}

.post-item {
  @apply border-b border-gray-100 dark:border-gray-700 last:border-b-0 pb-4 last:pb-0;
}

.post-link {
  @apply flex items-center justify-between hover:bg-gray-50 dark:hover:bg-gray-700 -mx-2 px-2 py-2 rounded-lg transition-colors duration-200;
}

.post-info {
  @apply flex-1;
}

.post-title {
  @apply font-medium text-gray-900 dark:text-gray-100 mb-1;
}

.post-meta {
  @apply text-sm text-gray-500 dark:text-gray-400;
}

.post-stats {
  @apply flex items-center gap-4 text-sm text-gray-500 dark:text-gray-400;
}

.post-stat {
  @apply flex items-center gap-1;
}

.post-stat .stat-icon {
  @apply w-3 h-3;
}

/* =============================================================================
// 📈 활동 피드
// ============================================================================= */
.activity-list {
  @apply space-y-4;
}

.activity-item {
  @apply flex items-start gap-3;
}

.activity-icon {
  @apply w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0;
}

.activity-icon.post {
  @apply bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400;
}

.activity-icon.comment {
  @apply bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400;
}

.activity-icon.like {
  @apply bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400;
}

.activity-type-icon {
  @apply w-4 h-4;
}

.activity-content {
  @apply flex-1;
}

.activity-text {
  @apply text-sm text-gray-900 dark:text-gray-100 mb-1;
}

.activity-user {
  @apply font-medium;
}

.activity-time {
  @apply text-xs text-gray-500 dark:text-gray-400;
}

/* =============================================================================
// 📊 차트
// ============================================================================= */
.chart-container {
  @apply h-48;
}

/* =============================================================================
// 🏷️ 태그 클라우드
// ============================================================================= */
.tag-cloud {
  @apply flex flex-wrap gap-2;
}

.tag {
  @apply px-3 py-1 rounded-full text-sm cursor-pointer transition-all duration-200 hover:scale-105;
}

.tag-small {
  @apply bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300;
}

.tag-medium {
  @apply bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400;
}

.tag-large {
  @apply bg-primary-200 dark:bg-primary-800/50 text-primary-700 dark:text-primary-300;
}

/* =============================================================================
// ⚡ 퀵 액션
// ============================================================================= */
.quick-actions {
  @apply space-y-3;
}

.action-btn {
  @apply w-full justify-start;
}

.action-icon {
  @apply w-4 h-4;
}

/* =============================================================================
// 🎭 빈 상태
// ============================================================================= */
.empty-state {
  @apply text-center py-8;
}

.empty-icon {
  @apply w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3;
}

.empty-text {
  @apply text-gray-500 dark:text-gray-400;
}

/* =============================================================================
// 📱 반응형 디자인
// ============================================================================= */
@media (max-width: 1024px) {
  .dashboard-content {
    @apply grid-cols-1;
  }
  
  .dashboard-left,
  .dashboard-right {
    @apply col-span-1;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    @apply grid-cols-1;
  }
  
  .page-title {
    @apply text-2xl;
  }
}
</style>