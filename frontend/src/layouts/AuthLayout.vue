<template>
  <div class="auth-layout">
    <!-- 배경 그라데이션 -->
    <div class="auth-background">
      <div class="bg-gradient-overlay"></div>
    </div>

    <!-- 메인 컨텐츠 -->
    <div class="auth-container">
      <!-- 로고 및 브랜드 -->
      <div class="auth-header">
        <div class="logo-container">
          <img src="/logo.svg" alt="Web3 Community" class="logo" />
          <h1 class="brand-title">Web3 Community</h1>
        </div>
        <p class="brand-subtitle">탈중앙화된 커뮤니티 플랫폼</p>
      </div>

      <!-- 라우터 뷰 -->
      <div class="auth-content">
        <router-view v-slot="{ Component, route }">
          <transition :name="route.meta.transition || 'fade'" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </div>

      <!-- 푸터 -->
      <div class="auth-footer">
        <div class="footer-links">
          <router-link to="/about" class="footer-link">소개</router-link>
          <span class="separator">·</span>
          <router-link to="/terms" class="footer-link">이용약관</router-link>
          <span class="separator">·</span>
          <router-link to="/privacy" class="footer-link">개인정보처리방침</router-link>
          <span class="separator">·</span>
          <router-link to="/help" class="footer-link">도움말</router-link>
        </div>
        <p class="copyright">© 2024 Web3 Community. All rights reserved.</p>
      </div>
    </div>

    <!-- 데코레이션 요소 -->
    <div class="decoration-elements">
      <div class="decoration-circle decoration-circle-1"></div>
      <div class="decoration-circle decoration-circle-2"></div>
      <div class="decoration-circle decoration-circle-3"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
// =============================================================================
// 🎨 인증 레이아웃 컴포넌트
// =============================================================================
// 설명: 로그인, 회원가입 등 인증 관련 페이지의 레이아웃
// 특징: 반응형 디자인, 애니메이션, 브랜딩
// 목적: 일관된 인증 페이지 UI 제공
// =============================================================================

import { onMounted } from 'vue'

// =============================================================================
// 🎯 컴포넌트 로직
// =============================================================================
onMounted(() => {
  // 페이지 로드 시 초기화
  document.body.classList.add('auth-page')
  
  // 테마 설정 (항상 라이트 모드)
  document.documentElement.classList.remove('dark')
})

// 컴포넌트 언마운트 시 클래스 제거
onUnmounted(() => {
  document.body.classList.remove('auth-page')
})
</script>

