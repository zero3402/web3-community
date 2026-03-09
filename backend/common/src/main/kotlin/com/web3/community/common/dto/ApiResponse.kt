package com.web3.community.common.dto

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun <T> success(data: T, message: String = "Success"): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }

        fun <T> success(message: String = "Success"): ApiResponse<T> {
            return ApiResponse(success = true, message = message)
        }

        fun <T> error(errorCode: String, message: String): ApiResponse<T> {
            return ApiResponse(success = false, errorCode = errorCode, message = message)
        }
    }
}
