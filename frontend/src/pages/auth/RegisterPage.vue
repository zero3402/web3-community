<template>
  <div class="register-page">
    <div class="register-form">
      <!-- 회원가입 헤더 -->
      <div class="form-header">
        <h2 class="form-title">회원가입</h2>
        <p class="form-subtitle">Web3 Community 커뮤니티에 참여하세요</p>
      </div>

      <!-- 회원가입 폼 -->
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        @submit.prevent="handleRegister"
        label-position="top"
        size="large"
        class="register-form-element"
      >
        <!-- 사용자 이름 -->
        <el-form-item label="사용자 이름" prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="영문, 숫자 조합 3-20자"
            :prefix-icon="User"
            :disabled="isLoading"
            autocomplete="username"
            @blur="checkUsername"
          />
          <div v-if="usernameStatus" class="username-status" :class="usernameStatus.type">
            <Icon :icon="usernameStatus.icon" class="status-icon" />
            {{ usernameStatus.message }}
          </div>
        </el-form-item>

        <!-- 이메일 -->
        <el-form-item label="이메일" prop="email">
          <el-input
            v-model="registerForm.email"
            type="email"
            placeholder="example@email.com"
            :prefix-icon="Message"
            :disabled="isLoading"
            autocomplete="email"
            @blur="checkEmail"
          />
          <div v-if="emailStatus" class="email-status" :class="emailStatus.type">
            <Icon :icon="emailStatus.icon" class="status-icon" />
            {{ emailStatus.message }}
          </div>
        </el-form-item>

        <!-- 비밀번호 -->
        <el-form-item label="비밀번호" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="8자 이상, 영문/숫자/특수문자 포함"
            :prefix-icon="Lock"
            :disabled="isLoading"
            show-password
            autocomplete="new-password"
            @input="checkPasswordStrength"
          />
          <div class="password-strength">
            <div class="strength-bar">
              <div 
                class="strength-fill" 
                :class="passwordStrength.level"
                :style="{ width: passwordStrength.percentage + '%' }"
              ></div>
            </div>
            <span class="strength-text" :class="passwordStrength.level">
              {{ passwordStrength.text }}
            </span>
          </div>
        </el-form-item>

        <!-- 비밀번호 확인 -->
        <el-form-item label="비밀번호 확인" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="비밀번호를 다시 입력하세요"
            :prefix-icon="Lock"
            :disabled="isLoading"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>

        <!-- 약관 동의 -->
        <div class="terms-section">
          <el-form-item prop="agreeToTerms" class="terms-item">
            <el-checkbox v-model="registerForm.agreeToTerms" :disabled="isLoading">
              <span class="terms-text">
                <router-link to="/terms" target="_blank" class="terms-link">이용약관</router-link>에
                동의합니다
              </span>
            </el-checkbox>
          </el-form-item>

          <el-form-item prop="agreeToPrivacy" class="terms-item">
            <el-checkbox v-model="registerForm.agreeToPrivacy" :disabled="isLoading">
              <span class="terms-text">
                <router-link to="/privacy" target="_blank" class="terms-link">개인정보처리방침</router-link>에
                동의합니다
              </span>
            </el-checkbox>
          </el-form-item>
        </div>

        <!-- 회원가입 버튼 -->
        <el-form-item class="register-button-container">
          <el-button
            type="primary"
            size="large"
            :loading="isLoading"
            :disabled="!isFormValid"
            @click="handleRegister"
            class="register-button"
          >
            {{ isLoading ? '가입 중...' : '회원가입' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 구분선 -->
      <div class="divider">
        <span class="divider-text">또는</span>
      </div>

      <!-- 소셜 회원가입 -->
      <div class="social-register">
        <p class="social-register-title">소셜 계정으로 가입</p>
        <div class="social-buttons">
          <el-button
            size="large"
            :disabled="isLoading"
            @click="handleSocialRegister('google')"
            class="social-button google-button"
          >
            <img src="/icons/google.svg" alt="Google" class="social-icon" />
            Google
          </el-button>
          <el-button
            size="large"
            :disabled="isLoading"
            @click="handleSocialRegister('github')"
            class="social-button github-button"
          >
            <img src="/icons/github.svg" alt="GitHub" class="social-icon" />
            GitHub
          </el-button>
          <el-button
            size="large"
            :disabled="isLoading"
            @click="handleSocialRegister('discord')"
            class="social-button discord-button"
          >
            <img src="/icons/discord.svg" alt="Discord" class="social-icon" />
            Discord
          </el-button>
        </div>
      </div>

      <!-- 로그인 링크 -->
      <div class="login-link">
        <span class="login-text">이미 계정이 있으신가요?</span>
        <router-link to="/auth/login" class="login-button">
          로그인
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Message, Lock } from '@element-plus/icons-vue'
import { Icon } from '@iconify/vue'
import { useAuthStore } from '@/stores/auth'
import type { RegisterData } from '@/types/api'
import api from '@/utils/api'

// =============================================================================
// 🎯 컴포넌트 상태
// =============================================================================
const router = useRouter()
const authStore = useAuthStore()

// 폼 참조
const registerFormRef = ref<FormInstance>()

// 폼 데이터
const registerForm = ref<RegisterData>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreeToTerms: false,
  agreeToPrivacy: false
})

