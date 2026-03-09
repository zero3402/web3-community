package com.web3community.common.kafka

/**
 * KafkaTopics - Kafka 토픽 이름 상수 관리
 *
 * MSA 환경에서 여러 서비스가 동일한 토픽 이름을 사용해야 하므로
 * 공통 모듈에서 중앙 관리합니다. 토픽 이름 오타로 인한 메시지 유실을 방지합니다.
 *
 * 토픽 네이밍 규칙: {도메인}.{이벤트명}
 * - 소문자 + 하이픈 구분
 * - 발생 도메인 기준으로 그룹화
 *
 * 토픽 구성:
 * ┌─────────────────────────────────────────────────────────┐
 * │ 토픽                     │ 발행자          │ 소비자        │
 * ├─────────────────────────────────────────────────────────┤
 * │ user.created             │ user-service    │ blockchain    │
 * │ wallet.created           │ blockchain      │ user-service  │
 * │ transaction.requested    │ user-service    │ blockchain    │
 * │ transaction.completed    │ blockchain      │ user-service  │
 * │ withdrawal.batch         │ user-service    │ blockchain    │
 * └─────────────────────────────────────────────────────────┘
 *
 * Kafka 토픽 생성 설정 (application.yml):
 * ```yaml
 * spring:
 *   kafka:
 *     bootstrap-servers: localhost:9092
 *
 * # 토픽 자동 생성 또는 Admin Client로 사전 생성 권장
 * ```
 */
object KafkaTopics {

    // ============================================================
    // 사용자 관련 토픽
    // ============================================================

    /**
     * 사용자 생성 완료 이벤트 토픽
     *
     * 발행: user-service (회원가입 완료 시)
     * 소비: blockchain-service (신규 회원 지갑 자동 생성 트리거)
     *
     * 이벤트: UserCreatedEvent
     * 파티션 수: 3 (사용자 ID 기반 파티셔닝)
     */
    const val USER_CREATED = "user.created"

    // ============================================================
    // 블록체인 지갑 관련 토픽
    // ============================================================

    /**
     * 지갑 생성 완료 이벤트 토픽
     *
     * 발행: blockchain-service (BTC/ETH 지갑 생성 완료 시)
     * 소비: user-service (사용자 프로필에 지갑 주소 업데이트)
     *
     * 이벤트: WalletCreatedEvent
     * 파티션 수: 3
     */
    const val WALLET_CREATED = "wallet.created"

    // ============================================================
    // 트랜잭션 관련 토픽
    // ============================================================

    /**
     * 트랜잭션 요청 토픽
     *
     * 발행: user-service 또는 api-gateway (출금 요청 수신 시)
     * 소비: blockchain-service (트랜잭션 처리 담당)
     *
     * 처리 흐름:
     * 1. 사용자 출금 요청 → Kafka 발행 (빠른 응답 반환)
     * 2. blockchain-service에서 비동기로 트랜잭션 처리
     * 3. 처리 완료 후 transaction.completed 토픽으로 결과 발행
     *
     * 이벤트: TransactionEvent (status: REQUESTED)
     * 파티션 수: 6 (처리량이 높으므로 파티션 더 많이)
     * 보존 기간: 72시간 (처리 실패 재시도 대비)
     */
    const val TRANSACTION_REQUESTED = "transaction.requested"

    /**
     * 트랜잭션 완료 이벤트 토픽
     *
     * 발행: blockchain-service (트랜잭션 성공/실패 확인 후)
     * 소비:
     *   - user-service (사용자에게 알림 전송)
     *   - board-service (필요 시 블록체인 인증 뱃지 부여)
     *
     * 이벤트: TransactionEvent (status: COMPLETED 또는 FAILED)
     * 파티션 수: 6
     */
    const val TRANSACTION_COMPLETED = "transaction.completed"

    /**
     * 다중 출금 배치 처리 토픽
     *
     * 발행: user-service (다수 수신자에게 일괄 전송 요청 시)
     * 소비: blockchain-service (배치 출금 처리)
     *
     * 다중 출금 전략:
     * BTC: 여러 수신자를 하나의 트랜잭션으로 묶어 UTXO 최적화
     * ETH: 수신자별 별도 트랜잭션 (스마트 컨트랙트 배치 전송 고려)
     *
     * 이벤트: WithdrawalBatchEvent (별도 클래스 - blockchain-service에 정의)
     * 파티션 수: 3
     * 보존 기간: 168시간 (7일 - 배치 처리 실패 재시도)
     */
    const val WITHDRAWAL_BATCH = "withdrawal.batch"

    // ============================================================
    // 알림 관련 토픽 (향후 확장용)
    // ============================================================

    /**
     * 사용자 알림 이벤트 토픽
     *
     * 발행: 각 서비스 (알림 발생 시)
     * 소비: notification-service (향후 구현 예정)
     *
     * 알림 유형: 거래 완료, 댓글 알림, 좋아요 알림 등
     */
    const val USER_NOTIFICATION = "user.notification"
}
