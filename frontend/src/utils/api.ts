// =============================================================================
// 🌐 HTTP 클라이언트 설정
// =============================================================================
// 설명: Axios 기반 HTTP 클라이언트 및 API 통신 관리
// 특징: 인증, 에러 처리, 인터셉터, 재시도 로직
// 목적: 백엔드 API와의 안정적인 통신 제공
// =============================================================================

import axios, { 
  AxiosInstance, 
  AxiosRequestConfig, 
  AxiosResponse, 
  AxiosError,
  InternalAxiosRequestConfig 
} from 'axios'
import { ElMessage, ElLoading } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse, ApiError } from '@/types/api'

// =============================================================================
// 🔧 API 기본 설정
// =============================================================================
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const API_TIMEOUT = parseInt(import.meta.env.VITE_API_TIMEOUT || '10000')

// =============================================================================
// 🚀 Axios 인스턴스 생성
// =============================================================================
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
})

// =============================================================================
// 📡 로딩 상태 관리
// =============================================================================
let loadingInstance: any = null
let requestCount = 0

const showLoading = () => {
  if (requestCount === 0) {
    loadingInstance = ElLoading.service({
      lock: true,
      text: '로딩 중...',
      background: 'rgba(0, 0, 0, 0.7)',
      customClass: 'global-loading'
    })
  }
  requestCount++
}

const hideLoading = () => {
  requestCount--
  if (requestCount <= 0 && loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
    requestCount = 0
  }
}

// =============================================================================
// 🔐 요청 인터셉터
// =============================================================================
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    // 인증 토큰 추가
    const authStore = useAuthStore()
    if (authStore.accessToken) {
      config.headers.Authorization = `Bearer ${authStore.accessToken}`
    }

    // 요청 ID 추가 (디버깅용)
    config.headers['X-Request-ID'] = generateRequestId()
    
    // 사용자 에이전트 추가
    config.headers['X-User-Agent'] = navigator.userAgent

    // 디버그 모드에서 로깅
    if (import.meta.env.DEV) {
      console.log(`🚀 API Request: ${config.method?.toUpperCase()} ${config.url}`, {
        params: config.params,
        data: config.data,
        headers: config.headers
      })
    }

    // 긴 요청에만 로딩 표시
    if (config.showLoading !== false) {
      showLoading()
    }

    return config
  },
  (error: AxiosError): Promise<AxiosError> => {
    hideLoading()
    console.error('❌ Request Error:', error)
    return Promise.reject(error)
  }
)

// =============================================================================
// 📥 응답 인터셉터
// =============================================================================
apiClient.interceptors.response.use(
  (response: AxiosResponse): AxiosResponse => {
    hideLoading()

    // 디버그 모드에서 로깅
    if (import.meta.env.DEV) {
      console.log(`✅ API Response: ${response.config.method?.toUpperCase()} ${response.config.url}`, {
        status: response.status,
        data: response.data
      })
    }

    return response
  },
  async (error: AxiosError): Promise<any> => {
    hideLoading()

    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    // 토큰 만료 처리
    if (error.response?.status === 401 && !originalRequest._retry) {
      const authStore = useAuthStore()
      
      if (authStore.refreshToken) {
        try {
          originalRequest._retry = true
          await authStore.refreshAccessToken()
          
          // 새 토큰으로 요청 재시도
          if (authStore.accessToken) {
            originalRequest.headers.Authorization = `Bearer ${authStore.accessToken}`
            return apiClient(originalRequest)
          }
        } catch (refreshError) {
          // 리프레시 토큰 만료
          authStore.logout()
          redirectToLogin()
          return Promise.reject(refreshError)
        }
      } else {
        authStore.logout()
        redirectToLogin()
      }
    }

    // 에러 처리
    handleApiError(error)
    return Promise.reject(error)
  }
)

