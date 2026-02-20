package com.mindbridge.oye.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()

        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            val method = request.method
            val uri = request.requestURI
            val query = request.queryString?.let { "?$it" } ?: ""
            val status = response.status
            val userId = getUserId()

            if (duration >= 3000) {
                log.warn("[API][SLOW] {} {}{} -> {} ({}ms) user={}", method, uri, query, status, duration, userId ?: "anonymous")
            } else {
                log.info("[API] {} {}{} -> {} ({}ms) user={}", method, uri, query, status, duration, userId ?: "anonymous")
            }
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/health") ||
                path.startsWith("/actuator") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/h2-console") ||
                path.startsWith("/favicon")
    }

    private fun getUserId(): Long? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        return auth.principal as? Long
    }
}
