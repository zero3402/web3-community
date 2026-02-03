// =============================================================================
// 🎨 Tailwind CSS 설정 - 반응형 디자인
// =============================================================================
// 설명: Web3 Community Platform을 위한 Tailwind CSS 설정
// 특징: 반응형 디자인, 커스텀 테마, 다크 모드 지원
// 목적: 일관된 디자인 시스템과 반응형 UI 구현
// =============================================================================

/** @type {import('tailwindcss').Config} */
export default {
  // =============================================================================
  // 📋 콘텐츠 설정 (CSS 클래스를 생성할 파일들)
  // =============================================================================
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
    './src/**/*.{css,scss,less}'
  ],
  
  // =============================================================================
  // 🎨 테마 설정
  // =============================================================================
  theme: {
    extend: {
      // =============================================================================
      // 🎨 색상 팔레트
      // =============================================================================
      colors: {
        // 메인 브랜드 색상
        primary: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
          950: '#172554'
        },
        
        // 보조 색상
        secondary: {
          50: '#fdf4ff',
          100: '#fae8ff',
          200: '#f5d0fe',
          300: '#f0abfc',
          400: '#e879f9',
          500: '#d946ef',
          600: '#c026d3',
          700: '#a21caf',
          800: '#86198f',
          900: '#701a75',
          950: '#4a044e'
        },
        
        // 상태 색상
        success: {
          50: '#f0fdf4',
          100: '#dcfce7',
          200: '#bbf7d0',
          300: '#86efac',
          400: '#4ade80',
          500: '#22c55e',
          600: '#16a34a',
          700: '#15803d',
          800: '#166534',
          900: '#14532d',
          950: '#052e16'
        },
        
        warning: {
          50: '#fffbeb',
          100: '#fef3c7',
          200: '#fde68a',
          300: '#fcd34d',
          400: '#fbbf24',
          500: '#f59e0b',
          600: '#d97706',
          700: '#b45309',
          800: '#92400e',
          900: '#78350f',
          950: '#451a03'
        },
        
        error: {
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#f87171',
          500: '#ef4444',
          600: '#dc2626',
          700: '#b91c1c',
          800: '#991b1b',
          900: '#7f1d1d',
          950: '#450a0a'
        },
        
        // 중성 색상
        gray: {
          50: '#f9fafb',
          100: '#f3f4f6',
          200: '#e5e7eb',
          300: '#d1d5db',
          400: '#9ca3af',
          500: '#6b7280',
          600: '#4b5563',
          700: '#374151',
          800: '#1f2937',
          900: '#111827',
          950: '#030712'
        }
      },
      
      // =============================================================================
      // 📱 반응형 폰트 크기 (타이포그래피)
      // =============================================================================
      fontSize: {
        'xs': ['0.75rem', { lineHeight: '1rem' }],
        'sm': ['0.875rem', { lineHeight: '1.25rem' }],
        'base': ['1rem', { lineHeight: '1.5rem' }],
        'lg': ['1.125rem', { lineHeight: '1.75rem' }],
        'xl': ['1.25rem', { lineHeight: '1.75rem' }],
        '2xl': ['1.5rem', { lineHeight: '2rem' }],
        '3xl': ['1.875rem', { lineHeight: '2.25rem' }],
        '4xl': ['2.25rem', { lineHeight: '2.5rem' }],
        '5xl': ['3rem', { lineHeight: '1' }],
        '6xl': ['3.75rem', { lineHeight: '1' }],
        '7xl': ['4.5rem', { lineHeight: '1' }],
        '8xl': ['6rem', { lineHeight: '1' }],
        '9xl': ['8rem', { lineHeight: '1' }]
      },
      
      // =============================================================================
      // 🖼️ 스크린 사이즈 (반응형 브레이크포인트)
      // =============================================================================
      screens: {
        'xs': '475px',     // 초소형 모바일
        'sm': '640px',     // 소형 모바일
        'md': '768px',     // 태블릿
        'lg': '1024px',    // 작은 데스크탑
        'xl': '1280px',    // 데스크탑
        '2xl': '1536px'    // 대형 데스크탑
      },
      
      // =============================================================================
      // 🎛️ 컴포넌트 간격 (스페이싱)
      // =============================================================================
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '128': '32rem',
        '144': '36rem'
      },
      
      // =============================================================================
      // 🎨 그림자 효과
      // =============================================================================
      boxShadow: {
        'soft': '0 2px 15px -3px rgba(0, 0, 0, 0.07), 0 10px 20px -2px rgba(0, 0, 0, 0.04)',
        'medium': '0 4px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
        'large': '0 10px 40px -10px rgba(0, 0, 0, 0.1), 0 20px 25px -5px rgba(0, 0, 0, 0.04)',
        'xl': '0 20px 60px -10px rgba(0, 0, 0, 0.15), 0 25px 35px -5px rgba(0, 0, 0, 0.04)',
        'inner': 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.06)',
        'upward': '0 -4px 10px -1px rgba(0, 0, 0, 0.1), 0 -2px 4px -1px rgba(0, 0, 0, 0.06)',
        'downward': '0 4px 10px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)'
      },
      
      // =============================================================================
      // 🎯 애니메이션
      // =============================================================================
      animation: {
        'fade-in': 'fadeIn 0.3s ease-in-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'slide-down': 'slideDown 0.3s ease-out',
        'slide-left': 'slideLeft 0.3s ease-out',
        'slide-right': 'slideRight 0.3s ease-out',
        'bounce-in': 'bounceIn 0.6s ease-out',
        'pulse': 'pulse 2s infinite',
        'spin': 'spin 1s linear infinite'
      },
      
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' }
        },
        slideUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' }
        },
        slideDown: {
          '0%': { transform: 'translateY(-10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' }
        },
        slideLeft: {
          '0%': { transform: 'translateX(10px)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' }
        },
        slideRight: {
          '0%': { transform: 'translateX(-10px)', opacity: '0' },
          '100%': { transform: 'translateX(0)', opacity: '1' }
        },
        bounceIn: {
          '0%': { transform: 'scale(0.3)', opacity: '0' },
          '50%': { transform: 'scale(1.05)' },
          '70%': { transform: 'scale(0.9)' },
          '100%': { transform: 'scale(1)', opacity: '1' }
        },
        pulse: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.5' }
        },
        spin: {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' }
        }
      },
      
      // =============================================================================
      // 🎪 Z-Index 레이어
      // =============================================================================
      zIndex: {
        'hide': -1,
        'auto': 'auto',
        'base': 0,
        'top': 10,
        'sticky': 20,
        'fixed': 30,
        'modal': 40,
        'dropdown': 50,
        'popover': 60,
        'tooltip': 70,
        'notification': 80,
        'overlay': 90,
        'max': 9999
      },
      
      // =============================================================================
      // 🎨 보더 레디우스
      // =============================================================================
      borderRadius: {
        '4xl': '2rem',
        '5xl': '2.5rem'
      },
      
      // =============================================================================
      // 📏 최대 너비 (콘텐츠 제약)
      // =============================================================================
      maxWidth: {
        '8xl': '88rem',
        '9xl': '96rem',
        '10xl': '112rem',
        '11xl': '128rem',
        '12xl': '144rem'
      }
    }
  },
  
  // =============================================================================
  // 🔧 플러그인 설정
  // =============================================================================
  plugins: [
    // 다크 모드 지준을 위한 플러그인 (선택사항)
    // require('@tailwindcss/typography'),
    // require('@tailwindcss/forms'),
    // require('@tailwindcss/aspect-ratio'),
    // require('@tailwindcss/container-queries')
  ],
  
  // =============================================================================
  // 🌍 프리셋 설정
  // =============================================================================
  presets: [
    // 기본 프리셋을 그대로 사용
  ],
  
  // =============================================================================
  // 🛠️ 유틸리티 제어 (필요한 유틸리티만 생성)
  // =============================================================================
  corePlugins: {
    // 기본 플러그인 모두 활성화
    preflight: true,
    container: true,
    spacing: true,
    divide: true,
    accessibility: true,
    typography: true,
    background: true,
    borders: true,
    effects: true,
    flexbox: true,
    grid: true,
    layout: true,
    sizing: true,
    spacing: true,
    text: true,
    transform: true,
    transition: true,
    animation: true,
    touch: true,
    appearance: true,
    scrollBehavior: true,
    willChange: true,
    float: true,
    clear: true,
    objectFit: true,
    objectPosition: true,
    inset: true,
    overflow: true,
    overscrollBehavior: true,
    position: true,
    top: true,
    right: true,
    bottom: true,
    left: true,
    zIndex: true,
    order: true,
    gridColumn: true,
    gridRow: true,
    gridAutoColumns: true,
    gridAutoFlow: true,
    gridAutoRows: true,
    gap: true,
    spaceBetween: true,
    spaceY: true,
    spaceX: true,
    justifyContent: true,
    justifyItems: true,
    justifySelf: true,
    alignContent: true,
    alignItems: true,
    alignSelf: true,
    placeContent: true,
    placeItems: true,
    placeSelf: true,
    placeItems: true,
    flex: true,
    flexDirection: true,
    flexWrap: true,
    flexGrow: true,
    flexShrink: true,
    flexBasis: true,
    display: true,
    visibility: true,
    width: true,
    minWidth: true,
    maxWidth: true,
    height: true,
    minHeight: true,
    maxHeight: true,
    color: true,
    backgroundColor: true,
    backgroundImage: true,
    backgroundSize: true,
    backgroundPosition: true,
    backgroundRepeat: true,
    boxShadow: true,
    opacity: true,
    mixBlendMode: true,
    filter: true,
    backdropFilter: true,
    transitionProperty: true,
    transitionDuration: true,
    transitionTimingFunction: true,
    transitionDelay: true,
    transform: true,
    scale: true,
    rotate: true,
    translate: true,
    skew: true,
    transformOrigin: true,
    animation: true,
    animationPlayState: true,
    animationDuration: true,
    animationTimingFunction: true,
    animationDelay: true,
    animationIterationCount: true,
    animationDirection: true,
    animationFillMode: true
  }
}