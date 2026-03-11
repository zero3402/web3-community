package com.web3community.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.web3community.board.domain.entity.Board
import com.web3community.board.domain.entity.BoardStatus
import java.time.LocalDateTime

/**
 * 게시글 응답 DTO
 *
 * Board 엔티티를 직접 노출하지 않고 DTO로 변환하여 API 응답 형식을 독립적으로 관리합니다.
 * Redis에 JSON 형태로 캐싱됩니다 (TTL: 5분).
 *
 * @property id 게시글 ID (String으로 직렬화 - API 하위 호환성 유지)
 * @property authorId 작성자 사용자 ID
 */
data class BoardResponse(

    val id: String,
    val title: String,
    val content: String,
    val authorId: String,
    val authorNickname: String,
    val tags: List<String>,
    val likeCount: Long,
    val dislikeCount: Long,
    val viewCount: Long,
    val status: BoardStatus,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime?,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val updatedAt: LocalDateTime?
) {
    companion object {
        /**
         * Board 엔티티 → BoardResponse DTO 변환
         */
        fun from(board: Board): BoardResponse {
            return BoardResponse(
                id = board.id.toString(),
                title = board.title,
                content = board.content,
                authorId = board.authorId.toString(),
                authorNickname = board.authorNickname,
                tags = board.tags,
                likeCount = board.likeCount,
                dislikeCount = board.dislikeCount,
                viewCount = board.viewCount,
                status = board.status,
                createdAt = board.createdAt,
                updatedAt = board.updatedAt
            )
        }
    }
}

/**
 * 게시글 목록 응답 DTO (페이지네이션 포함)
 */
data class BoardPageResponse(
    val content: List<BoardResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
