# =============================================================================
// 📦 Build Script - Frontend & Backend Build Automation
// =============================================================================
// 설명: Web3 Community Platform의 모든 서비스 빌드 자동화
// 특징: Gradle 멀티모듈, NPM 패키지 빌드, Docker 이미지 생성
// 목적: 개발/프로덕션 환경에서의 일관된 빌드 프로세스
// =============================================================================

@echo off
setlocal enableextensions

// =============================================================================
// 🎯 빌드 설정
// =============================================================================
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%~dp0"
set "ENVIRONMENT=%1:dev"
set "SERVICES=%2:all"
set "SKIP_TESTS=%3:false"

echo "🚀 Web3 Community Platform - Build Script"
echo "Environment: %ENVIRONMENT%"
echo "Services: %SERVICES%"
echo "Skip Tests: %SKIP_TESTS%"

// =============================================================================
// 🔍 환경 확인
// =============================================================================
echo ""
echo "🔍 Checking prerequisites..."

// Node.js 확인
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo "❌ Node.js not found. Please install Node.js first."
    exit /b 1
)
echo "✓ Node.js found:"

node --version

// Docker 확인
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo "❌ Docker not found. Please install Docker first."
    exit /b 1
)
echo "✓ Docker found:"
docker --version

// =============================================================================
// 📁 프로젝트 구조 확인
// =============================================================================
if not exist "%PROJECT_ROOT%\frontend" (
    echo "❌ Frontend directory not found: %PROJECT_ROOT%\frontend"
    exit /b 1
)

if not exist "%PROJECT_ROOT%\backend" (
    echo "❌ Backend directory not found: %PROJECT_ROOT%\backend"
    exit /b 1
)

echo "✓ Project structure verified"

// =============================================================================
// 🔧 빌드 변수 설정
// =============================================================================
set "BUILD_TIMESTAMP=%date:~0,4%date:~5,2%date:~10,2%date:~13,2%"
set "GIT_COMMIT="
for /f "tokens=*" %%i in ('git rev-parse HEAD') do set "GIT_COMMIT=%%i"
if errorlevel 1 set "GIT_COMMIT=unknown"

echo "📝 Build Configuration:"
echo "  Timestamp: %BUILD_TIMESTAMP%"
echo "  Git Commit: %GIT_COMMIT%"
echo "  Environment: %ENVIRONMENT%"

// =============================================================================
// 🎨 Frontend 빌드 (Vue.js 3 + Vite)
// =============================================================================
echo ""
echo "🎨 Building Frontend (Vue.js 3 + Vite)..."

cd "%PROJECT_ROOT%\frontend"

// Node.js 의존성 설치
echo "📦 Installing frontend dependencies..."
call npm ci --no-audit --no-fund
if %errorlevel% neq 0 (
    echo "❌ Frontend dependency installation failed"
    exit /b 1
)

// 타입 체크 및 빌드
if "%SKIP_TESTS%"=="false" (
    echo "🔍 Running frontend type checks..."
    call npm run type-check
    if %errorlevel% neq 0 (
        echo "❌ Frontend type check failed"
        exit /b 1
    )
)

echo "🏗️ Building frontend application..."
if "%ENVIRONMENT%"=="dev" (
    call npm run build:dev
) else if "%ENVIRONMENT%"=="prod" (
    call npm run build:prod
) else (
    call npm run build
)

if %errorlevel% neq 0 (
    echo "❌ Frontend build failed"
    exit /b 1
)

echo "✅ Frontend build completed successfully"

// =============================================================================
// 🐳 Frontend Docker 이미지 빌드
// =============================================================================
echo ""
echo "🐳 Building frontend Docker image..."

cd "%PROJECT_ROOT%"
docker build -t web3-community/frontend:%ENVIRONMENT% -f docker/frontend/Dockerfile .
if %errorlevel% neq 0 (
    echo "❌ Frontend Docker image build failed"
    exit /b 1
)

