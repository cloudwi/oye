package com.mindbridge.oye.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공통 API 응답")
data class ApiResponse<T>(
    @Schema(description = "성공 여부", example = "true")
    val success: Boolean,

    @Schema(description = "응답 데이터")
    val data: T? = null,

    @Schema(description = "응답 메시지")
    val message: String? = null,

    @Schema(description = "응답 코드")
    val code: String? = null
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data)
        }

        fun <T> success(data: T, message: String): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }

        fun <T> error(message: String, code: String): ApiResponse<T> {
            return ApiResponse(success = false, message = message, code = code)
        }
    }
}

@Schema(description = "페이지네이션 응답")
data class PageResponse<T>(
    @Schema(description = "데이터 목록")
    val content: List<T>,

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    val page: Int,

    @Schema(description = "페이지 크기", example = "20")
    val size: Int,

    @Schema(description = "전체 요소 수", example = "100")
    val totalElements: Long,

    @Schema(description = "전체 페이지 수", example = "5")
    val totalPages: Int
)
