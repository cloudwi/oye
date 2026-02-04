package com.mindbridge.oye.service

import com.mindbridge.oye.domain.User
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
    private val userRepository: UserRepository
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val kakaoId = oAuth2User.name

        val user = userRepository.findByKakaoId(kakaoId)
            ?: createUser(kakaoId, oAuth2User)

        val attributes = oAuth2User.attributes.toMutableMap()
        attributes["userId"] = user.id

        return DefaultOAuth2User(
            oAuth2User.authorities,
            attributes,
            "id"
        )
    }

    private fun createUser(kakaoId: String, oAuth2User: OAuth2User): User {
        val properties = oAuth2User.attributes["properties"] as? Map<*, *>
        val nickname = properties?.get("nickname") as? String ?: "사용자"

        val user = User(
            kakaoId = kakaoId,
            name = nickname,
            birthDate = LocalDate.of(2000, 1, 1)
        )
        return userRepository.save(user)
    }
}
