package com.web3community.user.service

import com.web3community.common.dto.UserCreatedEvent
import com.web3community.common.exception.BusinessException
import com.web3community.common.exception.ErrorCode
import com.web3community.user.domain.entity.User
import com.web3community.user.domain.repository.UserRepository
import com.web3community.user.dto.InternalUserResponse
import com.web3community.user.dto.UserResponse
import com.web3community.user.dto.UserUpdateRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * UserService - 사용자 비즈니스 로직 서비스
 *
 * 사용자 생성, 조회, 수정 핵심 비즈니스 로직을 담당한다.
 *
 * 주요 기능:
 * - [createFromEvent]: Kafka 이벤트 소비 시 사용자 생성 (멱등성 보장)
 * - [findByEmail]: auth-service Feign 호출용 내부 조회 (비밀번호 포함)
 * - [findByProviderAndProviderId]: OAuth2 기존 회원 조회
 * - [findById]: 공개 프로필 조회
 * - [findMe]: 본인 프로필 조회
 * - [updateMe]: 본인 프로필 수정 (닉네임, 프로필 이미지)
 *
 * 트랜잭션 정책:
 * - 클래스 레벨에 @Transactional(readOnly = true) 적용
 * - 쓰기 작업 메서드에만 @Transactional(readOnly = false) 오버라이드
 */
@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {

    private val log = LoggerFactory.getLogger(UserService::class.java)

    /**
     * Kafka 이벤트로부터 사용자 생성 (멱등성 처리)
     *
     * auth-service가 발행한 [UserCreatedEvent]를 소비하여 DB에 사용자를 생성한다.
     *
     * 멱등성 보장:
     * - [UserCreatedEvent.userId] (externalId)가 이미 존재하면 생성을 건너뛴다.
     * - Kafka의 at-least-once 전달 보장으로 인해 동일 이벤트가 중복 소비될 수 있다.
     *   이 경우 두 번째 소비부터는 무시하여 중복 레코드 생성을 방지한다.
     *
     * @param event auth-service가 발행한 사용자 생성 이벤트
     */
    @Transactional
    fun createFromEvent(event: UserCreatedEvent) {
        // 멱등성 체크: 동일한 externalId가 이미 존재하면 건너뜀
        if (userRepository.findByExternalId(event.userId).isPresent) {
            log.info("사용자 이미 존재, 생성 건너뜀: externalId={}", event.userId)
            return
        }

        val user = User(
            externalId = event.userId,
            email = event.email,
            password = event.password,
            nickname = event.nickname,
            provider = event.provider,
            providerId = event.providerId,
            profileImageUrl = event.profileImageUrl,
        )

        userRepository.save(user)
        log.info("사용자 생성 완료: externalId={}, email={}", event.userId, event.email)
    }

    /**
     * 이메일로 사용자 조회 (내부 API 전용)
     *
     * auth-service Feign 클라이언트가 로그인 시 호출하는 내부 메서드이다.
     * 비밀번호(BCrypt 해시) 포함 [InternalUserResponse]를 반환한다.
     *
     * 보안 주의:
     * - 반환값에 비밀번호가 포함되므로 내부 통신([InternalUserController])에서만 사용해야 한다.
     * - 공개 API에서 이 메서드를 호출하지 않도록 주의한다.
     *
     * @param email 조회할 이메일 주소
     * @return 내부 응답 DTO (비밀번호 포함)
     * @throws BusinessException 사용자가 존재하지 않으면 [ErrorCode.USER_001]
     */
    fun findByEmail(email: String): InternalUserResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { BusinessException(ErrorCode.USER_001) }
        return InternalUserResponse.from(user)
    }

    /**
     * 제공자와 제공자 ID로 사용자 조회 (OAuth2 내부 API 전용)
     *
     * OAuth2 소셜 로그인 시 auth-service Feign 클라이언트가 기존 회원 여부를 확인한다.
     *
     * @param provider 인증 제공자 ("GOOGLE", "NAVER", "KAKAO")
     * @param providerId 제공자가 부여한 사용자 고유 ID
     * @return 내부 응답 DTO (비밀번호 포함)
     * @throws BusinessException 사용자가 존재하지 않으면 [ErrorCode.USER_001]
     */
    fun findByProviderAndProviderId(provider: String, providerId: String): InternalUserResponse {
        val user = userRepository.findByProviderAndProviderId(provider, providerId)
            .orElseThrow { BusinessException(ErrorCode.USER_001) }
        return InternalUserResponse.from(user)
    }

    /**
     * DB PK로 공개 프로필 조회
     *
     * 다른 사용자의 공개 프로필을 조회할 때 사용한다.
     * 비밀번호 등 민감 정보는 포함하지 않는 [UserResponse]를 반환한다.
     *
     * @param id DB auto-increment PK (Long)
     * @return 공개 프로필 응답 DTO
     * @throws BusinessException 사용자가 존재하지 않으면 [ErrorCode.USER_001]
     */
    fun findById(id: Long): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.USER_001) }
        return UserResponse.from(user)
    }

    /**
     * 본인 프로필 조회
     *
     * API Gateway가 전달하는 X-User-Id 헤더 값(Long)으로 자신의 프로필을 조회한다.
     * [findById]와 동일한 로직이지만 의미적으로 분리하여 컨트롤러 가독성을 높인다.
     *
     * @param userId X-User-Id 헤더에서 파싱한 DB PK (Long)
     * @return 공개 프로필 응답 DTO
     * @throws BusinessException 사용자가 존재하지 않으면 [ErrorCode.USER_001]
     */
    fun findMe(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_001) }
        return UserResponse.from(user)
    }

    /**
     * 본인 프로필 수정 (닉네임, 프로필 이미지)
     *
     * API Gateway가 전달하는 X-User-Id 헤더 값(Long)으로 자신의 프로필을 수정한다.
     *
     * 수정 가능 항목:
     * - 닉네임: 2~30자, 다른 사용자가 사용 중이면 [ErrorCode.USER_003] 예외
     * - 프로필 이미지 URL: null이면 이미지 제거
     *
     * 수정 불가 항목:
     * - 이메일, 비밀번호, provider, providerId, status (별도 프로세스 필요)
     *
     * @param userId X-User-Id 헤더에서 파싱한 DB PK (Long)
     * @param req 수정 요청 DTO
     * @return 수정된 공개 프로필 응답 DTO
     * @throws BusinessException 사용자가 없으면 [ErrorCode.USER_001]
     * @throws BusinessException 닉네임 중복이면 [ErrorCode.USER_003]
     */
    @Transactional
    fun updateMe(userId: Long, req: UserUpdateRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_001) }

        // 닉네임이 변경된 경우에만 중복 검사 수행
        if (user.nickname != req.nickname && userRepository.existsByNickname(req.nickname)) {
            throw BusinessException(ErrorCode.USER_003)
        }

        // Dirty Checking으로 UPDATE 쿼리 자동 실행 (@Transactional 범위 내)
        user.nickname = req.nickname
        user.profileImageUrl = req.profileImageUrl

        log.info("사용자 프로필 수정 완료: userId={}, nickname={}", userId, req.nickname)
        return UserResponse.from(user)
    }
}