docker build -t web3-community/frontend:latest -f docker/frontend/Dockerfile .
if %errorlevel% neq 0 (
    echo "❌ Frontend Docker image (latest) build failed"
    exit /b 1
)

echo "✅ Frontend Docker images built successfully"

// =============================================================================
// 📦 Backend 빌드 (Gradle + Kotlin + Spring Boot)
// =============================================================================
echo ""
echo "📦 Building Backend (Gradle + Kotlin + Spring Boot)..."

cd "%PROJECT_ROOT%\backend"

// Gradle Wrapper 권한 확인 및 설정
if not exist "gradlew.bat" (
    echo "❌ Gradle wrapper not found in backend directory"
    exit /b 1
)

echo "🔧 Configuring Gradle wrapper..."
call gradlew --version

// 특정 서비스만 빌드 또는 전체 빌드
if "%SERVICES%"=="all" (
    echo "🏗️ Building all backend services..."
    
    // Gradle 빌드 실행 (멀티 모듈)
    if "%SKIP_TESTS%"=="false" (
        echo "🧪 Running backend tests..."
        call gradlew test
        if %errorlevel% neq 0 (
            echo "❌ Backend tests failed"
            exit /b 1
        )
    )
    
    echo "📦 Building all services..."
    call gradlew build -x test --no-daemon --configuration-cache
    if %errorlevel% neq 0 (
        echo "❌ Backend build failed"
        exit /b 1
    )
) else (
    echo "🏗️ Building specific services: %SERVICES%"
    
    // 공통 모듈 빌드
    call gradlew :common:build
    if %errorlevel% neq 0 (
        echo "❌ Common module build failed"
        exit /b 1
    )
    
    // 개별 서비스 빌드
    for %%s in (%SERVICES%) do (
        echo "📦 Building %%s service..."
        call gradlew :%%s:build
        if %errorlevel% neq 0 (
            echo "❌ %%s service build failed"
            exit /b 1
        )
    )
)

echo "✅ Backend build completed successfully"

// =============================================================================
// 🐳 Backend Docker 이미지 빌드
// =============================================================================
echo ""
echo "🐳 Building backend Docker images..."

cd "%PROJECT_ROOT%"

// 모든 서비스 Docker 이미지 빌드
for %%s in (api-gateway user-service post-service comment-service auth-service notification-service) do (
    echo "🐳 Building %%s Docker image..."
    docker build -t web3-community/%%s:%ENVIRONMENT% -f docker/backend/%%s/Dockerfile .
    if %errorlevel% neq 0 (
        echo "❌ %%s Docker image build failed"
        exit /b 1
    )
    
    docker build -t web3-community/%%s:latest -f docker/backend/%%s/Dockerfile .
    if %errorlevel% neq 0 (
        echo "❌ %%s Docker image (latest) build failed"
        exit /b 1
    )
)

echo "✅ All backend Docker images built successfully"

// =============================================================================
// 📊 빌드 결과 확인
// =============================================================================
echo ""
echo "📊 Build Results Summary:"
echo ""

// 생성된 이미지 목록
echo "🐳 Built Docker Images:"
echo "  - web3-community/frontend:%ENVIRONMENT%"
echo "  - web3-community/frontend:latest"

for %%s in (api-gateway user-service post-service comment-service auth-service notification-service) do (
    echo "  - web3-community/%%s:%ENVIRONMENT%"
    echo "  - web3-community/%%s:latest"
)

// 빌드된 JAR 파일 목록
echo ""
echo "📦 Built JAR Files:"
for %%f in (backend\**\build\libs\*.jar) do (
    echo "  - %%f"
)

// 빌드 정보 요약
echo ""
echo "📝 Build Information:"
echo "  Environment: %ENVIRONMENT%"
echo "  Timestamp: %BUILD_TIMESTAMP%"
echo "  Git Commit: %GIT_COMMIT%"
echo "  Build Duration: %TIME%"

