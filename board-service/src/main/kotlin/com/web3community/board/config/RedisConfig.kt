package com.web3community.board.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.web3community.board.dto.BoardResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Redis Reactive 설정 클래스
 *
 * ## 역할
 * - ReactiveRedisTemplate Bean 등록 (타입별 직렬화 설정)
 * - 게시글 캐시용 JSON 직렬화 ObjectMapper 설정
 *
 * ## 캐싱 전략
 * ### 게시글 상세 캐시 (BoardResponse)
 * - Key: `board:detail:{boardId}`
 * - TTL: 5분 (300초)
 * - 직렬화: JSON (Jackson)
 * - 무효화: 게시글 수정/삭제 시 해당 키 삭제
 *
 * ### 좋아요/싫어요 카운트 캐시
 * - Key: `board:likes:{boardId}`, `board:dislikes:{boardId}`
 * - TTL: 없음 (MongoDB 동기화 시 갱신)
 * - 연산: INCR/DECR (원자적 카운터)
 * - 직렬화: String (숫자 문자열)
 *
 * ## 왜 두 가지 Template을 사용하는가?
 * - `ReactiveStringRedisTemplate`: String 키/값 전용 (카운터, 단순 값 저장)
 * - `ReactiveRedisTemplate<String, BoardResponse>`: JSON 객체 캐싱 (게시글 상세)
 *
 * @see BoardService 캐싱 로직 적용 서비스
 * @see ReactionService Redis INCR/DECR 사용 서비스
 */
@Configuration
class RedisConfig {

    /**
     * 게시글 캐싱용 Jackson ObjectMapper 설정
     *
     * Redis에 저장될 JSON의 직렬화/역직렬화 규칙을 정의합니다.
     *
     * 설정 내용:
     * - `JavaTimeModule`: LocalDateTime, Instant 등 Java Time API 지원
     * - `KotlinModule`: Kotlin data class, null-safety 지원
     * - `WRITE_DATES_AS_TIMESTAMPS=false`: 날짜를 ISO-8601 문자열로 저장
     *
     * @return 캐시 전용 ObjectMapper (Spring MVC용 ObjectMapper와 별도 관리)
     */
    @Bean(name = ["redisObjectMapper"])
    fun redisObjectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            // Java 8+ 날짜/시간 타입 지원 (LocalDateTime, ZonedDateTime 등)
            registerModule(JavaTimeModule())
            // Kotlin data class 직렬화 지원 (기본 생성자 없어도 역직렬화 가능)
            registerModule(kotlinModule())
            // 날짜를 Unix 타임스탬프(숫자) 대신 ISO-8601 문자열로 저장
            // 예: 1705312200000 → "2024-01-15T12:30:00"
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // null 값은 JSON에 포함하지 않음 (저장 용량 최적화)
            // disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        }
    }

    /**
     * 문자열 전용 ReactiveRedisTemplate
     *
     * 주요 사용 사례:
     * - 좋아요 카운터: `board:likes:{boardId}` → INCR/DECR
     * - 조회수 카운터: `board:views:{boardId}` → INCR
     * - 단순 플래그 값 저장
     *
     * Key/Value 모두 StringRedisSerializer 사용으로 가장 단순하고 효율적.
     *
     * @param connectionFactory Lettuce 기반 Reactive Redis 연결 팩토리 (자동 주입)
     * @return String 타입 ReactiveRedisTemplate
     */
    @Bean
    fun reactiveStringRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory
    ): ReactiveStringRedisTemplate {
        // ReactiveStringRedisTemplate은 String 직렬화가 기본 설정된 편의 클래스
        return ReactiveStringRedisTemplate(connectionFactory)
    }

    /**
     * 게시글 응답 DTO 캐싱용 ReactiveRedisTemplate
     *
     * 게시글 상세 조회 결과(BoardResponse)를 JSON으로 직렬화하여 Redis에 저장합니다.
     *
     * 직렬화 구성:
     * - Key: StringRedisSerializer (UTF-8 문자열)
     * - Value: Jackson2JsonRedisSerializer<BoardResponse> (JSON 바이트 배열)
     * - Hash Key/Value: StringRedisSerializer (해시 연산용)
     *
     * 사용 예시:
     * ```kotlin
     * // 캐시 저장
     * boardRedisTemplate.opsForValue()
     *     .set("board:detail:$boardId", boardResponse, Duration.ofMinutes(5))
     *     .awaitSingle()
     *
     * // 캐시 조회
     * boardRedisTemplate.opsForValue()
     *     .get("board:detail:$boardId")
     *     .awaitSingleOrNull()
     * ```
     *
     * @param connectionFactory Lettuce 기반 Reactive Redis 연결 팩토리 (자동 주입)
     * @param redisObjectMapper 캐시 전용 Jackson ObjectMapper
     * @return BoardResponse 타입 ReactiveRedisTemplate
     */
    @Bean
    fun boardRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory,
        redisObjectMapper: ObjectMapper
    ): ReactiveRedisTemplate<String, BoardResponse> {
        // String 키 직렬화기: Redis 키를 UTF-8 문자열로 저장
        val stringSerializer = StringRedisSerializer()

        // BoardResponse 값 직렬화기: JSON 형태로 Redis에 저장
        // Jackson2JsonRedisSerializer: 타입 정보를 JSON에 포함하여 안전한 역직렬화 보장
        val boardResponseSerializer = Jackson2JsonRedisSerializer(
            redisObjectMapper,
            BoardResponse::class.java
        )

        // 직렬화 컨텍스트 구성: 키/값/해시키/해시값 각각의 직렬화기 설정
        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, BoardResponse>(stringSerializer)
            .value(boardResponseSerializer)           // 값 직렬화기 (BoardResponse → JSON)
            .hashKey(stringSerializer)                // 해시 키 직렬화기
            .hashValue(boardResponseSerializer)       // 해시 값 직렬화기
            .build()

        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }

    /**
     * 범용 String-String ReactiveRedisTemplate
     *
     * JSON 문자열을 직접 저장/조회할 때 사용합니다.
     * BoardResponse 외 다른 DTO 캐싱 시 이 Template으로 JSON 문자열 변환 후 저장.
     *
     * @param connectionFactory Lettuce 기반 Reactive Redis 연결 팩토리
     * @return String-String 타입 ReactiveRedisTemplate
     */
    @Bean
    fun genericRedisTemplate(
        connectionFactory: ReactiveRedisConnectionFactory
    ): ReactiveRedisTemplate<String, String> {
        val stringSerializer = StringRedisSerializer()

        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, String>(stringSerializer)
            .value(stringSerializer)
            .build()

        return ReactiveRedisTemplate(connectionFactory, serializationContext)
    }
}
