<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postApi } from '@/api/post'
import type { Category } from '@/types'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const title = ref('')
const content = ref('')
const categoryId = ref('')
const tagsInput = ref('')
const categories = ref<Category[]>([])
const loading = ref(false)
const error = ref('')

async function fetchCategories() {
  const res = await postApi.getCategories()
  if (res.data.success && res.data.data) {
    categories.value = res.data.data
    if (categories.value.length > 0 && !categoryId.value) {
      categoryId.value = categories.value[0].id
    }
  }
}

async function fetchPost() {
  if (!isEdit.value) return
  const res = await postApi.getPostById(route.params.id as string)
  if (res.data.success && res.data.data) {
    const post = res.data.data
    title.value = post.title
    content.value = post.content
    categoryId.value = post.categoryId
    tagsInput.value = post.tags.join(', ')
  }
}

async function handleSubmit() {
  error.value = ''
  loading.value = true
  const tags = tagsInput.value
    .split(',')
    .map((t) => t.trim())
    .filter((t) => t)

  try {
    if (isEdit.value) {
      await postApi.updatePost(route.params.id as string, {
        title: title.value,
        content: content.value,
        categoryId: categoryId.value,
        tags,
      })
      router.push(`/posts/${route.params.id}`)
    } else {
      const res = await postApi.createPost({
        title: title.value,
        content: content.value,
        categoryId: categoryId.value,
        tags,
      })
      if (res.data.success && res.data.data) {
        router.push(`/posts/${res.data.data.id}`)
      }
    }
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Failed to save post'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchCategories()
  await fetchPost()
})
</script>

<template>
  <div class="post-write">
    <h1>{{ isEdit ? 'Edit Post' : 'Write Post' }}</h1>
    <form class="card" @submit.prevent="handleSubmit">
      <div class="form-group">
        <label for="category">Category</label>
        <select id="category" v-model="categoryId" required>
          <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
        </select>
      </div>
      <div class="form-group">
        <label for="title">Title</label>
        <input id="title" v-model="title" type="text" placeholder="Enter post title" required />
      </div>
      <div class="form-group">
        <label for="content">Content</label>
        <textarea id="content" v-model="content" rows="15" placeholder="Write your post..." required></textarea>
      </div>
      <div class="form-group">
        <label for="tags">Tags (comma separated)</label>
        <input id="tags" v-model="tagsInput" type="text" placeholder="e.g. web3, blockchain, defi" />
      </div>
      <p v-if="error" class="error-message">{{ error }}</p>
      <div class="form-actions">
        <button type="button" class="btn btn-secondary" @click="router.back()">Cancel</button>
        <button type="submit" class="btn btn-primary" :disabled="loading">
          {{ loading ? 'Saving...' : isEdit ? 'Update' : 'Publish' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.post-write h1 {
  margin-bottom: 20px;
}

textarea {
  resize: vertical;
  min-height: 300px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
</style>