<style scoped>
/* =============================================================================
// 🎨 인증 레이아웃 스타일
// ============================================================================= */
.auth-layout {
  @apply min-h-screen flex items-center justify-center relative overflow-hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* =============================================================================
// 🌅 배경 그라데이션
// ============================================================================= */
.auth-background {
  @apply absolute inset-0;
  background: url('/auth-bg.jpg') center/cover no-repeat;
}

.bg-gradient-overlay {
  @apply absolute inset-0;
  background: linear-gradient(135deg, 
    rgba(102, 126, 234, 0.9) 0%, 
    rgba(118, 75, 162, 0.9) 50%,
    rgba(237, 66, 100, 0.8) 100%
  );
}

/* =============================================================================
// 📦 컨테이너
// ============================================================================= */
.auth-container {
  @apply relative z-10 w-full max-w-md mx-auto px-4 py-8;
}

/* =============================================================================
// 🎯 헤더
// ============================================================================= */
.auth-header {
  @apply text-center mb-8 text-white;
}

.logo-container {
  @apply flex items-center justify-center mb-4;
}

.logo {
  @apply w-12 h-12 mr-3;
  filter: brightness(0) invert(1);
}

.brand-title {
  @apply text-2xl font-bold;
  background: linear-gradient(to right, #ffffff, #f0f0f0);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-subtitle {
  @apply text-sm opacity-90;
  color: rgba(255, 255, 255, 0.8);
}

/* =============================================================================
// 📄 컨텐츠
// ============================================================================= */
.auth-content {
  @apply bg-white dark:bg-gray-800 rounded-2xl shadow-2xl p-6 sm:p-8;
}

/* =============================================================================
// 📱 푸터
// ============================================================================= */
.auth-footer {
  @apply mt-8 text-center text-white text-sm;
}

.footer-links {
  @apply mb-2;
}

.footer-link {
  @apply hover:underline transition-colors duration-200;
  color: rgba(255, 255, 255, 0.8);
}

.footer-link:hover {
  color: rgba(255, 255, 255, 1);
}

.separator {
  @apply mx-2;
  color: rgba(255, 255, 255, 0.6);
}

.copyright {
  @apply opacity-70 text-xs;
  color: rgba(255, 255, 255, 0.7);
}

/* =============================================================================
// 🎨 데코레이션 요소
// ============================================================================= */
.decoration-elements {
  @apply absolute inset-0 pointer-events-none;
}

.decoration-circle {
  @apply absolute rounded-full opacity-20;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
}

.decoration-circle-1 {
  @apply w-64 h-64;
  top: -10%;
  left: -10%;
  animation: float 6s ease-in-out infinite;
}

.decoration-circle-2 {
  @apply w-48 h-48;
  top: 20%;
  right: -5%;
  animation: float 8s ease-in-out infinite reverse;
}

.decoration-circle-3 {
  @apply w-32 h-32;
  bottom: 10%;
  left: 10%;
  animation: float 4s ease-in-out infinite;
}

/* =============================================================================
// 🎭 애니메이션
// ============================================================================= */
@keyframes float {
  0%, 100% {
    transform: translateY(0px) scale(1);
  }
  50% {
    transform: translateY(-20px) scale(1.05);
  }
}

/* =============================================================================
// 📱 반응형 디자인
// ============================================================================= */
@media (max-width: 640px) {
  .auth-container {
    @apply px-4 py-6;
  }
  
  .auth-content {
    @apply p-4;
  }
  
  .brand-title {
    @apply text-xl;
  }
  
  .logo {
    @apply w-10 h-10;
  }
}

@media (max-width: 400px) {
  .auth-header {
    @apply mb-6;
  }
  
  .footer-links {
    @apply flex flex-wrap justify-center items-center gap-2;
  }
  
  .separator {
    @apply hidden;
  }
}

/* =============================================================================
// 🌙 다크 모드
// ============================================================================= */
@media (prefers-color-scheme: dark) {
  .auth-layout {
    background: linear-gradient(135deg, #1e3a8a 0%, #581c87 100%);
  }
  
  .bg-gradient-overlay {
    background: linear-gradient(135deg, 
      rgba(30, 58, 138, 0.9) 0%, 
      rgba(88, 28, 135, 0.9) 50%,
      rgba(127, 29, 29, 0.8) 100%
    );
  }
  
  .brand-title {
    background: linear-gradient(to right, #f3f4f6, #e5e7eb);
    background-clip: text;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

/* =============================================================================
// 🔄 트랜지션
// ============================================================================= */
.fade-enter-active,
.fade-leave-active {
  @apply transition-all duration-300 ease-in-out;
}

.fade-enter-from {
  @apply opacity-0 transform translate-y-4;
}

.fade-leave-to {
  @apply opacity-0 transform -translate-y-4;
}

.slide-up-enter-active,
.slide-up-leave-active {
  @apply transition-all duration-300 ease-in-out;
}

.slide-up-enter-from {
  @apply opacity-0 transform translate-y-8;
}

.slide-up-leave-to {
  @apply opacity-0 transform -translate-y-8;
}

/* =============================================================================
// 🎨 로딩 상태
// ============================================================================= */
.auth-page .auth-content {
  @apply relative;
}

.auth-page .loading-overlay {
  @apply absolute inset-0 bg-white dark:bg-gray-800 bg-opacity-90 rounded-2xl flex items-center justify-center;
  z-index: 50;
}

/* =============================================================================
// 🎯 접근성
// ============================================================================= */
@media (prefers-reduced-motion: reduce) {
  .decoration-circle {
    animation: none;
  }
  
  .fade-enter-active,
  .fade-leave-active,
  .slide-up-enter-active,
  .slide-up-leave-active {
    transition: none;
  }
}

/* =============================================================================
// 🖨️ 프린트 스타일
// ============================================================================= */
@media print {
  .auth-layout {
    @apply bg-white;
  }
  
  .auth-background,
  .decoration-elements {
    display: none;
  }
  
  .auth-container {
    max-width: none;
    @apply p-0;
  }
  
  .auth-content {
    @apply shadow-none border border-gray-300;
  }
  
  .auth-footer {
    display: none;
  }
}
</style>