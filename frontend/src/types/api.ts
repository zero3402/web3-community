// =============================================================================
// 🔐 API 타입 정의
// =============================================================================
// 설명: API 요청/응답을 위한 TypeScript 타입 정의
// 특징: 타입 안전성, 자동 완성, 에러 방지
// 목적: 백엔드 API와의 타입 일관성 보장
// =============================================================================

// =============================================================================
// 📋 기본 응답 타입
// =============================================================================
export interface ApiResponse<T = any> {
  success: boolean
  data?: T
  error?: ApiError
  message?: string
  timestamp: string
}

export interface ApiError {
  code: string
  message: string
  details?: any
  field?: string
}

// =============================================================================
// 👤 인증 관련 타입
// =============================================================================
export interface LoginCredentials {
  email: string
  password: string
  rememberMe?: boolean
}

export interface RegisterData {
  username: string
  email: string
  password: string
  confirmPassword: string
  agreeToTerms: boolean
  agreeToPrivacy: boolean
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export interface UserProfile {
  id: string
  username: string
  email: string
  firstName?: string
  lastName?: string
  avatar?: string
  bio?: string
  website?: string
  location?: string
  birthDate?: string
  phone?: string
  status: 'active' | 'inactive' | 'suspended'
  role: 'user' | 'moderator' | 'admin'
  preferences: UserPreferences
  socialLinks: SocialLinks
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
  isEmailVerified: boolean
  isPhoneVerified: boolean
}

export interface UserPreferences {
  language: string
  timezone: string
  theme: 'light' | 'dark' | 'auto'
  notifications: NotificationPreferences
  privacy: PrivacyPreferences
}

export interface NotificationPreferences {
  email: boolean
  push: boolean
  inApp: boolean
  mentions: boolean
  comments: boolean
  likes: boolean
  follows: boolean
  system: boolean
}

export interface PrivacyPreferences {
  profileVisibility: 'public' | 'friends' | 'private'
  emailVisibility: boolean
  phoneVisibility: boolean
  showOnlineStatus: boolean
  allowMessages: 'everyone' | 'friends' | 'none'
}

export interface SocialLinks {
  github?: string
  twitter?: string
  linkedin?: string
  instagram?: string
  youtube?: string
  website?: string
}

// =============================================================================
// 🔄 소셜 로그인 타입
// =============================================================================
export interface SocialLoginProvider {
  provider: 'google' | 'github' | 'discord' | 'kakao' | 'naver'
  clientId: string
  redirectUri: string
  scope: string[]
}

export interface SocialLoginRequest {
  provider: string
  code: string
  state?: string
}

export interface SocialProfile {
  id: string
  provider: string
  providerId: string
  username?: string
  email?: string
  name?: string
  avatar?: string
}

// =============================================================================
// 📱 푸시 알림 타입
// =============================================================================
export interface PushNotification {
  id: string
  title: string
  body: string
  icon?: string
  badge?: string
  image?: string
  data?: Record<string, any>
  actions?: NotificationAction[]
  tag?: string
  requireInteraction?: boolean
  silent?: boolean
  timestamp: number
}

export interface NotificationAction {
  action: string
  title: string
  icon?: string
}

export interface NotificationSubscription {
  endpoint: string
  keys: {
    p256dh: string
    auth: string
  }
}

// =============================================================================
// 🌍 다국어 타입
// =============================================================================
export interface Locale {
  code: string
  name: string
  nativeName: string
  flag: string
  rtl: boolean
}

// =============================================================================
// 📊 테마 타입
// =============================================================================
export interface ThemeConfig {
  mode: 'light' | 'dark' | 'auto'
  primaryColor: string
  accentColor: string
  customColors?: Record<string, string>
}

// =============================================================================
// 🔒 보안 타입
// =============================================================================
export interface SecuritySettings {
  twoFactorEnabled: boolean
  twoFactorSecret?: string
  backupCodes?: string[]
  trustedDevices: TrustedDevice[]
  loginHistory: LoginHistory[]
}

export interface TrustedDevice {
  id: string
  name: string
  userAgent: string
  ipAddress: string
  createdAt: string
  lastUsedAt: string
}

export interface LoginHistory {
  id: string
  timestamp: string
  ipAddress: string
  userAgent: string
  location?: string
  success: boolean
  failureReason?: string
}

// =============================================================================
// 📈 활동 로그 타입
// =============================================================================
export interface ActivityLog {
  id: string
  userId: string
  type: 'login' | 'logout' | 'post' | 'comment' | 'like' | 'follow' | 'profile_update'
  action: string
  details: Record<string, any>
  ipAddress: string
  userAgent: string
  timestamp: string
}

// =============================================================================
// 🔐 검증 타입
// =============================================================================
export interface ValidationRule {
  required?: boolean
  minLength?: number
  maxLength?: number
  pattern?: RegExp
  custom?: (value: any) => boolean | string
}

export interface ValidationErrors {
  [key: string]: string[]
}

// =============================================================================
// 📋 폼 상태 타입
// =============================================================================
export interface FormState<T = any> {
  data: T
  errors: ValidationErrors
  touched: Record<keyof T, boolean>
  isSubmitting: boolean
  isValid: boolean
}

// =============================================================================
// 🌐 WebSocket 타입
// =============================================================================
export interface WebSocketMessage {
  type: string
  payload: any
  timestamp: string
  id?: string
}

export interface WebSocketState {
  connected: boolean
  connecting: boolean
  error?: string
  lastMessage?: WebSocketMessage
}

// =============================================================================
// 📊 분석 타입
// =============================================================================
export interface UserAnalytics {
  userId: string
  totalPosts: number
  totalComments: number
  totalLikes: number
  totalFollowers: number
  totalFollowing: number
  joinDate: string
  lastActiveDate: string
  engagementRate: number
  growthRate: number
}

// =============================================================================
// 🔍 검색 타입
// =============================================================================
export interface SearchQuery {
  query: string
  type?: 'posts' | 'users' | 'tags' | 'all'
  filters?: SearchFilters
  sortBy?: 'relevance' | 'date' | 'popularity'
  sortOrder?: 'asc' | 'desc'
  page?: number
  limit?: number
}

export interface SearchFilters {
  dateRange?: {
    start: string
    end: string
  }
  category?: string
  tags?: string[]
  author?: string
  status?: 'published' | 'draft' | 'archived'
}

export interface SearchResult<T = any> {
  items: T[]
  total: number
  page: number
  limit: number
  hasMore: boolean
}

// =============================================================================
// 📄 페이지네이션 타입
// =============================================================================
export interface PaginationParams {
  page: number
  limit: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export interface PaginationResponse<T = any> {
  items: T[]
  pagination: {
    page: number
    limit: number
    total: number
    totalPages: number
    hasNext: boolean
    hasPrev: boolean
  }
}

// =============================================================================
// 🎯 파일 업로드 타입
// =============================================================================
export interface FileUpload {
  file: File
  id: string
  name: string
  size: number
  type: string
  progress: number
  status: 'pending' | 'uploading' | 'success' | 'error'
  url?: string
  error?: string
}

export interface UploadedFile {
  id: string
  name: string
  originalName: string
  size: number
  type: string
  url: string
  thumbnailUrl?: string
  uploadedAt: string
}

// =============================================================================
// 🌐 외부 API 타입
// =============================================================================
export interface ExternalApiConfig {
  baseUrl: string
  timeout: number
  retries: number
  headers?: Record<string, string>
}

// =============================================================================
// 📱 PWA 타입
// =============================================================================
export interface PWAInstallPrompt {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

export interface ServiceWorkerUpdate {
  available: boolean
  registration?: ServiceWorkerRegistration
  newWorker?: ServiceWorker
}

// =============================================================================
// 🧪 테스트 타입
// =============================================================================
export interface TestUser {
  id: string
  username: string
  email: string
  password: string
  role: string
  isActive: boolean
}

export interface TestData {
  users: TestUser[]
  posts: any[]
  comments: any[]
}