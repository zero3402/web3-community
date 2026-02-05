<template>
  <div class="notification-dropdown">
    <div class="notification-header">
      <h3 class="notification-title">알림</h3>
      <el-button 
        size="small" 
        text 
        @click="markAllAsRead"
        :disabled="!hasUnreadNotifications"
      >
        모두 읽음
      </el-button>
    </div>

    <div class="notification-list">
      <!-- 로딩 상태 -->
      <div v-if="isLoading" class="notification-loading">
        <el-skeleton :rows="3" animated />
      </div>

      <!-- 알림 목록 -->
      <div v-else-if="notifications.length > 0" class="notifications">
        <div 
          v-for="notification in notifications" 
          :key="notification.id"
          :class="['notification-item', { unread: !notification.read }]"
          @click="handleNotificationClick(notification)"
        >
          <div class="notification-icon" :class="notification.type">
            <Icon :icon="getNotificationIcon(notification.type)" class="notification-type-icon" />
          </div>
          
          <div class="notification-content">
            <div class="notification-message">
              {{ notification.message }}
            </div>
            <div class="notification-time">
              {{ formatTime(notification.createdAt) }}
            </div>
          </div>
          
          <div class="notification-actions">
            <el-button 
              size="small" 
              text 
              @click.stop="markAsRead(notification.id)"
              v-if="!notification.read"
            >
              읽음
            </el-button>
            <el-button 
              size="small" 
              text 
              @click.stop="removeNotification(notification.id)"
            >
              <Icon icon="mdi:close" class="remove-icon" />
            </el-button>
          </div>
        </div>
      </div>

      <!-- 빈 상태 -->
      <div v-else class="empty-notifications">
        <Icon icon="mdi:bell-off" class="empty-icon" />
        <p class="empty-text">알림이 없습니다</p>
      </div>
    </div>

    <div class="notification-footer">
      <router-link to="/notifications" class="view-all-link">
        모든 알림 보기
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'

// =============================================================================
// 🎯 컴포넌트 상태
// =============================================================================
const router = useRouter()

const isLoading = ref(false)
const notifications = ref([
  {
    id: 1,
    type: 'comment',
    message: '김개발님이 "Web3 기술의 현재와 미래"에 댓글을 달았습니다',
    read: false,
    createdAt: new Date('2024-01-15T10:30:00'),
    url: '/community/posts/1#comment-5'
  },
  {
    id: 2,
    type: 'like',
    message: '이블록님이 "스마트 컨트랙트 개발 가이드"를 좋아합니다',
    read: false,
    createdAt: new Date('2024-01-15T09:15:00'),
    url: '/community/posts/2'
  },
  {
    id: 3,
    type: 'follow',
    message: '박디파이님이 당신을 팔로우합니다',
    read: true,
    createdAt: new Date('2024-01-14T16:45:00'),
    url: '/profile/parkdev'
  },
  {
    id: 4,
    type: 'mention',
    message: '최고관리자님이 당신을 언급했습니다: "@web3dev 님 확인 부탁드립니다"',
    read: true,
    createdAt: new Date('2024-01-14T14:20:00'),
    url: '/community/posts/3#comment-12'
  },
  {
    id: 5,
    type: 'system',
    message: '새로운 기능이 추가되었습니다: 실시간 채팅',
    read: true,
    createdAt: new Date('2024-01-13T10:00:00'),
    url: '/help'
  }
])

// =============================================================================
// 🎯 컴퓨티드 프로퍼티
// =============================================================================
const hasUnreadNotifications = computed(() => {
  return notifications.value.some(n => !n.read)
})

const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.read).length
})

// =============================================================================
// 🔧 유틸리티 함수
// =============================================================================
const getNotificationIcon = (type: string): string => {
  const icons = {
    comment: 'mdi:comment',
    like: 'mdi:heart',
    follow: 'mdi:account-plus',
    mention: 'mdi:at',
    system: 'mdi:information',
    message: 'mdi:message',
    post: 'mdi:file-document'
  }
  return icons[type as keyof typeof icons] || 'mdi:bell'
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
  if (days < 7) return `${days}일 전`
  return date.toLocaleDateString('ko-KR')
}

