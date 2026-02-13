package com.mindbridge.oye.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        val jwtProperties = JwtProperties(
            secret = "test-secret-key-for-jwt-testing-must-be-at-least-32-bytes-long!!",
            accessTokenExpiration = 3600000,
            refreshTokenExpiration = 604800000
        )
        jwtTokenProvider = JwtTokenProvider(jwtProperties)
    }

    @Test
    fun `generateAccessToken - creates valid token`() {
        val userId = 1L

        val token = jwtTokenProvider.generateAccessToken(userId)

        assertTrue(token.isNotBlank())
        assertTrue(jwtTokenProvider.validateToken(token))
    }

    @Test
    fun `generateRefreshToken - creates valid token`() {
        val userId = 1L

        val token = jwtTokenProvider.generateRefreshToken(userId)

        assertTrue(token.isNotBlank())
        assertTrue(jwtTokenProvider.validateToken(token))
    }

    @Test
    fun `getUserIdFromToken - extracts correct userId`() {
        val userId = 42L
        val token = jwtTokenProvider.generateAccessToken(userId)

        val extractedUserId = jwtTokenProvider.getUserIdFromToken(token)

        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `validateToken - returns false for expired token`() {
        val jwtProperties = JwtProperties(
            secret = "test-secret-key-for-jwt-testing-must-be-at-least-32-bytes-long!!",
            accessTokenExpiration = -1000,
            refreshTokenExpiration = -1000
        )
        val provider = JwtTokenProvider(jwtProperties)

        val token = provider.generateAccessToken(1L)

        assertFalse(jwtTokenProvider.validateToken(token))
    }

    @Test
    fun `validateToken - returns false for invalid token`() {
        val result = jwtTokenProvider.validateToken("invalid.token.string")

        assertFalse(result)
    }

    @Test
    fun `validateToken - returns false for tampered token`() {
        val token = jwtTokenProvider.generateAccessToken(1L)
        val tamperedToken = token + "tampered"

        assertFalse(jwtTokenProvider.validateToken(tamperedToken))
    }

    @Test
    fun `validateToken - returns false for token signed with different key`() {
        val otherProperties = JwtProperties(
            secret = "different-secret-key-for-jwt-testing-must-be-at-least-32-bytes!!",
            accessTokenExpiration = 3600000,
            refreshTokenExpiration = 604800000
        )
        val otherProvider = JwtTokenProvider(otherProperties)
        val token = otherProvider.generateAccessToken(1L)

        assertFalse(jwtTokenProvider.validateToken(token))
    }
}
