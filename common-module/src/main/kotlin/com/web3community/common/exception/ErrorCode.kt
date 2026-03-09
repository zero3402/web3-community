package com.web3community.common.exception

import org.springframework.http.HttpStatus

/**
 * ErrorCode - 전체 서비스 공통 에러 코드 열거형
 *
 * MSA 환경에서 모든 서비스가 일관된 에러 코드를 사용하도록 중앙에서 관리합니다.
 * 각 에러 코드는 HTTP 상태 코드와 사용자 친화적인 메시지를 포함합니다.
 *
 * 네이밍 규칙:
 * - {도메인}_{세 자리 숫자}
 * - AUTH: 인증/인가 관련 (AUTH_001 ~ AUTH_010)
 * - USER: 사용자 관련 (USER_001 ~ USER_010)
 * - BOARD: 게시판 관련 (BOARD_001 ~ BOARD_010)
 * - BLOCKCHAIN: 블록체인 관련 (BLOCKCHAIN_001 ~ BLOCKCHAIN_020)
 * - COMMON: 공통 에러 (COMMON_001 ~ COMMON_010)
 *
 * 사용 예시:
 * ```kotlin
 * throw BusinessException(ErrorCode.AUTH_001)
 * // 또는
 * return ApiResponse.error(ErrorCode.USER_001.status.value(), ErrorCode.USER_001.message)
 * ```
 *
 * @property status HTTP 상태 코드 (Spring HttpStatus)
 * @property message 사용자에게 전달할 에러 메시지
 */