// =============================================================================
// 🔧 유틸리티 함수
// =============================================================================
const generateRequestId = (): string => {
  return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

const redirectToLogin = (): void => {
  if (window.location.pathname !== '/auth/login') {
    window.location.href = '/auth/login?redirect=' + encodeURIComponent(window.location.pathname)
  }
}

const handleApiError = (error: AxiosError): void => {
  const status = error.response?.status
  const errorData = error.response?.data as ApiError

  // 에러 메시지 결정
  let message = '요청 처리 중 오류가 발생했습니다.'
  
  if (errorData?.message) {
    message = errorData.message
  } else if (status) {
    switch (status) {
      case 400:
        message = '잘못된 요청입니다.'
        break
      case 401:
        message = '인증이 필요합니다.'
        break
      case 403:
        message = '권한이 없습니다.'
        break
      case 404:
        message = '요청한 리소스를 찾을 수 없습니다.'
        break
      case 409:
        message = '데이터 충돌이 발생했습니다.'
        break
      case 422:
        message = '입력 데이터가 유효하지 않습니다.'
        break
      case 429:
        message = '너무 많은 요청을 보냈습니다. 잠시 후 다시 시도해주세요.'
        break
      case 500:
        message = '서버 오류가 발생했습니다.'
        break
      case 502:
        message = '서버가 응답하지 않습니다.'
        break
      case 503:
        message = '서비스를 일시적으로 사용할 수 없습니다.'
        break
    }
  }

  // 메시지 표시
  if (error.config?.showError !== false) {
    ElMessage({
      message,
      type: 'error',
      duration: 5000,
      showClose: true
    })
  }

  // 디버그 로깅
  if (import.meta.env.DEV) {
    console.error('❌ API Error Details:', {
      status,
      message,
      errorData,
      config: error.config
    })
  }
}

// =============================================================================
// 🌐 API 메소드
// =============================================================================
class ApiClient {
  // GET 요청
  async get<T = any>(
    url: string, 
    params?: Record<string, any>, 
    config?: AxiosRequestConfig
  ): Promise<ApiResponse<T>> {
    const response = await apiClient.get<ApiResponse<T>>(url, {
      params,
      ...config
    })
    return response.data
  }

  // POST 요청
  async post<T = any>(
    url: string, 
    data?: any, 
    config?: AxiosRequestConfig
  ): Promise<ApiResponse<T>> {
    const response = await apiClient.post<ApiResponse<T>>(url, data, config)
    return response.data
  }

  // PUT 요청
  async put<T = any>(
    url: string, 
    data?: any, 
    config?: AxiosRequestConfig
  ): Promise<ApiResponse<T>> {
    const response = await apiClient.put<ApiResponse<T>>(url, data, config)
    return response.data
  }

  // PATCH 요청
  async patch<T = any>(
    url: string, 
    data?: any, 
    config?: AxiosRequestConfig
  ): Promise<ApiResponse<T>> {
    const response = await apiClient.patch<ApiResponse<T>>(url, data, config)
    return response.data
  }

  // DELETE 요청
  async delete<T = any>(
    url: string, 
    config?: AxiosRequestConfig
  ): Promise<ApiResponse<T>> {
    const response = await apiClient.delete<ApiResponse<T>>(url, config)
    return response.data
  }

  // 파일 업로드
  async upload<T = any>(
    url: string, 
    file: File | FormData, 
    onProgress?: (progress: number) => void,
    config?: AxiosRequestConfig
  ): Promise<ApiResponse<T>> {
    const formData = file instanceof FormData ? file : new FormData()
    if (file instanceof File) {
      formData.append('file', file)
    }

    const response = await apiClient.post<ApiResponse<T>>(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      },
      ...config
    })

    return response.data
  }

  // 파일 다운로드
  async download(
    url: string, 
    filename?: string,
    config?: AxiosRequestConfig
  ): Promise<void> {
    const response = await apiClient.get(url, {
      responseType: 'blob',
      ...config
    })

    // 브라우저 다운로드
    const blob = new Blob([response.data])
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = filename || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
  }

  // 인터셉터 추가
  addRequestInterceptor(
    onFulfilled?: (config: InternalAxiosRequestConfig) => InternalAxiosRequestConfig,
    onRejected?: (error: AxiosError) => any
  ): number {
    return apiClient.interceptors.request.use(onFulfilled, onRejected)
  }

  addResponseInterceptor(
    onFulfilled?: (response: AxiosResponse) => AxiosResponse,
    onRejected?: (error: AxiosError) => any
  ): number {
    return apiClient.interceptors.response.use(onFulfilled, onRejected)
  }

  // 인터셉터 제거
  removeRequestInterceptor(id: number): void {
    apiClient.interceptors.request.eject(id)
  }

  removeResponseInterceptor(id: number): void {
    apiClient.interceptors.response.eject(id)
  }
}

// =============================================================================
// 🏭 API 클라이언트 인스턴스
// =============================================================================
export const api = new ApiClient()

// =============================================================================
// 🔄 캐싱 유틸리티
// =============================================================================
export class ApiCache {
  private cache = new Map<string, { data: any; timestamp: number; ttl: number }>()

  set(key: string, data: any, ttl: number = 300000): void { // 5분 기본
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttl
    })
  }

  get(key: string): any | null {
    const cached = this.cache.get(key)
    if (!cached) return null

    if (Date.now() - cached.timestamp > cached.ttl) {
      this.cache.delete(key)
      return null
    }

    return cached.data
  }

  delete(key: string): void {
    this.cache.delete(key)
  }

  clear(): void {
    this.cache.clear()
  }
}

export const apiCache = new ApiCache()

// =============================================================================
// 🔄 재시도 유틸리티
// =============================================================================
export const withRetry = async <T>(
  apiCall: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> => {
  let lastError: any

  for (let i = 0; i <= maxRetries; i++) {
    try {
      return await apiCall()
    } catch (error) {
      lastError = error
      
      if (i === maxRetries) break
      
      // 지수 백오프
      const backoffDelay = delay * Math.pow(2, i)
      await new Promise(resolve => setTimeout(resolve, backoffDelay))
      
      console.warn(`Retry ${i + 1}/${maxRetries} after ${backoffDelay}ms`, error)
    }
  }

  throw lastError
}

export default api