// 로딩 상태
const isLoading = ref(false)

// 실시간 검증 상태
const usernameStatus = ref<{ type: string; message: string; icon: string } | null>(null)
const emailStatus = ref<{ type: string; message: string; icon: string } | null>(null)
const passwordStrength = ref({
  level: 'weak',
  percentage: 0,
  text: '약함'
})

// =============================================================================
// 📋 폼 검증 규칙
// =============================================================================
const registerRules: FormRules<RegisterData> = {
  username: [
    { required: true, message: '사용자 이름을 입력해주세요.', trigger: 'blur' },
    { 
      min: 3, 
      max: 20, 
      message: '사용자 이름은 3-20자 사이여야 합니다.', 
      trigger: ['blur', 'change'] 
    },
    { 
      pattern: /^[a-zA-Z0-9_]+$/, 
      message: '사용자 이름은 영문, 숫자, 언더스코어만 사용 가능합니다.', 
      trigger: ['blur', 'change'] 
    }
  ],
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
      min: 8, 
      message: '비밀번호는 최소 8자 이상이어야 합니다.', 
      trigger: ['blur', 'change'] 
    },
    {
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/,
      message: '비밀번호는 영문 대/소문자, 숫자, 특수문자를 포함해야 합니다.',
      trigger: ['blur', 'change']
    }
  ],
  confirmPassword: [
    { required: true, message: '비밀번호 확인을 입력해주세요.', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.value.password) {
          callback(new Error('비밀번호가 일치하지 않습니다.'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change']
    }
  ],
  agreeToTerms: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error('이용약관에 동의해주세요.'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  agreeToPrivacy: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error('개인정보처리방침에 동의해주세요.'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// =============================================================================
// 🎯 컴퓨티드 프로퍼티
// =============================================================================
const isFormValid = computed(() => {
  return registerForm.value.username.length >= 3 &&
         registerForm.value.username.length <= 20 &&
         /^[a-zA-Z0-9_]+$/.test(registerForm.value.username) &&
         /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.value.email) &&
         registerForm.value.password.length >= 8 &&
         /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/.test(registerForm.value.password) &&
         registerForm.value.password === registerForm.value.confirmPassword &&
         registerForm.value.agreeToTerms &&
         registerForm.value.agreeToPrivacy &&
         !usernameStatus.value?.type.includes('error') &&
         !emailStatus.value?.type.includes('error')
})

// =============================================================================
// 🔍 실시간 검증
// =============================================================================
const checkUsername = async (): Promise<void> => {
  const username = registerForm.value.username
  
  if (!username || username.length < 3) {
    usernameStatus.value = null
    return
  }

  if (!/^[a-zA-Z0-9_]+$/.test(username)) {
    usernameStatus.value = {
      type: 'error',
      message: '영문, 숫자, 언더스코어만 사용 가능합니다',
      icon: 'mdi:alert-circle'
    }
    return
  }

  try {
    const response = await api.get(`/auth/check-username?username=${username}`)
    
    if (response.data?.available) {
      usernameStatus.value = {
        type: 'success',
        message: '사용 가능한 사용자 이름입니다',
        icon: 'mdi:check-circle'
      }
    } else {
      usernameStatus.value = {
        type: 'error',
        message: '이미 사용 중인 사용자 이름입니다',
        icon: 'mdi:close-circle'
      }
    }
  } catch (error) {
    // 에러 발생 시 검증하지 않음
    usernameStatus.value = null
  }
}

const checkEmail = async (): Promise<void> => {
  const email = registerForm.value.email
  
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    emailStatus.value = null
    return
  }

  try {
    const response = await api.get(`/auth/check-email?email=${email}`)
    
    if (response.data?.available) {
      emailStatus.value = {
        type: 'success',
        message: '사용 가능한 이메일입니다',
        icon: 'mdi:check-circle'
      }
    } else {
      emailStatus.value = {
        type: 'error',
        message: '이미 가입된 이메일입니다',
        icon: 'mdi:close-circle'
      }
    }
  } catch (error) {
    // 에러 발생 시 검증하지 않음
    emailStatus.value = null
  }
}

const checkPasswordStrength = (password: string): void => {
  if (!password) {
    passwordStrength.value = {
      level: 'weak',
      percentage: 0,
      text: '암함'
    }
    return
  }

  let strength = 0
  
  // 길이 체크
  if (password.length >= 8) strength += 20
  if (password.length >= 12) strength += 10
  
  // 복잡도 체크
  if (/[a-z]/.test(password)) strength += 15
  if (/[A-Z]/.test(password)) strength += 15
  if (/\d/.test(password)) strength += 15
  if (/[@$!%*?&]/.test(password)) strength += 15
  if (/[^a-zA-Z\d@$!%*?&]/.test(password)) strength += 10

  // 강도 레벨 결정
  if (strength < 40) {
    passwordStrength.value = { level: 'weak', percentage: strength, text: '약함' }
  } else if (strength < 70) {
    passwordStrength.value = { level: 'medium', percentage: strength, text: '보통' }
  } else if (strength < 90) {
    passwordStrength.value = { level: 'strong', percentage: strength, text: '강함' }
  } else {
    passwordStrength.value = { level: 'very-strong', percentage: 100, text: '매우 강함' }
  }
}

// =============================================================================
// 📝 회원가입 처리
// =============================================================================
const handleRegister = async (): Promise<void> => {
  if (!registerFormRef.value) return

  try {
    // 폼 검증
    const isValid = await registerFormRef.value.validate()
    if (!isValid) return

    isLoading.value = true

    // 회원가입 요청
    await authStore.register(registerForm.value)
    
    // 성공 시 AuthStore에서 처리
  } catch (error: any) {
    console.error('Register error:', error)
    
    // 에러 메시지 처리 (AuthStore에서 표시)
    if (!error.response?.data?.message) {
      ElMessage.error('회원가입 중 오류가 발생했습니다. 다시 시도해주세요.')
    }
  } finally {
    isLoading.value = false
  }
}

// =============================================================================
// 🌐 소셜 회원가입
// =============================================================================
const handleSocialRegister = async (provider: string): Promise<void> => {
  if (isLoading.value) return

  try {
    isLoading.value = true

    // 소셜 로그인 URL 생성
    const redirectUri = `${window.location.origin}/auth/social/${provider}/callback`
    const socialAuthUrl = getSocialAuthUrl(provider, redirectUri)

    // 팝업 열기
    const popup = window.open(
      socialAuthUrl,
      'socialRegister',
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
          reject(new Error('소셜 회원가입이 취소되었습니다.'))
        }
      }, 1000)

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
    console.error('Social register error:', error)
    ElMessage.error(error.message || '소셜 회원가입 중 오류가 발생했습니다.')
  } finally {
    isLoading.value = false
  }
}

