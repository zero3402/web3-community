package com.web3community.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.web3community.board.domain.document.Board
import com.web3community.board.domain.document.BoardStatus
import java.time.LocalDateTime

/**
 * 게시글 응답 DTO
 *
 * API 클라이언트에게 반환되는 게시글 데이터 구조입니다.
 * Board Document를 직접 노출하지 않고 DTO로 변환하여:
 * 1. 내부 구현 세부사항 은닉
 * 2. API 응답 형식의 독립적 관리
 * 3. 민감 데이터 필터링 가능
 *
 * ## Redis 캐싱
 * - 이 DTO가 Redis에 JSON 형태로 캐싱됨 (TTL: 5분)
 * - Jackson 역직렬화를 위해 기본 생성자 불필요 (data class + Jackson Kotlin Module)
 *
 * ## 날짜 형식
 * - ISO-8601 형식: "2024-01-15T12:30:00" (UTC 기준)
 * - @JsonFormat으로 직렬화 패턴 명시
 *
 * @property id 게시글 고유 ID (MongoDB ObjectId 문자열)
 * @property title 게시글 제목
 * @property content 게시글 본문 (Markdown)
 * @property authorId 작성자 사용자 ID
 * @property authorNickname 작성자 닉네임 (비정규화)
 * @property tags 태그 목록
 * @property likeCount 좋아요 수 (Redis 카운터 반영)
 * @property dislikeCount 싫어요 수
 * @property viewCount 조회수
 * @property status 게시글 상태 (ACTIVE/DELETED)
 * @property createdAt 작성일시
 * @property updatedAt 수정일시
 */
data class BoardResponse(

    /** 게시글 고유 ID */
    val id: String,

    /** 게시글 제목 */
    val title: String,

    /** 게시글 본문 (Markdown 지원) */
    val content: String,

    /** 작성자 사용자 ID */
    val authorId: String,

    /** 작성자 닉네임 (비정규화 저장값) */
    val authorNickname: String,

    /** 태그 목록 */
    val tags: List<String>,

    /** 좋아요 수 */
    val likeCount: Long,

    /** 싫어요 수 */
    val dislikeCount: Long,

    /** 조회수 */
    val viewCount: Long,

    /** 게시글 상태 */
    val status: BoardStatus,

    /**
     * 작성일시
     * ISO-8601 형식으로 직렬화: "yyyy-MM-dd'T'HH:mm:ss"
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime?,

    /**
     * 수정일시
     * ISO-8601 형식으로 직렬화
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val updatedAt: LocalDateTime?
) {
    companion object {
        /**
         * Board Document → BoardResponse DTO 변환
         *
         * 서비스 계층에서 Document를 DTO로 변환할 때 사용하는 팩토리 메서드.
         * companion object의 from()은 Kotlin에서 정적 팩토리 패턴의 관용 표현입니다.
         *
         * @param board 변환할 Board 도큐먼트
         * @return 변환된 BoardResponse DTO
         * @throws IllegalStateException board.id가 null인 경우 (저장되지 않은 도큐먼트)
         */
        fun from(board: Board): BoardResponse {
            return BoardResponse(
                // id가 null이면 IllegalStateException 발생 (저장 전 도큐먼트 변환 방지)
                id = requireNotNull(board.id) { "게시글 ID가 null입니다. 저장된 도큐먼트에서만 변환 가능합니다." },
                title = board.title,
                content = board.content,
                authorId = board.authorId,
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
 *
 * GET /boards 응답에서 사용되며, 게시글 목록과 페이지 정보를 함께 반환합니다.
 *
 * @property content 게시글 목록
 * @property totalElements 전체 게시글 수
 * @property totalPages 전체 페이지 수
 * @property currentPage 현재 페이지 번호 (0부터 시작)
 * @property pageSize 페이지당 항목 수
 * @property hasNext 다음 페이지 존재 여부
 * @property hasPrevious 이전 페이지 존재 여부
 */
data class BoardPageResponse(
    /** 현재 페이지의 게시글 목록 */
    val content: List<BoardResponse>,

    /** 조건에 맞는 전체 게시글 수 */
    val totalElements: Long,

    /** 전체 페이지 수 (ceil(totalElements / pageSize)) */
    val totalPages: Int,

    /** 현재 페이지 번호 (0-based) */
    val currentPage: Int,

    /** 페이지당 표시할 최대 항목 수 */
    val pageSize: Int,

    /** 다음 페이지 존재 여부 */
    val hasNext: Boolean,

    /** 이전 페이지 존재 여부 */
    val hasPrevious: Boolean
)
