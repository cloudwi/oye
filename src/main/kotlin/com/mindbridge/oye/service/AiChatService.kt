package com.mindbridge.oye.service

import com.mindbridge.oye.exception.TooManyRequestsException
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

@Service
class AiChatService(
    chatClientBuilder: ChatClient.Builder
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val chatClient: ChatClient = chatClientBuilder.build()

    companion object {
        private const val MAX_RETRY_COUNT = 2
    }

    fun <T> callWithRetry(
        systemPrompt: String,
        userPrompt: String,
        errorMessage: String,
        parser: (String) -> T
    ): T {
        var lastException: Exception? = null

        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                val response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content()

                if (!response.isNullOrBlank()) {
                    return parser(response)
                }
                log.warn("AI 응답이 비어있습니다. 재시도 {}/{}", attempt + 1, MAX_RETRY_COUNT)
            } catch (e: Exception) {
                lastException = e
                log.warn("AI 호출 실패 (재시도 {}/{}): {}", attempt + 1, MAX_RETRY_COUNT, e.message)
                if (isRateLimitError(e)) {
                    throw TooManyRequestsException("AI 서비스가 일시적으로 사용량 한도에 도달했습니다. 잠시 후 다시 시도해주세요.")
                }
            }
        }

        throw RuntimeException("$errorMessage: ${lastException?.message ?: "빈 응답"}", lastException)
    }

    private fun isRateLimitError(e: Exception): Boolean {
        val message = e.message ?: return false
        return message.contains("429") || message.contains("RESOURCE_EXHAUSTED") || message.contains("rate limit", ignoreCase = true)
    }
}
