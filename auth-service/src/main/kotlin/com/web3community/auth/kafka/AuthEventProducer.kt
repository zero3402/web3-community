package com.web3community.auth.kafka

import com.web3community.common.dto.UserCreatedEvent
import com.web3community.common.kafka.KafkaTopics
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * AuthEventProducer - Auth Service Kafka 이벤트 발행 컴포넌트
 *
 * auth-service에서 발생하는 도메인 이벤트를 Kafka에 발행합니다.
 * 현재는 회원가입 완료 이벤트([UserCreatedEvent])만 발행하며,
 * 향후 비밀번호 변경, 계정 잠금 등 인증 관련 이벤트를 추가할 수 있습니다.
 *
 * Kafka 설정 (application.yml):
 * ```yaml
 * spring:
 *   kafka:
 *     bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
 *     producer:
 *       key-serializer: org.apache.kafka.common.serialization.StringSerializer
 *       value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
 *       acks: all          # 모든 ISR 복제본에 쓰기 완료 후 ack (데이터 손실 방지)
 *       retries: 3
 *       properties:
 *         enable.idempotence: true    # 중복 메시지 방지
 * ```
 *
 * 이벤트 발행 패턴:
 * - 파티션 키로 userId를 사용합니다.
 *   동일 사용자의 이벤트가 동일 파티션으로 전달되어 소비자 측에서 순서를 보장합니다.
 * - Kafka send()는 비동기입니다. 전송 결과는 로그로 확인합니다.
 *   전송 실패 시 재시도는 Kafka Producer의 `retries` 설정에 따릅니다.
 *
 * 장애 처리:
 * - Kafka 브로커가 일시적으로 다운된 경우, `retries: 3` 설정으로 재시도합니다.
 * - 재시도 모두 실패 시 예외가 발생하며, auth-service의 예외 핸들러에서 처리합니다.
 * - 이벤트 발행 실패 시 회원가입 응답도 실패로 처리하는 것이 데이터 일관성을 위해 안전합니다.
 */
@Component
class AuthEventProducer(
    /**
     * Kafka 메시지 발행 템플릿
     *
     * - 키 타입: String (userId)
     * - 값 타입: Any (다양한 이벤트 DTO를 하나의 KafkaTemplate으로 처리)
     * - 직렬화: JSON (spring-kafka JsonSerializer)
     */
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {

    private val log = LoggerFactory.getLogger(AuthEventProducer::class.java)

    /**
     * 사용자 생성 이벤트 발행
     *
     * 회원가입 완료 후 Kafka 토픽 `user.created`에 이벤트를 발행합니다.
     *
     * 소비자:
     * - user-service: 이벤트를 소비하여 사용자 정보를 DB에 저장
     * - blockchain-service: 이벤트를 소비하여 신규 회원의 블록체인 지갑 자동 생성
     *
     * 파티셔닝:
     * - 파티션 키: `event.userId` (동일 사용자 이벤트는 동일 파티션으로 전달, 순서 보장)
     *
     * 발행 실패 처리:
     * - Kafka send()의 반환값인 CompletableFuture는 여기서 구독하지 않습니다.
     * - 프로듀서 설정의 `acks: all` + `retries: 3`으로 at-least-once 전송을 보장합니다.
     * - 운영 환경에서는 Dead Letter Topic(DLT) 설정을 통해 실패 이벤트를 보관하는 것을 권장합니다.
     *
     * @param event 발행할 사용자 생성 이벤트 (userId, email, 해싱된 password, nickname, provider 포함)
     */
    fun publishUserCreatedEvent(event: UserCreatedEvent) {
        log.info(
            "UserCreatedEvent 발행: topic={}, userId={}, email={}",
            KafkaTopics.USER_CREATED,
            event.userId,
            event.email,
        )

        kafkaTemplate.send(
            KafkaTopics.USER_CREATED,   // 토픽: "user.created"
            event.userId,               // 파티션 키: userId (UUID)
            event,                      // 메시지 값: UserCreatedEvent (JSON 직렬화)
        )
    }
}
