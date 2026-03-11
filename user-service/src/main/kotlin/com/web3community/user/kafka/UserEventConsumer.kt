package com.web3community.user.kafka

import com.web3community.common.dto.UserCreatedEvent
import com.web3community.common.kafka.KafkaTopics
import com.web3community.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * UserEventConsumer - Kafka 사용자 이벤트 소비자
 *
 * auth-service가 발행하는 사용자 관련 Kafka 이벤트를 소비하여
 * user-service DB에 사용자 레코드를 생성한다.
 *
 * 소비 토픽:
 * - [KafkaTopics.USER_CREATED] (`user.created`): 회원가입 완료 이벤트
 *
 * 멱등성 설계:
 * - Kafka는 at-least-once 전달을 보장하므로 동일 이벤트가 중복 소비될 수 있다.
 * - [UserService.createFromEvent]에서 externalId 존재 여부를 먼저 확인하여
 *   중복 처리 시 DB 저장을 건너뛴다.
 *
 * 에러 처리:
 * - 비즈니스 로직 예외는 로그만 남기고 오프셋을 커밋한다 (dead-letter-queue 미적용).
 *   동일한 잘못된 메시지를 무한 재시도하는 것을 방지하기 위함이다.
 * - 향후 Dead Letter Topic(DLT)을 추가하여 처리 실패 메시지를 별도 관리 권장.
 *
 * Kafka Consumer 설정 (application.yml):
 * ```yaml
 * spring:
 *   kafka:
 *     consumer:
 *       group-id: user-service-group
 *       auto-offset-reset: earliest
 *       key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
 *       value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
 * ```
 */
@Component
class UserEventConsumer(
    private val userService: UserService,
) {

    private val log = LoggerFactory.getLogger(UserEventConsumer::class.java)

    /**
     * 사용자 생성 이벤트 처리
     *
     * auth-service가 회원가입 완료 후 발행하는 [UserCreatedEvent]를 소비한다.
     * 이벤트를 수신하면 [UserService.createFromEvent]를 호출하여 DB에 저장한다.
     *
     * 처리 흐름:
     * 1. Kafka 브로커에서 `user.created` 토픽의 메시지 수신
     * 2. JSON 역직렬화 → [UserCreatedEvent]
     * 3. [UserService.createFromEvent] 호출 (멱등성 체크 포함)
     * 4. 오프셋 자동 커밋 (enable-auto-commit: true 기본값)
     *
     * 에러 처리:
     * - Exception 발생 시 로그 기록 후 오프셋 커밋 (메시지 스킵)
     * - 처리 실패한 메시지는 DLT(Dead Letter Topic)으로 전송 권장 (현재 미적용)
     *
     * @param event 역직렬화된 [UserCreatedEvent] 페이로드
     * @param partition 메시지가 수신된 파티션 번호 (로깅용)
     * @param offset 메시지 오프셋 (로깅용)
     */
    @KafkaListener(
        topics = [KafkaTopics.USER_CREATED],
        groupId = "user-service-group",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun handleUserCreated(
        @Payload event: UserCreatedEvent,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
    ) {
        log.info(
            "UserCreatedEvent 수신: userId={}, email={}, provider={}, partition={}, offset={}",
            event.userId, event.email, event.provider, partition, offset,
        )

        try {
            userService.createFromEvent(event)
            log.info("UserCreatedEvent 처리 완료: userId={}", event.userId)
        } catch (e: Exception) {
            // 처리 실패 시 로그만 남기고 오프셋 커밋 (무한 재처리 방지)
            // 향후 Dead Letter Topic(DLT)으로 전송하는 방식으로 개선 가능
            log.error(
                "UserCreatedEvent 처리 실패: userId={}, error={}",
                event.userId, e.message, e
            )
        }
    }
}
