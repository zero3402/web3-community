<template>
  <div class="websocket-client">
    <!-- 연결 상태 표시 -->
    <div class="connection-status" :class="connectionStatus">
      <div class="status-indicator"></div>
      <span class="status-text">{{ statusText }}</span>
    </div>

    <!-- 메시지 수신/발송 -->
    <div class="message-container">
      <div class="message-list">
        <div 
          v-for="message in messages" 
          :key="message.id"
          :class="['message', message.type]"
        >
          <div class="message-header">
            <span class="message-time">{{ formatTime(message.timestamp) }}</span>
            <span class="message-type">{{ message.type }}</span>
          </div>
          <div class="message-content">
            <pre>{{ message.payload }}</pre>
          </div>
        </div>
      </div>

      <!-- 메시지 전송 폼 -->
      <div class="message-form">
        <el-input
          v-model="messageInput"
          type="textarea"
          :rows="3"
          placeholder="메시지를 입력하세요..."
          class="message-input"
        />
        <div class="form-actions">
          <el-button 
            @click="sendMessage"
            :disabled="!isConnected || !messageInput.trim()"
            type="primary"
          >
            전송
          </el-button>
          <el-button 
            @click="connect"
            :disabled="isConnected"
            type="success"
          >
            연결
          </el-button>
          <el-button 
            @click="disconnect"
            :disabled="!isConnected"
            type="danger"
          >
            연결 해제
          </el-button>
          <el-button 
            @click="clearMessages"
            type="info"
          >
            지우기
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

// =============================================================================
// 🎯 컴포넌트 상태
// =============================================================================
const ws = ref<WebSocket | null>(null)
const messages = ref<any[]>([])
const messageInput = ref('')
const reconnectAttempts = ref(0)
const maxReconnectAttempts = 5
const reconnectDelay = 3000

// =============================================================================
// 🎯 컴퓨티드 프로퍼티
// =============================================================================
const isConnected = computed(() => ws.value?.readyState === WebSocket.OPEN)
const connectionStatus = computed(() => {
  if (ws.value?.readyState === WebSocket.OPEN) return 'connected'
  if (ws.value?.readyState === WebSocket.CONNECTING) return 'connecting'
  if (ws.value?.readyState === WebSocket.CLOSING) return 'closing'
  if (ws.value?.readyState === WebSocket.CLOSED) return 'disconnected'
  return 'unknown'
})

const statusText = computed(() => {
  switch (connectionStatus.value) {
    case 'connected': return '연결됨'
    case 'connecting': return '연결 중...'
    case 'closing': return '연결 해제 중...'
    case 'disconnected': return '연결 안됨'
    default: return '알 수 없음'
  }
})

// =============================================================================
// 🔌 WebSocket 함수
// =============================================================================
const connect = (): void => {
  if (ws.value?.readyState === WebSocket.OPEN || 
      ws.value?.readyState === WebSocket.CONNECTING) {
    return
  }

  try {
    const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8085/ws'
    ws.value = new WebSocket(wsUrl)

    ws.value.onopen = handleOpen
    ws.value.onmessage = handleMessage
    ws.value.onclose = handleClose
    ws.value.onerror = handleError

  } catch (error) {
    console.error('WebSocket connection error:', error)
    ElMessage.error('WebSocket 연결에 실패했습니다.')
  }
}

const disconnect = (): void => {
  if (ws.value) {
    ws.value.close()
  }
}

const sendMessage = (): void => {
  if (!isConnected.value || !messageInput.value.trim()) {
    return
  }

  try {
    const message = {
      id: Date.now(),
      type: 'client',
      payload: messageInput.value.trim(),
      timestamp: new Date()
    }

    ws.value?.send(JSON.stringify(message))
    messages.value.push(message)
    messageInput.value = ''

  } catch (error) {
    console.error('WebSocket send error:', error)
    ElMessage.error('메시지 전송에 실패했습니다.')
  }
}

// =============================================================================
// 📡 WebSocket 이벤트 핸들러
// =============================================================================
const handleOpen = (): void => {
  console.log('WebSocket connected')
  reconnectAttempts.value = 0
  ElMessage.success('WebSocket에 연결되었습니다.')

  // 연결 성공 메시지 전송
  const welcomeMessage = {
    id: Date.now(),
    type: 'system',
    payload: 'WebSocket 연결이 성공적으로 수립되었습니다.',
    timestamp: new Date()
  }
  messages.value.push(welcomeMessage)
}

const handleMessage = (event: MessageEvent): void => {
  try {
    const data = JSON.parse(event.data)
    
    const message = {
      id: Date.now(),
      type: data.type || 'server',
      payload: data,
      timestamp: new Date()
    }

    messages.value.push(message)

    // 알림 타입 메시지 처리
    if (data.type === 'notification') {
      handleNotification(data)
    }

  } catch (error) {
    console.error('WebSocket message parse error:', error)
  }
}

