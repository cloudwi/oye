package com.mindbridge.oye.config

import com.mindbridge.oye.service.OAuth2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val oAuth2UserService: OAuth2UserService,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
    private val rateLimitFilter: RateLimitFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
                headers.httpStrictTransportSecurity { hsts ->
                    hsts.includeSubDomains(true)
                    hsts.maxAgeInSeconds(31536000)
                }
                headers.contentTypeOptions { }
                headers.xssProtection { xss ->
                    xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                }
                headers.referrerPolicy { referrer ->
                    referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                }
                headers.permissionsPolicy { permissions ->
                    permissions.policy("camera=(), microphone=(), geolocation=()")
                }
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/health").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/login/**", "/oauth2/**").permitAll()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(oAuth2UserService)
                    }
                    .successHandler(oAuth2SuccessHandler)
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter::class.java)
            .logout { it.disable() }

        return http.build()
    }
}
