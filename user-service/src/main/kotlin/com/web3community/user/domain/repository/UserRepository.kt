package com.web3community.user.domain.repository

import com.web3community.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/**
 * UserRepository - 사용자 JPA 리포지토리
 *
 * Spring Data JPA의 [JpaRepository]를 상속하여 기본 CRUD 및
 * 커스텀 쿼리 메서드를 제공한다.
 *
 * 기본 제공 메서드 (JpaRepository 상속):
 * - `save(entity)`: 저장/수정
 * - `findById(id)`: PK로 조회
 * - `findAll()`: 전체 조회
 * - `deleteById(id)`: PK로 삭제
 * - `count()`: 전체 수
 * - `existsById(id)`: PK 존재 여부
 *
 * 커스텀 쿼리 메서드:
 * Spring Data JPA의 메서드 이름 기반 쿼리 생성(Query Derivation)을 활용한다.
 */
interface UserRepository : JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     *
     * auth-service Feign 클라이언트에서 로그인/이메일 중복 확인 시 호출한다.
     * Optional로 반환하여 호출자가 null 처리를 명시적으로 수행하도록 강제한다.
     *
     * 쿼리: `SELECT * FROM users WHERE email = ?`
     *
     * @param email 조회할 이메일 주소
     * @return 사용자 Optional (없으면 Optional.empty())
     */
    fun findByEmail(email: String): Optional<User>

    /**
     * externalId(UUID)로 사용자 조회
     *
     * Kafka 이벤트 소비 시 멱등성 처리에 사용한다.
     * 이미 동일한 externalId의 사용자가 존재하면 생성을 건너뛴다.
     *
     * 쿼리: `SELECT * FROM users WHERE external_id = ?`
     *
     * @param externalId auth-service가 생성한 UUID 문자열
     * @return 사용자 Optional (없으면 Optional.empty())
     */
    fun findByExternalId(externalId: String): Optional<User>

    /**
     * 이메일 존재 여부 확인
     *
     * 회원가입 시 이메일 중복 검사에 사용한다.
     * SELECT 대신 EXISTS 쿼리로 최적화된다.
     *
     * 쿼리: `SELECT COUNT(*) > 0 FROM users WHERE email = ?`
     *
     * @param email 확인할 이메일 주소
     * @return 이미 사용 중인 이메일이면 true
     */
    fun existsByEmail(email: String): Boolean

    /**
     * 닉네임 존재 여부 확인
     *
     * 닉네임 변경 시 중복 검사에 사용한다.
     * SELECT 대신 EXISTS 쿼리로 최적화된다.
     *
     * 쿼리: `SELECT COUNT(*) > 0 FROM users WHERE nickname = ?`
     *
     * @param nickname 확인할 닉네임
     * @return 이미 사용 중인 닉네임이면 true
     */
    fun existsByNickname(nickname: String): Boolean

    /**
     * 제공자와 제공자 ID로 사용자 조회
     *
     * OAuth2 소셜 로그인 시 기존 회원 조회에 사용한다.
     * provider + providerId 조합으로 소셜 계정을 고유하게 식별한다.
     *
     * 쿼리: `SELECT * FROM users WHERE provider = ? AND provider_id = ?`
     *
     * @param provider 인증 제공자 ("GOOGLE", "NAVER", "KAKAO")
     * @param providerId 제공자가 부여한 사용자 고유 ID
     * @return 사용자 Optional (없으면 Optional.empty())
     */
    fun findByProviderAndProviderId(provider: String, providerId: String): Optional<User>
}
