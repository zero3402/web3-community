// =============================================================================
// 🛣️ Vue Router 설정 - 반응형 라우팅
// =============================================================================
// 설명: Vue.js 3 라우터 설정 with Composition API
// 특징: 반응형 경로, 권한 체크, 뷰 전환 효과
// 목적: SPA 라우팅과 네비게이션 관리
// =============================================================================

import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { RouteRecordRaw } from 'vue-router'

// =============================================================================
// 📋 라우트 타입 정의
// =============================================================================
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    role?: string[]
    keepAlive?: boolean
    transition?: string
    layout?: string
  }
}

// =============================================================================
// 🏗️ 라우트 정의
// =============================================================================
const routes: Array<RouteRecordRaw> = [
  // =============================================================================
  // 🏠 메인 페이지
  // =============================================================================
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: {
      title: '홈',
      transition: 'fade'
    }
  },
  
  // =============================================================================
  // 👥 인증 관련 라우트
  // =============================================================================
  {
    path: '/auth',
    name: 'Auth',
    component: () => import('@/layouts/AuthLayout.vue'),
    meta: {
      layout: 'auth'
    },
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: {
          title: '로그인',
          transition: 'slide-up'
        }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/auth/RegisterView.vue'),
        meta: {
          title: '회원가입',
          transition: 'slide-up'
        }
      },
      {
        path: 'forgot-password',
        name: 'ForgotPassword',
        component: () => import('@/views/auth/ForgotPasswordView.vue'),
        meta: {
          title: '비밀번호 찾기',
          transition: 'slide-up'
        }
      }
    ]
  },
  
  // =============================================================================
  // 📝 게시판 관련 라우트
  // =============================================================================
  {
    path: '/posts',
    name: 'Posts',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: {
      requiresAuth: true,
      transition: 'slide-left'
    },
    children: [
      {
        path: '',
        name: 'PostList',
        component: () => import('@/views/posts/PostListView.vue'),
        meta: {
          title: '게시글 목록',
          keepAlive: true
        }
      },
      {
        path: ':id',
        name: 'PostDetail',
        component: () => import('@/views/posts/PostDetailView.vue'),
        meta: {
          title: '게시글 상세',
          keepAlive: false
        }
      },
      {
        path: 'create',
        name: 'PostCreate',
        component: () => import('@/views/posts/PostCreateView.vue'),
        meta: {
          title: '게시글 작성',
          requiresAuth: true
        }
      },
      {
        path: ':id/edit',
        name: 'PostEdit',
        component: () => import('@/views/posts/PostEditView.vue'),
        meta: {
          title: '게시글 수정',
          requiresAuth: true
        }
      }
    ]
  },
  
  // =============================================================================
  // 👤 사용자 프로필 라우트
  // =============================================================================
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: '',
        name: 'ProfileHome',
        component: () => import('@/views/profile/ProfileHomeView.vue'),
        meta: {
          title: '내 프로필'
        }
      },
      {
        path: 'settings',
        name: 'ProfileSettings',
        component: () => import('@/views/profile/ProfileSettingsView.vue'),
        meta: {
          title: '프로필 설정'
        }
      },
      {
        path: 'activity',
        name: 'ProfileActivity',
        component: () => import('@/views/profile/ProfileActivityView.vue'),
        meta: {
          title: '활동 내역'
        }
      }
    ]
  },
  
  // =============================================================================
  // 🗨️ 관리자 라우트
  // =============================================================================
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: {
      requiresAuth: true,
      role: ['admin']
    },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: {
          title: '관리자 대시보드'
        }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManageView.vue'),
        meta: {
          title: '사용자 관리'
        }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/SettingsView.vue'),
        meta: {
          title: '시스템 설정'
        }
      }
    ]
  },
  
  // =============================================================================
  // 🔍 검색 라우트
  // =============================================================================
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: {
      title: '검색',
      transition: 'fade'
    },
    children: [
      {
        path: '',
        name: 'SearchResults',
        component: () => import('@/views/search/SearchResultsView.vue'),
        meta: {
          title: '검색 결과',
          keepAlive: true
        }
      },
      {
        path: 'advanced',
        name: 'AdvancedSearch',
        component: () => import('@/views/search/AdvancedSearchView.vue'),
        meta: {
          title: '상세 검색'
        }
      }
    ]
  },
  
  // =============================================================================
  // 📧 알림 라우트
  // =============================================================================
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: '',
        name: 'NotificationList',
        component: () => import('@/views/notifications/NotificationListView.vue'),
        meta: {
          title: '알림 목록',
          keepAlive: true
        }
      },
      {
        path: ':id',
        name: 'NotificationDetail',
        component: () => import('@/views/notifications/NotificationDetailView.vue'),
        meta: {
          title: '알림 상세'
        }
      }
    ]
  },
  
  // =============================================================================
  // 📱 모바일 전용 라우트
  // =============================================================================
  {
    path: '/mobile',
    name: 'Mobile',
    component: () => import('@/layouts/MobileLayout.vue'),
    meta: {
      layout: 'mobile'
    },
    children: [
      {
        path: '',
        name: 'MobileHome',
        component: () => import('@/views/mobile/MobileHomeView.vue'),
        meta: {
          title: '모바일 홈'
        }
      },
      {
        path: 'menu',
        name: 'MobileMenu',
        component: () => import('@/views/mobile/MobileMenuView.vue'),
        meta: {
          title: '메뉴'
        }
      }
    ]
  },
  
  // =============================================================================
  // ❌ 404 에러 페이지
  // =============================================================================
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: {
      title: '페이지를 찾을 수 없습니다'
    }
  }
]

