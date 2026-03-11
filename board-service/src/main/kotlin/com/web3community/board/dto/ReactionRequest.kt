package com.web3community.board.dto

import com.web3community.board.domain.entity.ReactionType
import jakarta.validation.constraints.NotNull

/**
 * 반응(좋아요/싫어요) 요청 DTO
 *
 * POST /boards/{boardId}/reactions 엔드포인트에서 사용합니다.
 * 토글 방식: 동일 반응 재요청 시 취소, 다른 반응 요청 시 변경.
 */
data class ReactionRequest(

    /**
     * 반응 종류 (필수)
     * - LIKE: 좋아요
     * - DISLIKE: 싫어요
     */
    @field:NotNull(message = "반응 타입은 필수입니다. (LIKE 또는 DISLIKE)")
    val reactionType: ReactionType
)

/**
 * 반응 응답 DTO
 *
 * @property action 수행된 작업 (CREATED: 신규, UPDATED: 변경, DELETED: 취소)
 */
data class ReactionResponse(
    val boardId: String,
    val userId: String,
    val reactionType: ReactionType?,
    val likeCount: Long,
    val dislikeCount: Long,
    val action: ReactionAction
)

/**
 * 반응 작업 유형
 */
enum class ReactionAction {
    CREATED,
    UPDATED,
    DELETED
}
