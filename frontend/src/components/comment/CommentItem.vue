<template>
  <div class="comment-item" :style="{ marginLeft: `${level * 24}px` }">
    <!-- 댓글 본문 -->
    <div class="comment-content">
      <!-- 사용자 정보 -->
      <div class="comment-author">
        <el-avatar 
          :src="comment.author.avatar" 
          :size="40"
          class="author-avatar"
        >
          <Icon icon="mdi:account" />
        </el-avatar>
        <div class="author-info">
          <div class="author-name">{{ comment.author.username }}</div>
          <div class="comment-time">{{ formatTime(comment.createdAt) }}</div>
        </div>
        
        <!-- 댓글 메뉴 -->
        <el-dropdown trigger="click" @command="handleMenuCommand">
          <el-button text class="more-btn">
            <Icon icon="mdi:dots-vertical" />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item 
                v-if="canEdit"
                command="edit"
              >
                <Icon icon="mdi:pencil" class="menu-icon" />
                수정
              </el-dropdown-item>
              <el-dropdown-item 
                v-if="canDelete"
                command="delete"
              >
                <Icon icon="mdi:delete" class="menu-icon" />
                삭제
              </el-dropdown-item>
              <el-dropdown-item command="report">
                <Icon icon="mdi:flag" class="menu-icon" />
                신고
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <!-- 댓글 텍스트 -->
      <div class="comment-text">
        <div v-if="isEditing" class="edit-form">
          <el-input
            v-model="editContent"
            type="textarea"
            :rows="3"
            placeholder="댓글을 수정하세요..."
            class="edit-input"
          />
          <div class="edit-actions">
            <el-button 
              size="small" 
              @click="cancelEdit"
            >
              취소
            </el-button>
            <el-button 
              size="small" 
              type="primary"
              :loading="isSubmitting"
              @click="submitEdit"
            >
              저장
            </el-button>
          </div>
        </div>
        <p v-else class="comment-message">{{ comment.content }}</p>
      </div>

      <!-- 리액션 및 액션 -->
      <div class="comment-actions">
        <!-- 좋아요/싫어요 -->
        <div class="reactions">
          <button 
            :class="['reaction-btn', { active: comment.isLiked }]"
            @click="handleLike"
          >
            <Icon icon="mdi:thumb-up" class="reaction-icon" />
            <span class="reaction-count">{{ comment.likes }}</span>
          </button>
          
          <button 
            :class="['reaction-btn', { active: comment.isDisliked }]"
            @click="handleDislike"
          >
            <Icon icon="mdi:thumb-down" class="reaction-icon" />
            <span class="reaction-count">{{ comment.dislikes }}</span>
          </button>
        </div>

        <!-- 답글 작성 -->
        <button 
          v-if="authStore.isAuthenticated && level < 4"
          class="reply-btn"
          @click="handleReply"
        >
          <Icon icon="mdi:reply" class="reply-icon" />
          답글
        </button>
      </div>

      <!-- 편집 시간 표시 -->
      <div v-if="comment.updatedAt > comment.createdAt" class="edited-time">
        ({{ formatTime(comment.updatedAt) }}에 수정됨)
      </div>
    </div>

    <!-- 대댓글 작성 폼 -->
    <div v-if="showReplyForm" class="reply-form-container">
      <CommentForm 
        :post-id="postId" 
        :parent-comment="comment"
        @submit="handleReplySubmit"
        @cancel="toggleReplyForm"
      />
    </div>

    <!-- 대댓글 목록 -->
    <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
      <CommentItem
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        :post-id="postId"
        :level="level + 1"
        @reply="$emit('reply', $event)"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
        @like="$emit('like', $event)"
        @unlike="$emit('unlike', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Icon } from '@iconify/vue'
import { useAuthStore } from '@/stores/auth'
import CommentForm from './CommentForm.vue'

// =============================================================================
// 🎯 Props 정의
// =============================================================================
interface Props {
  comment: any
  postId: string
  level: number
}

const props = defineProps<Props>()

// =============================================================================
// 🎯 Emits 정의
// =============================================================================
const emit = defineEmits<{
  reply: [commentId: number]
  edit: [commentId: number]
  delete: [commentId: number]
  like: [commentId: number]
  unlike: [commentId: number]
}>()

// =============================================================================
// 🎯 컴포넌트 상태
// =============================================================================
const authStore = useAuthStore()

const isEditing = ref(false)
const isSubmitting = ref(false)
const editContent = ref('')
const showReplyForm = ref(false)

// =============================================================================
// 🎯 컴퓨티드 프로퍼티
// =============================================================================
const canEdit = computed(() => {
  return authStore.isAuthenticated && 
         (authStore.user?.id === props.comment.author.id || 
          authStore.isModerator)
})

