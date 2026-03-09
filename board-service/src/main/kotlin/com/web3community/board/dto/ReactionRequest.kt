package com.web3community.board.dto

import com.web3community.board.domain.document.ReactionType
import jakarta.validation.constraints.NotNull

/**
 * 반응(좋아요/싫어요) 요청 DTO
 *
 * POST /boards/{boardId}/reactions 엔드포인트에서 사용합니다.
 *
 * ## 요청 처리 흐름
 * 1. 동일 사용자가 해당 게시글에 기존 반응이 없으면 → 새로 생성 (INSERT)
 * 2. 기존 반응이 있고 같은 타입이면 → 중복 처리 (예외 또는 무시)
 * 3. 기존 반응이 있고 다른 타입이면 → 반응 변경 (UPDATE)
 *
 * ## 사용 예시 (JSON)
 * ```json
 * { "reactionType": "LIKE" }
 * ```
 * 또는
 * ```json
 * { "reactionType": "DISLIKE" }
 * ```
 *
 * @property reactionType 반응 종류 (LIKE 또는 DISLIKE, 필수)
 */
data class ReactionRequest(

    /**
     * 반응 종류
     * - LIKE: 좋아요
     * - DISLIKE: 싫어요
     * - @NotNull: null 값 불허 (JSON에 reactionType 필드 누락 시 400 응답)
     *
     * JSON 역직렬화 시 대소문자 구분:
     * - "LIKE" → ReactionType.LIKE (정상)
     * - "like" → 역직렬화 실패 (대소문자 구분, 필요 시 @JsonProperty 추가)
     */
    @field:NotNull(message = "반응 타입은 필수입니다. (LIKE 또는 DISLIKE)")
    val reactionType: ReactionType
)

/**
 * 반응 응답 DTO
 *
 * POST/DELETE /boards/{boardId}/reactions 응답에서 사용됩니다.
 * 반응 처리 결과와 업데이트된 카운트를 반환합니다.
 *
 * @property boardId 반응 대상 게시글 ID
 * @property userId 반응한 사용자 ID
 * @property reactionType 적용된 반응 타입 (null이면 반응 취소)
 * @property likeCount 업데이트된 좋아요 수
 * @property dislikeCount 업데이트된 싫어요 수
 * @property action 수행된 작업 (CREATED: 신규, UPDATED: 변경, DELETED: 취소)
 */
data class ReactionResponse(
    /** 반응 대상 게시글 ID */
    val boardId: String,

    /** 반응한 사용자 ID */
    val userId: String,

    /** 적용된 반응 타입 (취소 시 null) */
    val reactionType: ReactionType?,

    /** 업데이트 후 좋아요 수 */
    val likeCount: Long,

    /** 업데이트 후 싫어요 수 */
    val dislikeCount: Long,

    /** 수행된 반응 작업 유형 */
    val action: ReactionAction
)

/**
 * 반응 작업 유형 열거형
 *
 * @property CREATED 반응 최초 등록
 * @property UPDATED 반응 타입 변경 (좋아요 ↔ 싫어요)
 * @property DELETED 반응 취소
 */
enum class ReactionAction {
    /** 반응 최초 등록 */
    CREATED,
    /** 기존 반응을 다른 타입으로 변경 */
    UPDATED,
    /** 반응 취소 */
    DELETED
}
