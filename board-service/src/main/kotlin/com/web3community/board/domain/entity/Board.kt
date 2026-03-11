package com.web3community.board.domain.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 게시글 JPA 엔티티
 *
 * ## 테이블 구조
 * - boards: 게시글 본문 데이터
 * - board_tags: 태그 목록 (@ElementCollection, 별도 테이블)
 *
 * ## Soft Delete 전략
 * - 실제 삭제(DELETE) 대신 status를 DELETED로 변경
 * - 데이터 감사(Audit) 및 복구 가능성 확보
 *
 * @property id Auto-increment PK
 * @property authorId 작성자 사용자 ID (user-service의 User.id)
 * @property authorNickname 작성자 닉네임 (비정규화 - 조인 없이 조회 가능)
 */
@Entity
@Table(
    name = "boards",
    indexes = [
        Index(name = "idx_boards_author_id", columnList = "author_id"),
        Index(name = "idx_boards_status_created_at", columnList = "status, created_at DESC")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Board(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** 게시글 제목 (최대 200자) */
    @Column(nullable = false, length = 200)
    var title: String,

    /** 게시글 본문 (Markdown 형식 지원) */
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    /**
     * 작성자 사용자 ID
     * - user-service의 User.id와 참조 관계 (외래키 대신 ID 참조)
     * - 수정/삭제 권한 검증에 사용
     */
    @Column(name = "author_id", nullable = false)
    val authorId: Long,

    /**
     * 작성자 닉네임 (비정규화 필드)
     * - 게시글 작성 시점의 닉네임을 저장하여 user-service 조회 없이 표시
     * - 닉네임 변경 시 기존 게시글에 반영되지 않음 (허용된 비정합성)
     */
    @Column(name = "author_nickname", nullable = false, length = 50)
    val authorNickname: String,

    /**
     * 태그 목록 (최대 10개)
     * - board_tags 별도 테이블에 저장
     * - EAGER 로딩: 게시글 조회 시 태그도 함께 로드
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "board_tags",
        joinColumns = [JoinColumn(name = "board_id")]
    )
    @Column(name = "tag", nullable = false, length = 30)
    var tags: List<String> = emptyList(),

    /** 좋아요 수 */
    @Column(nullable = false)
    var likeCount: Long = 0,

    /** 싫어요 수 */
    @Column(nullable = false)
    var dislikeCount: Long = 0,

    /**
     * 조회수
     * - 게시글 상세 조회 시마다 1씩 증가
     */
    @Column(nullable = false)
    var viewCount: Long = 0,

    /**
     * 게시글 상태
     * - ACTIVE: 정상 게시글 (목록/상세 조회 가능)
     * - DELETED: Soft Delete된 게시글 (목록에서 제외)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: BoardStatus = BoardStatus.ACTIVE,

    /** 생성일시 (@EnableJpaAuditing + @CreatedDate로 자동 주입) */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    /** 수정일시 (@LastModifiedDate로 save 시마다 자동 갱신) */
    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime? = null
)

/**
 * 게시글 상태 열거형
 *
 * @property ACTIVE 정상 활성 상태
 * @property DELETED Soft Delete 상태 (논리적 삭제)
 */
enum class BoardStatus {
    ACTIVE,
    DELETED
}
