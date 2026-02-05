# 🌿 Web3 Community Frontend README
# Vue.js 3 + Vite + TypeScript + Tailwind CSS

## 📋 Overview

Web3 Community Platform의 Vue.js 기반 프론트엔드 애플리케이션입니다. DDD 아키텍처로 설계된 백엔드 서비스와 연동하여 탈중앙화 커뮤니티 플랫폼을 제공합니다.

## 🚀 Features

- 🎨 **Modern Vue 3** - Composition API, TypeScript, 반응형 디자인
- 🌍 **Responsive Design** - 모바일, 태블릿, 데스크탑 지원
- 🌙 **Dark Mode** - 자동/수정 다크 모드 지원
- 🔐 **Authentication** - JWT 기반 인증 및 소셜 로그인
- 📱 **PWA Support** - 오프라인 지원 및 앱 설치 가능
- 🔄 **Real-time Updates** - WebSocket 기반 실시간 통신
- 📊 **Analytics Dashboard** - 사용자 및 커뮤니티 데이터 시각화
- 🌐 **Internationalization** - 다국어 지원 (한국어, 영어, 일본어, 중국어)
- ♿ **Accessibility** - WCAG 2.1 AA 준수
- 🔍 **SEO Optimized** - 검색 엔진 최적화

## 🛠️ Tech Stack

### Core Framework
- **Vue 3.4** - Composition API, `<script setup>`
- **Vite 5** - 빠른 빌드 및 HMR
- **TypeScript 5** - 타입 안전성
- **Vue Router 4** - 라우팅 관리
- **Pinia 2** - 상태 관리

### UI & Styling
- **Tailwind CSS 3** - 유틸리티 우선 CSS
- **Element Plus 2** - Vue 3 UI 컴포넌트 라이브러리
- **Tailwind Plugins** - Forms, Typography, Aspect Ratio
- **Headless UI** - 접근성 있는 컴포넌트

### Development Tools
- **Vitest** - 단위 테스트
- **Vue Test Utils** - Vue 컴포넌트 테스팅
- **ESLint + Prettier** - 코드 품질 관리
- **Husky + lint-staged** - Git hooks
- **Storybook** - 컴포넌트 문서화

### Additional Libraries
- **Axios** - HTTP 클라이언트
- **VueUse** - Composition 유틸리티
- **Day.js** - 날짜 조작
- **Chart.js** - 데이터 시각화
- **Socket.io** - 실시간 통신
- **Web Push API** - 푸시 알림

## 🏗️ Project Structure

```
frontend/
├── public/                 # 정적 파일
│   ├── favicon.ico
│   ├── manifest.json
│   └── icons/
├── src/
│   ├── assets/            # 에셋 파일
│   │   ├── css/
│   │   │   └── main.css
│   │   ├── images/
│   │   └── fonts/
│   ├── components/        # 재사용 가능한 컴포넌트
│   │   ├── common/       # 공통 컴포넌트
│   │   ├── forms/        # 폼 컴포넌트
│   │   ├── layout/       # 레이아웃 컴포넌트
│   │   └── ui/           # UI 컴포넌트
│   ├── composables/      # Vue Composables
│   │   ├── useAuth.ts
│   │   ├── useApi.ts
│   │   └── useWebSocket.ts
│   ├── layouts/          # 페이지 레이아웃
│   │   ├── DefaultLayout.vue
│   │   ├── AuthLayout.vue
│   │   └── AdminLayout.vue
│   ├── pages/            # 페이지 컴포넌트
│   │   ├── auth/
│   │   ├── community/
│   │   ├── profile/
│   │   └── admin/
│   ├── router/           # 라우터 설정
│   │   └── index.ts
│   ├── stores/           # Pinia 스토어
│   │   ├── auth.ts
│   │   ├── user.ts
│   │   └── ui.ts
│   ├── types/            # TypeScript 타입 정의
│   │   ├── api.ts
│   │   ├── user.ts
│   │   └── post.ts
│   ├── utils/            # 유틸리티 함수
│   │   ├── api.ts
│   │   ├── auth.ts
│   │   └── format.ts
│   ├── locales/          # 다국어 파일
│   │   ├── ko.json
│   │   ├── en.json
│   │   ├── ja.json
│   │   └── zh.json
│   ├── services/         # API 서비스
│   │   ├── api.ts
│   │   ├── auth.ts
│   │   └── websocket.ts
│   ├── App.vue           # 루트 컴포넌트
│   └── main.ts           # 앱 진입점
├── tests/                # 테스트 파일
│   ├── unit/
│   └── e2e/
├── .env.example          # 환경 변수 예시
├── .eslintrc.cjs         # ESLint 설정
├── .prettierrc           # Prettier 설정
├── tailwind.config.js    # Tailwind CSS 설정
├── tsconfig.json         # TypeScript 설정
├── vite.config.ts        # Vite 설정
└── package.json          # 패키지 의존성
```

