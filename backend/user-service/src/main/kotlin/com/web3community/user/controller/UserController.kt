package com.web3community.user.controller

import com.web3community.user.dto.*
import com.web3community.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

/**
 * 사용자 관리 REST 컨트롤러
 * 사용자 등록, 조회, 수정, 삭제, 검색 기능 제공
 * 모든 엔드포인트는 리액티브 스트림(Flux/Mono)으로 처리
 */
@RestController
@RequestMapping("/api/v1/users")
@Validated
@CrossOrigin(origins = ["*"], allowedHeaders = ["*"])
class UserController(
    private val userService: UserService
) {

    /**
     * 사용자 등록 API
     * @param request 사용자 등록 요청 DTO
     * @return 등록된 사용자 정보
     */
    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody request: UserRegistrationRequest): Mono<ResponseEntity<UserResponse>> {
        return userService.registerUser(request)
            .map { user -> ResponseEntity.status(HttpStatus.CREATED).body(user) }
    }

    /**
     * 사용자 로그인 API
     * @param request 로그인 요청 DTO
     * @return 로그인 응답 (JWT 토큰 포함)
     */
    @PostMapping("/login")
    fun loginUser(@Valid @RequestBody request: UserLoginRequest): Mono<ResponseEntity<LoginResponse>> {
        return userService.authenticateUser(request)
            .map { response -> ResponseEntity.ok(response) }
    }

    /**
     * 전체 사용자 목록 조회 (페이지네이션)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 당 사용자 수
     * @param sortBy 정렬 기준 필드
     * @param direction 정렬 방향
     * @return 사용자 목록
     */
    @GetMapping
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") direction: String
    ): Flux<UserResponse> {
        return userService.getAllUsers(page, size, sortBy, direction)
    }

    /**
     * ID로 사용자 조회
     * @param id 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): Mono<ResponseEntity<UserResponse>> {
        return userService.getUserById(id)
            .map { user -> ResponseEntity.ok(user) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 이메일로 사용자 조회
     * @param email 사용자 이메일
     * @return 사용자 정보
     */
    @GetMapping("/email/{email}")
    fun getUserByEmail(@PathVariable email: String): Mono<ResponseEntity<UserResponse>> {
        return userService.getUserByEmail(email)
            .map { user -> ResponseEntity.ok(user) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 사용자 이름으로 사용자 검색
     * @param username 검색할 사용자 이름
     * @return 검색된 사용자 목록
     */
    @GetMapping("/search")
    fun searchUsersByUsername(
        @RequestParam @NotBlank(message = "검색어는 필수입니다.") username: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): Flux<UserResponse> {
        return userService.searchUsersByUsername(username, page, size)
    }

    /**
     * 이메일로 사용자 검색 (부분 일치)
     * @param email 검색할 이메일
     * @return 검색된 사용자 목록
     */
    @GetMapping("/search/email")
    fun searchUsersByEmail(
        @RequestParam @NotBlank(message = "이메일 검색어는 필수입니다.") email: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): Flux<UserResponse> {
        return userService.searchUsersByEmail(email, page, size)
    }

    /**
     * 사용자 정보 수정
     * @param id 사용자 ID
     * @param request 수정 요청 DTO
     * @return 수정된 사용자 정보
     */
    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: UserUpdateRequest
    ): Mono<ResponseEntity<UserResponse>> {
        return userService.updateUser(id, request)
            .map { user -> ResponseEntity.ok(user) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 사용자 부분 정보 수정
     * @param id 사용자 ID
     * @param request 부분 수정 요청 DTO
     * @return 수정된 사용자 정보
     */
    @PatchMapping("/{id}")
    fun patchUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: UserPatchRequest
    ): Mono<ResponseEntity<UserResponse>> {
        return userService.patchUser(id, request)
            .map { user -> ResponseEntity.ok(user) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 사용자 비밀번호 변경
     * @param id 사용자 ID
     * @param request 비밀번호 변경 요청 DTO
     * @return 성공 메시지
     */
    @PutMapping("/{id}/password")
    fun changePassword(
        @PathVariable id: Long,
        @Valid @RequestBody request: PasswordChangeRequest
    ): Mono<ResponseEntity<String>> {
        return userService.changePassword(id, request)
            .map { ResponseEntity.ok(it) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 사용자 삭제 (소프트 삭제)
     * @param id 사용자 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): Mono<ResponseEntity<String>> {
        return userService.deleteUser(id)
            .map { ResponseEntity.ok(it) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 사용자 영구 삭제 (관리자용)
     * @param id 사용자 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{id}/permanent")
    fun permanentlyDeleteUser(@PathVariable id: Long): Mono<ResponseEntity<String>> {
        return userService.permanentlyDeleteUser(id)
            .map { ResponseEntity.ok(it) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 사용자 활성화 상태 토글
     * @param id 사용자 ID
     * @return 수정된 사용자 정보
     */
    @PutMapping("/{id}/toggle-active")
    fun toggleUserActive(@PathVariable id: Long): Mono<ResponseEntity<UserResponse>> {
        return userService.toggleUserActive(id)
            .map { user -> ResponseEntity.ok(user) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 사용자 역할 변경
     * @param id 사용자 ID
     * @param request 역할 변경 요청 DTO
     * @return 수정된 사용자 정보
     */
    @PutMapping("/{id}/role")
    fun changeUserRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: RoleChangeRequest
    ): Mono<ResponseEntity<UserResponse>> {
        return userService.changeUserRole(id, request)
            .map { user -> ResponseEntity.ok(user) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 사용자 프로필 이미지 업로드
     * @param id 사용자 ID
     * @param imageUrl 프로필 이미지 URL
     * @return 수정된 사용자 정보
     */
    @PutMapping("/{id}/profile-image")
    fun updateProfileImage(
        @PathVariable id: Long,
        @RequestParam @NotBlank(message = "이미지 URL은 필수입니다.") imageUrl: String
    ): Mono<ResponseEntity<UserResponse>> {
        return userService.updateProfileImage(id, imageUrl)
            .map { user -> ResponseEntity.ok(user) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }

    /**
     * 사용자 통계 정보 조회
     * @return 사용자 통계 정보
     */
    @GetMapping("/stats")
    fun getUserStats(): Mono<ResponseEntity<UserStatsResponse>> {
        return userService.getUserStats()
            .map { stats -> ResponseEntity.ok(stats) }
    }

    /**
     * 최근 가입한 사용자 목록 조회
     * @param limit 조회할 사용자 수 (기본값: 10)
     * @return 최근 가입한 사용자 목록
     */
    @GetMapping("/recent")
    fun getRecentUsers(
        @RequestParam(defaultValue = "10") limit: Int
    ): Flux<UserResponse> {
        return userService.getRecentUsers(limit)
    }

    /**
     * 활성 사용자 목록 조회
     * @param page 페이지 번호
     * @param size 페이지 당 사용자 수
     * @return 활성 사용자 목록
     */
    @GetMapping("/active")
    fun getActiveUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Flux<UserResponse> {
        return userService.getActiveUsers(page, size)
    }

    /**
     * 비활성 사용자 목록 조회
     * @param page 페이지 번호
     * @param size 페이지 당 사용자 수
     * @return 비활성 사용자 목록
     */
    @GetMapping("/inactive")
    fun getInactiveUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Flux<UserResponse> {
        return userService.getInactiveUsers(page, size)
    }

    /**
     * 사용자 존재 여부 확인
     * @param id 사용자 ID
     * @return 존재 여부
     */
    @GetMapping("/{id}/exists")
    fun checkUserExists(@PathVariable id: Long): Mono<ResponseEntity<Map<String, Boolean>>> {
        return userService.userExists(id)
            .map { exists -> ResponseEntity.ok(mapOf("exists" to exists)) }
    }

    /**
     * 이메일 중복 확인
     * @param email 확인할 이메일
     * @return 중복 여부
     */
    @GetMapping("/email/{email}/exists")
    fun checkEmailExists(@PathVariable email: String): Mono<ResponseEntity<Map<String, Boolean>>> {
        return userService.emailExists(email)
            .map { exists -> ResponseEntity.ok(mapOf("exists" to exists)) }
    }

    /**
     * 사용자 이름 중복 확인
     * @param username 확인할 사용자 이름
     * @return 중복 여부
     */
    @GetMapping("/username/{username}/exists")
    fun checkUsernameExists(@PathVariable username: String): Mono<ResponseEntity<Map<String, Boolean>>> {
        return userService.usernameExists(username)
            .map { exists -> ResponseEntity.ok(mapOf("exists" to exists)) }
    }

    /**
     * 사용자 프로필 상세 조회 (게시물, 댓글 수 포함)
     * @param id 사용자 ID
     * @return 상세 프로필 정보
     */
    @GetMapping("/{id}/profile")
    fun getUserProfile(@PathVariable id: Long): Mono<ResponseEntity<UserProfileResponse>> {
        return userService.getUserProfile(id)
            .map { profile -> ResponseEntity.ok(profile) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
    }
}