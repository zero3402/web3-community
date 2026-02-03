// =============================================================================
// 🚀 Vue.js 3 반응형 메인 엔트리 파일
// =============================================================================
// 설명: Vue.js 애플리케이션 초기화 및 설정
// 특징: Composition API, 반응형 디자인, PWA 지원
// 목적: 모든 디바이스에서 최적화된 사용자 경험 제공
// =============================================================================

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// =============================================================================
// 🎨 UI 라이브러리 임포트
// =============================================================================
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

// =============================================================================
// 🎨 글로벌 CSS 임포트 (Tailwind CSS)
// =============================================================================
import './assets/css/main.css'

// =============================================================================
// 🔧 애플리케이션 설정
// =============================================================================
const app = createApp(App)

// =============================================================================
// 📱 반응형 및 접근성 설정
// =============================================================================
// 터치 디바이스 감지
const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0
if (isTouchDevice) {
  document.documentElement.classList.add('touch-device')
}

// 화면 크기 감지
const updateScreenSize = () => {
  const width = window.innerWidth
  if (width < 640) {
    document.documentElement.classList.add('mobile')
    document.documentElement.classList.remove('tablet', 'desktop')
  } else if (width < 1024) {
    document.documentElement.classList.add('tablet')
    document.documentElement.classList.remove('mobile', 'desktop')
  } else {
    document.documentElement.classList.add('desktop')
    document.documentElement.classList.remove('mobile', 'tablet')
  }
}

updateScreenSize()
window.addEventListener('resize', updateScreenSize)

// =============================================================================
// 🎯 플러그인 및 설정 적용
// =============================================================================
app.use(createPinia())
app.use(router)
app.use(ElementPlus, {
  // 반응형 Element Plus 설정
  size: isTouchDevice ? 'small' : 'default',
  locale: 'ko'
})

// =============================================================================
// 🌍 글로벌 속성 설정
// =============================================================================
app.config.globalProperties.$isMobile = isTouchDevice
app.config.globalProperties.$appVersion = import.meta.env.VITE_APP_VERSION || '1.0.0'

// =============================================================================
// 🔧 개발 환경 설정
// =============================================================================
if (import.meta.env.DEV) {
  // Vue DevTools 활성화
  app.config.devtools = true
  
  // 성능 모니터링
  app.config.performance = true
  
  // 전역 에러 핸들링
  app.config.errorHandler = (err, vm, info) => {
    console.error('Global error:', err)
    console.error('Component:', vm)
    console.error('Error info:', info)
  }
  
  // 전경 경고 핸들링
  app.config.warnHandler = (msg, vm, trace) => {
    console.warn('Global warning:', msg)
    console.warn('Component:', vm)
    console.warn('Trace:', trace)
  }
}

// =============================================================================
// 🚀 앱 마운트
// =============================================================================
app.mount('#app')

// =============================================================================
// 📱 PWA 기능 활성화 (선택사항)
// =============================================================================
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js')
      .then((registration) => {
        console.log('SW registered: ', registration)
      })
      .catch((error) => {
        console.log('SW registration failed: ', error)
      })
  })
}

// =============================================================================
// 🌙 다크 모드 지원
// =============================================================================
const prefersDark = window.matchMedia('(prefers-color-scheme: dark)')
const updateTheme = (isDark) => {
  if (isDark) {
    document.documentElement.classList.add('dark')
    ElementPlus.config.globalProperties.$ElMessage.theme = 'dark'
  } else {
    document.documentElement.classList.remove('dark')
    ElementPlus.config.globalProperties.$ElMessage.theme = 'light'
  }
}

updateTheme(prefersDark.matches)
prefersDark.addListener((e) => updateTheme(e.matches))

// =============================================================================
// 📊 앱 성능 모니터링
// =============================================================================
window.addEventListener('load', () => {
  if (import.meta.env.DEV) {
    const loadTime = performance.now()
    console.log(`🚀 App loaded in ${loadTime.toFixed(2)}ms`)
    
    // Lighthouse 성능 측정 (개발 환경)
    if ('PerformanceObserver' in window) {
      const observer = new PerformanceObserver((list) => {
        for (const entry of list.getEntries()) {
          if (entry.entryType === 'navigation') {
            console.log('📊 Navigation Performance:', {
              domContentLoaded: entry.domContentLoadedEventEnd - entry.domContentLoadedEventStart,
              loadComplete: entry.loadEventEnd - entry.loadEventStart,
              firstPaint: entry.paintTiming?.firstPaint,
              firstContentfulPaint: entry.paintTiming?.firstContentfulPaint
            })
          }
        }
      })
      observer.observe({ entryTypes: ['navigation', 'paint'] })
    }
  }
})

// =============================================================================
// 🔄 라우팅 가드 (권한 체크 등)
// =============================================================================
router.beforeEach((to, from, next) => {
  // 페이지 전환 로딩 표시
  if (import.meta.env.DEV) {
    console.log(`🔄 Routing from ${from.path} to ${to.path}`)
  }
  
  // 인증 필요한 라우트 체크
  if (to.meta.requiresAuth && !localStorage.getItem('token')) {
    next({ path: '/auth/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

router.afterEach((to, from) => {
  // 페이지 타이틀 업데이트
  if (to.meta.title) {
    document.title = `${to.meta.title} - Web3 Community`
  } else {
    document.title = 'Web3 Community Platform'
  }
  
  // 스크롤 상단 이동
  if (!to.meta.preserveScroll) {
    window.scrollTo(0, 0)
  }
})

// =============================================================================
// 🌐 글로벌 이벤트 핸들러
// =============================================================================
window.addEventListener('online', () => {
  console.log('🌐 Back online')
  // 온라인 상태 UI 업데이트
})

window.addEventListener('offline', () => {
  console.log('📱 Went offline')
  // 오프라인 상태 UI 업데이트
})

window.addEventListener('beforeinstallprompt', (e) => {
  // PWA 설치 프롬프트 저장
  window.deferredPrompt = e
  console.log('📱 PWA install prompt ready')
})

window.addEventListener('appinstalled', () => {
  console.log('📱 PWA installed successfully')
})

// =============================================================================
// 📱 모바일 최적화 튜닝
// =============================================================================
// iOS Safari 스크롤 바 처리
if (/iPad|iPhone|iPod/.test(navigator.userAgent)) {
  document.documentElement.style.setProperty('--safe-area-inset-top', 'env(safe-area-inset-top)')
  document.documentElement.style.setProperty('--safe-area-inset-bottom', 'env(safe-area-inset-bottom)')
}

// 안드로이드 뷰포 설정
if (/Android/.test(navigator.userAgent)) {
  const viewport = document.querySelector('meta[name="viewport"]')
  if (viewport) {
    viewport.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no')
  }
}

export default app