// =============================================================================
// 📝 빌드 정보 저장
// =============================================================================
echo ""
echo "📝 Saving build information..."
set "BUILD_INFO_FILE=%PROJECT_ROOT%\build-info.json"

echo {> "%BUILD_INFO_FILE%"
echo   "timestamp": "%BUILD_TIMESTAMP%",>> "%BUILD_INFO_FILE%"
echo   "git_commit": "%GIT_COMMIT%",>> "%BUILD_INFO_FILE%"
echo   "environment": "%ENVIRONMENT%",>> "%BUILD_INFO_FILE%"
echo   "services": "%SERVICES%",>> "%BUILD_INFO_FILE%"
echo   "frontend_image": "web3-community/frontend:%ENVIRONMENT%",>> "%BUILD_INFO_FILE%"
echo   "backend_images": [>> "%BUILD_INFO_FILE%"

for %%s in (api-gateway user-service post-service comment-service auth-service notification-service) do (
    echo     "web3-community/%%s:%ENVIRONMENT%",>> "%BUILD_INFO_FILE%"
    echo     "web3-community/%%s:latest",>> "%BUILD_INFO_FILE%"
)

echo   ],>> "%BUILD_INFO_FILE%"

// 빌드된 JAR 파일 목록
echo   "jar_files": [>> "%BUILD_INFO_FILE%"

for %%f in (backend\**\build\libs\*.jar) do (
    echo     "%%f",>> "%BUILD_INFO_FILE%"
)

echo   ]>> "%BUILD_INFO_FILE%"
echo }>> "%BUILD_INFO_FILE%"

// =============================================================================
// 🧹 클린업 (선택사항)
// =============================================================================
echo ""
echo "🧹 Cleaning up build artifacts..."

// 임시 파일 정리
if exist "%PROJECT_ROOT%\frontend\node_modules\.cache" (
    rd /s /q "%PROJECT_ROOT%\frontend\node_modules\.cache"
)

if exist "%PROJECT_ROOT%\frontend\.vite" (
    rd /s /q "%PROJECT_ROOT%\frontend\.vite"
)

if exist "%PROJECT_ROOT%\backend\.gradle" (
    rd /s /q "%PROJECT_ROOT%\backend\.gradle"
)

echo "✅ Cleanup completed"

// =============================================================================
// 🎯 완료 메시지
// =============================================================================
echo ""
echo "🎉 Build completed successfully!"
echo ""
echo "📋 Next Steps:"
echo "1. Deploy to Kubernetes: ./scripts/deploy.sh deploy"
echo "2. Check status: ./scripts/status.sh"
echo "3. View logs: ./scripts/logs.sh"
echo ""
echo "🐳 To run locally:"
echo "   - Frontend: docker run -p 3000:3000 web3-community/frontend:%ENVIRONMENT%"
echo "   - Backend (example): docker run -p 8080:8080 web3-community/api-gateway:%ENVIRONMENT%"
echo ""
echo "📊 Build information saved to: %BUILD_INFO_FILE%"

cd "%PROJECT_ROOT%"

endlocal

// =============================================================================
// 🔧 도움말 함수
// =============================================================================
:help
echo ""
echo "🚀 Web3 Community Platform Build Script"
echo ""
echo "Usage: build.bat [ENVIRONMENT] [SERVICES] [SKIP_TESTS]"
echo ""
echo "Arguments:"
echo "  ENVIRONMENT  - dev, prod, test (default: dev)"
echo "  SERVICES      - all, api-gateway, user-service, post-service, comment-service, auth-service, notification-service (default: all)"
echo "  SKIP_TESTS    - true, false (default: false)"
echo ""
echo "Examples:"
echo "  build.bat dev all                    # Build all for development"
echo "  build.bat prod all                   # Build all for production"
echo "  build.bat dev api-gateway           # Build only API Gateway"
echo "  build.bat dev api-gateway true        # Build API Gateway without tests"
echo ""
goto :eof

:eof
exit /b 0