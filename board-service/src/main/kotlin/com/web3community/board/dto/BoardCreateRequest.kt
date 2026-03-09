package com.web3community.board.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 게시글 작성 요청 DTO
 *
 * POST /boards 엔드포인트에서 클라이언트가 전송하는 요청 본문입니다.
 *
 * ## 유효성 검증
 * - `title`: 공백 불허, 1~200자
 * - `content`: 공백 불허, 최소 1자
 * - `tags`: 선택 사항, 각 태그 최대 30자, 최대 10개
 *
 * ## 사용 예시 (JSON)
 * ```json
 * {
 *   "title": "Kotlin Coroutines 입문",
 *   "content": "## 소개\nKotlin Coroutines는...",
 *   "tags": ["kotlin", "coroutines", "async"]
 * }
 * ```
 *
 * @property title 게시글 제목 (필수, 1~200자)
 * @property content 게시글 본문 (필수, Markdown 지원)
 * @property tags 태그 목록 (선택, 최대 10개, 각 30자 이하)
 */
data class BoardCreateRequest(

    /**
     * 게시글 제목
     * - @NotBlank: null, 빈 문자열, 공백 문자열 모두 불허
     * - @Size(max=200): 최대 200자 제한
     */
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하로 입력해주세요.")
    val title: String,

    /**
     * 게시글 본문
     * - Markdown 형식 지원 (프론트엔드에서 렌더링)
     * - @NotBlank: 빈 내용 불허
     * - @Size(max=50000): 최대 50,000자 (약 50KB)
     */
    @field:NotBlank(message = "내용은 필수입니다.")
    @field:Size(min = 1, max = 50_000, message = "내용은 1자 이상 50,000자 이하로 입력해주세요.")
    val content: String,

    /**
     * 태그 목록 (선택 사항)
     * - 최대 10개의 태그 허용
     * - 각 태그는 1~30자 이하
     * - 기본값: 빈 리스트
     */
    @field:Size(max = 10, message = "태그는 최대 10개까지 입력할 수 있습니다.")
    val tags: List<
        @NotBlank(message = "태그는 공백일 수 없습니다.")
        @Size(min = 1, max = 30, message = "태그는 1자 이상 30자 이하로 입력해주세요.")
        String
    > = emptyList()
)
