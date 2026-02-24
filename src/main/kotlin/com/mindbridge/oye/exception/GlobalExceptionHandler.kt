package com.mindbridge.oye.exception

import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@Schema(description = "에러 응답")
data class ErrorResponse(
    @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다.")
    val message: String,
    @Schema(description = "에러 코드", example = "USER_NOT_FOUND")
    val code: String
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    // --- Domain exceptions ---

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFoundException(e: UserNotFoundException) =
        errorResponse(HttpStatus.NOT_FOUND, e.message ?: "사용자를 찾을 수 없습니다.", "USER_NOT_FOUND")

    @ExceptionHandler(FortuneGenerationException::class)
    fun handleFortuneGenerationException(e: FortuneGenerationException): ResponseEntity<ErrorResponse> {
        log.error("예감 생성 실패: {}", e.message)
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.message ?: "예감 생성에 실패했습니다.", "FORTUNE_GENERATION_FAILED")
    }

    @ExceptionHandler(InquiryNotFoundException::class)
    fun handleInquiryNotFoundException(e: InquiryNotFoundException) =
        errorResponse(HttpStatus.NOT_FOUND, e.message ?: "문의를 찾을 수 없습니다.", "INQUIRY_NOT_FOUND")

    @ExceptionHandler(ConnectionNotFoundException::class)
    fun handleConnectionNotFoundException(e: ConnectionNotFoundException) =
        errorResponse(HttpStatus.NOT_FOUND, e.message ?: "연결을 찾을 수 없습니다.", "CONNECTION_NOT_FOUND")

    @ExceptionHandler(CompatibilityGenerationException::class)
    fun handleCompatibilityGenerationException(e: CompatibilityGenerationException): ResponseEntity<ErrorResponse> {
        log.error("궁합 생성 실패: {}", e.message)
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.message ?: "궁합 생성에 실패했습니다.", "COMPATIBILITY_GENERATION_FAILED")
    }

    @ExceptionHandler(SelfConnectionException::class)
    fun handleSelfConnectionException(e: SelfConnectionException) =
        errorResponse(HttpStatus.BAD_REQUEST, e.message ?: "자기 자신과는 연결할 수 없습니다.", "SELF_CONNECTION")

    @ExceptionHandler(DuplicateConnectionException::class)
    fun handleDuplicateConnectionException(e: DuplicateConnectionException) =
        errorResponse(HttpStatus.CONFLICT, e.message ?: "이미 연결된 사용자입니다.", "DUPLICATE_CONNECTION")

    @ExceptionHandler(TooManyRequestsException::class)
    fun handleTooManyRequestsException(e: TooManyRequestsException) =
        errorResponse(HttpStatus.TOO_MANY_REQUESTS, e.message ?: "요청이 너무 많습니다.", "TOO_MANY_REQUESTS")

    @ExceptionHandler(LottoRoundNotFoundException::class)
    fun handleLottoRoundNotFoundException(e: LottoRoundNotFoundException) =
        errorResponse(HttpStatus.NOT_FOUND, e.message ?: "로또 회차를 찾을 수 없습니다.", "LOTTO_ROUND_NOT_FOUND")

    @ExceptionHandler(LottoAlreadyRecommendedException::class)
    fun handleLottoAlreadyRecommendedException(e: LottoAlreadyRecommendedException) =
        errorResponse(HttpStatus.CONFLICT, e.message ?: "이미 해당 회차에 추천을 받았습니다.", "LOTTO_ALREADY_RECOMMENDED")

    @ExceptionHandler(LottoDrawNotAvailableException::class)
    fun handleLottoDrawNotAvailableException(e: LottoDrawNotAvailableException) =
        errorResponse(HttpStatus.NOT_FOUND, e.message ?: "아직 추첨 결과를 가져올 수 없습니다.", "LOTTO_DRAW_NOT_AVAILABLE")

    // --- Auth/access exceptions ---

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(e: UnauthorizedException) =
        errorResponse(HttpStatus.UNAUTHORIZED, e.message ?: "인증이 필요합니다.", "UNAUTHORIZED")

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(e: ForbiddenException) =
        errorResponse(HttpStatus.FORBIDDEN, e.message ?: "권한이 없습니다.", "FORBIDDEN")

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
        log.warn("접근 거부: {}", e.message)
        return errorResponse(HttpStatus.FORBIDDEN, "접근이 거부되었습니다.", "ACCESS_DENIED")
    }

    // --- Request validation exceptions ---

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return errorResponse(HttpStatus.BAD_REQUEST, message, "VALIDATION_ERROR")
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException) =
        errorResponse(HttpStatus.BAD_REQUEST, e.message ?: "잘못된 요청입니다.", "BAD_REQUEST")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException) =
        errorResponse(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다.", "MESSAGE_NOT_READABLE")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(e: HttpRequestMethodNotSupportedException) =
        errorResponse(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다: ${e.method}", "METHOD_NOT_ALLOWED")

    // --- Infrastructure exceptions ---

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(e: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        log.warn("데이터 무결성 위반: {}", e.message)
        return errorResponse(HttpStatus.CONFLICT, "데이터 무결성 위반이 발생했습니다.", "DATA_INTEGRITY_VIOLATION")
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("처리되지 않은 예외 발생", e)
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR")
    }

    private fun errorResponse(status: HttpStatus, message: String, code: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(ErrorResponse(message, code))
}
