package com.web3community.board.domain.document

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 게시글 MongoDB Document
 *
 * ## 컬렉션 구조
 * MongoDB의 `boards` 컬렉션에 저장되는 게시글 문서입니다.
 *
 * ## 설계 결정
 * ### MongoDB를 선택한 이유
 * - 게시글은 태그(List), 첨부파일(List) 등 가변 길이 배열 필드가 많음
 * - RDBMS의 정규화(N:M 관계 테이블)보다 Document 모델이 쿼리 단순화에 유리
 * - Schema-less로 향후 필드 추가가 마이그레이션 없이 가능
 *
 * ### Soft Delete 전략 (status 필드)
 * - 실제 삭제(DELETE) 대신 status를 DELETED로 변경
 * - 삭제된 게시글의 반응(Reaction) 데이터 정합성 유지
 * - 데이터 감사(Audit) 및 복구 가능성 확보
 *
 * ## 인덱스 전략
 * - `authorId`: 마이페이지에서 사용자 작성 게시글 목록 조회
 * - `createdAt DESC`: 기본 목록 최신순 정렬
 * - `tags`: 태그 기반 필터링 (Multikey Index 자동 생성)
 * - `(status, createdAt)`: 활성 게시글 최신순 복합 조회 (가장 빈번한 쿼리)
 *
 * @property id MongoDB ObjectId (24자리 16진수 문자열)
 * @property title 게시글 제목 (최대 200자)
 * @property content 게시글 본문 (Markdown 지원)
 * @property authorId 작성자 사용자 ID (user-service의 userId)
 * @property authorNickname 작성자 닉네임 (비정규화 - 조인 없이 조회 가능)
 * @property tags 태그 목록 (최대 10개)
 * @property likeCount 좋아요 수 (Redis 카운터와 주기적으로 동기화)
 * @property dislikeCount 싫어요 수
 * @property viewCount 조회수
 * @property status 게시글 상태 (ACTIVE: 활성, DELETED: 삭제됨)
 * @property createdAt 생성일시 (@CreatedDate 자동 주입)
 * @property updatedAt 수정일시 (@LastModifiedDate 자동 주입)
 */
@Document(collection = "boards")
// 복합 인덱스 선언: @CompoundIndexes로 여러 복합 인덱스를 한 번에 정의
@CompoundIndexes(
    // 활성 게시글 최신순 조회 (가장 빈번한 쿼리 패턴)
    // ESR Rule: Equality(status) → Sort(createdAt)
    CompoundIndex(
        name = "idx_boards_status_createdAt",
        def = "{'status': 1, 'createdAt': -1}"
    ),
    // 작성자별 활성 게시글 최신순 조회 (마이페이지)
    CompoundIndex(
        name = "idx_boards_authorId_status_createdAt",
        def = "{'authorId': 1, 'status': 1, 'createdAt': -1}"
    )
)
data class Board(

    /** MongoDB 문서 고유 식별자 (ObjectId → String 변환) */
    @Id
    val id: String? = null,

    /** 게시글 제목 (필수, 최대 200자) */
    val title: String,

    /** 게시글 본문 (필수, Markdown 형식 지원) */
    val content: String,

    /**
     * 작성자 사용자 ID
     * - user-service의 User.id와 참조 관계 (외래키 대신 ID 참조)
     * - 수정/삭제 권한 검증에 사용
     */
    @Indexed  // 단일 인덱스: 작성자별 게시글 목록 조회 최적화
    val authorId: String,

    /**
     * 작성자 닉네임 (비정규화 필드)
     *
     * 게시글 목록/상세에서 닉네임을 표시하기 위해 user-service를 호출하는 대신
     * 작성 시점의 닉네임을 비정규화하여 저장합니다.
     *
     * 트레이드오프: 닉네임 변경 시 기존 게시글에 반영되지 않음
     * (일반적인 커뮤니티 서비스에서 허용되는 비정합성)
     */
    val authorNickname: String,

    /**
     * 태그 목록 (최대 10개)
     * MongoDB의 Multikey Index로 각 태그 값으로 독립적인 인덱스 항목 생성
     * 예: ["kotlin", "spring", "webflux"] → 3개의 인덱스 항목
     */
    @Indexed  // Multikey Index: 배열 필드의 각 요소를 개별 인덱스로 처리
    val tags: List<String> = emptyList(),

    /**
     * 좋아요 수
     * - Redis의 원자적 INCR/DECR로 실시간 업데이트
     * - 주기적으로(또는 이벤트 기반으로) MongoDB와 동기화
     * - 목록 조회 시 Redis 캐시 우선 사용
     */
    val likeCount: Long = 0,

    /** 싫어요 수 (likeCount와 동일한 전략으로 관리) */
    val dislikeCount: Long = 0,

    /**
     * 조회수
     * - Redis INCR으로 실시간 증가 (DB 직접 업데이트 시 부하 방지)
     * - 주기적 배치로 MongoDB에 동기화 (eventual consistency 허용)
     */
    val viewCount: Long = 0,

    /**
     * 게시글 상태
     * - ACTIVE: 정상 게시글 (목록/상세 조회 가능)
     * - DELETED: Soft Delete된 게시글 (목록에서 제외, 상세 조회 시 404)
     */
    val status: BoardStatus = BoardStatus.ACTIVE,

    /**
     * 생성일시 (자동 주입)
     * @EnableReactiveMongoAuditing + @CreatedDate 조합으로 최초 저장 시 자동 설정
     */
    @CreatedDate
    val createdAt: LocalDateTime? = null,

    /**
     * 수정일시 (자동 갱신)
     * @LastModifiedDate: 문서가 저장(save)될 때마다 현재 시각으로 자동 갱신
     */
    @LastModifiedDate
    val updatedAt: LocalDateTime? = null
)

/**
 * 게시글 상태 열거형
 *
 * @property ACTIVE 정상 활성 상태 (목록/상세 조회 가능)
 * @property DELETED Soft Delete 상태 (논리적 삭제, 물리적 데이터는 보존)
 */
enum class BoardStatus {
    /** 정상 게시글: 목록/상세 조회 가능 */
    ACTIVE,
    /** 삭제된 게시글: 쿼리에서 제외, 데이터는 MongoDB에 보존 */
    DELETED
}
