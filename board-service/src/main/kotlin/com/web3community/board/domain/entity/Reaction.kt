package com.web3community.board.domain.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 반응(좋아요/싫어요) JPA 엔티티
 *
 * ## 테이블 구조
 * - reactions: 사용자 반응 데이터
 * - (board_id, user_id) UNIQUE 제약으로 동일 사용자의 중복 반응 방지
 *
 * ## 반응 처리 전략
 * - 동일 반응 재요청 시: 반응 취소 (DELETE)
 * - 다른 반응으로 변경 시: reactionType 업데이트 (UPDATE)
 */
@Entity
@Table(
    name = "reactions",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_reactions_board_user",
        columnNames = ["board_id", "user_id"]
    )],
    indexes = [
        Index(name = "idx_reactions_board_id", columnList = "board_id")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Reaction(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 반응 대상 게시글 ID
     * - Board 테이블의 id를 참조 (Manual Reference, FK 없음)
     */
    @Column(name = "board_id", nullable = false)
    val boardId: Long,

    /**
     * 반응한 사용자 ID
     * - API Gateway의 JwtAuthFilter가 주입하는 X-User-Id 헤더 값
     */
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    /**
     * 반응 종류
     * - LIKE: 좋아요 (Board.likeCount에 반영)
     * - DISLIKE: 싫어요 (Board.dislikeCount에 반영)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var reactionType: ReactionType,

    /** 반응 등록일시 (@CreatedDate로 자동 주입, 변경 불가) */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
)

/**
 * 반응 타입 열거형
 *
 * @property LIKE 좋아요 반응
 * @property DISLIKE 싫어요 반응
 */
enum class ReactionType {
    LIKE,
    DISLIKE
}
