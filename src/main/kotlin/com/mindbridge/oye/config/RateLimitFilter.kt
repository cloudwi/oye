package com.mindbridge.oye.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class RateLimitFilter : OncePerRequestFilter() {

    data class RateLimitEntry(
        val count: AtomicInteger = AtomicInteger(0),
        @Volatile var windowStart: Long = System.currentTimeMillis()
    )

    private val rateLimitMap = ConcurrentHashMap<String, RateLimitEntry>()

    companion object {
        private const val WINDOW_MS = 60_000L // 1분
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        val rateLimit = getRateLimit(path)

        if (rateLimit != null) {
            val key = buildKey(request, path)
            val (limit, _) = rateLimit

            if (!tryConsume(key, limit)) {
                response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.characterEncoding = "UTF-8"
                response.writer.write("""{"message":"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.","code":"TOO_MANY_REQUESTS"}""")
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    /**
     * 경로별 rate limit 설정 반환. Pair(limit, keyType)
     * keyType: "user" = 인증된 사용자 기준, "ip" = IP 기준
     */
    private fun getRateLimit(path: String): Pair<Int, String>? {
        return when {
            path.startsWith("/api/fortune/today") -> 5 to "user"
            path.startsWith("/api/auth/login") -> 10 to "ip"
            path == "/api/auth/refresh" -> 10 to "user"
            else -> null
        }
    }

    private fun buildKey(request: HttpServletRequest, path: String): String {
        val rateLimit = getRateLimit(path)!!
        val (_, keyType) = rateLimit

        val identifier = if (keyType == "user") {
            val auth = SecurityContextHolder.getContext().authentication
            if (auth != null && auth.principal is Long) {
                "user:${auth.principal}"
            } else {
                // 인증되지 않은 요청은 IP로 폴백
                "ip:${getClientIp(request)}"
            }
        } else {
            "ip:${getClientIp(request)}"
        }

        // 경로 카테고리별로 구분
        val pathCategory = when {
            path.startsWith("/api/fortune/today") -> "fortune"
            path.startsWith("/api/auth/login") -> "auth-login"
            path == "/api/auth/refresh" -> "auth-refresh"
            else -> "default"
        }

        return "$pathCategory:$identifier"
    }

    private fun tryConsume(key: String, limit: Int): Boolean {
        val now = System.currentTimeMillis()
        val entry = rateLimitMap.compute(key) { _, existing ->
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                RateLimitEntry(AtomicInteger(0), now)
            } else {
                existing
            }
        }!!

        return entry.count.incrementAndGet() <= limit
    }

    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (!xForwardedFor.isNullOrBlank()) {
            xForwardedFor.split(",")[0].trim()
        } else {
            request.remoteAddr
        }
    }
}
