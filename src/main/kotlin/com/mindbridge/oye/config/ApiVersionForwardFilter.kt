package com.mindbridge.oye.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.DispatcherType

/**
 * 하위 호환 필터: 버전 없는 /api/ 요청을 /api/v1/으로 포워딩.
 * 구버전 앱이 /api/ 경로를 사용하더라도 정상 동작하도록 보장.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class ApiVersionForwardFilter : OncePerRequestFilter() {

    private val unversionedApi = Regex("^/api/(?!v\\d+/)(.*)")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        if (request.dispatcherType == DispatcherType.REQUEST && unversionedApi.containsMatchIn(path)) {
            val forwardPath = path.replaceFirst("/api/", "/api/v1/")
            request.getRequestDispatcher(forwardPath).forward(request, response)
            return
        }

        filterChain.doFilter(request, response)
    }
}
