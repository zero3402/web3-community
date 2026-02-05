<template>
  <div class="comment-section">
    <!-- 댓글 헤더 -->
    <div class="comment-header">
      <h3 class="comment-title">
        댓글 
        <span class="comment-count">({{ comments.length }})</span>
      </h3>
      <el-button 
        v-if="authStore.isAuthenticated"
        size="small" 
        type="primary" 
        @click="toggleWriteComment"
      >
        <Icon icon="mdi:pencil" class="btn-icon" />
        댓글 작성
      </el-button>
    </div>

    <!-- 댓글 작성 폼 -->
    <div v-if="showWriteForm" class="comment-form-container">
      <CommentForm 
        :post-id="postId" 
        :parent-comment="null"
        @submit="handleCommentSubmit"
        @cancel="toggleWriteComment"
      />
    </div>

    <!-- 댓글 목록 -->
    <div class="comment-list">
      <div v-if="isLoading" class="comment-loading">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="comments.length === 0" class="empty-comments">
        <Icon icon="mdi:comment-outline" class="empty-icon" />
        <p class="empty-text">댓글이 없습니다. 첫 댓글을 작성해보세요!</p>
      </div>

      <div v-else class="comments-tree">
        <CommentItem 
          v-for="comment in comments" 
          :key="comment.id"
          :comment="comment"
          :post-id="postId"
          :level="0"
          @reply="handleReply"
          @edit="handleEdit"
          @delete="handleDelete"
          @like="handleLike"
          @unlike="handleUnlike"
        />
      </div>
    </div>

    <!-- 더보기 버튼 -->
    <div v-if="hasMore && !isLoading" class="load-more">
      <el-button 
        :loading="isLoadingMore"
        @click="loadMoreComments"
        class="load-more-btn"
      >
        더보기
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Icon } from '@iconify/vue'
import { useAuthStore } from '@/stores/auth'
import CommentForm from './CommentForm.vue'
import CommentItem from './CommentItem.vue'

// =============================================================================
// 🎯 Props 정의
// =============================================================================
interface Props {
  postId: string
}

const props = defineProps<Props>()

// =============================================================================
// 🎯 컴포넌트 상태
// =============================================================================
const authStore = useAuthStore()

const comments = ref<any[]>([])
const isLoading = ref(false)
const isLoadingMore = ref(false)
const showWriteForm = ref(false)
const currentPage = ref(0)
const pageSize = 20
const hasMore = ref(true)

// =============================================================================
// 🔧 유틸리티 함수
// =============================================================================
const toggleWriteComment = (): void => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('로그인이 필요합니다.')
    return
  }
  showWriteForm.value = !showWriteForm.value
}

