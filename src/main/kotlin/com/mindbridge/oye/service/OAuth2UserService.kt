package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.SocialAccount
import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.repository.SocialAccountRepository
import com.mindbridge.oye.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OAuth2UserService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val kakaoId = oAuth2User.name

        val socialAccount = socialAccountRepository.findByProviderAndProviderId(SocialProvider.KAKAO, kakaoId)
        val isNewUser = socialAccount == null
        val user = socialAccount?.user ?: createUser(kakaoId, oAuth2User)

        val attributes = oAuth2User.attributes.toMutableMap()
        attributes["userId"] = user.id
        attributes["isNewUser"] = isNewUser

        return DefaultOAuth2User(
            oAuth2User.authorities,
            attributes,
            "id"
        )
    }

    private fun createUser(kakaoId: String, oAuth2User: OAuth2User): User {
        val properties = oAuth2User.attributes["properties"] as? Map<*, *>
        val nickname = properties?.get("nickname") as? String ?: "사용자"

        val user = userRepository.save(
            User(
                name = nickname,
                birthDate = LocalDate.of(2000, 1, 1),
                calendarType = CalendarType.SOLAR
            )
        )

        socialAccountRepository.save(
            SocialAccount(
                user = user,
                provider = SocialProvider.KAKAO,
                providerId = kakaoId
            )
        )

        return user
    }
}
