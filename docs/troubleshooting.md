# 🐛 문제 해결 가이드

이 문서는 Web3 Community Platform에서 발생할 수 있는 흔한 문제들과 해결 방법을 설명합니다.

## 🔍 문제 진단 순서

### 1단계: 기본 상태 확인
```bash
# 네임스페이스 상태
kubectl get namespace web3-community

# 전체 파드 상태
kubectl get pods -n web3-community

# 서비스 상태
kubectl get services -n web3-community

# PVC 상태
kubectl get pvc -n web3-community
```

### 2단계: 상세 문제 분석
```bash
# 이벤트 확인
kubectl get events -n web3-community --sort-by='.lastTimestamp'

# 파드 상세 정보
kubectl describe pods -n web3-community

# 로그 확인
./scripts/logs.sh show <service-name>
```

## 🚨 흔한 문제와 해결책

### 문제 1: Pod가 Pending 상태

#### 원인
- 리소스 부족 (CPU, Memory)
- PVC 미바인딩
- 노드 선택자 불일치
- 이미지 풀 에러

#### 해결책
```bash
# 1. 리소스 사용량 확인
kubectl top nodes
kubectl describe nodes

# 2. Pod 상세 정보 확인
kubectl describe pod <pod-name> -n web3-community

# 3. PVC 상태 확인
kubectl get pvc -n web3-community
kubectl describe pvc <pvc-name> -n web3-community

# 4. 이미지 확인
kubectl get events -n web3-community | grep "Failed to pull image"

# 5. 리소스 제한 조정
kubectl edit deployment <deployment-name> -n web3-community
# resources.requests/limits 값을 조정
```

#### 수정 예시
```yaml
# k8s/05-applications/user-service/deployment.yaml
resources:
  requests:
    memory: "512Mi"    # 증량
    cpu: "500m"        # 증량
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### 문제 2: Pod가 CrashLoopBackOff 상태

#### 원인
- 애플리케이션 시작 실패
- 데이터베이스 연결 오류
- 포트 충돌
- 메모리 부족

#### 해결책
```bash
# 1. Pod 로그 확인
kubectl logs <pod-name> -n web3-community

# 2. 이전 로그 확인
kubectl logs <pod-name> -n web3-community --previous

# 3. Pod 내부 접속하여 확인
kubectl exec -it <pod-name> -n web3-community -- /bin/bash

# 4. ConfigMap/Secret 확인
kubectl get configmaps -n web3-community
kubectl get secrets -n web3-community

# 5. 데이터베이스 연결 확인
kubectl run db-test --image=mysql:8.0 --rm -it --restart=Never \
  -- mysql -h mysql-service -u web3user -p
```

#### 일반적인 에러 해결
```bash
# MySQL 연결 에러
# 1. Secret 값 확인
echo "mysql-password" | base64 -d

# 2. Secret 업데이트
kubectl patch secret database-secrets -n web3-community \
  -p '{"data":{"mysql-password":"'$(echo -n 'newpassword' | base64)'"}}'

# 3. Deployment 재시작
kubectl rollout restart deployment/user-service-deployment -n web3-community
```

### 문제 3: 서비스 접속 불가

#### 원인
- 엔드포인트 미생성
- 포트 불일치
- 서비스 선택자 오류
- 방화벽 문제

#### 해결책
```bash
# 1. 서비스 상태 확인
kubectl get services -n web3-community -o wide

# 2. 엔드포인트 확인
kubectl get endpoints -n web3-community

# 3. 서비스 상세 정보
kubectl describe service <service-name> -n web3-community

# 4. Pod 라벨 확인
kubectl get pods -n web3-community --show-labels

# 5. 네트워크 연결 테스트
kubectl run network-test --image=busybox --rm -it --restart=Never -- \
  nslookup <service-name>.web3-community
```

#### 서비스 디버깅 예시
```bash
# API Gateway 접속 테스트
kubectl run api-test --image=curlimages/curl --rm -it --restart=Never -- \
  curl -X GET http://api-gateway-service:8080/actuator/health