## 🚀 Getting Started

### Prerequisites

- Node.js 18+ 
- npm 9+ 또는 yarn 1.22+

### Installation

```bash
# 클론 프로젝트
git clone https://github.com/your-org/web3-community.git
cd web3-community/frontend

# 의존성 설치
npm install

# 환경 변수 설정
cp .env.example .env.local
# .env.local 파일에 필요한 환경 변수 설정

# 개발 서버 시작
npm run dev
```

### Environment Variables

`.env.local` 파일에 다음 환경 변수들을 설정하세요:

```env
# API 설정
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8085/ws

# 인증 설정
VITE_TOKEN_KEY=web3_community_token
VITE_REFRESH_TOKEN_KEY=web3_community_refresh_token

# 기능 플래그
VITE_DARK_MODE_ENABLED=true
VITE_PWA_ENABLED=true
VITE_I18N_ENABLED=true
```

## 📱 Build & Deploy

### Development

```bash
# 개발 서버 (HMR)
npm run dev

# 타입 체크
npm run type-check

# 린트
npm run lint

# 테스트
npm run test
```

### Production

```bash
# 프로덕션 빌드
npm run build

# 빌드된 앱 미리보기
npm run preview

# 번들 분석
npm run analyze
```

### Docker Deploy

```bash
# Docker 이미지 빌드
docker build -t web3-community-frontend .

# Docker 컨테이너 실행
docker run -p 3000:80 web3-community-frontend
```

## 🧪 Testing

### Unit Tests

```bash
# 모든 테스트 실행
npm run test

# Watch 모드
npm run test:watch

# 커버리지
npm run test:coverage

# UI 모드
npm run test:ui
```

### E2E Tests

```bash
# E2E 테스트 실행
npm run test:e2e

# E2E 테스트 UI
npm run test:e2e:ui
```

## 📚 Storybook

```bash
# Storybook 개발 서버
npm run storybook

# Storybook 빌드
npm run build-storybook
```

## 🎨 Component Development

### Creating New Components

1. **Component Structure**
```vue
<template>
  <div class="component-name">
    <!-- Template content -->
  </div>
</template>

<script setup lang="ts">
// Component logic
interface Props {
  // Props definition
}

interface Emits {
  // Emits definition
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
</script>

<style scoped>
/* Component styles */
</style>
```

2. **Composable Pattern**
```typescript
// composables/useComponentName.ts
import { ref, computed } from 'vue'

export function useComponentName() {
  // Reactive state
  const state = ref()

  // Computed properties
  const computed = computed(() => {
    // Computation logic
  })

  // Methods
  const method = () => {
    // Method logic
  }

  return {
    state,
    computed,
    method
  }
}
```

## 🌍 Internationalization

### Adding New Language

1. **Create language file** `src/locales/[language].json`
2. **Update supported locales** in `src/i18n/index.ts`
3. **Add language selector** in components

### Usage

```vue
<template>
  <h1>{{ $t('welcome.title') }}</h1>
  <p>{{ $t('welcome.description') }}</p>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
</script>
```

## 🔐 Authentication

### JWT Token Management

```typescript
// composables/useAuth.ts
export function useAuth() {
  const token = ref(localStorage.getItem('token'))
  const user = ref(null)

  const login = async (credentials: LoginCredentials) => {
    // Login logic
  }

  const logout = () => {
    // Logout logic
  }

  return {
    token,
    user,
    login,
    logout
  }
}
```

### Route Guards

```typescript
// router/index.ts
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    next('/login')
  } else {
    next()
  }
})
```

