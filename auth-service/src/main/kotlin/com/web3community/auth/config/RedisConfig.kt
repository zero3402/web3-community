package com.web3community.auth.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import java.time.Duration

/**
 * Redis 연결 및 템플릿 설정 클래스
 *
 * Auth Service에서 Redis를 사용하는 목적:
 * 1. Refresh Token 저장 (TTL 7일, Refresh Token Rotation 전략)
 * 2. (옵션) 로그아웃된 Access Token 블랙리스트 관리
 *
 * 클라이언트: Lettuce (비동기, 스레드 안전, 커넥션 풀링 지원)
 * Jedis 대신 Lettuce를 선택한 이유:
 * - 단일 커넥션 공유 (스레드 안전): 커넥션 풀 없이도 동시성 처리 가능
 * - 비동기 I/O 지원 (Netty 기반)
 * - Spring Boot 기본 내장 클라이언트
 */
@Configuration
class RedisConfig(
    @Value("\${spring.data.redis.host}") private val host: String,
    @Value("\${spring.data.redis.port}") private val port: Int,
    @Value("\${spring.data.redis.password:}") private val password: String,
    @Value("\${spring.data.redis.database:0}") private val database: Int,
) {

    /**
     * Lettuce 커넥션 팩토리 빈 등록
     *
     * Lettuce 커넥션 풀 설정 포함.
     * 커넥션 풀을 사용하면 트랜잭션이나 블로킹 명령어(BLPOP 등) 사용 시 안전하다.
     *
     * @return LettuceConnectionFactory Redis 연결 팩토리
     */
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        // Redis 서버 접속 정보 설정
        val redisConfig = RedisStandaloneConfiguration(host, port).apply {
            if (password.isNotBlank()) {
                // 비밀번호가 설정된 경우에만 인증 정보 등록
                setPassword(password)
            }
            setDatabase(database)
        }

        // Lettuce 커넥션 풀 설정
        val poolConfig = GenericObjectPoolConfig<Any>().apply {
            maxTotal = 8          // 최대 커넥션 수
            maxIdle = 8           // 최대 유휴 커넥션 수
            minIdle = 2           // 최소 유휴 커넥션 수 (미리 유지)
            setMaxWait(Duration.ofMillis(-1))  // 커넥션 획득 최대 대기 시간 (-1: 무한 대기)
        }

        // Lettuce 풀링 클라이언트 설정
        val lettuceClientConfig: LettuceClientConfiguration =
            LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofSeconds(2))  // Redis 명령 실행 타임아웃
                .build()

        return LettuceConnectionFactory(redisConfig, lettuceClientConfig)
    }

    /**
     * RedisTemplate 빈 등록
     *
     * 키와 값을 모두 String으로 직렬화한다.
     * Refresh Token은 문자열(JWT 토큰 값)이므로 StringRedisSerializer가 적합하다.
     *
     * 직렬화 전략:
     * - keySerializer: StringRedisSerializer (사람이 읽기 쉬운 키)
     * - valueSerializer: StringRedisSerializer (JWT 토큰 문자열 저장)
     * - hashKeySerializer: StringRedisSerializer
     * - hashValueSerializer: StringRedisSerializer
     *
     * 기본 JdkSerializationRedisSerializer를 사용하지 않는 이유:
     * - JDK 직렬화는 Java 클래스에 의존적이어서 다른 언어/서비스에서 읽기 불편
     * - 저장된 데이터가 바이너리로 표현되어 Redis CLI 디버깅이 어려움
     *
     * @param redisConnectionFactory Redis 연결 팩토리
     * @return RedisTemplate<String, String>
     */
    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        return RedisTemplate<String, String>().apply {
            // 연결 팩토리 주입
            setConnectionFactory(redisConnectionFactory)

            // 키 직렬화: UTF-8 문자열
            keySerializer = StringRedisSerializer()

            // 값 직렬화: UTF-8 문자열 (JWT 토큰 저장)
            valueSerializer = StringRedisSerializer()

            // Hash 자료구조 키 직렬화
            hashKeySerializer = StringRedisSerializer()

            // Hash 자료구조 값 직렬화
            hashValueSerializer = StringRedisSerializer()

            // 설정 적용 (afterPropertiesSet 내부적으로 호출)
            afterPropertiesSet()
        }
    }
}
