package com.mindbridge.oye.config

import com.mindbridge.oye.exception.ExternalApiException
import org.springframework.beans.factory.annotation.Value
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
class KakaoTokenVerifier(
    @Value("\${kakao.native-app-key}")
    private val nativeAppKey: String
) {

    companion object {
        private const val KAKAO_TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info"
        private const val KAKAO_USER_ME_URL = "https://kapi.kakao.com/v2/user/me"
    }

    fun verify(accessToken: String): KakaoUserInfo {
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()
        headers.setBearerAuth(accessToken)
        val entity = HttpEntity<Void>(headers)

        // 1. 토큰이 우리 앱에서 발급된 것인지 검증
        val tokenInfoResponse = restTemplate.exchange(
            KAKAO_TOKEN_INFO_URL,
            HttpMethod.GET,
            entity,
            Map::class.java
        )
        val tokenInfo = tokenInfoResponse.body ?: throw ExternalApiException("카카오 토큰 정보 응답이 비어있습니다.")
        val appId = tokenInfo["app_id"]?.toString() ?: throw ExternalApiException("카카오 앱 ID를 확인할 수 없습니다.")
        if (appId != nativeAppKey) {
            throw ExternalApiException("카카오 토큰의 앱 ID가 일치하지 않습니다.")
        }

        // 2. 사용자 정보 조회
        val response = restTemplate.exchange(
            KAKAO_USER_ME_URL,
            HttpMethod.GET,
            entity,
            Map::class.java
        )

        val body = response.body ?: throw ExternalApiException("카카오 사용자 정보 응답이 비어있습니다.")
        val id = body["id"]?.toString() ?: throw ExternalApiException("카카오 사용자 ID를 찾을 수 없습니다.")
        val properties = body["properties"] as? Map<*, *>
        val nickname = properties?.get("nickname") as? String ?: "사용자"

        return KakaoUserInfo(id = id, nickname = nickname)
    }
}
