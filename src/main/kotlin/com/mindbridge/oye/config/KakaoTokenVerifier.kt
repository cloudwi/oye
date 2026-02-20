package com.mindbridge.oye.config

import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod

data class KakaoUserInfo(
    val id: String,
    val nickname: String
)

@Component
class KakaoTokenVerifier {

    companion object {
        private const val KAKAO_USER_ME_URL = "https://kapi.kakao.com/v2/user/me"
    }

    fun verify(accessToken: String): KakaoUserInfo {
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()
        headers.setBearerAuth(accessToken)
        val entity = HttpEntity<Void>(headers)

        val response = restTemplate.exchange(
            KAKAO_USER_ME_URL,
            HttpMethod.GET,
            entity,
            Map::class.java
        )

        val body = response.body ?: throw IllegalStateException("카카오 사용자 정보 응답이 비어있습니다.")
        val id = body["id"]?.toString() ?: throw IllegalStateException("카카오 사용자 ID를 찾을 수 없습니다.")
        val properties = body["properties"] as? Map<*, *>
        val nickname = properties?.get("nickname") as? String ?: "사용자"

        return KakaoUserInfo(id = id, nickname = nickname)
    }
}
