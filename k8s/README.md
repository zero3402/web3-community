# Kubernetes Deployment Guide

Kustomize 기반 멀티 스테이지 배포 구성입니다.

## 디렉토리 구조

```
k8s/
├── base/                          # 공통 리소스 (환경 무관)
│   ├── namespace.yaml
│   ├── kustomization.yaml
│   ├── infrastructure/
│   │   ├── mysql/                 # StatefulSet + ConfigMap + Service
│   │   ├── redis/                 # Deployment + Service
│   │   ├── zookeeper/             # StatefulSet + Service
│   │   └── kafka/                 # StatefulSet + Service
│   └── services/
│       ├── api-gateway/           # Deployment + Service
│       ├── auth-service/          # Deployment + Service
│       ├── user-service/          # Deployment + Service
│       ├── post-service/          # Deployment + Service
│       └── comment-service/       # Deployment + Service
└── overlays/
    ├── local/                     # 로컬 개발 환경
    │   ├── kustomization.yaml
    │   ├── secrets.yaml           # 개발용 자격증명 (committed)
    │   ├── ingress.yaml           # api.web3community.local
    │   └── patches/
    │       ├── app-patch.yaml     # profile=local, 최소 리소스
    │       └── mysql-pvc-patch.yaml  # PVC 1Gi
    ├── dev/                       # 개발 서버 환경
    │   ├── kustomization.yaml
    │   ├── secrets.yaml           # 플레이스홀더 (실제값은 외부 관리)
    │   ├── ingress.yaml           # api.dev.web3community.com + TLS
    │   └── patches/
    │       ├── app-patch.yaml     # profile=dev, 중간 리소스
    │       └── mysql-pvc-patch.yaml  # PVC 5Gi
    └── prod/                      # 프로덕션 환경
        ├── kustomization.yaml
        ├── secrets.yaml           # gitignore됨 - 직접 생성 필요
        ├── ingress.yaml           # api.web3community.com + TLS
        ├── patches/
        │   ├── app-patch.yaml     # profile=prod, 풀 리소스
        │   ├── replicas-patch.yaml   # replicas=2
        │   └── mysql-pvc-patch.yaml  # PVC 20Gi
        ├── hpa/                   # HorizontalPodAutoscaler (min2/max5)
        │   ├── api-gateway-hpa.yaml
        │   ├── auth-service-hpa.yaml
        │   ├── user-service-hpa.yaml
        │   ├── post-service-hpa.yaml
        │   └── comment-service-hpa.yaml
        └── pdb/                   # PodDisruptionBudget (minAvailable=1)
            ├── api-gateway-pdb.yaml
            ├── auth-service-pdb.yaml
            ├── user-service-pdb.yaml
            ├── post-service-pdb.yaml
            └── comment-service-pdb.yaml
```

## 사전 요구사항

- `kubectl` 1.24+
- `kustomize` 5.0+ (또는 `kubectl apply -k` 지원 버전)
- Kubernetes 클러스터 접근 권한
- (prod/dev) cert-manager 설치 및 ClusterIssuer 구성

## 환경별 배포 방법

### Local

```bash
# 로컬 k8s 클러스터 (Docker Desktop / minikube)
kubectl apply -k k8s/overlays/local

# 배포 확인
kubectl get all -n web3-community
```

로컬 환경에서 Ingress를 사용하려면 `/etc/hosts`에 추가:
```
127.0.0.1 api.web3community.local
```

### Dev

```bash
# secrets.yaml의 플레이스홀더를 실제 값으로 교체하거나
# 외부 시크릿 관리 도구(sealed-secrets, external-secrets)로 관리

kubectl apply -k k8s/overlays/dev

kubectl get all -n web3-community
```

### Prod

**사전 준비:** `k8s/overlays/prod/secrets.yaml`은 `.gitignore`에 포함되어 있습니다.
아래 형식으로 직접 생성해야 합니다:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: web3-secrets
  namespace: web3-community
type: Opaque
stringData:
  MYSQL_ROOT_PASSWORD: "<실제 비밀번호>"
  MYSQL_PASSWORD: "<실제 비밀번호>"
  JWT_SECRET: "<최소 32자 이상의 랜덤 시크릿>"
  REDIS_PASSWORD: "<실제 비밀번호>"
  GOOGLE_CLIENT_ID: "<Google OAuth2 Client ID>"
  GOOGLE_CLIENT_SECRET: "<Google OAuth2 Client Secret>"
```

```bash
# prod 배포
kubectl apply -k k8s/overlays/prod

# HPA 확인
kubectl get hpa -n web3-community

# PDB 확인
kubectl get pdb -n web3-community

# 배포 상태 확인
kubectl rollout status deployment/api-gateway -n web3-community
```

## 환경별 차이점

| 항목 | local | dev | prod |
|------|-------|-----|------|
| 이미지 태그 | `local` | `dev` | `1.0.0` |
| Spring Profile | `local` | `dev` | `prod` |
| replicas | 1 | 1 | 2 |
| MySQL PVC | 1Gi | 5Gi | 20Gi |
| HPA | X | X | O (min2/max5) |
| PDB | X | X | O (minAvailable=1) |
| Ingress TLS | X | staging | prod |
| 메모리 (request→limit) | 256Mi→512Mi | 512Mi→1Gi | 512Mi→1Gi |
| CPU (request→limit) | 100m→500m | 200m→1000m | 500m→2000m |

## 이미지 빌드 및 푸시

```bash
# 루트 Dockerfile은 ARG MODULE로 서비스 지정
docker build --build-arg MODULE=api-gateway-service \
  -t ghcr.io/web3-community/api-gateway-service:1.0.0 \
  -f backend/Dockerfile backend/

docker push ghcr.io/web3-community/api-gateway-service:1.0.0
```

## 롤링 업데이트 (prod)

```bash
# 이미지 태그 업데이트 후 적용
# kustomization.yaml의 newTag를 변경하거나 --set-image 옵션 사용

kubectl set image deployment/api-gateway \
  api-gateway=ghcr.io/web3-community/api-gateway-service:1.1.0 \
  -n web3-community

# 롤백
kubectl rollout undo deployment/api-gateway -n web3-community
```

## 리소스 제거

```bash
# 특정 환경 제거
kubectl delete -k k8s/overlays/local

# 네임스페이스 전체 제거 (주의: 데이터 포함)
kubectl delete namespace web3-community
```