const handleClose = (event: CloseEvent): void => {
  console.log('WebSocket disconnected:', event.code, event.reason)
  
  const errorMessage = {
    id: Date.now(),
    type: 'system',
    payload: `WebSocket 연결이 해제되었습니다. (코드: ${event.code}, 이유: ${event.reason})`,
    timestamp: new Date()
  }
  messages.value.push(errorMessage)

  // 자동 재연결 시도
  if (reconnectAttempts.value < maxReconnectAttempts) {
    setTimeout(() => {
      reconnectAttempts.value++
      console.log(`Reconnecting... Attempt ${reconnectAttempts.value}`)
      connect()
    }, reconnectDelay)
  } else {
    ElMessage.error('WebSocket 재연결에 실패했습니다. 수동으로 다시 시도해주세요.')
  }
}

const handleError = (error: Event): void => {
  console.error('WebSocket error:', error)
  ElMessage.error('WebSocket 연결 중 오류가 발생했습니다.')
}

// =============================================================================
// 🔔 알림 처리
// =============================================================================
const handleNotification = (data: any): void => {
  // 브라우저 알림
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(data.title || '새 알림', {
      body: data.message || data.content,
      icon: '/favicon.ico',
      tag: data.id
    })
  }

  // Element Plus 알림
  if (data.type === 'mention' || data.type === 'like' || data.type === 'comment') {
    ElMessage({
      message: data.message || data.content,
      type: 'info',
      duration: 5000,
      showClose: true
    })
  }
}

// =============================================================================
// 🔧 유틸리티 함수
// =============================================================================
const clearMessages = (): void => {
  messages.value = []
  ElMessage.info('메시지 기록을 지웠습니다.')
}

const formatTime = (date: Date): string => {
  return date.toLocaleTimeString('ko-KR')
}

const requestNotificationPermission = async (): Promise<void> => {
  if ('Notification' in window && Notification.permission === 'default') {
    try {
      await Notification.requestPermission()
    } catch (error) {
      console.error('Notification permission error:', error)
    }
  }
}

// =============================================================================
// 🎯 라이프사이클
// =============================================================================
onMounted(() => {
  // 알림 권한 요청
  requestNotificationPermission()
  
  // 자동 연결
  connect()
})

onUnmounted(() => {
  disconnect()
})
</script>

<style scoped>
/* =============================================================================
// 🎨 WebSocket 클라이언트 스타일
// ============================================================================= */
.websocket-client {
  @apply max-w-4xl mx-auto p-6 bg-white dark:bg-gray-800 rounded-lg shadow-lg;
}

/* =============================================================================
// 📡 연결 상태
// ============================================================================= */
.connection-status {
  @apply flex items-center gap-2 mb-6 p-3 rounded-lg border;
}

.connection-status.connected {
  @apply bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800;
}

.connection-status.connecting {
  @apply bg-yellow-50 dark:bg-yellow-900/20 border-yellow-200 dark:border-yellow-800;
}

.connection-status.disconnected {
  @apply bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800;
}

.status-indicator {
  @apply w-3 h-3 rounded-full;
}

.connection-status.connected .status-indicator {
  @apply bg-green-500 animate-pulse;
}

.connection-status.connecting .status-indicator {
  @apply bg-yellow-500 animate-pulse;
}

.connection-status.disconnected .status-indicator {
  @apply bg-red-500;
}

.status-text {
  @apply font-medium text-gray-700 dark:text-gray-300;
}

/* =============================================================================
// 📝 메시지 컨테이너
// ============================================================================= */
.message-container {
  @apply space-y-6;
}

.message-list {
  @apply max-h-96 overflow-y-auto space-y-3 p-4 bg-gray-50 dark:bg-gray-900 rounded-lg border border-gray-200 dark:border-gray-700;
}

.message {
  @apply p-3 rounded-lg border;
}

.message.server {
  @apply bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800;
}

.message.client {
  @apply bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800;
}

.message.system {
  @apply bg-gray-100 dark:bg-gray-700 border-gray-200 dark:border-gray-600;
}

.message-header {
  @apply flex items-center justify-between mb-2 text-xs;
}

.message-time {
  @apply text-gray-500 dark:text-gray-400;
}

.message-type {
  @apply px-2 py-1 rounded text-xs font-medium;
}

.message.server .message-type {
  @apply bg-blue-200 dark:bg-blue-800 text-blue-800 dark:text-blue-200;
}

.message.client .message-type {
  @apply bg-green-200 dark:bg-green-800 text-green-800 dark:text-green-200;
}

.message.system .message-type {
  @apply bg-gray-200 dark:bg-gray-600 text-gray-800 dark:text-gray-200;
}

.message-content {
  @apply text-sm;
}

.message-content pre {
  @apply whitespace-pre-wrap font-mono bg-white dark:bg-gray-800 p-2 rounded border border-gray-200 dark:border-gray-700;
}

/* =============================================================================
// 📝 메시지 폼
// ============================================================================= */
.message-form {
  @apply space-y-3;
}

.message-input {
  @apply w-full;
}

.form-actions {
  @apply flex gap-2;
}

/* =============================================================================
// 📱 반응형 디자인
// ============================================================================= */
@media (max-width: 640px) {
  .websocket-client {
    @apply p-4;
  }
  
  .message-list {
    @apply max-h-64;
  }
  
  .form-actions {
    @apply flex-col;
  }
  
  .form-actions .el-button {
    @apply w-full;
  }
}
</style>