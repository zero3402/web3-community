package com.web3community.user.repository

import com.web3community.user.entity.User
import com.web3community.user.dto.UserRole
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * 사용자 리포지토리 인터페이스
 * Spring Data Reactive를 사용하여 리액티브 CRUD operations 제공
 * 커스텀 쿼리 메소드 추가
 */
@Repository
interface UserRepository : ReactiveCrudRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     * @param email 사용자 이메일
     * @return 사용자 정보 (Mono<User>)
     */
    fun findByEmail(email: String): Mono<User>

    /**
     * 사용자 이름으로 사용자 조회
     * @param username 사용자 이름
     * @return 사용자 정보 (Mono<User>)
     */
    fun findByUsername(username: String): Mono<User>

    /**
     * 사용자 이름으로 사용자 존재 여부 확인
     * @param username 사용자 이름
     * @return 존재 여부 (Mono<Boolean>)
     */
    fun existsByUsername(username: String): Mono<Boolean>

    /**
     * 이메일로 사용자 존재 여부 확인
     * @param email 사용자 이메일
     * @return 존재 여부 (Mono<Boolean>)
     */
    fun existsByEmail(email: String): Mono<Boolean>

    /**
     * 활성화 상태로 사용자 목록 조회
     * @param isActive 활성화 상태
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByIsActive(isActive: Boolean, pageable: Pageable): Flux<User>

    /**
     * 사용자 역할로 사용자 목록 조회
     * @param role 사용자 역할
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByRole(role: UserRole, pageable: Pageable): Flux<User>

    /**
     * 이메일 인증 상태로 사용자 목록 조회
     * @param isEmailVerified 이메일 인증 상태
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByIsEmailVerified(isEmailVerified: Boolean, pageable: Pageable): Flux<User>

    /**
     * 사용자 이름으로 사용자 검색 (대소문자 무시, 부분 일치)
     * @param username 검색할 사용자 이름
     * @param pageable 페이징 정보
     * @return 검색된 사용자 목록 (Flux<User>)
     */
    fun findByUsernameContainingIgnoreCase(username: String, pageable: Pageable): Flux<User>

    /**
     * 이메일로 사용자 검색 (대소문자 무시, 부분 일치)
     * @param email 검색할 이메일
     * @param pageable 페이징 정보
     * @return 검색된 사용자 목록 (Flux<User>)
     */
    fun findByEmailContainingIgnoreCase(email: String, pageable: Pageable): Flux<User>

    /**
     * 표시 이름으로 사용자 검색 (대소문자 무시, 부분 일치)
     * @param displayName 검색할 표시 이름
     * @param pageable 페이징 정보
     * @return 검색된 사용자 목록 (Flux<User>)
     */
    fun findByDisplayNameContainingIgnoreCase(displayName: String, pageable: Pageable): Flux<User>

    /**
     * 활성화된 사용자 수 카운트
     * @param isActive 활성화 상태
     * @return 사용자 수 (Mono<Long>)
     */
    fun countByIsActive(isActive: Boolean): Mono<Long>

    /**
     * 이메일 인증된 사용자 수 카운트
     * @param isEmailVerified 이메일 인증 상태
     * @return 사용자 수 (Mono<Long>)
     */
    fun countByIsEmailVerified(isEmailVerified: Boolean): Mono<Long>

    /**
     * 역할별 사용자 수 카운트
     * @param role 사용자 역할
     * @return 사용자 수 (Mono<Long>)
     */
    fun countByRole(role: UserRole): Mono<Long>

    /**
     * 특정 날짜 이후에 가입한 사용자 수 카운트
     * @param createdAt 기준 날짜
     * @return 사용자 수 (Mono<Long>)
     */
    fun countByCreatedAtAfter(createdAt: LocalDateTime): Mono<Long>

    /**
     * 마지막 로그인 시간이 특정 시간 이후인 사용자 목록 조회
     * @param lastLoginAt 기준 시간
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByLastLoginAtAfter(lastLoginAt: LocalDateTime, pageable: Pageable): Flux<User>

    /**
     * 마지막 로그인 시간이 특정 시간 이전인 사용자 목록 조회 (오랜동안 접속하지 않은 사용자)
     * @param lastLoginAt 기준 시간
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByLastLoginAtBefore(lastLoginAt: LocalDateTime, pageable: Pageable): Flux<User>

    /**
     * 생성일 기준으로 내림차순 정렬된 전체 사용자 목록 조회
     * @return 사용자 목록 (Flux<User>)
     */
    fun findAllByOrderByCreatedAtDesc(): Flux<User>

    /**
     * 마지막 로그인 시간 기준으로 내림차순 정렬된 사용자 목록 조회
     * @return 사용자 목록 (Flux<User>)
     */
    fun findAllByOrderByLastLoginAtDesc(): Flux<User>

    /**
     * 페이징된 전체 사용자 목록 조회
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findAllBy(pageable: Pageable): Flux<User>

    /**
     * 다수의 ID로 사용자 목록 조회
     * @param ids 사용자 ID 목록
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByIdIn(ids: List<Long>): Flux<User>

    /**
     * 활성화되고 이메일 인증된 사용자 목록 조회
     * @param isActive 활성화 상태
     * @param isEmailVerified 이메일 인증 상태
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByIsActiveAndIsEmailVerified(isActive: Boolean, isEmailVerified: Boolean, pageable: Pageable): Flux<User>

    /**
     * 특정 기간에 가입한 사용자 목록 조회
     * @param startDate 시작일
     * @param endDate 종료일
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime, pageable: Pageable): Flux<User>

    /**
     * 특정 위치에 있는 사용자 목록 조회
     * @param location 위치
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByLocationContainingIgnoreCase(location: String, pageable: Pageable): Flux<User>

    /**
     * 전화번호를 가진 사용자 목록 조회
     * @param phoneNumber 전화번호
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByPhoneNumber(phoneNumber: String, pageable: Pageable): Flux<User>

    /**
     * 웹사이트 URL을 가진 사용자 목록 조회
     * @param websiteUrl 웹사이트 URL
     * @param pageable 페이징 정보
     * @return 사용자 목록 (Flux<User>)
     */
    fun findByWebsiteUrl(websiteUrl: String, pageable: Pageable): Flux<User>
}