// =============================================================================
// 🔄 알림 관리 함수
// =============================================================================
const handleNotificationClick = (notification: any): void => {
  // 알림을 읽음으로 표시
  markAsRead(notification.id)
  
  // 해당 페이지로 이동
  if (notification.url) {
    router.push(notification.url)
  }
}

const markAsRead = async (notificationId: number): Promise<void> => {
  try {
    const notification = notifications.value.find(n => n.id === notificationId)
    if (notification && !notification.read) {
      notification.read = true
      
      // TODO: API 호출
      console.log('Mark notification as read:', notificationId)
    }
  } catch (error) {
    console.error('Failed to mark notification as read:', error)
  }
}

const markAllAsRead = async (): Promise<void> => {
  try {
    notifications.value.forEach(n => n.read = true)
    
    // TODO: API 호출
    console.log('Mark all notifications as read')
    
    ElMessage.success('모든 알림을 읽음으로 표시했습니다.')
  } catch (error) {
    console.error('Failed to mark all notifications as read:', error)
    ElMessage.error('알림을 읽음으로 표시하는 데 실패했습니다.')
  }
}

const removeNotification = async (notificationId: number): Promise<void> => {
  try {
    const index = notifications.value.findIndex(n => n.id === notificationId)
    if (index !== -1) {
      notifications.value.splice(index, 1)
      
      // TODO: API 호출
      console.log('Remove notification:', notificationId)
    }
  } catch (error) {
    console.error('Failed to remove notification:', error)
    ElMessage.error('알림을 삭제하는 데 실패했습니다.')
  }
}
</script>

<style scoped>
/* =============================================================================
// 🎨 알림 드롭다운 스타일
// ============================================================================= */
.notification-dropdown {
  @apply w-80 bg-white dark:bg-gray-800 rounded-lg shadow-xl border border-gray-200 dark:border-gray-700 overflow-hidden;
}

/* =============================================================================
// 📋 헤더
// ============================================================================= */
.notification-header {
  @apply flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700;
}

.notification-title {
  @apply text-sm font-semibold text-gray-900 dark:text-gray-100;
}

/* =============================================================================
// 📋 알림 목록
// ============================================================================= */
.notification-list {
  @apply max-h-96 overflow-y-auto;
}

.notification-loading {
  @apply p-4;
}

.notifications {
  @apply divide-y divide-gray-100 dark:divide-gray-700;
}

.notification-item {
  @apply flex items-start gap-3 p-4 hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer transition-colors duration-200;
}

.notification-item.unread {
  @apply bg-blue-50 dark:bg-blue-900/10 border-l-4 border-primary-500;
}

/* =============================================================================
// 🎭 알림 아이콘
// ============================================================================= */
.notification-icon {
  @apply w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0;
}

.notification-icon.comment {
  @apply bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400;
}

.notification-icon.like {
  @apply bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400;
}

.notification-icon.follow {
  @apply bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400;
}

.notification-icon.mention {
  @apply bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400;
}

.notification-icon.system {
  @apply bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400;
}

.notification-icon.message {
  @apply bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400;
}

.notification-icon.post {
  @apply bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400;
}

.notification-type-icon {
  @apply w-4 h-4;
}

/* =============================================================================
// 📝 알림 콘텐츠
// ============================================================================= */
.notification-content {
  @apply flex-1 min-w-0;
}

.notification-message {
  @apply text-sm text-gray-900 dark:text-gray-100 mb-1 line-clamp-2;
}

.notification-time {
  @apply text-xs text-gray-500 dark:text-gray-400;
}

/* =============================================================================
// ⚙️ 알림 액션
// ============================================================================= */
.notification-actions {
  @apply flex items-center gap-1 opacity-0 transition-opacity duration-200;
}

.notification-item:hover .notification-actions {
  @apply opacity-100;
}

.remove-icon {
  @apply w-3 h-3;
}

/* =============================================================================
// 📭 빈 상태
// ============================================================================= */
.empty-notifications {
  @apply text-center py-8;
}

.empty-icon {
  @apply w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-3;
}

.empty-text {
  @apply text-gray-500 dark:text-gray-400 text-sm;
}

/* =============================================================================
// 📋 푸터
// ============================================================================= */
.notification-footer {
  @apply p-3 border-t border-gray-200 dark:border-gray-700 text-center;
}

.view-all-link {
  @apply text-sm text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 transition-colors duration-200;
}
</style>