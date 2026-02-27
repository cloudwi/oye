package com.mindbridge.oye.service

import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.repository.SocialAccountRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OAuth2UserService(
    private val socialAccountRepository: SocialAccountRepository,
    private val authService: AuthService
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val kakaoId = oAuth2User.name

        val socialAccount = socialAccountRepository.findByProviderAndProviderId(SocialProvider.KAKAO, kakaoId)
        val isNewUser = socialAccount == null
        val user = socialAccount?.user ?: run {
            val properties = oAuth2User.attributes["properties"] as? Map<*, *>
            val nickname = properties?.get("nickname") as? String
            authService.createUser(SocialProvider.KAKAO, kakaoId, nickname)
        }

        val attributes = oAuth2User.attributes.toMutableMap()
        attributes["userId"] = user.id
        attributes["isNewUser"] = isNewUser

        return DefaultOAuth2User(
            oAuth2User.authorities,
            attributes,
            "id"
        )
    }
}
