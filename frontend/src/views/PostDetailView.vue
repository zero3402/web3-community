<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postApi } from '@/api/post'
import { useAuthStore } from '@/stores/auth'
import type { Post } from '@/types'
import CommentSection from '@/components/comment/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const post = ref<Post | null>(null)
const loading = ref(true)

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function fetchPost() {
  loading.value = true
  try {
    const res = await postApi.getPostById(route.params.id as string)
    if (res.data.success && res.data.data) {
      post.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

async function handleLike() {
  if (!post.value) return
  const res = await postApi.toggleLike(post.value.id)
  if (res.data.success && res.data.data) {
    post.value = res.data.data
  }
}

async function handleDelete() {
  if (!post.value || !confirm('Are you sure you want to delete this post?')) return
  await postApi.deletePost(post.value.id)
  router.push('/')
}

onMounted(fetchPost)
</script>

<template>
  <div v-if="loading" class="loading">Loading...</div>
  <div v-else-if="!post" class="not-found">Post not found.</div>
  <article v-else class="post-detail">
    <div class="post-detail-header card">
      <div class="post-category">{{ post.categoryName }}</div>
      <h1>{{ post.title }}</h1>
      <div class="post-info">
        <span class="author">{{ post.authorNickname }}</span>
        <span class="date">{{ formatDate(post.createdAt) }}</span>
        <span class="views">Views {{ post.viewCount }}</span>
      </div>
      <div class="post-tags" v-if="post.tags.length">
        <span class="tag" v-for="tag in post.tags" :key="tag">#{{ tag }}</span>
      </div>
    </div>

    <div class="post-content card">
      <div v-html="post.content"></div>
    </div>

    <div class="post-actions">
      <button class="btn" @click="handleLike">
        Likes {{ post.likeCount }}
      </button>
      <div v-if="auth.user?.id === post.authorId" class="author-actions">
        <router-link :to="`/posts/${post.id}/edit`" class="btn btn-secondary">Edit</router-link>
        <button class="btn btn-danger" @click="handleDelete">Delete</button>
      </div>
    </div>

    <CommentSection :post-id="post.id" />
  </article>
</template>

<style scoped>
.post-detail-header {
  margin-bottom: 16px;
}

.post-category {
  color: #3b82f6;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.post-detail-header h1 {
  font-size: 28px;
  margin-bottom: 12px;
}

.post-info {
  display: flex;
  gap: 16px;
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 8px;
}

.author {
  font-weight: 600;
}

.post-content {
  margin-bottom: 16px;
  line-height: 1.8;
  min-height: 200px;
}

.post-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.author-actions {
  display: flex;
  gap: 8px;
}

.not-found {
  text-align: center;
  padding: 60px;
  color: #9ca3af;
  font-size: 18px;
}
</style>
