import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// =============================================================================
// 🚀 Vite 설정 파일 - Vue 3 반응형 프론트엔드
// =============================================================================
// 설명: 개발 서버, 빌드, 플러그인 설정
// 특징: 핫 리로드, 최적화된 빌드, 반응형 지원
// 목적: 개발 생산성과 프로덕션 성능 최적화
// =============================================================================

export default defineConfig({
  plugins: [
    // Vue 3 플러그인
    vue(),
    
    // 자동 임포트 (Vue, VueRouter, Pinia 등)
    AutoImport({
      imports: [
        'vue',
        'vue-router',
        'pinia',
        '@vueuse/core'
      ],
      dts: true,
      eslintrc: {
        enabled: true
      }
    }),
    
    // 컴포넌트 자동 임포트 (Element Plus 등)
    Components({
      resolvers: [ElementPlusResolver()],
      dts: true
    })
  ],
  
  // =============================================================================
  // 📁 경로 설정
  // =============================================================================
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@/components': resolve(__dirname, 'src/components'),
      '@/views': resolve(__dirname, 'src/views'),
      '@/stores': resolve(__dirname, 'src/stores'),
      '@/utils': resolve(__dirname, 'src/utils'),
      '@/types': resolve(__dirname, 'src/types'),
      '@/assets': resolve(__dirname, 'src/assets')
    }
  },
  
  // =============================================================================
  // 🌐 개발 서버 설정
  // =============================================================================
  server: {
    host: '0.0.0.0',           // 외부 접속 허용 (Docker 개발용)
    port: 3000,                // 개발 서버 포트
    strictPort: false,          // 포트 점유시 다른 포트 사용
    open: true,                 // 브라우저 자동 열기
    cors: true,                 // CORS 활성화
    
    // 프록시 설정 (API 서버 연동)
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 백엔드 API 서버
        changeOrigin: true,
        secure: false,
        ws: true,                          // WebSocket 지원
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('proxy error', err)
          })
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            console.log('Sending Request to the Target:', req.method, req.url)
          })
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log('Received Response from the Target:', proxyRes.statusCode, req.url)
          })
        }
      }
    }
  },
  
  // =============================================================================
  // 🏗️ 빌드 설정
  // =============================================================================
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    
    // 빌드 최적화
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,           // 콘솔 로그 제거
        drop_debugger: true           // debugger 제거
      }
    },
    
    // 롤링 업 배치 전략
    rollupOptions: {
      output: {
        manualChunks: {
          // 벤더 라이브러리 별도 청크로 분리
          vendor: ['vue', 'vue-router', 'pinia'],
          ui: ['element-plus'],
          utils: ['axios', 'dayjs', 'lodash-es', '@vueuse/core']
        }
      }
    },
    
    // 소스맵 설정
    sourcemap: process.env.NODE_ENV === 'development',
    
    // 청크 크기 경고 기준
    chunkSizeWarningLimit: 1000,  // 1MB 이상 청크 경고
    
    // 빌드 시 콘솔 출력
    reportCompressedSize: false
  },
  
  // =============================================================================
  // 🎨 CSS 설정
  // =============================================================================
  css: {
    // PostCSS 설정 (Tailwind CSS)
    postcss: {
      plugins: [
        require('tailwindcss'),
        require('autoprefixer')
      ]
    },
    
    // 모듈별 CSS 분리
    modules: {
      localsConvention: 'camelCase'
    },
    
    // Devtool 소스맵
    devSourcemap: true
  },
  
  // =============================================================================
  // 🔧 최적화 설정
  // =============================================================================
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'axios',
      'element-plus',
      '@vueuse/core',
      'dayjs',
      'lodash-es'
    ]
  },
  
  // =============================================================================
  // 🔍 개발 툴 설정
  // =============================================================================
  define: {
    // 전역 상수 정의
    __VUE_OPTIONS_API__: JSON.stringify(true),
    __VUE_PROD_DEVTOOLS__: JSON.stringify(false),
    __APP_VERSION__: JSON.stringify(process.env.npm_package_version)
  },
  
  // =============================================================================
  // 🌍 환경 변수
  // =============================================================================
  envPrefix: ['VUE_', 'APP_'],
  
  // =============================================================================
  // 🧪 테스트 설정
  // =============================================================================
  test: {
    globals: true,
    environment: 'jsdom'
  }
})