## 📱 PWA Features

### Service Worker

```typescript
// main.ts
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js')
    .then((registration) => {
      console.log('SW registered:', registration)
    })
    .catch((error) => {
      console.log('SW registration failed:', error)
    })
}
```

### Offline Support

```typescript
// composables/useOffline.ts
export function useOffline() {
  const isOnline = ref(navigator.onLine)
  
  window.addEventListener('online', () => {
    isOnline.value = true
  })
  
  window.addEventListener('offline', () => {
    isOnline.value = false
  })
  
  return { isOnline }
}
```

## 🔄 Real-time Updates

### WebSocket Connection

```typescript
// composables/useWebSocket.ts
export function useWebSocket() {
  const socket = ref<WebSocket | null>(null)
  const messages = ref<any[]>([])
  
  const connect = () => {
    socket.value = new WebSocket(import.meta.env.VITE_WS_URL)
    
    socket.value.onmessage = (event) => {
      messages.value.push(JSON.parse(event.data))
    }
  }
  
  const disconnect = () => {
    socket.value?.close()
  }
  
  return {
    socket,
    messages,
    connect,
    disconnect
  }
}
```

## 📊 Performance Optimization

### Code Splitting

```typescript
// router/index.ts
const routes = [
  {
    path: '/admin',
    component: () => import('@/pages/admin/AdminDashboard.vue'),
    meta: { requiresAuth: true }
  }
]
```

### Lazy Loading

```vue
<template>
  <AsyncComponent v-if="showComponent" />
</template>

<script setup>
import { defineAsyncComponent } from 'vue'

const AsyncComponent = defineAsyncComponent(() => 
  import('@/components/HeavyComponent.vue')
)
</script>
```

## 🎯 Best Practices

### Vue 3 Composition API

```typescript
// ✅ Good
<script setup>
import { ref, computed, watch } from 'vue'

const count = ref(0)
const doubled = computed(() => count.value * 2)

watch(count, (newCount) => {
  console.log('Count changed:', newCount)
})
</script>

// ❌ Avoid
<script>
export default {
  data() {
    return { count: 0 }
  },
  computed: {
    doubled() {
      return this.count * 2
    }
  }
}
</script>
```

### TypeScript Usage

```typescript
// ✅ Strong typing
interface User {
  id: string
  name: string
  email: string
}

const users = ref<User[]>([])

// ❌ Any type
const users = ref<any>([])
```

### Component Design

```vue
<!-- ✅ Single responsibility -->
<template>
  <UserAvatar :user="user" :size="size" />
</template>

<!-- ❌ Multiple responsibilities -->
<template>
  <div>
    <UserAvatar :user="user" />
    <UserActions :user="user" />
    <UserStats :user="user" />
  </div>
</template>
```

## 🐛 Troubleshooting

### Common Issues

1. **HMR not working**
   - Check Vite configuration
   - Ensure proper imports
   - Clear browser cache

2. **TypeScript errors**
   - Run `npm run type-check`
   - Check tsconfig.json
   - Verify type definitions

3. **Build failures**
   - Check for circular dependencies
   - Verify environment variables
   - Clean node_modules and reinstall

## 📝 Contributing

### Git Workflow

```bash
# Feature branch
git checkout -b feature/new-feature
git commit -m "feat: add new feature"
git push origin feature/new-feature

# Conventional commits
feat: new feature
fix: bug fix
docs: documentation
style: formatting
refactor: code refactoring
test: add tests
chore: maintenance
```

### Code Review Checklist

- [ ] TypeScript types are correct
- [ ] Component follows Vue 3 best practices
- [ ] Tests are included
- [ ] Documentation is updated
- [ ] Accessibility is considered
- [ ] Performance is optimized

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Support

- 📧 Email: support@web3community.com
- 💬 Discord: [Web3 Community Discord](https://discord.gg/web3community)
- 📖 Documentation: [docs.web3community.com](https://docs.web3community.com)
- 🐛 Issues: [GitHub Issues](https://github.com/your-org/web3-community/issues)

## 🎉 Acknowledgments

- Vue.js team for the amazing framework
- Element Plus for the UI components
- Tailwind CSS team for the utility-first CSS framework
- Vite team for the blazing fast build tool
- All contributors and community members