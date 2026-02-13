package com.mindbridge.oye.config

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI

@Component
class AppleTokenVerifier(
    @Value("\${apple.bundle-id}")
    private val bundleId: String
) {
    companion object {
        private const val APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys"
        private const val APPLE_ISSUER = "https://appleid.apple.com"
    }

    fun verify(identityToken: String): String {
        val jwkSet = JWKSet.load(URI(APPLE_JWKS_URL).toURL())
        val keySource = ImmutableJWKSet<SecurityContext>(jwkSet)

        val jwtProcessor = DefaultJWTProcessor<SecurityContext>()
        jwtProcessor.jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, keySource)
        jwtProcessor.jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
            JWTClaimsSet.Builder()
                .issuer(APPLE_ISSUER)
                .audience(bundleId)
                .build(),
            setOf("sub", "iss", "aud", "exp", "iat")
        )

        val claimsSet = jwtProcessor.process(identityToken, null)
        return claimsSet.subject
    }
}
