import axios from 'axios'
import type { ApiResponse } from '@/types'

const TOKEN_KEY = 'web3_community_token'
const REFRESH_TOKEN_KEY = 'web3_community_refresh_token'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
      if (refreshToken) {
        try {
          const res = await axios.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
            '/auth/refresh',
            { refreshToken }
          )
          if (res.data.success && res.data.data) {
            localStorage.setItem(TOKEN_KEY, res.data.data.accessToken)
            localStorage.setItem(REFRESH_TOKEN_KEY, res.data.data.refreshToken)
            originalRequest.headers.Authorization = `Bearer ${res.data.data.accessToken}`
            return api(originalRequest)
          }
        } catch {
          localStorage.removeItem(TOKEN_KEY)
          localStorage.removeItem(REFRESH_TOKEN_KEY)
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  }
)

export default api
export { TOKEN_KEY, REFRESH_TOKEN_KEY }
