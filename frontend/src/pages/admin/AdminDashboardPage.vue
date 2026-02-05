<template>
  <div class="admin-dashboard">
    <!-- 대시보드 헤더 -->
    <div class="dashboard-header">
      <h1 class="page-title">관리자 대시보드</h1>
      <p class="page-subtitle">시스템 현황 및 통계를 확인하세요</p>
    </div>

    <!-- 통계 카드 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon-wrapper primary">
          <Icon icon="mdi:account-group" class="stat-icon" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalUsers }}</div>
          <div class="stat-label">전체 사용자</div>
          <div class="stat-change positive">
            <Icon icon="mdi:trending-up" class="change-icon" />
            {{ stats.userGrowth }}%
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper success">
          <Icon icon="mdi:file-document" class="stat-icon" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalPosts }}</div>
          <div class="stat-label">전체 게시글</div>
          <div class="stat-change positive">
            <Icon icon="mdi:trending-up" class="change-icon" />
            {{ stats.postGrowth }}%
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper warning">
          <Icon icon="mdi:message" class="stat-icon" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalComments }}</div>
          <div class="stat-label">전체 댓글</div>
          <div class="stat-change positive">
            <Icon icon="mdi:trending-up" class="change-icon" />
            {{ stats.commentGrowth }}%
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper info">
          <Icon icon="mdi:server" class="stat-icon" />
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.systemLoad }}%</div>
          <div class="stat-label">시스템 부하</div>
          <div class="stat-change" :class="stats.systemLoad > 80 ? 'negative' : 'positive'">
            <Icon :icon="stats.systemLoad > 80 ? 'mdi:trending-up' : 'mdi:trending-down'" class="change-icon" />
            {{ stats.systemLoad > 80 ? '높음' : '정상' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 메인 컨텐츠 -->
    <div class="dashboard-content">
      <!-- 좌측: 차트 및 데이터 -->
      <div class="dashboard-left">
        <!-- 사용자 가입 추이 -->
        <div class="content-card">
          <div class="card-header">
            <h2 class="card-title">사용자 가입 추이</h2>
            <el-select v-model="userChartPeriod" size="small">
              <el-option label="7일" value="7d" />
              <el-option label="30일" value="30d" />
              <el-option label="90일" value="90d" />
            </el-select>
          </div>
          <div class="card-content">
            <div class="chart-container">
              <canvas ref="userChartCanvas" width="400" height="200"></canvas>
            </div>
          </div>
        </div>

        <!-- 게시글 활동 -->
        <div class="content-card">
          <div class="card-header">
            <h2 class="card-title">게시글 활동</h2>
            <el-select v-model="postChartPeriod" size="small">
              <el-option label="7일" value="7d" />
              <el-option label="30일" value="30d" />
              <el-option label="90일" value="90d" />
            </el-select>
          </div>
          <div class="card-content">
            <div class="chart-container">
              <canvas ref="postChartCanvas" width="400" height="200"></canvas>
            </div>
          </div>
        </div>

        <!-- 시스템 리소스 -->
        <div class="content-card">
          <div class="card-header">
            <h2 class="card-title">시스템 리소스</h2>
            <el-button size="small" @click="refreshSystemInfo">
              <Icon icon="mdi:refresh" class="refresh-icon" />
            </el-button>
          </div>
          <div class="card-content">
            <div class="resource-metrics">
              <div class="resource-item">
                <div class="resource-label">CPU 사용률</div>
                <div class="resource-bar">
                  <div 
                    class="resource-fill cpu"
                    :style="{ width: systemInfo.cpu + '%' }"
                  ></div>
                </div>
                <div class="resource-value">{{ systemInfo.cpu }}%</div>
              </div>

              <div class="resource-item">
                <div class="resource-label">메모리 사용률</div>
                <div class="resource-bar">
                  <div 
                    class="resource-fill memory"
                    :style="{ width: systemInfo.memory + '%' }"
                  ></div>
                </div>
                <div class="resource-value">{{ systemInfo.memory }}%</div>
              </div>

              <div class="resource-item">
                <div class="resource-label">디스크 사용률</div>
                <div class="resource-bar">
                  <div 
                    class="resource-fill disk"
                    :style="{ width: systemInfo.disk + '%' }"
                  ></div>
                </div>
                <div class="resource-value">{{ systemInfo.disk }}%</div>
              </div>

              <div class="resource-item">
                <div class="resource-label">네트워크</div>
                <div class="resource-value">
                  ↓ {{ systemInfo.networkIn }} MB/s ↑ {{ systemInfo.networkOut }} MB/s
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 우측: 알림 및 액션 -->
      <div class="dashboard-right">
        <!-- 시스템 알림 -->
        <div class="content-card">
          <div class="card-header">
            <h2 class="card-title">시스템 알림</h2>
            <el-badge :value="unreadAlerts" type="danger" />
          </div>
          <div class="card-content">
            <div v-if="alerts.length === 0" class="empty-state">
              <Icon icon="mdi:bell-check" class="empty-icon" />
              <p class="empty-text">알림이 없습니다</p>
            </div>
            <ul v-else class="alert-list">
              <li 
                v-for="alert in alerts" 
                :key="alert.id"
                :class="['alert-item', alert.type]"
                @click="markAlertAsRead(alert.id)"
              >
                <div class="alert-icon">
                  <Icon :icon="getAlertIcon(alert.type)" class="alert-type-icon" />
                </div>
                <div class="alert-content">
                  <div class="alert-title">{{ alert.title }}</div>
                  <div class="alert-message">{{ alert.message }}</div>
                  <div class="alert-time">{{ formatTime(alert.createdAt) }}</div>
                </div>
                <div v-if="!alert.read" class="alert-indicator"></div>
              </li>
            </ul>
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
                @click="createAnnouncement"
              >
                <Icon icon="mdi:bullhorn" class="action-icon" />
                공지사항 작성
              </el-button>
              <el-button 
                type="success" 
                class="action-btn"
                @click="backupSystem"
              >
                <Icon icon="mdi:database-backup" class="action-icon" />
                시스템 백업
              </el-button>
              <el-button 
                type="warning" 
                class="action-btn"
                @click="clearCache"
              >
                <Icon icon="mdi:cached" class="action-icon" />
                캐시 정리
              </el-button>
              <el-button 
                type="info" 
                class="action-btn"
                @click="exportLogs"
              >
                <Icon icon="mdi:file-export" class="action-icon" />
                로그 내보내기
              </el-button>
            </div>
          </div>
        </div>

        <!-- 최근 활동 -->
        <div class="content-card">
          <div class="card-header">
            <h2 class="card-title">최근 활동</h2>
            <el-button size="small" @click="refreshActivity">
              <Icon icon="mdi:refresh" class="refresh-icon" />
            </el-button>
          </div>
          <div class="card-content">
            <div v-if="recentActivities.length === 0" class="empty-state">
              <Icon icon="mdi:timeline" class="empty-icon" />
              <p class="empty-text">활동 내역이 없습니다</p>
            </div>
            <ul v-else class="activity-list">
              <li 
                v-for="activity in recentActivities" 
                :key="activity.id"
                class="activity-item"
              >
                <div class="activity-icon" :class="activity.type">
                  <Icon :icon="getActivityIcon(activity.type)" class="activity-type-icon" />
                </div>
                <div class="activity-content">
                  <div class="activity-text">{{ activity.description }}</div>
                  <div class="activity-time">{{ formatTime(activity.createdAt) }}</div>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'
import { Chart, registerables } from 'chart.js'

// =============================================================================
// 🎯 컴포넌트 상태
// =============================================================================
const userChartCanvas = ref<HTMLCanvasElement>()
const postChartCanvas = ref<HTMLCanvasElement>()
const userChartPeriod = ref('7d')
const postChartPeriod = ref('7d')

// 통계 데이터
const stats = ref({
  totalUsers: 1234,
  userGrowth: 12.5,
  totalPosts: 5678,
  postGrowth: 8.3,
  totalComments: 12345,
  commentGrowth: 15.7,
  systemLoad: 45
})

// 시스템 정보
const systemInfo = ref({
  cpu: 45,
  memory: 62,
  disk: 78,
  networkIn: 15.2,
  networkOut: 8.7
})

// 알림 데이터
const alerts = ref([
  {
    id: 1,
    type: 'error',
    title: '서비스 다운',
    message: '인증 서비스가 응답하지 않습니다.',
    createdAt: new Date('2024-01-15T14:30:00'),
    read: false
  },
  {
    id: 2,
    type: 'warning',
    title: '디스크 공간 부족',
    message: '디스크 사용률이 85%에 도달했습니다.',
    createdAt: new Date('2024-01-15T13:15:00'),
    read: false
  },
  {
    id: 3,
    type: 'info',
    title: '업데이트 완료',
    message: '시스템 업데이트가 성공적으로 완료되었습니다.',
    createdAt: new Date('2024-01-15T10:00:00'),
    read: true
  }
])

// 최근 활동
const recentActivities = ref([
  {
    id: 1,
    type: 'user',
    description: '새로운 사용자가 가입했습니다: newuser123',
    createdAt: new Date('2024-01-15T15:30:00')
  },
  {
    id: 2,
    type: 'post',
    description: '게시글이 신고되었습니다: "스팸 광고"',
    createdAt: new Date('2024-01-15T15:00:00')
  },
  {
    id: 3,
    type: 'system',
    description: '백업 작업이 완료되었습니다.',
    createdAt: new Date('2024-01-15T14:00:00')
  }
])

// =============================================================================
// 🎯 컴퓨티드 프로퍼티
// =============================================================================
const unreadAlerts = computed(() => {
  return alerts.value.filter(alert => !alert.read).length
})

// =============================================================================
// 🔧 유틸리티 함수
// =============================================================================
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

const getAlertIcon = (type: string): string => {
  const icons = {
    error: 'mdi:alert-circle',
    warning: 'mdi:alert',
    info: 'mdi:information',
    success: 'mdi:check-circle'
  }
  return icons[type as keyof typeof icons] || 'mdi:information'
}

const getActivityIcon = (type: string): string => {
  const icons = {
    user: 'mdi:account-plus',
    post: 'mdi:file-document',
    system: 'mdi:cog',
    security: 'mdi:shield-alert'
  }
  return icons[type as keyof typeof icons] || 'mdi:information'
}

// =============================================================================
// 🔄 액션 함수
// =============================================================================
const markAlertAsRead = (alertId: number): void => {
  const alert = alerts.value.find(a => a.id === alertId)
  if (alert && !alert.read) {
    alert.read = true
  }
}

const refreshSystemInfo = (): void => {
  // TODO: 실제 시스템 정보 API 호출
  ElMessage.success('시스템 정보를 새로고침했습니다.')
}

const refreshActivity = (): void => {
  // TODO: 실제 활동 API 호출
  ElMessage.success('활동 내역을 새로고침했습니다.')
}

const createAnnouncement = (): void => {
  ElMessage.info('공지사항 작성 기능은 준비 중입니다.')
}

const backupSystem = async (): Promise<void> => {
  try {
    ElMessage.info('시스템 백업을 시작합니다...')
    // TODO: 실제 백업 API 호출
    setTimeout(() => {
      ElMessage.success('시스템 백업이 완료되었습니다.')
    }, 3000)
  } catch (error) {
    ElMessage.error('백업에 실패했습니다.')
  }
}

const clearCache = (): void => {
  ElMessage.info('캐시 정리 기능은 준비 중입니다.')
}

const exportLogs = (): void => {
  ElMessage.info('로그 내보내기 기능은 준비 중입니다.')
}

// =============================================================================
// 📊 차트 초기화
// =============================================================================
const initCharts = (): void => {
  // 사용자 차트
  if (userChartCanvas.value) {
    const ctx = userChartCanvas.value.getContext('2d')
    if (ctx) {
      Chart.register(...registerables)

      new Chart(ctx, {
        type: 'line',
        data: {
          labels: ['1월 9일', '1월 10일', '1월 11일', '1월 12일', '1월 13일', '1월 14일', '1월 15일'],
          datasets: [{
            label: '가입자',
            data: [12, 19, 15, 25, 22, 30, 28],
            borderColor: 'rgb(59, 130, 246)',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            tension: 0.4
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            y: { beginAtZero: true }
          }
        }
      })
    }
  }

  // 게시글 차트
  if (postChartCanvas.value) {
    const ctx = postChartCanvas.value.getContext('2d')
    if (ctx) {
      new Chart(ctx, {
        type: 'bar',
        data: {
          labels: ['1월 9일', '1월 10일', '1월 11일', '1월 12일', '1월 13일', '1월 14일', '1월 15일'],
          datasets: [{
            label: '게시글',
            data: [45, 52, 48, 62, 58, 75, 68],
            backgroundColor: 'rgba(16, 185, 129, 0.8)'
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            y: { beginAtZero: true }
          }
        }
      })
    }
  }
}

// =============================================================================
// 🎯 라이프사이클
// =============================================================================
onMounted(async () => {
  await nextTick()
  initCharts()
})
</script>

<style scoped>
/* =============================================================================
// 🎨 관리자 대시보드 스타일
// ============================================================================= */
.admin-dashboard {
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

.change-icon {
  @apply w-3 h-3;
}

/* =============================================================================
// 📋 메인 컨텐츠
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

.card-content {
  @apply p-6;
}

/* =============================================================================
// 📊 차트 컨테이너
// ============================================================================= */
.chart-container {
  @apply h-48;
}

/* =============================================================================
// 🖥️ 시스템 리소스
// ============================================================================= */
.resource-metrics {
  @apply space-y-4;
}

.resource-item {
  @apply space-y-2;
}

.resource-label {
  @apply text-sm font-medium text-gray-700 dark:text-gray-300;
}

.resource-bar {
  @apply w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden;
}

.resource-fill {
  @apply h-full transition-all duration-300 ease-in-out;
}

.resource-fill.cpu {
  @apply bg-blue-500;
}

.resource-fill.memory {
  @apply bg-green-500;
}

.resource-fill.disk {
  @apply bg-yellow-500;
}

.resource-value {
  @apply text-sm text-gray-600 dark:text-gray-400;
}

/* =============================================================================
// 🚨 알림 목록
// ============================================================================= */
.alert-list {
  @apply space-y-3;
}

.alert-item {
  @apply flex items-start gap-3 p-3 rounded-lg cursor-pointer transition-colors duration-200;
}

.alert-item.error {
  @apply bg-red-50 dark:bg-red-900/10 border border-red-200 dark:border-red-800;
}

.alert-item.warning {
  @apply bg-yellow-50 dark:bg-yellow-900/10 border border-yellow-200 dark:border-yellow-800;
}

.alert-item.info {
  @apply bg-blue-50 dark:bg-blue-900/10 border border-blue-200 dark:border-blue-800;
}

.alert-item.unread {
  @apply border-l-4 border-primary-500;
}

.alert-icon {
  @apply w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0;
}

.alert-icon.error {
  @apply bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400;
}

.alert-icon.warning {
  @apply bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400;
}

.alert-icon.info {
  @apply bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400;
}

.alert-type-icon {
  @apply w-4 h-4;
}

.alert-content {
  @apply flex-1 min-w-0;
}

.alert-title {
  @apply font-medium text-gray-900 dark:text-gray-100 mb-1;
}

.alert-message {
  @apply text-sm text-gray-600 dark:text-gray-400 mb-1;
}

.alert-time {
  @apply text-xs text-gray-500 dark:text-gray-400;
}

.alert-indicator {
  @apply w-2 h-2 bg-primary-500 rounded-full flex-shrink-0 mt-2;
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
  @apply w-4 h-4 mr-2;
}

/* =============================================================================
// 📈 활동 목록
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

.activity-icon.user {
  @apply bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400;
}

.activity-icon.post {
  @apply bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400;
}

.activity-icon.system {
  @apply bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400;
}

.activity-icon.security {
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

.activity-time {
  @apply text-xs text-gray-500 dark:text-gray-400;
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