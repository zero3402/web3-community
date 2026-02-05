<template>
  <div class="login-page">
    <div class="login-form">
      <!-- 로그인 헤더 -->
      <div class="form-header">
        <h2 class="form-title">로그인</h2>
        <p class="form-subtitle">Web3 Community에 오신 것을 환영합니다</p>
      </div>

      <!-- 로그인 폼 -->
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        @submit.prevent="handleLogin"
        label-position="top"
        size="large"
        class="login-form-element"
      >
        <!-- 이메일 -->
        <el-form-item label="이메일" prop="email">
          <el-input
            v-model="loginForm.email"
            type="email"
            placeholder="example@email.com"
            :prefix-icon="Message"
            :disabled="isLoading"
            autocomplete="email"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!-- 비밀번호 -->
        <el-form-item label="비밀번호" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="비밀번호를 입력하세요"
            :prefix-icon="Lock"
            :disabled="isLoading"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!-- 추가 옵션 -->
        <div class="form-options">
          <el-checkbox v-model="loginForm.rememberMe" :disabled="isLoading">
            로그인 상태 유지
          </el-checkbox>
          <router-link 
            to="/auth/forgot-password" 
            class="forgot-password-link"
          >
            비밀번호를 잊으셨나요?
          </router-link>
        </div>

        <!-- 로그인 버튼 -->
        <el-form-item class="login-button-container">
          <el-button
            type="primary"
            size="large"
            :loading="isLoading"
            :disabled="!isFormValid"
            @click="handleLogin"
            class="login-button"
          >
            {{ isLoading ? '로그인 중...' : '로그인' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 구분선 -->
      <div class="divider">
        <span class="divider-text">또는</span>
      </div>

      <!-- 소셜 로그인 -->
      <div class="social-login">
        <p class="social-login-title">소셜 계정으로 로그인</p>
        <div class="social-buttons">
          <el-button
            size="large"
            :disabled="isLoading"
            @click="handleSocialLogin('google')"
            class="social-button google-button"
          >
            <img src="/icons/google.svg" alt="Google" class="social-icon" />
            Google
          </el-button>
          <el-button
            size="large"
            :disabled="isLoading"
            @click="handleSocialLogin('github')"
            class="social-button github-button"
          >
            <img src="/icons/github.svg" alt="GitHub" class="social-icon" />
            GitHub
          </el-button>
          <el-button
            size="large"
            :disabled="isLoading"
            @click="handleSocialLogin('discord')"
            class="social-button discord-button"
          >
            <img src="/icons/discord.svg" alt="Discord" class="social-icon" />
            Discord
          </el-button>
        </div>
      </div>

      <!-- 회원가입 링크 -->
      <div class="signup-link">
        <span class="signup-text">아직 계정이 없으신가요?</span>
        <router-link to="/auth/register" class="signup-button">
          회원가입
        </router-link>
      </div>
    </div>

    <!-- 디버그 모드 -->
    <div v-if="isDevMode" class="debug-panel">
      <details>
        <summary>디버그 정보</summary>
        <pre>{{ { loginForm, isFormValid, isLoading } }}</pre>
      </details>
    </div>
  </div>
</template>

<script setup lang="ts">
// =============================================================================
// 🔐 로그인 페이지 컴포넌트
// =============================================================================
// 설명: 사용자 로그인 기능 및 소셜 로그인
// 특징: 폼 검증, 소셜 로그인, 반응형 디자인
// 목적: 안전하고 편리한 사용자 인증 제공
// =============================================================================

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Message, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { LoginCredentials } from '@/types/api'

// =============================================================================
// 🎯 컴포넌트 상태
// =============================================================================
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 폼 참조
const loginFormRef = ref<FormInstance>()

// 폼 데이터
const loginForm = ref<LoginCredentials>({
  email: '',
  password: '',
  rememberMe: false
})

// 로딩 상태
const isLoading = ref(false)

// =============================================================================
// 📋 폼 검증 규칙
// =============================================================================
const loginRules: FormRules<LoginCredentials> = {
  email: [
    { required: true, message: '이메일을 입력해주세요.', trigger: 'blur' },
    { 
      type: 'email', 
      message: '유효한 이메일 주소를 입력해주세요.', 
      trigger: ['blur', 'change'] 
    }
  ],
  password: [
    { required: true, message: '비밀번호를 입력해주세요.', trigger: 'blur' },
    { 
      min: 6, 
      message: '비밀번호는 최소 6자 이상이어야 합니다.', 
      trigger: ['blur', 'change'] 
    }
  ]
}

// =============================================================================
// 🎯 컴퓨티드 프로퍼티
// =============================================================================
const isFormValid = computed(() => {
  return loginForm.value.email && 
         loginForm.value.password && 
         loginForm.value.password.length >= 6 &&
         /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(loginForm.value.email)
})

const isDevMode = computed(() => import.meta.env.DEV)

// =============================================================================
// 🔐 로그인 처리
// =============================================================================
const handleLogin = async (): Promise<void> => {
  if (!loginFormRef.value) return

  try {
    // 폼 검증
    const isValid = await loginFormRef.value.validate()
    if (!isValid) return

    isLoading.value = true

    // 로그인 요청
    await authStore.login(loginForm.value)
    
    // 성공 시 AuthStore에서 처리
  } catch (error: any) {
    console.error('Login error:', error)
    
    // 에러 메시지 처리 (AuthStore에서 표시)
    if (!error.response?.data?.message) {
      ElMessage.error('로그인 중 오류가 발생했습니다. 다시 시도해주세요.')
    }
  } finally {
    isLoading.value = false
  }
}

// =============================================================================
// 🌐 소셜 로그인
// =============================================================================
const handleSocialLogin = async (provider: string): Promise<void> => {
  if (isLoading.value) return

  try {
    isLoading.value = true

    // 소셜 로그인 URL 생성
    const redirectUri = `${window.location.origin}/auth/social/${provider}/callback`
    const socialAuthUrl = getSocialAuthUrl(provider, redirectUri)

    // 팝업 열기
    const popup = window.open(
      socialAuthUrl,
      'socialLogin',
      'width=600,height=600,scrollbars=yes,resizable=yes'
    )

    if (!popup) {
      ElMessage.error('팝업이 차단되었습니다. 팝업을 허용해주세요.')
      return
    }

    // OAuth 응답 대기
    await new Promise<void>((resolve, reject) => {
      const checkClosed = setInterval(() => {
        if (popup.closed) {
          clearInterval(checkClosed)
          reject(new Error('소셜 로그인이 취소되었습니다.'))
        }
      }, 1000)

      // 메시지 리스너
      const messageHandler = async (event: MessageEvent) => {
        if (event.origin !== window.location.origin) return

        if (event.data.type === 'SOCIAL_LOGIN_SUCCESS') {
          clearInterval(checkClosed)
          window.removeEventListener('message', messageHandler)
          popup.close()

          try {
            await authStore.socialLogin(provider, event.data.code)
            resolve()
          } catch (error) {
            reject(error)
          }
        } else if (event.data.type === 'SOCIAL_LOGIN_ERROR') {
          clearInterval(checkClosed)
          window.removeEventListener('message', messageHandler)
          popup.close()
          reject(new Error(event.data.message))
        }
      }

      window.addEventListener('message', messageHandler)
    })
  } catch (error: any) {
    console.error('Social login error:', error)
    ElMessage.error(error.message || '소셜 로그인 중 오류가 발생했습니다.')
  } finally {
    isLoading.value = false
  }
}

// =============================================================================
// 🔧 소셜 로그인 URL 생성
// =============================================================================
const getSocialAuthUrl = (provider: string, redirectUri: string): string => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL
  const clientId = getSocialClientId(provider)
  
  const urls = {
    google: `https://accounts.google.com/oauth/authorize?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=openid email profile`,
    github: `https://github.com/login/oauth/authorize?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&scope=user:email`,
    discord: `https://discord.com/oauth2/authorize?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=identify email`
  }

  return urls[provider as keyof typeof urls] || ''
}

const getSocialClientId = (provider: string): string => {
  const clientIds = {
    google: import.meta.env.VITE_GOOGLE_CLIENT_ID,
    github: import.meta.env.VITE_GITHUB_CLIENT_ID,
    discord: import.meta.env.VITE_DISCORD_CLIENT_ID
  }

  return clientIds[provider as keyof typeof clientIds] || ''
}

// =============================================================================
// 🎯 라이프사이클
// =============================================================================
onMounted(() => {
  // 이미 로그인된 경우 리다이렉트
  if (authStore.isAuthenticated) {
    const redirect = route.query.redirect as string
    router.push(redirect || '/dashboard')
    return
  }

  // URL에서 이메일 파라미터 가져오기 (소셜 로그인 후)
  const email = route.query.email as string
  if (email) {
    loginForm.value.email = email
  }

  // 자동 포커스
  if (!loginForm.value.email) {
    setTimeout(() => {
      const emailInput = document.querySelector('input[type="email"]') as HTMLInputElement
      emailInput?.focus()
    }, 100)
  }
})

onUnmounted(() => {
  // 클린업
})
</script>

<style scoped>
/* =============================================================================
// 🎨 로그인 페이지 스타일
// ============================================================================= */
.login-page {
  @apply w-full;
}

.login-form {
  @apply space-y-6;
}

/* =============================================================================
// 📋 폼 헤더
// ============================================================================= */
.form-header {
  @apply text-center mb-6;
}

.form-title {
  @apply text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2;
}

.form-subtitle {
  @apply text-sm text-gray-600 dark:text-gray-400;
}

/* =============================================================================
// 📝 폼 요소
// ============================================================================= */
.login-form-element {
  @apply space-y-4;
}

.login-form-element :deep(.el-form-item__label) {
  @apply text-sm font-medium text-gray-700 dark:text-gray-300;
}

.login-form-element :deep(.el-input__wrapper) {
  @apply rounded-lg border-gray-300 dark:border-gray-600;
}

.login-form-element :deep(.el-input__wrapper:hover) {
  @apply border-primary-500 dark:border-primary-400;
}

.login-form-element :deep(.el-input__wrapper.is-focus) {
  @apply border-primary-500 dark:border-primary-400 shadow-sm;
}

/* =============================================================================
// ⚙️ 폼 옵션
// ============================================================================= */
.form-options {
  @apply flex items-center justify-between text-sm;
}

.forgot-password-link {
  @apply text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300 transition-colors duration-200;
}

/* =============================================================================
// 🎯 로그인 버튼
// ============================================================================= */
.login-button-container {
  @apply mb-0;
}

.login-button {
  @apply w-full py-3 text-base font-medium rounded-lg transition-all duration-200 hover:shadow-lg;
}

/* =============================================================================
// ➖ 구분선
// ============================================================================= */
.divider {
  @apply relative my-6;
}

.divider::before {
  content: '';
  @apply absolute top-1/2 left-0 right-0 h-px bg-gray-300 dark:bg-gray-600;
}

.divider-text {
  @apply relative bg-white dark:bg-gray-800 px-4 text-sm text-gray-500 dark:text-gray-400;
}

/* =============================================================================
// 🌐 소셜 로그인
// ============================================================================= */
.social-login {
  @apply space-y-4;
}

.social-login-title {
  @apply text-center text-sm font-medium text-gray-700 dark:text-gray-300;
}

.social-buttons {
  @apply grid grid-cols-3 gap-3;
}

.social-button {
  @apply flex items-center justify-center gap-2 py-2 px-3 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-all duration-200;
}

.social-icon {
  @apply w-4 h-4;
}

.google-button:hover {
  @apply border-blue-500 bg-blue-50 dark:bg-blue-900/20;
}

.github-button:hover {
  @apply border-gray-900 dark:border-gray-100 bg-gray-100 dark:bg-gray-800;
}

.discord-button:hover {
  @apply border-indigo-500 bg-indigo-50 dark:bg-indigo-900/20;
}

/* =============================================================================
// 🔗 회원가입 링크
// ============================================================================= */
.signup-link {
  @apply text-center pt-4 border-t border-gray-200 dark:border-gray-700;
}

.signup-text {
  @apply text-sm text-gray-600 dark:text-gray-400;
}

.signup-button {
  @apply text-sm font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300 ml-1;
}

/* =============================================================================
// 🐛 디버그 패널
// ============================================================================= */
.debug-panel {
  @apply mt-8 p-4 bg-gray-100 dark:bg-gray-800 rounded-lg text-xs;
}

.debug-panel details {
  @apply cursor-pointer;
}

.debug-panel pre {
  @apply mt-2 overflow-x-auto;
}

/* =============================================================================
// 📱 반응형 디자인
// ============================================================================= */
@media (max-width: 640px) {
  .form-title {
    @apply text-xl;
  }
  
  .social-buttons {
    @apply grid-cols-1 gap-2;
  }
  
  .social-button {
    @apply justify-start py-3;
  }
}

/* =============================================================================
// 🎨 로딩 상태
// ============================================================================= */
.login-form-element :deep(.el-loading-mask) {
  @apply rounded-lg;
}

/* =============================================================================
// 🎯 접근성
// ============================================================================= */
@media (prefers-reduced-motion: reduce) {
  .login-button,
  .social-button {
    transition: none;
  }
}

/* =============================================================================
// 🖨️ 프린트 스타일
// ============================================================================= */
@media print {
  .social-login,
  .debug-panel {
    display: none;
  }
  
  .login-form-element :deep(.el-input__wrapper),
  .login-button {
    @apply border border-gray-300;
  }
}
</style>