const canDelete = computed(() => {
  return authStore.isAuthenticated && 
         (authStore.user?.id === props.comment.author.id || 
          authStore.isModerator)
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
  if (days < 7) return `${days}일 전`
  return date.toLocaleDateString('ko-KR')
}

// =============================================================================
// 📝 댓글 관리
// =============================================================================
const handleMenuCommand = (command: string): void => {
  switch (command) {
    case 'edit':
      startEdit()
      break
    case 'delete':
      emit('delete', props.comment.id)
      break
    case 'report':
      reportComment()
      break
  }
}

const startEdit = (): void => {
  isEditing.value = true
  editContent.value = props.comment.content
}

const cancelEdit = (): void => {
  isEditing.value = false
  editContent.value = ''
}

const submitEdit = async (): Promise<void> => {
  if (!editContent.value.trim()) {
    ElMessage.warning('댓글 내용을 입력해주세요.')
    return
  }

  try {
    isSubmitting.value = true

    // TODO: 실제 API 호출
    props.comment.content = editContent.value.trim()
    props.comment.updatedAt = new Date()

    isEditing.value = false
    editContent.value = ''
    ElMessage.success('댓글이 수정되었습니다.')

  } catch (error) {
    console.error('Failed to edit comment:', error)
    ElMessage.error('댓글 수정에 실패했습니다.')
  } finally {
    isSubmitting.value = false
  }
}

const handleReply = (): void => {
  showReplyForm.value = !showReplyForm.value
}

const toggleReplyForm = (): void => {
  showReplyForm.value = false
}

const handleReplySubmit = async (replyData: any): Promise<void> => {
  try {
    // TODO: 실제 API 호출
    const newReply = {
      id: Date.now(),
      content: replyData.content,
      author: {
        id: authStore.user?.id,
        username: authStore.userName,
        avatar: authStore.userAvatar
      },
      createdAt: new Date(),
      updatedAt: new Date(),
      likes: 0,
      dislikes: 0,
      isLiked: false,
      isDisliked: false,
      replies: [],
      level: props.level + 1
    }

    if (!props.comment.replies) {
      props.comment.replies = []
    }
    props.comment.replies.push(newReply)

    showReplyForm.value = false
    ElMessage.success('답글이 작성되었습니다.')

  } catch (error) {
    console.error('Failed to submit reply:', error)
    ElMessage.error('답글 작성에 실패했습니다.')
  }
}

const handleLike = (): void => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('로그인이 필요합니다.')
    return
  }

  if (props.comment.isLiked) {
    emit('unlike', props.comment.id)
  } else {
    emit('like', props.comment.id)
  }
}

const handleDislike = (): void => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('로그인이 필요합니다.')
    return
  }

  if (props.comment.isDisliked) {
    emit('unlike', props.comment.id)
  } else {
    emit('like', props.comment.id)
  }
}

const reportComment = async (): void => {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      '신고 사유를 입력해주세요.',
      '댓글 신고',
      {
        confirmButtonText: '신고',
        cancelButtonText: '취소',
        inputPlaceholder: '신고 사유',
        inputValidator: (value) => {
          if (!value || !value.trim()) {
            return '신고 사유를 입력해주세요.'
          }
          return true
        }
      }
    )

    // TODO: 실제 API 호출
    console.log('Report comment:', props.comment.id, reason)
    ElMessage.success('댓글이 신고되었습니다.')

  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to report comment:', error)
      ElMessage.error('댓글 신고에 실패했습니다.')
    }
  }
}
</script>

<style scoped>
/* =============================================================================
// 🎨 댓글 아이템 스타일
// ============================================================================= */
.comment-item {
  @apply border-l-2 border-transparent pl-6 relative;
}

.comment-item:not(:last-child) {
  @apply mb-4;
}

.comment-item.level-0 {
  @apply border-gray-200 dark:border-gray-700;
}

.comment-item.level-1 {
  @apply border-blue-200 dark:border-blue-800;
}

.comment-item.level-2 {
  @apply border-green-200 dark:border-green-800;
}

.comment-item.level-3 {
  @apply border-purple-200 dark:border-purple-800;
}

.comment-item.level-4 {
  @apply border-orange-200 dark:border-orange-800;
}

/* =============================================================================
// 📝 댓글 콘텐츠
// ============================================================================= */
.comment-content {
  @apply bg-white dark:bg-gray-800 rounded-lg p-4 shadow-sm;
}

.comment-author {
  @apply flex items-center justify-between mb-3;
}

.author-avatar {
  @apply border-2 border-transparent hover:border-primary-500 transition-colors duration-200;
}

.author-info {
  @apply flex-1 ml-3;
}

.author-name {
  @apply font-medium text-gray-900 dark:text-gray-100;
}

.comment-time {
  @apply text-xs text-gray-500 dark:text-gray-400;
}

.more-btn {
  @apply text-gray-400 hover:text-gray-600 dark:hover:text-gray-300;
}

.comment-text {
  @apply mb-3;
}

.comment-message {
  @apply text-gray-800 dark:text-gray-200 leading-relaxed whitespace-pre-wrap;
}

/* =============================================================================
// ✏️ 수정 폼
// ============================================================================= */
.edit-form {
  @apply space-y-3;
}

.edit-input {
  @apply w-full;
}

.edit-actions {
  @apply flex justify-end gap-2;
}

/* =============================================================================
// ⚡ 리액션 및 액션
// ============================================================================= */
.comment-actions {
  @apply flex items-center justify-between;
}

.reactions {
  @apply flex items-center gap-4;
}

.reaction-btn {
  @apply flex items-center gap-1 px-2 py-1 rounded text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors duration-200;
}

.reaction-btn.active {
  @apply text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/20;
}

.reaction-icon {
  @apply w-4 h-4;
}

.reaction-count {
  @apply text-xs;
}

.reply-btn {
  @apply flex items-center gap-1 px-2 py-1 rounded text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors duration-200;
}

.reply-icon {
  @apply w-4 h-4;
}

.edited-time {
  @apply text-xs text-gray-400 dark:text-gray-500 italic;
}

/* =============================================================================
// 💬 대댓글 폼
// ============================================================================= */
.reply-form-container {
  @apply mt-4 ml-6;
}

/* =============================================================================
// 📋 대댓글 목록
// ============================================================================= */
.replies-list {
  @apply mt-4 space-y-4;
}

/* =============================================================================
// 📱 반응형 디자인
// ============================================================================= */
@media (max-width: 640px) {
  .comment-item {
    @apply pl-4;
  }
  
  .comment-content {
    @apply p-3;
  }
  
  .reactions {
    @apply flex-wrap gap-2;
  }
  
  .reply-form-container {
    @apply ml-4;
  }
}
</style>