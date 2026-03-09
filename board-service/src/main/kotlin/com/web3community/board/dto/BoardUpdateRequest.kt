package com.web3community.board.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 게시글 수정 요청 DTO
 *
 * PUT /boards/{boardId} 엔드포인트에서 클라이언트가 전송하는 요청 본문입니다.
 *
 * ## 수정 정책
 * - 게시글 작성자 본인만 수정 가능 (X-User-Id 헤더 검증)
 * - title, content, tags 모두 수정 가능
 * - DELETED 상태의 게시글은 수정 불가
 *
 * ## 부분 업데이트 vs 전체 업데이트
 * - 현재 구현: PUT (전체 업데이트) - 모든 필드를 새 값으로 교체
 * - 향후 PATCH로 전환 시: null 필드는 기존 값 유지하는 방식으로 변경 가능
 *
 * ## 사용 예시 (JSON)
 * ```json
 * {
 *   "title": "수정된 제목",
 *   "content": "수정된 본문 내용...",
 *   "tags": ["kotlin", "spring"]
 * }
 * ```
 *
 * @property title 수정할 게시글 제목 (필수)
 * @property content 수정할 게시글 본문 (필수)
 * @property tags 수정할 태그 목록 (선택, 빈 리스트로 전송 시 태그 전체 삭제)
 */
data class BoardUpdateRequest(

    /**
     * 수정할 게시글 제목
     * - @NotBlank: 제목을 빈 값으로 수정 불허
     * - @Size(max=200): 최대 200자
     */
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하로 입력해주세요.")
    val title: String,

    /**
     * 수정할 게시글 본문
     * - @NotBlank: 내용을 빈 값으로 수정 불허
     */
    @field:NotBlank(message = "내용은 필수입니다.")
    @field:Size(min = 1, max = 50_000, message = "내용은 1자 이상 50,000자 이하로 입력해주세요.")
    val content: String,

    /**
     * 수정할 태그 목록
     * - 빈 리스트([])를 전송하면 기존 태그 전체 삭제
     * - null이 아닌 빈 리스트로 기본값 설정 (명시적 초기화 의도)
     */
    @field:Size(max = 10, message = "태그는 최대 10개까지 입력할 수 있습니다.")
    val tags: List<
        @Size(min = 1, max = 30, message = "태그는 1자 이상 30자 이하로 입력해주세요.")
        String
    > = emptyList()
)
