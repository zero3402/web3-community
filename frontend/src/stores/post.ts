import { defineStore } from 'pinia'
import { ref } from 'vue'
import { postApi } from '@/api/post'
import type { PostSummary, Category, PageResponse } from '@/types'

export const usePostStore = defineStore('post', () => {
  const posts = ref<PostSummary[]>([])
  const categories = ref<Category[]>([])
  const totalPages = ref(0)
  const totalElements = ref(0)
  const currentPage = ref(0)
  const loading = ref(false)

  async function fetchPosts(params?: { categoryId?: string; tag?: string; keyword?: string; page?: number; size?: number }) {
    loading.value = true
    try {
      const res = await postApi.getPosts(params)
      if (res.data.success && res.data.data) {
        const page: PageResponse<PostSummary> = res.data.data
        posts.value = page.content
        totalPages.value = page.totalPages
        totalElements.value = page.totalElements
        currentPage.value = page.page
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchCategories() {
    try {
      const res = await postApi.getCategories()
      if (res.data.success && res.data.data) {
        categories.value = res.data.data
      }
    } catch {
      categories.value = []
    }
  }

  return { posts, categories, totalPages, totalElements, currentPage, loading, fetchPosts, fetchCategories }
})