# 직접 Pod 접속 테스트
kubectl port-forward deployment/api-gateway-deployment 8080:8080 -n web3-community
curl -X GET http://localhost:8080/actuator/health
```

### 문제 4: 데이터베이스 연결 실패

#### MySQL 연결 실패
```bash
# 1. MySQL Pod 상태 확인
kubectl get pods -l component=database,database=mysql -n web3-community

# 2. MySQL 로그 확인
kubectl logs deployment/mysql-deployment -n web3-community

# 3. MySQL 접속 테스트
kubectl exec -it deployment/mysql-deployment -n web3-community -- \
  mysql -u root -p -e "SHOW DATABASES;"

# 4. 사용자 확인
kubectl exec -it deployment/mysql-deployment -n web3-community -- \
  mysql -u root -p -e "SELECT user, host FROM mysql.user;"

# 5. 권한 부여
kubectl exec -it deployment/mysql-deployment -n web3-community -- \
  mysql -u root -p -e "GRANT ALL PRIVILEGES ON *.* TO 'web3user'@'%'; FLUSH PRIVILEGES;"
```

#### MongoDB 연결 실패
```bash
# 1. MongoDB Pod 상태 확인
kubectl get pods -l component=database,database=mongodb -n web3-community

# 2. MongoDB 접속 테스트
kubectl exec -it deployment/mongodb-deployment -n web3-community -- \
  mongosh --eval "db.adminCommand('ping')"

# 3. 사용자 확인
kubectl exec -it deployment/mongodb-deployment -n web3-community -- \
  mongosh --eval "db.getUsers()"

# 4. 사용자 생성
kubectl exec -it deployment/mongodb-deployment -n web3-community -- \
  mongosh --eval "
    use admin
    db.createUser({
      user: 'mongouser',
      pwd: 'mongopass',
      roles: [
        {
          role: 'readWrite',
          db: 'web3_posts'
        }
      ]
    })
  "
```

#### Redis 연결 실패
```bash
# 1. Redis Pod 상태 확인
kubectl get pods -l component=database,database=redis -n web3-community

# 2. Redis 접속 테스트
kubectl exec -it deployment/redis-deployment -n web3-community -- \
  redis-cli -a $REDIS_PASSWORD ping

# 3. 메모리 상태 확인
kubectl exec -it deployment/redis-deployment -n web3-community -- \
  redis-cli -a $REDIS_PASSWORD INFO memory
```

#### Kafka 연결 실패
```bash
# 1. Kafka 상태 확인
kubectl get pods -l component=database,database=kafka -n web3-community

# 2. Zookeeper 상태 확인 (Kafka 선행 조건)
kubectl exec -it deployment/zookeeper-deployment -n web3-community -- \
  zookeeper-shell zookeeper-service:2181 -e "ls /"

# 3. Kafka 브로커 상태 확인
kubectl exec -it deployment/kafka-deployment -n web3-community -- \
  kafka-broker-api-versions --bootstrap-server localhost:9092

# 4. 토픽 생성 테스트
kubectl exec -it deployment/kafka-deployment -n web3-community -- \
  kafka-topics --bootstrap-server localhost:9092 --create --topic test --partitions 1 --replication-factor 1
```

### 문제 5: Ingress 접속 불가

#### 원인
- Ingress Controller 미설치
- DNS 설정 문제
- 인증서 오류
- 경로 룰 오류

#### 해결책
```bash
# 1. Ingress Controller 확인
kubectl get pods -n ingress-nginx

# 2. Ingress 상태 확인
kubectl get ingress -n web3-community
kubectl describe ingress web3-community-ingress -n web3-community

# 3. Ingress Controller 로그 확인
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller

# 4. DNS 확인
nslookup web3-community.local

# 5. 로컬 hosts 파일 확인
cat /etc/hosts | grep web3-community.local
```

#### Windows hosts 파일 수정
```powershell
# 관리자 권한으로 PowerShell 실행
notepad $env:windir\System32\drivers\etc\hosts

