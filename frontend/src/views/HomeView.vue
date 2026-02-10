<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { usePostStore } from '@/stores/post'
import PostCard from '@/components/post/PostCard.vue'
import CategoryFilter from '@/components/post/CategoryFilter.vue'

const store = usePostStore()
const selectedCategory = ref<string | undefined>()
const searchKeyword = ref('')

async function fetchData(page = 0) {
  await store.fetchPosts({
    categoryId: selectedCategory.value,
    keyword: searchKeyword.value || undefined,
    page,
    size: 20,
  })
}

function handleCategorySelect(categoryId: string | undefined) {
  selectedCategory.value = categoryId
  fetchData()
}

function handleSearch() {
  fetchData()
}

function handlePageChange(page: number) {
  fetchData(page)
}

onMounted(async () => {
  await store.fetchCategories()
  await fetchData()
})
</script>

<template>
  <div class="home">
    <div class="home-header">
      <h1>Posts</h1>
      <div class="search-bar">
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="Search posts..."
          @keyup.enter="handleSearch"
        />
        <button class="btn btn-primary" @click="handleSearch">Search</button>
      </div>
    </div>

    <CategoryFilter
      :categories="store.categories"
      :selected-id="selectedCategory"
      @select="handleCategorySelect"
    />

    <div v-if="store.loading" class="loading">Loading...</div>
    <div v-else-if="store.posts.length === 0" class="no-posts">
      <p>No posts found.</p>
    </div>
    <div v-else>
      <PostCard v-for="post in store.posts" :key="post.id" :post="post" />
    </div>

    <div v-if="store.totalPages > 1" class="pagination">
      <button :disabled="store.currentPage === 0" @click="handlePageChange(store.currentPage - 1)">
        Prev
      </button>
      <button
        v-for="page in store.totalPages"
        :key="page"
        :class="{ active: store.currentPage === page - 1 }"
        @click="handlePageChange(page - 1)"
      >
        {{ page }}
      </button>
      <button
        :disabled="store.currentPage >= store.totalPages - 1"
        @click="handlePageChange(store.currentPage + 1)"
      >
        Next
      </button>
    </div>
  </div>
</template>

<style scoped>
.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.home-header h1 {
  font-size: 24px;
}

.search-bar {
  display: flex;
  gap: 8px;
}

.search-bar input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  width: 240px;
}

.search-bar input:focus {
  outline: none;
  border-color: #3b82f6;
}

.no-posts {
  text-align: center;
  padding: 60px;
  color: #9ca3af;
}
</style>
