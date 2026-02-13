package com.web3.community.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "Invalid input"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "Internal server error"),
    SYSTEM_BUSY(HttpStatus.SERVICE_UNAVAILABLE, "C003", "서비스 이용이 원활하지 않습니다."),

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A001", "Invalid credentials"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A002", "Token expired"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A003", "Invalid token"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "A004", "Email already exists"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A005", "Unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A006", "Forbidden"),
    OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A007", "OAuth authentication failed"),
    SOCIAL_LOGIN_ONLY(HttpStatus.BAD_REQUEST, "A008", "This account uses social login. Please login with the social provider."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "Post not found"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "Category not found"),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "Comment not found"),
}
