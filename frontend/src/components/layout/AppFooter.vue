<template>
  <footer class="app-footer">
    <div class="footer-container">
      <!-- 푸터 콘텐츠 -->
      <div class="footer-content">
        <!-- 회사 정보 -->
        <div class="footer-section">
          <h3 class="footer-title">Web3 Community</h3>
          <p class="footer-description">
            탈중앙화된 기술로 구축된 차세대 커뮤니티 플랫폼
          </p>
          <div class="footer-social">
            <a href="#" class="social-link" title="Twitter">
              <Icon icon="mdi:twitter" class="social-icon" />
            </a>
            <a href="#" class="social-link" title="GitHub">
              <Icon icon="mdi:github" class="social-icon" />
            </a>
            <a href="#" class="social-link" title="Discord">
              <Icon icon="mdi:discord" class="social-icon" />
            </a>
            <a href="#" class="social-link" title="Telegram">
              <Icon icon="mdi:telegram" class="social-icon" />
            </a>
          </div>
        </div>

        <!-- 네비게이션 -->
        <div class="footer-section">
          <h4 class="footer-subtitle">네비게이션</h4>
          <ul class="footer-links">
            <li>
              <router-link to="/community/posts" class="footer-link">
                커뮤니티
              </router-link>
            </li>
            <li>
              <router-link to="/community/members" class="footer-link">
                멤버
              </router-link>
            </li>
            <li>
              <router-link to="/dashboard" class="footer-link">
                대시보드
              </router-link>
            </li>
            <li>
              <router-link to="/about" class="footer-link">
                소개
              </router-link>
            </li>
          </ul>
        </div>

        <!-- 지원 -->
        <div class="footer-section">
          <h4 class="footer-subtitle">지원</h4>
          <ul class="footer-links">
            <li>
              <router-link to="/help" class="footer-link">
                도움말
              </router-link>
            </li>
            <li>
              <router-link to="/terms" class="footer-link">
                이용약관
              </router-link>
            </li>
            <li>
              <router-link to="/privacy" class="footer-link">
                개인정보처리방침
              </router-link>
            </li>
            <li>
              <a href="mailto:support@web3community.com" class="footer-link">
                문의하기
              </a>
            </li>
          </ul>
        </div>

        <!-- 개발자 -->
        <div class="footer-section">
          <h4 class="footer-subtitle">개발자</h4>
          <ul class="footer-links">
            <li>
              <a href="/api/docs" class="footer-link" target="_blank">
                API 문서
              </a>
            </li>
            <li>
              <a href="https://github.com/web3-community" class="footer-link" target="_blank">
                GitHub
              </a>
            </li>
            <li>
              <a href="/status" class="footer-link" target="_blank">
                서비스 상태
              </a>
            </li>
            <li>
              <a href="/changelog" class="footer-link">
                변경 로그
              </a>
            </li>
          </ul>
        </div>
      </div>

      <!-- 푸터 하단 -->
      <div class="footer-bottom">
        <div class="footer-bottom-left">
          <span class="copyright">
            © 2024 Web3 Community. All rights reserved.
          </span>
        </div>
        
        <div class="footer-bottom-right">
          <span class="footer-text">
            Made with 
            <Icon icon="mdi:heart" class="heart-icon" />
            using Web3 technologies
          </span>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue'
</script>

<style scoped>
/* =============================================================================
// 🎨 푸터 스타일
// ============================================================================= */
.app-footer {
  @apply bg-gray-50 dark:bg-gray-900 border-t border-gray-200 dark:border-gray-700 mt-auto;
}

.footer-container {
  @apply container-responsive py-8;
}

/* =============================================================================
// 📋 푸터 콘텐츠
// ============================================================================= */
.footer-content {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 mb-8;
}

.footer-section {
  @apply space-y-4;
}

.footer-title {
  @apply text-lg font-bold text-gray-900 dark:text-gray-100;
}

.footer-subtitle {
  @apply text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3;
}

.footer-description {
  @apply text-sm text-gray-600 dark:text-gray-400 leading-relaxed;
}

.footer-social {
  @apply flex items-center gap-3 mt-4;
}

.social-link {
  @apply flex items-center justify-center w-8 h-8 rounded-full bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-primary-100 dark:hover:bg-primary-900/30 hover:text-primary-600 dark:hover:text-primary-400 transition-colors duration-200;
}

.social-icon {
  @apply w-4 h-4;
}

.footer-links {
  @apply space-y-2;
}

.footer-link {
  @apply text-sm text-gray-600 dark:text-gray-400 hover:text-primary-600 dark:hover:text-primary-400 transition-colors duration-200;
}

/* =============================================================================
// 📱 푸터 하단
// ============================================================================= */
.footer-bottom {
  @apply flex flex-col sm:flex-row items-center justify-between pt-6 border-t border-gray-200 dark:border-gray-700;
}

.footer-bottom-left {
  @apply mb-2 sm:mb-0;
}

.copyright {
  @apply text-sm text-gray-500 dark:text-gray-400;
}

.footer-bottom-right {
  @apply flex items-center gap-1;
}

.footer-text {
  @apply text-sm text-gray-500 dark:text-gray-400;
}

.heart-icon {
  @apply w-4 h-4 text-red-500;
}

/* =============================================================================
// 📱 반응형 디자인
// ============================================================================= */
@media (max-width: 768px) {
  .footer-container {
    @apply py-6;
  }
  
  .footer-content {
    @apply gap-6;
  }
  
  .footer-bottom {
    @apply text-center;
  }
  
  .footer-bottom-right {
    @apply mt-2;
  }
}
</style>