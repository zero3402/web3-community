<script setup lang="ts">
import { ref } from 'vue'
import type { Comment } from '@/types'
import CommentForm from './CommentForm.vue'

const props = defineProps<{
  comment: Comment
  currentUserId?: number
}>()

const emit = defineEmits<{
  reply: [parentId: string, content: string]
  delete: [id: string]
  like: [id: string]
}>()

const showReplyForm = ref(false)

function handleReply(content: string) {
  emit('reply', props.comment.id, content)
  showReplyForm.value = false
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <div class="comment-item" :style="{ marginLeft: `${comment.depth * 24}px` }">
    <div class="comment-header">
      <span class="comment-author">{{ comment.authorNickname }}</span>
      <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
    </div>
    <p class="comment-content">{{ comment.content }}</p>
    <div class="comment-actions">
      <button @click="emit('like', comment.id)">
        Likes {{ comment.likeCount }}
      </button>
      <button @click="showReplyForm = !showReplyForm">Reply</button>
      <button
        v-if="currentUserId === comment.authorId && !comment.deleted"
        @click="emit('delete', comment.id)"
      >
        Delete
      </button>
    </div>
    <CommentForm
      v-if="showReplyForm"
      :parent-id="comment.id"
      placeholder="Write a reply..."
      @submit="handleReply"
      @cancel="showReplyForm = false"
    />
    <CommentItem
      v-for="child in comment.children"
      :key="child.id"
      :comment="child"
      :current-user-id="currentUserId"
      @reply="(pid, content) => emit('reply', pid, content)"
      @delete="(id) => emit('delete', id)"
      @like="(id) => emit('like', id)"
    />
  </div>
</template>

<style scoped>
.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.comment-header {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
}

.comment-author {
  font-weight: 600;
  font-size: 14px;
}

.comment-date {
  color: #9ca3af;
  font-size: 12px;
}

.comment-content {
  font-size: 14px;
  margin-bottom: 8px;
  line-height: 1.5;
}

.comment-actions {
  display: flex;
  gap: 12px;
}

.comment-actions button {
  background: none;
  border: none;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
}

.comment-actions button:hover {
  color: #3b82f6;
}
</style>
