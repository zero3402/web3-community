package com.web3community.board

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * Board Service 애플리케이션 진입점
 *
 * ## 서비스 개요
 * 게시판 서비스는 Web3 커뮤니티의 게시글 CRUD 및 반응(좋아요/싫어요) 기능을 담당합니다.
 *
 * ## 기술 스택
 * - Spring Boot Web MVC: Tomcat 기반 REST API 서버
 * - Spring Data JPA: MySQL 연동 (Hibernate ORM)
 * - Spring Data Redis: 게시글 상세 캐싱 (TTL: 5분)
 * - Apache Kafka: 게시글 이벤트 발행 (board-events 토픽)
 *
 * ## 포트: 8083
 *
 * ## 주요 엔드포인트
 * - GET  /boards           - 게시글 목록 (페이지네이션)
 * - GET  /boards/{id}      - 게시글 상세 (Redis 캐시 우선 조회)
 * - POST /boards           - 게시글 작성
 * - PUT  /boards/{id}      - 게시글 수정 (작성자 본인만)
 * - DELETE /boards/{id}    - 게시글 삭제 (Soft Delete)
 * - POST /boards/{id}/reactions - 좋아요/싫어요 (토글)
 * - GET  /boards/my        - 내 게시글 목록
 */
@SpringBootApplication
@EnableJpaAuditing  // @CreatedDate, @LastModifiedDate 자동 주입 활성화
class BoardApplication

fun main(args: Array<String>) {
    runApplication<BoardApplication>(*args)
}
