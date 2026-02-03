package com.web3community.post.controller

import com.web3community.post.dto.*
import com.web3community.post.service.PostService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Min

/**
 * 게시물 관리 REST 컨트롤러
 * 게시물 등록, 조회, 수정, 삭제, 검색 기능 제공
 * 모든 엔드포인트는 리액티브 스트림(Flux/Mono)으로 처리
 */
@RestController
@RequestMapping("/api/v1/posts")
@Validated
@CrossOrigin(origins = ["*"], allowedHeaders = ["*"])
class PostController(
    private val postService: PostService
) {

    /**
     * 게시물 등록 API
     * @param request 게시물 등록 요청 DTO
     * @return 등록된 게시물 정보
     */
    @PostMapping
    fun createPost(@Valid @RequestBody request: PostCreateRequest): Mono<ResponseEntity<PostResponse>> {
        return postService.createPost(request)
            .map { post -> ResponseEntity.status(HttpStatus.CREATED).body(post) }
    }

    /**
     * 전체 게시물 목록 조회 (페이지네이션)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 당 게시물 수
     * @param sortBy 정렬 기준 필드
     * @param direction 정렬 방향
     * @param category 카테고리 필터
     * @param status 상태 필터
     * @return 게시물 목록
     */
    @GetMapping
    fun getAllPosts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") direction: String,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) status: String?
    ): Flux<PostResponse> {
        return postService.getAllPosts(page, size, sortBy, direction, category, status)
    }

    /**
     * ID로 게시물 조회
     * @param id 게시물 ID
     * @return 게시물 정보
     */
    @GetMapping("/{id}")
    fun getPostById(@PathVariable id: String): Mono<ResponseEntity<PostResponse>> {
        return postService.getPostById(id)
            .map { post -> ResponseEntity.ok(post) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 사용자 ID로 게시물 목록 조회
     * @param authorId 작성자 ID
     * @param page 페이지 번호
     * @param size 페이지 당 게시물 수
     * @return 작성자의 게시물 목록
     */
    @GetMapping("/author/{authorId}")
    fun getPostsByAuthor(
        @PathVariable authorId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Flux<PostResponse> {
        return postService.getPostsByAuthor(authorId, page, size)
    }

    /**
     * 카테고리별 게시물 목록 조회
     * @param category 카테고리
     * @param page 페이지 번호
     * @param size 페이지 당 게시물 수
     * @return 카테고리별 게시물 목록
     */
    @GetMapping("/category/{category}")
    fun getPostsByCategory(
        @PathVariable category: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Flux<PostResponse> {
        return postService.getPostsByCategory(category, page, size)
    }

    /**
     * 태그로 게시물 검색
     * @param tag 검색할 태그
     * @param page 페이지 번호
     * @param size 페이지 당 게시물 수
     * @return 태그가 포함된 게시물 목록
     */
    @GetMapping("/tag/{tag}")
    fun getPostsByTag(
        @PathVariable tag: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Flux<PostResponse> {
        return postService.getPostsByTag(tag, page, size)
    }

    /**
     * 게시물 검색 (제목, 내용, 태그)
     * @param query 검색어
     * @param page 페이지 번호
     * @param size 페이지 당 게시물 수
     * @return 검색된 게시물 목록
     */
    @GetMapping("/search")
    fun searchPosts(
        @RequestParam @NotBlank(message = "검색어는 필수입니다.") query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): Flux<PostResponse> {
        return postService.searchPosts(query, page, size)
    }

    /**
     * 게시물 제목으로 검색
     * @param title 검색할 제목
     * @param page 페이지 번호
     * @param size 페이지 당 게시물 수
     * @return 검색된 게시물 목록
     */
    @GetMapping("/search/title")
    fun searchPostsByTitle(
        @RequestParam @NotBlank(message = "제목 검색어는 필수입니다.") title: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): Flux<PostResponse> {
        return postService.searchPostsByTitle(title, page, size)
    }

    /**
     * 게시물 내용으로 검색
     * @param content 검색할 내용
     * @param page 페이지 번호
     * @param size 페이지 당 게시물 수
     * @return 검색된 게시물 목록
     */
    @GetMapping("/search/content")
    fun searchPostsByContent(
        @RequestParam @NotBlank(message = "내용 검색어는 필수입니다.") content: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): Flux<PostResponse> {
        return postService.searchPostsByContent(content, page, size)
    }

    /**
     * 인기 게시물 목록 조회 (조회수, 좋아요 기준)
     * @param limit 조회할 게시물 수
     * @param period 기간 (day, week, month)
     * @return 인기 게시물 목록
     */
    @GetMapping("/popular")
    fun getPopularPosts(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "week") period: String
    ): Flux<PostResponse> {
        return postService.getPopularPosts(limit, period)
    }

    /**
     * 최근 게시물 목록 조회
     * @param limit 조회할 게시물 수
     * @return 최근 게시물 목록
     */
    @GetMapping("/recent")
    fun getRecentPosts(
        @RequestParam(defaultValue = "10") limit: Int
    ): Flux<PostResponse> {
        return postService.getRecentPosts(limit)
    }

    /**
     * 추천 게시물 목록 조회
     * @param userId 사용자 ID (개인화된 추천을 위해)
     * @param limit 조회할 게시물 수
     * @return 추천 게시물 목록
     */
    @GetMapping("/recommended")
    fun getRecommendedPosts(
        @RequestParam(required = false) userId: String?,
        @RequestParam(defaultValue = "10") limit: Int
    ): Flux<PostResponse> {
        return postService.getRecommendedPosts(userId, limit)
    }

    /**
     * 게시물 정보 수정
     * @param id 게시물 ID
     * @param request 수정 요청 DTO
     * @return 수정된 게시물 정보
     */
    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: String,
        @Valid @RequestBody request: PostUpdateRequest
    ): Mono<ResponseEntity<PostResponse>> {
        return postService.updatePost(id, request)
            .map { post -> ResponseEntity.ok(post) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 게시물 부분 정보 수정
     * @param id 게시물 ID
     * @param request 부분 수정 요청 DTO
     * @return 수정된 게시물 정보
     */
    @PatchMapping("/{id}")
    fun patchPost(
        @PathVariable id: String,
        @Valid @RequestBody request: PostPatchRequest
    ): Mono<ResponseEntity<PostResponse>> {
        return postService.patchPost(id, request)
            .map { post -> ResponseEntity.ok(post) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 게시물 삭제 (소프트 삭제)
     * @param id 게시물 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: String): Mono<ResponseEntity<String>> {
        return postService.deletePost(id)
            .map { ResponseEntity.ok(it) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 게시물 영구 삭제 (관리자용)
     * @param id 게시물 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{id}/permanent")
    fun permanentlyDeletePost(@PathVariable id: String): Mono<ResponseEntity<String>> {
        return postService.permanentlyDeletePost(id)
            .map { ResponseEntity.ok(it) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 게시물 상태 변경
     * @param id 게시물 ID
     * @param request 상태 변경 요청 DTO
     * @return 수정된 게시물 정보
     */
    @PutMapping("/{id}/status")
    fun changePostStatus(
        @PathVariable id: String,
        @Valid @RequestBody request: PostStatusChangeRequest
    ): Mono<ResponseEntity<PostResponse>> {
        return postService.changePostStatus(id, request)
            .map { post -> ResponseEntity.ok(post) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 게시물 좋아요
     * @param id 게시물 ID
     * @param userId 사용자 ID
     * @return 수정된 게시물 정보
     */
    @PostMapping("/{id}/like")
    fun likePost(
        @PathVariable id: String,
        @RequestParam userId: String
    ): Mono<ResponseEntity<PostResponse>> {
        return postService.likePost(id, userId)
            .map { post -> ResponseEntity.ok(post) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 게시물 좋아요 취소
     * @param id 게시물 ID
     * @param userId 사용자 ID
     * @return 수정된 게시물 정보
     */
    @DeleteMapping("/{id}/like")
    fun unlikePost(
        @PathVariable id: String,
        @RequestParam userId: String
    ): Mono<ResponseEntity<PostResponse>> {
        return postService.unlikePost(id, userId)
            .map { post -> ResponseEntity.ok(post) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 게시물 조회수 증가
     * @param id 게시물 ID
     * @return 수정된 게시물 정보
     */
    @PostMapping("/{id}/view")
    fun incrementViewCount(@PathVariable id: String): Mono<ResponseEntity<PostResponse>> {
        return postService.incrementViewCount(id)
            .map { post -> ResponseEntity.ok(post) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 게시물 존재 여부 확인
     * @param id 게시물 ID
     * @return 존재 여부
     */
    @GetMapping("/{id}/exists")
    fun checkPostExists(@PathVariable id: String): Mono<ResponseEntity<Map<String, Boolean>>> {
        return postService.postExists(id)
            .map { exists -> ResponseEntity.ok(mapOf("exists" to exists)) }
    }

    /**
     * 게시물 통계 정보 조회
     * @return 게시물 통계 정보
     */
    @GetMapping("/stats")
    fun getPostStats(): Mono<ResponseEntity<PostStatsResponse>> {
        return postService.getPostStats()
            .map { stats -> ResponseEntity.ok(stats) }
    }

    /**
     * 카테고리별 게시물 수 조회
     * @return 카테고리별 게시물 수
     */
    @GetMapping("/stats/categories")
    fun getPostStatsByCategory(): Mono<ResponseEntity<Map<String, Long>>> {
        return postService.getPostStatsByCategory()
            .map { stats -> ResponseEntity.ok(stats) }
    }

    /**
     * 일별 게시물 작성 통계 조회
     * @param days 조회할 일수 (기본값: 30일)
     * @return 일별 게시물 작성 수
     */
    @GetMapping("/stats/daily")
    fun getDailyPostStats(
        @RequestParam(defaultValue = "30") days: Int
    ): Mono<ResponseEntity<Map<String, Long>>> {
        return postService.getDailyPostStats(days)
            .map { stats -> ResponseEntity.ok(stats) }
    }

    /**
     * 사용자별 게시물 통계 조회
     * @param userId 사용자 ID
     * @return 사용자의 게시물 통계 정보
     */
    @GetMapping("/stats/user/{userId}")
    fun getUserPostStats(@PathVariable userId: String): Mono<ResponseEntity<UserPostStatsResponse>> {
        return postService.getUserPostStats(userId)
            .map { stats -> ResponseEntity.ok(stats) }
    }

    /**
     * 게시물 상세 조회 (댓글 포함)
     * @param id 게시물 ID
     * @param includeComments 댓글 포함 여부
     * @param commentPage 댓글 페이지 번호
     * @param commentSize 댓글 페이지 당 크기
     * @return 게시물 상세 정보
     */
    @GetMapping("/{id}/detailed")
    fun getPostDetailed(
        @PathVariable id: String,
        @RequestParam(defaultValue = "true") includeComments: Boolean,
        @RequestParam(defaultValue = "0") commentPage: Int,
        @RequestParam(defaultValue = "20") commentSize: Int
    ): Mono<ResponseEntity<PostDetailedResponse>> {
        return postService.getPostDetailed(id, includeComments, commentPage, commentSize)
            .map { detailed -> ResponseEntity.ok(detailed) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 임시 저장된 게시물 목록 조회
     * @param authorId 작성자 ID
     * @param page 페이지 번호
     * @param size 페이지 당 게시물 수
     * @return 임시 저장된 게시물 목록
     */
    @GetMapping("/drafts/{authorId}")
    fun getDraftPosts(
        @PathVariable authorId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): Flux<PostResponse> {
        return postService.getDraftPosts(authorId, page, size)
    }

    /**
     * 게시물 복원
     * @param id 게시물 ID
     * @return 복원된 게시물 정보
     */
    @PostMapping("/{id}/restore")
    fun restorePost(@PathVariable id: String): Mono<ResponseEntity<PostResponse>> {
        return postService.restorePost(id)
            .map { post -> ResponseEntity.ok(post) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }
}