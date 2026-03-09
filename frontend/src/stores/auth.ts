import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { TOKEN_KEY, REFRESH_TOKEN_KEY } from '@/api/index'
import type { LoginRequest, RegisterRequest, User } from '@/types'
import api from '@/api/index'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    if (res.data.success && res.data.data) {
      const loginData = res.data.data
      accessToken.value = loginData.accessToken
      localStorage.setItem(TOKEN_KEY, loginData.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, loginData.refreshToken)
      await fetchUser()
    }
  }

  async function register(data: RegisterRequest) {
    const res = await authApi.register(data)
    if (res.data.success && res.data.data) {
      const loginData = res.data.data
      accessToken.value = loginData.accessToken
      localStorage.setItem(TOKEN_KEY, loginData.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, loginData.refreshToken)
      await fetchUser()
    }
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      accessToken.value = null
      user.value = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
    }
  }

  async function fetchUser() {
    try {
      const res = await api.get('/api/users/me')
      if (res.data.success) {
        user.value = res.data.data
      }
    } catch {
      user.value = null
    }
  }

  return { accessToken, user, isAuthenticated, login, register, logout, fetchUser }
})
