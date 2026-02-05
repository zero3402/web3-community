// =============================================================================
// 🔐 인증 스토어 - Pinia
// =============================================================================
// 설명: 사용자 인증 상태 관리
// 특징: 로그인, 로그아웃, 토큰 관리, 권한 확인
// 목적: 전역 인증 상태 및 세션 관리
// =============================================================================

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import { apiCache } from '@/utils/api'
import type { 
  UserProfile, 
  AuthTokens, 
  LoginCredentials, 
  RegisterData,
  SecuritySettings 
} from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  // =============================================================================
  // 🔄 상태 정의
  // =============================================================================
  const router = useRouter()

  // 사용자 상태
  const user = ref<UserProfile | null>(null)
  const isLoading = ref(false)
  const isInitialized = ref(false)

  // 토큰 상태
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const tokenExpiresAt = ref<number | null>(null)

  // 보안 설정
  const securitySettings = ref<SecuritySettings | null>(null)

  // =============================================================================
  // 🎯 게터 (Computed)
  // =============================================================================
  const isAuthenticated = computed(() => {
    return !!(accessToken.value && user.value)
  })

  const isActiveUser = computed(() => {
    return user.value?.status === 'active'
  })

  const isEmailVerified = computed(() => {
    return user.value?.isEmailVerified ?? false
  })

  const userRole = computed(() => {
    return user.value?.role || 'user'
  })

  const isAdmin = computed(() => {
    return userRole.value === 'admin'
  })

  const isModerator = computed(() => {
    return ['admin', 'moderator'].includes(userRole.value)
  })

  const userName = computed(() => {
    return user.value?.username || 'Guest'
  })

  const userAvatar = computed(() => {
    return user.value?.avatar || '/default-avatar.png'
  })

  const hasPermission = computed(() => {
    return (permission: string) => {
      if (!user.value) return false
      
      // 관리자는 모든 권한 가짐
      if (isAdmin.value) return true
      
      // 기본 권한 체크
      const userPermissions = getRolePermissions(userRole.value)
      return userPermissions.includes(permission)
    }
  })

  const isTokenExpired = computed(() => {
    if (!tokenExpiresAt.value) return true
    return Date.now() >= tokenExpiresAt.value
  })

  const shouldRefreshToken = computed(() => {
    if (!tokenExpiresAt.value) return true
    // 토큰 만료 5분 전 리프레시
    const refreshThreshold = 5 * 60 * 1000 // 5분
    return Date.now() >= (tokenExpiresAt.value - refreshThreshold)
  })

  // =============================================================================
  // 🔑 토큰 관리 메소드
  // =============================================================================
  const setTokens = (tokens: AuthTokens): void => {
    accessToken.value = tokens.accessToken
    refreshToken.value = tokens.refreshToken
    tokenExpiresAt.value = Date.now() + (tokens.expiresIn * 1000)

    // 로컬 스토리지에 저장
    localStorage.setItem('accessToken', tokens.accessToken)
    localStorage.setItem('refreshToken', tokens.refreshToken)
    localStorage.setItem('tokenExpiresAt', tokenExpiresAt.value.toString())
  }

  const clearTokens = (): void => {
    accessToken.value = null
    refreshToken.value = null
    tokenExpiresAt.value = null

    // 로컬 스토리지에서 제거
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('tokenExpiresAt')
  }

  const loadTokensFromStorage = (): void => {
    accessToken.value = localStorage.getItem('accessToken')
    refreshToken.value = localStorage.getItem('refreshToken')
    const expiresAt = localStorage.getItem('tokenExpiresAt')
    tokenExpiresAt.value = expiresAt ? parseInt(expiresAt) : null
  }

  // =============================================================================
  // 🔄 인증 API 메소드
  // =============================================================================
  const login = async (credentials: LoginCredentials): Promise<void> => {
    try {
      isLoading.value = true

      const response = await api.post<AuthTokens>('/auth/login', credentials, {
        showError: false
      })

      if (response.success && response.data) {
        // 토큰 저장
        setTokens(response.data)

        // 사용자 정보 가져오기
        await fetchUserProfile()

        ElMessage.success('로그인되었습니다.')
        
        // 이전 페이지로 리다이렉트
        const redirect = router.currentRoute.value.query.redirect as string
        router.push(redirect || '/dashboard')
      } else {
        throw new Error(response.error?.message || '로그인 실패')
      }
    } catch (error: any) {
      console.error('Login error:', error)
      
      // 에러 메시지 표시
      const message = error.response?.data?.message || 
                     error.message || 
                     '로그인 중 오류가 발생했습니다.'
      ElMessage.error(message)
      
      throw error
    } finally {
      isLoading.value = false
    }
  }

  const register = async (data: RegisterData): Promise<void> => {
    try {
      isLoading.value = true

      const response = await api.post<AuthTokens>('/auth/register', data, {
        showError: false
      })

      if (response.success && response.data) {
        // 토큰 저장
        setTokens(response.data)

        // 사용자 정보 가져오기
        await fetchUserProfile()

        ElMessage.success('회원가입이 완료되었습니다.')
        router.push('/dashboard')
      } else {
        throw new Error(response.error?.message || '회원가입 실패')
      }
    } catch (error: any) {
      console.error('Register error:', error)
      
      const message = error.response?.data?.message || 
                     error.message || 
                     '회원가입 중 오류가 발생했습니다.'
      ElMessage.error(message)
      
      throw error
    } finally {
      isLoading.value = false
    }
  }

  const logout = async (): Promise<void> => {
    try {
      // 서버에 로그아웃 통지
      if (accessToken.value) {
        await api.post('/auth/logout').catch(() => {
          // 에러가 발생해도 로그아웃은 계속 진행
        })
      }
    } catch (error) {
      console.error('Logout API error:', error)
    } finally {
      // 로컬 상태 정리
      clearTokens()
      user.value = null
      securitySettings.value = null

      // 캐시 정리
      apiCache.clear()

      ElMessage.success('로그아웃되었습니다.')
      router.push('/auth/login')
    }
  }

  const refreshAccessToken = async (): Promise<void> => {
    if (!refreshToken.value) {
      throw new Error('No refresh token available')
    }

    try {
      const response = await api.post<AuthTokens>('/auth/refresh', {
        refreshToken: refreshToken.value
      }, {
        showError: false
      })

      if (response.success && response.data) {
        setTokens(response.data)
      } else {
        throw new Error('Token refresh failed')
      }
    } catch (error) {
      console.error('Token refresh error:', error)
      throw error
    }
  }

  const fetchUserProfile = async (): Promise<void> => {
    try {
      const response = await api.get<UserProfile>('/auth/profile')

      if (response.success && response.data) {
        user.value = response.data
        
        // 캐시에 저장
        apiCache.set('userProfile', response.data, 300000) // 5분
      }
    } catch (error) {
      console.error('Fetch profile error:', error)
      throw error
    }
  }

  const updateUserProfile = async (updates: Partial<UserProfile>): Promise<void> => {
    try {
      isLoading.value = true

      const response = await api.put<UserProfile>('/auth/profile', updates)

      if (response.success && response.data) {
        user.value = response.data
        
        // 캐시 업데이트
        apiCache.set('userProfile', response.data, 300000)
        
        ElMessage.success('프로필이 업데이트되었습니다.')
      }
    } catch (error) {
      console.error('Update profile error:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // =============================================================================
  // 🔐 비밀번호 관리
  // =============================================================================
  const changePassword = async (currentPassword: string, newPassword: string): Promise<void> => {
    try {
      await api.post('/auth/change-password', {
        currentPassword,
        newPassword
      })

      ElMessage.success('비밀번호가 변경되었습니다.')
    } catch (error) {
      console.error('Change password error:', error)
      throw error
    }
  }

  const forgotPassword = async (email: string): Promise<void> => {
    try {
      await api.post('/auth/forgot-password', { email })
      
      ElMessage.success('비밀번호 재설정 이메일이 발송되었습니다.')
    } catch (error) {
      console.error('Forgot password error:', error)
      throw error
    }
  }

  const resetPassword = async (token: string, newPassword: string): Promise<void> => {
    try {
      await api.post('/auth/reset-password', {
        token,
        newPassword
      })

      ElMessage.success('비밀번호가 재설정되었습니다.')
    } catch (error) {
      console.error('Reset password error:', error)
      throw error
    }
  }

  // =============================================================================
  // 🌐 소셜 로그인
  // =============================================================================
  const socialLogin = async (provider: string, code: string): Promise<void> => {
    try {
      isLoading.value = true

      const response = await api.post<AuthTokens>('/auth/social/login', {
        provider,
        code
      })

      if (response.success && response.data) {
        setTokens(response.data)
        await fetchUserProfile()
        
        ElMessage.success('소셜 로그인되었습니다.')
        router.push('/dashboard')
      }
    } catch (error) {
      console.error('Social login error:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // =============================================================================
  // 🔍 초기화
  // =============================================================================
  const initialize = async (): Promise<void> => {
    if (isInitialized.value) return

    try {
      loadTokensFromStorage()

      if (accessToken.value && !isTokenExpired.value) {
        // 토큰이 유효하면 프로필 가져오기
        await fetchUserProfile()
        
        // 보안 설정 가져오기
        await fetchSecuritySettings()
      } else if (shouldRefreshToken.value && refreshToken.value) {
        // 토큰 리프레시 필요
        await refreshAccessToken()
        await fetchUserProfile()
      } else {
        clearTokens()
      }
    } catch (error) {
      console.error('Auth initialization error:', error)
      clearTokens()
    } finally {
      isInitialized.value = true
    }
  }

  const fetchSecuritySettings = async (): Promise<void> => {
    try {
      const response = await api.get<SecuritySettings>('/auth/security')

      if (response.success && response.data) {
        securitySettings.value = response.data
      }
    } catch (error) {
      console.error('Fetch security settings error:', error)
    }
  }

  // =============================================================================
  // 🔍 유틸리티 함수
  // =============================================================================
  const getRolePermissions = (role: string): string[] => {
    const permissions: Record<string, string[]> = {
      user: [
        'read:posts',
        'write:posts',
        'read:comments',
        'write:comments',
        'update:profile'
      ],
      moderator: [
        'read:posts',
        'write:posts',
        'read:comments',
        'write:comments',
        'update:profile',
        'moderate:posts',
        'moderate:comments',
        'read:analytics'
      ],
      admin: [
        '*' // 모든 권한
      ]
    }

    return permissions[role] || []
  }

  const canAccessRoute = (route: any): boolean => {
    if (!route.meta?.requiresAuth) return true
    if (!isAuthenticated.value) return false

    // 권한 체크
    const requiredPermissions = route.meta?.permissions
    if (requiredPermissions) {
      return requiredPermissions.every((permission: string) => 
        hasPermission.value(permission)
      )
    }

    // 역할 체크
    const requiredRoles = route.meta?.roles
    if (requiredRoles) {
      return requiredRoles.includes(userRole.value)
    }

    return true
  }

  // =============================================================================
  // 🔄 상태 반환
  // =============================================================================
  return {
    // 상태
    user,
    isLoading,
    isInitialized,
    accessToken,
    refreshToken,
    securitySettings,

    // 게터
    isAuthenticated,
    isActiveUser,
    isEmailVerified,
    userRole,
    isAdmin,
    isModerator,
    userName,
    userAvatar,
    hasPermission,
    isTokenExpired,
    shouldRefreshToken,

    // 메소드
    login,
    register,
    logout,
    refreshAccessToken,
    fetchUserProfile,
    updateUserProfile,
    changePassword,
    forgotPassword,
    resetPassword,
    socialLogin,
    initialize,
    canAccessRoute,
    fetchSecuritySettings,
    setTokens,
    clearTokens
  }
})