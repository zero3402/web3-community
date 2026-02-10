export interface User {
  id: number
  nickname: string
  email: string
  bio?: string
  profileImageUrl?: string
  role: string
  createdAt: string
}

export interface Post {
  id: string
  title: string
  content: string
  authorId: number
  authorNickname: string
  categoryId: string
  categoryName: string
  tags: string[]
  viewCount: number
  likeCount: number
  commentCount: number
  createdAt: string
  updatedAt: string
}

export interface PostSummary {
  id: string
  title: string
  authorNickname: string
  categoryName: string
  tags: string[]
  viewCount: number
  likeCount: number
  commentCount: number
  createdAt: string
}

export interface Comment {
  id: string
  postId: string
  parentId?: string
  depth: number
  authorId: number
  authorNickname: string
  content: string
  likeCount: number
  deleted: boolean
  createdAt: string
  updatedAt: string
  children: Comment[]
}

export interface Category {
  id: string
  name: string
  description?: string
  displayOrder: number
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  message?: string
  errorCode?: string
  timestamp: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  isFirst: boolean
  isLast: boolean
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  nickname: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: number
  email: string
  role: string
}

export interface CreatePostRequest {
  title: string
  content: string
  categoryId: string
  tags: string[]
}

export interface UpdatePostRequest {
  title?: string
  content?: string
  categoryId?: string
  tags?: string[]
}

export interface CreateCommentRequest {
  postId: string
  parentId?: string
  content: string
}