// =============================================================================
// 🔧 소셜 인증 URL 생성
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
    router.push('/dashboard')
    return
  }

  // 자동 포커스
  setTimeout(() => {
    const usernameInput = document.querySelector('input[placeholder*="사용자 이름"]') as HTMLInputElement
    usernameInput?.focus()
  }, 100)
})
</script>

<style scoped>
/* =============================================================================
// 🎨 회원가입 페이지 스타일
// ============================================================================= */
.register-page {
  @apply w-full;
}

.register-form {
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
.register-form-element {
  @apply space-y-4;
}

.register-form-element :deep(.el-form-item__label) {
  @apply text-sm font-medium text-gray-700 dark:text-gray-300;
}

.register-form-element :deep(.el-input__wrapper) {
  @apply rounded-lg border-gray-300 dark:border-gray-600;
}

.register-form-element :deep(.el-input__wrapper:hover) {
  @apply border-primary-500 dark:border-primary-400;
}

.register-form-element :deep(.el-input__wrapper.is-focus) {
  @apply border-primary-500 dark:border-primary-400 shadow-sm;
}

/* =============================================================================
// ✅ 실시간 검증 상태
// ============================================================================= */
.username-status,
.email-status {
  @apply flex items-center gap-2 text-xs mt-1;
}

.status-icon {
  @apply w-4 h-4;
}

.username-status.success,
.email-status.success {
  @apply text-green-600 dark:text-green-400;
}

.username-status.error,
.email-status.error {
  @apply text-red-600 dark:text-red-400;
}

/* =============================================================================
// 🔒 비밀번호 강도
// ============================================================================= */
.password-strength {
  @apply mt-2 space-y-2;
}

.strength-bar {
  @apply w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden;
}

.strength-fill {
  @apply h-full transition-all duration-300 ease-in-out;
}

.strength-fill.weak {
  @apply bg-red-500;
}

.strength-fill.medium {
  @apply bg-yellow-500;
}

.strength-fill.strong {
  @apply bg-blue-500;
}

.strength-fill.very-strong {
  @apply bg-green-500;
}

.strength-text {
  @apply text-xs font-medium;
}

.strength-text.weak {
  @apply text-red-600 dark:text-red-400;
}

.strength-text.medium {
  @apply text-yellow-600 dark:text-yellow-400;
}

.strength-text.strong {
  @apply text-blue-600 dark:text-blue-400;
}

.strength-text.very-strong {
  @apply text-green-600 dark:text-green-400;
}

/* =============================================================================
// 📋 약관 섹션
// ============================================================================= */
.terms-section {
  @apply space-y-3 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg;
}

.terms-item {
  @apply mb-0;
}

.terms-text {
  @apply text-sm text-gray-700 dark:text-gray-300;
}

.terms-link {
  @apply text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300 underline;
}

/* =============================================================================
// 🎯 회원가입 버튼
// ============================================================================= */
.register-button-container {
  @apply mb-0;
}

.register-button {
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
// 🌐 소셜 회원가입
// ============================================================================= */
.social-register {
  @apply space-y-4;
}

.social-register-title {
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
// 🔗 로그인 링크
// ============================================================================= */
.login-link {
  @apply text-center pt-4 border-t border-gray-200 dark:border-gray-700;
}

.login-text {
  @apply text-sm text-gray-600 dark:text-gray-400;
}

.login-button {
  @apply text-sm font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300 ml-1;
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
  
  .terms-section {
    @apply p-3;
  }
}
</style>