# 다음 라인 추가
127.0.0.1 web3-community.local
```

### 문제 6: 리소스 과도 사용

#### CPU/메모리 부족
```bash
# 1. 리소스 사용량 확인
kubectl top pods -n web3-community
kubectl top nodes

# 2. HPA 상태 확인
kubectl get hpa -n web3-community

# 3. 리소스 제한 조정
kubectl edit deployment <deployment-name> -n web3-community

# 4. Pod 재시작
kubectl delete pod <pod-name> -n web3-community
```

#### 디스크 공간 부족
```bash
# 1. PVC 상태 확인
kubectl get pvc -n web3-community
kubectl describe pvc <pvc-name> -n web3-community

# 2. 노드 디스크 사용량 확인
kubectl exec -it <pod-name> -n web3-community -- df -h

# 3. 로그 파일 정리
kubectl exec -it <pod-name> -n web3-community -- find /var/log -name "*.log" -mtime +7 -delete

# 4. 데이터 정리 (MongoDB 예시)
kubectl exec -it deployment/mongodb-deployment -n web3-community -- \
  mongosh --eval "
    use web3_posts
    db.posts.deleteMany({createdAt: {\$lt: new Date(Date.now() - 30*24*60*60*1000)}})
  "
```

## 🛠️ 고급 문제 해결

### 디버깅 모드 활성화
```yaml
# Spring Boot 디버그 모드
env:
- name: JAVA_OPTS
  value: "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Vue.js 개발 모드
env:
- name: NODE_ENV
  value: "development"
```

### 포트 포워딩으로 로컬 디버깅
```bash
# API Gateway
kubectl port-forward deployment/api-gateway-deployment 8080:8080 -n web3-community

# MySQL
kubectl port-forward deployment/mysql-deployment 3306:3306 -n web3-community

# Redis
kubectl port-forward deployment/redis-deployment 6379:6379 -n web3-community
```

### 스냅샷 및 복원
```bash
# 디렉토리 구조 백업
kubectl get all -n web3-community -o yaml > backup.yaml

# 특정 리소스 백업
kubectl get secret,database-secrets -n web3-community -o yaml > secrets-backup.yaml

# 복원
kubectl apply -f backup.yaml
```

## 📞 추가 도움말

### 스크립트 활용
```bash
# 전체 상태 확인
./scripts/status.sh diagnose

# 실시간 로그 모니터링
./scripts/logs.sh follow api-gateway

# 네트워크 테스트
./scripts/status.sh network
```

### 유용한 명령어
```bash
# 모든 리소스 삭제
kubectl delete all --all -n web3-community

# 강제 Pod 삭제
kubectl delete pod <pod-name> -n web3-community --grace-period=0 --force

# 네임스페이스 초기화
kubectl delete namespace web3-community --grace-period=0 --force

# 롤아웃 히스토리 확인
kubectl rollout history deployment/api-gateway-deployment -n web3-community

# 특정 버전으로 롤백
kubectl rollout undo deployment/api-gateway-deployment -n web3-community --to-revision=2
```

## 🔄 재해 복구 절차

### 1. 데이터베이스 복원
```bash
# MySQL 복원
kubectl cp mysql-backup.sql deployment/mysql-deployment:/tmp/backup.sql -n web3-community
kubectl exec deployment/mysql-deployment -n web3-community -- \
  mysql -u root -p web3_community < /tmp/backup.sql

# MongoDB 복원
kubectl cp mongodb-backup/ deployment/mongodb-deployment:/tmp/restore -n web3-community
kubectl exec deployment/mongodb-deployment -n web3-community -- \
  mongorestore --db web3_posts /tmp/restore/web3_posts
```

### 2. 서비스 재배포
```bash
# 전체 재시작
./scripts/deploy.sh restart-all

# 무중단 업데이트
kubectl set image deployment/api-gateway-deployment \
  api-gateway=web3-community/api-gateway:new-version -n web3-community
kubectl rollout status deployment/api-gateway-deployment -n web3-community
```

이 가이드로 해결되지 않는 문제가 있다면 GitHub Issues에 상세한 에러 로그와 함께 문의해주세요!