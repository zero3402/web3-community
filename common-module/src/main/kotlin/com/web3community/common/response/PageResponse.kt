package com.web3community.common.response

/**
 * PageResponse - 페이지네이션 응답 래퍼
 *
 * 목록 조회 API에서 페이지네이션 정보와 함께 데이터를 반환하기 위한 클래스입니다.
 * Spring Data의 Page 객체를 API 응답 형식으로 변환하는 데 사용합니다.
 *
 * 응답 형식 예시:
 * ```json
 * {
 *   "content": [
 *     { "boardId": 1, "title": "첫 번째 게시글" },
 *     { "boardId": 2, "title": "두 번째 게시글" }
 *   ],
 *   "totalElements": 100,
 *   "totalPages": 10,
 *   "currentPage": 0,
 *   "size": 10,
 *   "isFirst": true,
 *   "isLast": false,
 *   "hasNext": true,
 *   "hasPrevious": false
 * }
 * ```
 *
 * @param T 페이지 내 요소의 타입 (제네릭)
 * @property content 현재 페이지의 데이터 목록
 * @property totalElements 전체 데이터 개수
 * @property totalPages 전체 페이지 수
 * @property currentPage 현재 페이지 번호 (0부터 시작, Spring Data 기본 방식)
 * @property size 페이지당 데이터 개수
 * @property isFirst 첫 번째 페이지 여부
 * @property isLast 마지막 페이지 여부
 * @property hasNext 다음 페이지 존재 여부
 * @property hasPrevious 이전 페이지 존재 여부
 */
data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int,
    val isFirst: Boolean,
    val isLast: Boolean,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {

    companion object {

        /**
         * Spring Data Page 객체로부터 PageResponse 생성
         *
         * Spring Data JPA, MongoDB의 Page<T> 객체를 API 응답 형식으로 변환합니다.
         *
         * @param page Spring Data의 Page<T> 객체
         * @return PageResponse<T> 인스턴스
         *
         * 사용 예시:
         * ```kotlin
         * val boardPage: Page<BoardDto> = boardService.findAll(pageable)
         * return ApiResponse.success(PageResponse.of(boardPage))
         * ```
         */
        fun <T> of(page: org.springframework.data.domain.Page<T>): PageResponse<T> {
            return PageResponse(
                content = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,     // 0-based index
                size = page.size,
                isFirst = page.isFirst,
                isLast = page.isLast,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious()
            )
        }

        /**
         * 수동으로 PageResponse 생성
         *
         * Spring Data Page를 사용하지 않고 직접 페이지네이션 정보를 구성할 때 사용합니다.
         * MongoDB Aggregation, 외부 API 연동 등의 경우에 활용합니다.
         *
         * @param content 현재 페이지 데이터 목록
         * @param totalElements 전체 데이터 개수
         * @param currentPage 현재 페이지 번호 (0부터 시작)
         * @param size 페이지당 크기
         * @return PageResponse<T> 인스턴스
         */
        fun <T> of(
            content: List<T>,
            totalElements: Long,
            currentPage: Int,
            size: Int
        ): PageResponse<T> {
            // 전체 페이지 수 계산 (나머지가 있으면 +1)
            val totalPages = if (totalElements == 0L) 0
                             else ((totalElements - 1) / size + 1).toInt()

            return PageResponse(
                content = content,
                totalElements = totalElements,
                totalPages = totalPages,
                currentPage = currentPage,
                size = size,
                isFirst = currentPage == 0,
                isLast = currentPage >= totalPages - 1,
                hasNext = currentPage < totalPages - 1,
                hasPrevious = currentPage > 0
            )
        }
    }
}