// =============================================================================
// 📝 댓글 관리
// =============================================================================
const fetchComments = async (reset = false): Promise<void> => {
  if (isLoading.value) return

  try {
    isLoading.value = true

    if (reset) {
      currentPage.value = 0
      comments.value = []
      hasMore.value = true
    }

    // TODO: 실제 API 호출
    const mockComments = [
      {
        id: 1,
        content: '정말 유용한 정보네요! 감사합니다.',
        author: {
          id: 'user1',
          username: '김개발',
          avatar: '/avatars/user1.jpg'
        },
        createdAt: new Date('2024-01-15T10:30:00'),
        updatedAt: new Date('2024-01-15T10:30:00'),
        likes: 12,
        dislikes: 1,
        isLiked: false,
        isDisliked: false,
        replies: [
          {
            id: 11,
            content: '저도 동의합니다. 정말 잘 정리된 글이네요.',
            author: {
              id: 'user2',
              username: '이블록',
              avatar: '/avatars/user2.jpg'
            },
            createdAt: new Date('2024-01-15T11:00:00'),
            updatedAt: new Date('2024-01-15T11:00:00'),
            likes: 5,
            dislikes: 0,
            isLiked: true,
            isDisliked: false,
            replies: [],
            level: 1
          }
        ],
        level: 0
      },
      {
        id: 2,
        content: '궁금한 점이 있습니다. 스마트 컨트랙트 배포 비용은 어느 정도인가요?',
        author: {
          id: 'user3',
          username: '박디파이',
          avatar: '/avatars/user3.jpg'
        },
        createdAt: new Date('2024-01-15T09:15:00'),
        updatedAt: new Date('2024-01-15T09:15:00'),
        likes: 3,
        dislikes: 0,
        isLiked: false,
        isDisliked: false,
        replies: [],
        level: 0
      }
    ]

    comments.value = [...comments.value, ...mockComments]
    currentPage.value++
    
    // 더보기 여부 결정 (실제로는 API 응답에 따라 결정)
    hasMore.value = currentPage.value < 3

  } catch (error) {
    console.error('Failed to fetch comments:', error)
    ElMessage.error('댓글을 불러오는데 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

const loadMoreComments = async (): Promise<void> => {
  if (isLoadingMore.value || !hasMore.value) return
  
  isLoadingMore.value = true
  await fetchComments()
  isLoadingMore.value = false
}

const handleCommentSubmit = async (commentData: any): Promise<void> => {
  try {
    // TODO: 실제 API 호출
    const newComment = {
      id: Date.now(),
      content: commentData.content,
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
      level: 0
    }

    if (commentData.parentId) {
      // 대댓글인 경우
      addReplyToComment(comments.value, commentData.parentId, newComment)
    } else {
      // 일반 댓글인 경우
      comments.value.unshift(newComment)
    }

    showWriteForm.value = false
    ElMessage.success('댓글이 작성되었습니다.')

  } catch (error) {
    console.error('Failed to submit comment:', error)
    ElMessage.error('댓글 작성에 실패했습니다.')
  }
}

const addReplyToComment = (comments: any[], parentId: number, reply: any): boolean => {
  for (const comment of comments) {
    if (comment.id === parentId) {
      if (!comment.replies) comment.replies = []
      comment.replies.push({ ...reply, level: comment.level + 1 })
      return true
    }
    if (comment.replies && comment.replies.length > 0) {
      const found = addReplyToComment(comment.replies, parentId, reply)
      if (found) return true
    }
  }
  return false
}

const handleReply = (commentId: number): void => {
  // 대댓글 작성 폼을 해당 댓글 아래에 표시
  console.log('Reply to comment:', commentId)
}

const handleEdit = (commentId: number): void => {
  // 댓글 수정 모드
  console.log('Edit comment:', commentId)
}

const handleDelete = async (commentId: number): Promise<void> => {
  try {
    await ElMessageBox.confirm(
      '정말로 이 댓글을 삭제하시겠습니까?',
      '댓글 삭제',
      {
        confirmButtonText: '삭제',
        cancelButtonText: '취소',
        type: 'warning'
      }
    )

    // TODO: 실제 API 호출
    removeComment(comments.value, commentId)
    ElMessage.success('댓글이 삭제되었습니다.')

  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete comment:', error)
      ElMessage.error('댓글 삭제에 실패했습니다.')
    }
  }
}

const removeComment = (comments: any[], commentId: number): boolean => {
  for (let i = 0; i < comments.length; i++) {
    if (comments[i].id === commentId) {
      comments.splice(i, 1)
      return true
    }
    if (comments[i].replies && comments[i].replies.length > 0) {
      const found = removeComment(comments[i].replies, commentId)
      if (found) return true
    }
  }
  return false
}

const handleLike = async (commentId: number): void => {
  try {
    // TODO: 실제 API 호출
    updateCommentReaction(comments.value, commentId, { isLiked: true, likes: 1 })
  } catch (error) {
    console.error('Failed to like comment:', error)
    ElMessage.error('좋아요 처리에 실패했습니다.')
  }
}

const handleUnlike = async (commentId: number): void => {
  try {
    // TODO: 실제 API 호출
    updateCommentReaction(comments.value, commentId, { isLiked: false, likes: -1 })
  } catch (error) {
    console.error('Failed to unlike comment:', error)
    ElMessage.error('좋아요 취소에 실패했습니다.')
  }
}

const updateCommentReaction = (comments: any[], commentId: number, update: any): boolean => {
  for (const comment of comments) {
    if (comment.id === commentId) {
      comment.isLiked = update.isLiked
      comment.likes += update.likes
      return true
    }
    if (comment.replies && comment.replies.length > 0) {
      const found = updateCommentReaction(comment.replies, commentId, update)
      if (found) return true
    }
  }
  return false
}

// =============================================================================
// 🎯 라이프사이클
// =============================================================================
onMounted(() => {
  fetchComments(true)
})
</script>

<style scoped>
/* =============================================================================
// 🎨 댓글 섹션 스타일
// ============================================================================= */
.comment-section {
  @apply space-y-6;
}

/* =============================================================================
// 📋 댓글 헤더
// ============================================================================= */
.comment-header {
  @apply flex items-center justify-between mb-6;
}

.comment-title {
  @apply text-lg font-semibold text-gray-900 dark:text-gray-100;
}

.comment-count {
  @apply text-gray-500 dark:text-gray-400;
}

.btn-icon {
  @apply w-4 h-4 mr-1;
}

/* =============================================================================
// 📝 댓글 작성 폼
// ============================================================================= */
.comment-form-container {
  @apply mb-6;
}

/* =============================================================================
// 📋 댓글 목록
// ============================================================================= */
.comment-list {
  @apply space-y-4;
}

.comment-loading {
  @apply p-4;
}

.empty-comments {
  @apply text-center py-8 text-gray-500 dark:text-gray-400;
}

.empty-icon {
  @apply w-12 h-12 mb-3;
}

.empty-text {
  @apply text-sm;
}

.comments-tree {
  @apply space-y-4;
}

/* =============================================================================
// 📄 더보기 버튼
// ============================================================================= */
.load-more {
  @apply text-center mt-6;
}

.load-more-btn {
  @apply w-full max-w-xs;
}
</style>