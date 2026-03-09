import api from './index'
import type {
  ApiResponse,
  PageResponse,
  Post,
  PostSummary,
  CreatePostRequest,
  UpdatePostRequest,
  Category,
} from '@/types'

export const postApi = {
  getPosts(params?: { categoryId?: string; tag?: string; keyword?: string; page?: number; size?: number }) {
    return api.get<ApiResponse<PageResponse<PostSummary>>>('/api/posts', { params })
  },

  getPostById(id: string) {
    return api.get<ApiResponse<Post>>(`/api/posts/${id}`)
  },

  createPost(data: CreatePostRequest) {
    return api.post<ApiResponse<Post>>('/api/posts', data)
  },

  updatePost(id: string, data: UpdatePostRequest) {
    return api.put<ApiResponse<Post>>(`/api/posts/${id}`, data)
  },

  deletePost(id: string) {
    return api.delete(`/api/posts/${id}`)
  },

  toggleLike(id: string) {
    return api.post<ApiResponse<Post>>(`/api/posts/${id}/like`)
  },

  getCategories() {
    return api.get<ApiResponse<Category[]>>('/api/posts/categories')
  },

  createCategory(data: { name: string; description?: string; displayOrder?: number }) {
    return api.post<ApiResponse<Category>>('/api/posts/categories', data)
  },
}