// =============================================================================
// 🛣️ 라우터 생성
// =============================================================================
const router = createRouter({
  // =============================================================================
  // 🌐 히스토리 모드 설정
  // =============================================================================
  history: createWebHistory(import.meta.env.BASE_URL),
  
  // =============================================================================
  // 📋 라우트 설정
  // =============================================================================
  routes,
  
  // =============================================================================
  // 🔗 링크 활성화 클래스 설정
  // =============================================================================
  linkActiveClass: 'router-link-active',
  linkExactActiveClass: 'router-link-exact-active',
  
  // =============================================================================
  // 🔄 스크롤 동작 설정
  // =============================================================================
  scrollBehavior(to, from, savedPosition) {
    // 저장된 스크롤 위치가 있으면 사용
    if (savedPosition) {
      return savedPosition
    }
    
    // 앵커가 있는 경우 해당 위치로 스크롤
    if (to.hash) {
      return {
        el: to.hash,
        behavior: 'smooth'
      }
    }
    
    // 기본적으로 상단으로 스크롤
    return { top: 0 }
  }
})

// =============================================================================
// 🔍 라우트 가드 설정
// =============================================================================
// =============================================================================
// 🔐 라우터 가드
// =============================================================================
// 전역前置 가드
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 인증 초기화
  if (!authStore.isInitialized) {
    try {
      await authStore.initialize()
    } catch (error) {
      console.error('Auth initialization failed:', error)
    }
  }

  // 로딩 상태 표시
  document.body.classList.add('route-changing')

  // 제목 설정
  if (to.meta?.title) {
    document.title = `${to.meta.title} - Web3 Community`
  } else {
    document.title = 'Web3 Community Platform'
  }

  // 게스트 필요 페이지
  if (to.meta?.requiresGuest && authStore.isAuthenticated) {
    next('/dashboard')
    return
  }

  // 인증 필요 페이지
  if (to.meta?.requiresAuth && !authStore.isAuthenticated) {
    next({
      path: '/auth/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  // 권한 체크
  if (!authStore.canAccessRoute(to)) {
    if (authStore.isAuthenticated) {
      next('/403')
    } else {
      next({
        path: '/auth/login',
        query: { redirect: to.fullPath }
      })
    }
    return
  }

  // 활성 사용자 체크
  if (to.meta?.requiresAuth && authStore.isAuthenticated && !authStore.isActiveUser) {
    ElMessage.warning('계정이 비활성화되었습니다. 관리자에게 문의해주세요.')
    next('/auth/login')
    return
  }

  // 이메일 인증 필요 페이지
  if (to.meta?.requiresEmailVerification && 
      authStore.isAuthenticated && 
      !authStore.isEmailVerified) {
    ElMessage.warning('이메일 인증이 필요합니다.')
    next('/auth/verify-email')
    return
  }

  next()
})

// 전역 후처리 가드
router.afterEach((to, from) => {
  // 로딩 상태 제거
  document.body.classList.remove('route-changing')

  // 분석 로깅 (선택사항)
  if (import.meta.env.PROD) {
    // Google Analytics 또는 다른 분석 툴에 페이지 뷰 전송
    if (typeof gtag !== 'undefined') {
      gtag('config', 'GA_MEASUREMENT_ID', {
        page_path: to.path
      })
    }
  }

  // 디버그 로그
  if (import.meta.env.DEV) {
    console.log(`Route changed: ${from.path} -> ${to.path}`)
  }
})

// 에러 핸들러
router.onError((error) => {
  console.error('Router error:', error)
  
  // 치명적인 에러인 경우 에러 페이지로 이동
  if (error.name === 'ChunkLoadError') {
    ElMessage.error('페이지 로딩 중 오류가 발생했습니다. 페이지를 새로고침해주세요.')
    window.location.reload()
  }
})

// =============================================================================
// 🎯 라우터 유틸리티
// =============================================================================
export const routerUtils = {
  // 현재 라우트 이름 가져오기
  getCurrentRouteName: () => router.currentRoute.value.name,
  
  // 라우트 이동
  push: (to: string | any) => router.push(to),
  
  // 라우트 교체
  replace: (to: string | any) => router.replace(to),
  
  // 뒤로 가기
  back: () => router.back(),
  
  // 앞으로 가기
  forward: () => router.forward(),
  
  // 특정 경로로 이동 (권한 체크)
  navigateWithPermission: async (to: string | any) => {
    const authStore = useAuthStore()
    
    if (typeof to === 'string') {
      const route = router.resolve(to)
      if (authStore.canAccessRoute(route)) {
        await router.push(to)
      } else {
        ElMessage.error('접근 권한이 없습니다.')
      }
    } else {
      if (authStore.canAccessRoute(to)) {
        await router.push(to)
      } else {
        ElMessage.error('접근 권한이 없습니다.')
      }
    }
  }
}

// =============================================================================
// 🔧 라우터 확장 메소드
// =============================================================================
// 모바일 라우트 확인
router.isMobile = (route: string) => {
  return route.startsWith('/mobile')
}

// 인증 필요 라우트 확인
router.requiresAuth = (to: any) => {
  return to.matched.some(record => record.meta.requiresAuth)
}

// 관리자 라우트 확인
router.isAdmin = (to: any) => {
  return to.matched.some(record => record.meta.role?.includes('admin'))
}

// =============================================================================
// 🌐 반응형 라우팅 도우미 함수
// =============================================================================
// 화면 너비에 따른 라우트 필터링
router.getResponsiveRoutes = () => {
  const width = window.innerWidth
  
  if (width < 768) {
    // 모바일 화면
    return routes.filter(route => 
      !route.meta?.desktopOnly
    ).map(route => {
      // 모바일 전용 라우트로 경로 수정
      if (router.isMobile(route.path || '')) {
        return {
          ...route,
          path: route.path.replace('/posts', '/mobile/posts'),
          component: () => import('@/views/mobile/MobilePostView.vue')
        }
      }
      return route
    })
  }
  
  return routes
}

export default router