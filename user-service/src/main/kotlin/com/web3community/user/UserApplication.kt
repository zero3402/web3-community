package com.web3community.user

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * User Service 메인 애플리케이션 클래스
 *
 * 이 서비스는 Web3 커뮤니티 플랫폼의 사용자 정보 관리를 전담한다.
 *
 * 주요 기능:
 * - 사용자 프로필 조회/수정 (닉네임, 프로필 이미지)
 * - Kafka consumer: auth-service가 발행한 UserCreatedEvent를 소비하여 DB에 저장
 * - Internal API: auth-service Feign 클라이언트가 호출하는 내부 엔드포인트 제공
 *   (이메일로 사용자 조회, BCrypt 해시 비밀번호 포함 반환)
 *
 * 포트: 8082
 * 기술 스택: Spring Boot MVC, Spring Data JPA, MySQL, Kafka Consumer, Redis
 *
 * 아키텍처 결정:
 * - auth-service는 인증/토큰만 담당, 사용자 데이터는 이 서비스가 소유
 * - 회원가입 시 Kafka를 통한 비동기 생성으로 서비스 간 결합도 최소화
 * - 내부 통신(Feign)과 외부 통신(공개 API)을 컨트롤러 레벨에서 분리
 */
@SpringBootApplication
@EnableJpaAuditing  // @CreatedDate, @LastModifiedDate JPA Auditing 활성화
class UserApplication

/**
 * 애플리케이션 진입점
 *
 * @param args 커맨드라인 인수 (Spring Boot가 자동으로 처리)
 */
fun main(args: Array<String>) {
    runApplication<UserApplication>(*args)
}
