package com.web3community.board.controller

import com.web3community.board.dto.*
import com.web3community.board.service.BoardService
import com.web3community.common.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 게시판 REST 컨트롤러
 *
 * 기본 경로: /boards
 *
 * 인증 처리:
 * - API Gateway의 JwtAuthFilter에서 JWT를 검증하고 헤더로 사용자 정보 전달
 * - X-User-Id: 사용자 ID (Long)
 * - X-User-Nickname: 사용자 닉네임
 *
 * API 목록:
 * - GET  /boards               - 게시글 목록 (페이지네이션)
 * - GET  /boards/{id}          - 게시글 상세
 * - POST /boards               - 게시글 작성 (인증 필요)
 * - PUT  /boards/{id}          - 게시글 수정 (작성자 본인만)
 * - DELETE /boards/{id}        - 게시글 삭제 (작성자 본인만, Soft Delete)
 * - POST /boards/{id}/reactions - 좋아요/싫어요 (토글, 인증 필요)
 * - GET  /boards/my            - 내 게시글 목록 (인증 필요)
 */
@RestController
@RequestMapping("/boards")
class BoardController(
    private val boardService: BoardService
) {

    /**
     * 게시글 목록 조회
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 게시글 목록 (페이지 정보 포함)
     */
    @GetMapping
    fun getBoards(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<BoardPageResponse>> {
        val response = boardService.getBoards(page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 내 게시글 목록 조회 (인증 필요)
     *
     * @param userId Gateway에서 주입한 사용자 ID
     */
    @GetMapping("/my")
    fun getMyBoards(
        @RequestHeader("X-User-Id") userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<BoardPageResponse>> {
        val response = boardService.getMyBoards(userId.toLong(), page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 게시글 상세 조회
     * - Redis 캐시 우선 조회, 조회수 +1
     *
     * @param boardId 게시글 ID
     */
    @GetMapping("/{boardId}")
    fun getBoard(
        @PathVariable boardId: Long
    ): ResponseEntity<ApiResponse<BoardResponse>> {
        val response = boardService.getBoard(boardId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 게시글 작성 (인증 필요)
     *
     * @param request 게시글 작성 요청 DTO
     * @param userId Gateway에서 주입한 작성자 ID
     * @param userNickname Gateway에서 주입한 작성자 닉네임
     * @return 201 Created + 생성된 게시글
     */
    @PostMapping
    fun createBoard(
        @Valid @RequestBody request: BoardCreateRequest,
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-User-Nickname") userNickname: String
    ): ResponseEntity<ApiResponse<BoardResponse>> {
        val response = boardService.createBoard(request, userId.toLong(), userNickname)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, 201, "게시글이 작성되었습니다."))
    }

    /**
     * 게시글 수정 (작성자 본인만)
     *
     * @param boardId 수정할 게시글 ID
     * @param request 수정 요청 DTO
     * @param userId Gateway에서 주입한 요청자 ID (작성자 검증에 사용)
     */
    @PutMapping("/{boardId}")
    fun updateBoard(
        @PathVariable boardId: Long,
        @Valid @RequestBody request: BoardUpdateRequest,
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<ApiResponse<BoardResponse>> {
        val response = boardService.updateBoard(boardId, request, userId.toLong())
        return ResponseEntity.ok(ApiResponse.success(response, "게시글이 수정되었습니다."))
    }

    /**
     * 게시글 삭제 (작성자 본인만, Soft Delete)
     *
     * @param boardId 삭제할 게시글 ID
     * @param userId Gateway에서 주입한 요청자 ID (작성자 검증에 사용)
     * @return 200 OK + 성공 메시지
     */
    @DeleteMapping("/{boardId}")
    fun deleteBoard(
        @PathVariable boardId: Long,
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        boardService.deleteBoard(boardId, userId.toLong())
        return ResponseEntity.ok(ApiResponse.successNoContent("게시글이 삭제되었습니다."))
    }

    /**
     * 좋아요/싫어요 반응 처리 (토글 방식, 인증 필요)
     *
     * 처리 결과:
     * - 기존 반응 없음 → 새 반응 생성 (action: CREATED)
     * - 동일 반응 재요청 → 반응 취소 (action: DELETED)
     * - 다른 반응으로 변경 → 반응 변경 (action: UPDATED)
     *
     * @param boardId 반응할 게시글 ID
     * @param request 반응 요청 DTO (LIKE 또는 DISLIKE)
     * @param userId Gateway에서 주입한 사용자 ID
     */
    @PostMapping("/{boardId}/reactions")
    fun react(
        @PathVariable boardId: Long,
        @Valid @RequestBody request: ReactionRequest,
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<ApiResponse<ReactionResponse>> {
        val response = boardService.react(boardId, request, userId.toLong())
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
