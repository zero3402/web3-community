<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { Comment } from '@/types'
import { commentApi } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'
import CommentForm from './CommentForm.vue'
import CommentItem from './CommentItem.vue'

const props = defineProps<{
  postId: string
}>()

const auth = useAuthStore()
const comments = ref<Comment[]>([])
const loading = ref(false)

async function fetchComments() {
  loading.value = true
  try {
    const res = await commentApi.getCommentsByPostId(props.postId)
    comments.value = res.data as Comment[]
  } catch {
    comments.value = []
  } finally {
    loading.value = false
  }
}

async function handleSubmit(content: string) {
  await commentApi.createComment({ postId: props.postId, content })
  await fetchComments()
}

async function handleReply(parentId: string, content: string) {
  await commentApi.createComment({ postId: props.postId, parentId, content })
  await fetchComments()
}

async function handleDelete(id: string) {
  await commentApi.deleteComment(id)
  await fetchComments()
}

async function handleLike(id: string) {
  await commentApi.toggleLike(id)
  await fetchComments()
}

onMounted(fetchComments)
</script>

<template>
  <div class="comment-section">
    <h3>Comments</h3>
    <CommentForm v-if="auth.isAuthenticated" @submit="handleSubmit" />
    <p v-else class="login-prompt">
      <router-link to="/login">Login</router-link> to leave a comment.
    </p>
    <div v-if="loading" class="loading">Loading comments...</div>
    <div v-else-if="comments.length === 0" class="no-comments">No comments yet.</div>
    <div v-else>
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        :current-user-id="auth.user?.id"
        @reply="handleReply"
        @delete="handleDelete"
        @like="handleLike"
      />
    </div>
  </div>
</template>

<style scoped>
.comment-section {
  margin-top: 32px;
}

.comment-section h3 {
  margin-bottom: 16px;
  font-size: 18px;
}

.login-prompt {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 16px;
}

.no-comments {
  color: #9ca3af;
  text-align: center;
  padding: 24px;
}
</style>