enum class ErrorCode(
    val status: HttpStatus,
    val message: String
) {

    // ============================================================
    // AUTH - 인증/인가 관련 에러 (AUTH_001 ~ AUTH_010)
    // ============================================================

    /** 로그인 필요 - JWT 토큰이 없거나 만료된 경우 */
    AUTH_001(HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 로그인 후 다시 시도해 주세요."),

    /** 권한 없음 - 접근 권한이 부족한 경우 */
    AUTH_002(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    /** JWT 토큰 만료 */
    AUTH_003(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다. 다시 로그인해 주세요."),

    /** JWT 토큰 형식 오류 - 위변조 또는 잘못된 형식 */
    AUTH_004(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),

    /** Refresh Token 없음 또는 만료 */
    AUTH_005(HttpStatus.UNAUTHORIZED, "Refresh Token이 유효하지 않습니다. 다시 로그인해 주세요."),

    /** 이메일 또는 비밀번호 불일치 */
    AUTH_006(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    /** 이메일 인증 미완료 계정 */
    AUTH_007(HttpStatus.FORBIDDEN, "이메일 인증이 완료되지 않은 계정입니다."),

    /** 정지된 계정 */
    AUTH_008(HttpStatus.FORBIDDEN, "정지된 계정입니다. 고객센터에 문의해 주세요."),

    /** 탈퇴한 계정 */
    AUTH_009(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다."),

    /** OAuth2 소셜 로그인 실패 */
    AUTH_010(HttpStatus.BAD_REQUEST, "소셜 로그인 처리 중 오류가 발생했습니다."),


    // ============================================================
    // USER - 사용자 관련 에러 (USER_001 ~ USER_010)
    // ============================================================

    /** 사용자를 찾을 수 없음 */
    USER_001(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    /** 이미 가입된 이메일 */
    USER_002(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    /** 이미 사용 중인 닉네임 */
    USER_003(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    /** 비밀번호 형식 오류 */
    USER_004(HttpStatus.BAD_REQUEST, "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다."),

    /** 현재 비밀번호 불일치 (비밀번호 변경 시) */
    USER_005(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),

    /** 자기 자신을 팔로우/신고 하는 경우 */
    USER_006(HttpStatus.BAD_REQUEST, "자기 자신에 대해 해당 작업을 수행할 수 없습니다."),

    /** 이미 팔로우 중인 사용자 */
    USER_007(HttpStatus.CONFLICT, "이미 팔로우 중인 사용자입니다."),

    /** 프로필 이미지 업로드 실패 */
    USER_008(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 이미지 업로드에 실패했습니다."),

    /** 이메일 형식 오류 */
    USER_009(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),

    /** 사용자 정보 수정 권한 없음 */
    USER_010(HttpStatus.FORBIDDEN, "다른 사용자의 정보를 수정할 수 없습니다."),


    // ============================================================
    // BOARD - 게시판 관련 에러 (BOARD_001 ~ BOARD_010)
    // ============================================================

    /** 게시글을 찾을 수 없음 */
    BOARD_001(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),

    /** 게시글 수정/삭제 권한 없음 */
    BOARD_002(HttpStatus.FORBIDDEN, "해당 게시글에 대한 권한이 없습니다."),

    /** 이미 좋아요한 게시글 */
    BOARD_003(HttpStatus.CONFLICT, "이미 좋아요를 누른 게시글입니다."),

    /** 이미 싫어요한 게시글 */
    BOARD_004(HttpStatus.CONFLICT, "이미 싫어요를 누른 게시글입니다."),

    /** 댓글을 찾을 수 없음 */
    BOARD_005(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),

    /** 댓글 수정/삭제 권한 없음 */
    BOARD_006(HttpStatus.FORBIDDEN, "해당 댓글에 대한 권한이 없습니다."),

    /** 게시글 제목 길이 초과 */
    BOARD_007(HttpStatus.BAD_REQUEST, "게시글 제목은 200자 이내여야 합니다."),

    /** 게시글 내용 길이 초과 */
    BOARD_008(HttpStatus.BAD_REQUEST, "게시글 내용이 허용 범위를 초과했습니다."),

    /** 삭제된 게시글 접근 */
    BOARD_009(HttpStatus.GONE, "삭제된 게시글입니다."),

    /** 카테고리를 찾을 수 없음 */
    BOARD_010(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),


    // ============================================================
    // BLOCKCHAIN - 블록체인 관련 에러 (BLOCKCHAIN_001 ~ BLOCKCHAIN_020)
    // ============================================================

    /** 지원하지 않는 블록체인 네트워크 */
    BLOCKCHAIN_001(HttpStatus.BAD_REQUEST, "지원하지 않는 블록체인 네트워크입니다."),

    /** 지갑이 이미 존재함 */
    BLOCKCHAIN_002(HttpStatus.CONFLICT, "해당 체인의 지갑이 이미 존재합니다."),

    /** 지갑을 찾을 수 없음 */
    BLOCKCHAIN_003(HttpStatus.NOT_FOUND, "지갑을 찾을 수 없습니다."),

    /** 잔액 부족 */
    BLOCKCHAIN_004(HttpStatus.BAD_REQUEST, "잔액이 부족합니다. 수수료를 포함한 금액을 확인해 주세요."),

    /** 유효하지 않은 블록체인 주소 */
    BLOCKCHAIN_005(HttpStatus.BAD_REQUEST, "유효하지 않은 블록체인 주소입니다."),

    /** 트랜잭션 생성 실패 */
    BLOCKCHAIN_006(HttpStatus.INTERNAL_SERVER_ERROR, "트랜잭션 생성에 실패했습니다."),

    /** 트랜잭션 브로드캐스트 실패 - 네트워크 오류 */
    BLOCKCHAIN_007(HttpStatus.SERVICE_UNAVAILABLE, "블록체인 네트워크 전송에 실패했습니다. 잠시 후 다시 시도해 주세요."),

    /** 트랜잭션을 찾을 수 없음 */
    BLOCKCHAIN_008(HttpStatus.NOT_FOUND, "트랜잭션을 찾을 수 없습니다."),

    /** UTXO 락 획득 실패 - 동시 출금 충돌 */
    BLOCKCHAIN_009(HttpStatus.CONFLICT, "현재 처리 중인 출금이 있습니다. 완료 후 다시 시도해 주세요."),

    /** Nonce 락 획득 실패 - Ethereum 동시 트랜잭션 충돌 */
    BLOCKCHAIN_010(HttpStatus.CONFLICT, "트랜잭션 처리 중입니다. 잠시 후 다시 시도해 주세요."),

    /** 프라이빗 키 암호화 실패 */
    BLOCKCHAIN_011(HttpStatus.INTERNAL_SERVER_ERROR, "지갑 키 처리 중 오류가 발생했습니다."),

    /** 프라이빗 키 복호화 실패 */
    BLOCKCHAIN_012(HttpStatus.INTERNAL_SERVER_ERROR, "지갑 접근 중 오류가 발생했습니다."),

    /** 가스비(수수료) 조회 실패 */
    BLOCKCHAIN_013(HttpStatus.SERVICE_UNAVAILABLE, "네트워크 수수료 조회에 실패했습니다."),

    /** 최소 전송 금액 미달 */
    BLOCKCHAIN_014(HttpStatus.BAD_REQUEST, "최소 전송 가능 금액에 미달합니다."),

    /** 최대 전송 금액 초과 */
    BLOCKCHAIN_015(HttpStatus.BAD_REQUEST, "1회 최대 전송 한도를 초과했습니다."),

    /** 다중 출금 수신자 수 초과 */
    BLOCKCHAIN_016(HttpStatus.BAD_REQUEST, "다중 출금 수신자는 최대 100명까지 가능합니다."),

    /** ERC-20 토큰 컨트랙트를 찾을 수 없음 */
    BLOCKCHAIN_017(HttpStatus.NOT_FOUND, "지원하지 않는 ERC-20 토큰입니다."),

    /** 블록체인 노드 연결 실패 */
    BLOCKCHAIN_018(HttpStatus.SERVICE_UNAVAILABLE, "블록체인 노드 연결에 실패했습니다."),

    /** 트랜잭션 이미 처리됨 (중복 요청 방지) */
    BLOCKCHAIN_019(HttpStatus.CONFLICT, "이미 처리된 트랜잭션입니다."),

    /** 지갑 생성 실패 */
    BLOCKCHAIN_020(HttpStatus.INTERNAL_SERVER_ERROR, "지갑 생성에 실패했습니다."),


    // ============================================================
    // COMMON - 공통 에러 (COMMON_001 ~ COMMON_010)
    // ============================================================

    /** 내부 서버 오류 */
    COMMON_001(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),

    /** 잘못된 요청 파라미터 */
    COMMON_002(HttpStatus.BAD_REQUEST, "잘못된 요청입니다. 요청 파라미터를 확인해 주세요."),

    /** 요청 리소스를 찾을 수 없음 */
    COMMON_003(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    /** 지원하지 않는 HTTP 메서드 */
    COMMON_004(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),

    /** 요청 처리 시간 초과 */
    COMMON_005(HttpStatus.REQUEST_TIMEOUT, "요청 처리 시간이 초과되었습니다."),

    /** 너무 많은 요청 (Rate Limiting) */
    COMMON_006(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),

    /** 파일 크기 초과 */
    COMMON_007(HttpStatus.PAYLOAD_TOO_LARGE, "업로드 파일 크기가 허용 범위를 초과했습니다."),

    /** 지원하지 않는 미디어 타입 */
    COMMON_008(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 파일 형식입니다."),

    /** 외부 서비스 오류 (써드파티 API 장애) */
    COMMON_009(HttpStatus.BAD_GATEWAY, "외부 서비스 연동 중 오류가 발생했습니다."),

    /** 데이터베이스 오류 */
    COMMON_010(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 처리 중 오류가 발생했습니다.");


    /**
     * HTTP 상태 코드 숫자값 반환
     * ApiResponse에서 직접 코드 숫자값이 필요할 때 사용합니다.
     */
    fun statusCode(): Int = status.value()
}
