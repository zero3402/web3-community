import api from './index'
import type { ApiResponse, Comment, CreateCommentRequest } from '@/types'

export const commentApi = {
  getCommentsByPostId(postId: string) {
    return api.get<Comment[]>(`/api/comments/post/${postId}`)
  },

  createComment(data: CreateCommentRequest) {
    return api.post<CommentResponse>('/api/comments', data)
  },

  updateComment(id: string, content: string) {
    return api.put<CommentResponse>(`/api/comments/${id}`, { content })
  },

  deleteComment(id: string) {
    return api.delete(`/api/comments/${id}`)
  },

  toggleLike(id: string) {
    return api.post<CommentResponse>(`/api/comments/${id}/like`)
  },

  getCommentCount(postId: string) {
    return api.get<number>(`/api/comments/post/${postId}/count`)
  },
}

type CommentResponse = ApiResponse<Comment>
