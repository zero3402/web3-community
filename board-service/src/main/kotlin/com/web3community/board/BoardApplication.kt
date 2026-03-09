package com.web3community.board

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

/**
 * Board Service 애플리케이션 진입점
 *
 * ## 서비스 개요
 * 게시판 서비스는 Web3 커뮤니티의 게시글(Board) CRUD 및 반응(Reaction) 기능을 담당합니다.
 *
 * ## 기술 스택
 * - **Spring Boot WebFlux**: Netty 기반 Non-blocking I/O 리액티브 웹 서버
 *   - 동시 접속자가 많은 게시판에서 스레드 블로킹 없이 높은 처리량 달성
 * - **MongoDB Reactive**: 비동기 Document DB
 *   - 게시글의 태그, 메타데이터 등 유연한 스키마 구조에 적합
 * - **Redis Reactive**: 게시글 상세 캐싱(5분 TTL), 좋아요 카운트 캐싱
 * - **Apache Kafka**: 게시글 이벤트 발행 (알림, 검색 인덱싱 연동)
 * - **Kotlin Coroutines**: Mono/Flux를 suspend 함수로 래핑하여 가독성 향상
 *
 * ## 포트
 * 8083 (application.yml에서 설정)
 *
 * ## 주요 엔드포인트
 * - GET  /boards           - 게시글 목록 (페이지네이션)
 * - GET  /boards/{id}      - 게시글 상세 (Redis 캐시 우선 조회)
 * - POST /boards           - 게시글 작성
 * - PUT  /boards/{id}      - 게시글 수정 (작성자 본인만 가능)
 * - DELETE /boards/{id}    - 게시글 삭제 (작성자 본인만 가능, Soft Delete)
 * - POST /boards/{id}/reactions   - 좋아요/싫어요 등록 또는 변경
 * - DELETE /boards/{id}/reactions - 반응 취소
 *
 * @see BoardApplication.main 애플리케이션 시작점
 */
@SpringBootApplication
// MongoDB Reactive 감사 기능 활성화: @CreatedDate, @LastModifiedDate 자동 주입
@EnableReactiveMongoAuditing
// Reactive MongoDB 리포지토리 활성화 (ReactiveMongoRepository 구현체 자동 생성)
@EnableReactiveMongoRepositories(basePackages = ["com.web3community.board.domain.repository"])
class BoardApplication

/**
 * 애플리케이션 메인 함수
 *
 * Spring Boot 애플리케이션을 시작합니다.
 * Kotlin의 최상위 함수로 정의하여 Java의 `public static void main`과 동일하게 동작합니다.
 *
 * @param args 커맨드라인 인수 (--server.port=8083 등 런타임 오버라이드 가능)
 */
fun main(args: Array<String>) {
    runApplication<BoardApplication>(*args)
}
