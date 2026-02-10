<script setup lang="ts">
import type { PostSummary } from '@/types'

defineProps<{
  post: PostSummary
}>()

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR')
}
</script>

<template>
  <router-link :to="`/posts/${post.id}`" class="post-card card">
    <div class="post-card-header">
      <span class="category">{{ post.categoryName }}</span>
      <span class="date">{{ formatDate(post.createdAt) }}</span>
    </div>
    <h3 class="post-title">{{ post.title }}</h3>
    <div class="post-meta">
      <span class="author">{{ post.authorNickname }}</span>
      <div class="post-stats">
        <span>Views {{ post.viewCount }}</span>
        <span>Likes {{ post.likeCount }}</span>
        <span>Comments {{ post.commentCount }}</span>
      </div>
    </div>
    <div class="post-tags" v-if="post.tags.length">
      <span class="tag" v-for="tag in post.tags" :key="tag">#{{ tag }}</span>
    </div>
  </router-link>
</template>

<style scoped>
.post-card {
  display: block;
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.2s;
  margin-bottom: 12px;
}

.post-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  text-decoration: none;
}

.post-card-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.category {
  color: #3b82f6;
  font-size: 12px;
  font-weight: 600;
}

.date {
  color: #9ca3af;
  font-size: 12px;
}

.post-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1f2937;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.post-stats {
  display: flex;
  gap: 12px;
}

.post-tags {
  margin-top: 4px;
}
</style>
