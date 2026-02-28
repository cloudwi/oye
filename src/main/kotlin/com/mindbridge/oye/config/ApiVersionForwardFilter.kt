package com.mindbridge.oye.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.DispatcherType

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class ApiVersionForwardFilter : OncePerRequestFilter() {

    private val versionPrefix = Regex("^/api/v\\d+/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        if (request.dispatcherType == DispatcherType.REQUEST && versionPrefix.containsMatchIn(path)) {
            val forwardPath = path.replaceFirst(versionPrefix, "/api/")
            request.getRequestDispatcher(forwardPath).forward(request, response)
            return
        }

        filterChain.doFilter(request, response)
    }
}
