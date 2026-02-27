package com.mindbridge.oye.util

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory

object AiResponseParser {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()

    fun sanitizeJson(response: String): String {
        val trimmed = response.trim()
        val jsonStart = trimmed.indexOf('{')
        val jsonEnd = trimmed.lastIndexOf('}')
        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            return trimmed
        }
        return trimmed.substring(jsonStart, jsonEnd + 1)
    }

    data class ScoreAndContent(val score: Int, val content: String)

    fun parseScoreAndContent(
        response: String,
        scoreRange: IntRange = 0..100,
        defaultScore: Int? = null,
        maxContentLength: Int = 80
    ): ScoreAndContent {
        val json: Map<String, Any> = objectMapper.readValue(response)

        val score = when (val raw = json["score"]) {
            is Number -> raw.toInt().coerceIn(scoreRange.first, scoreRange.last)
            else -> defaultScore
                ?: throw IllegalArgumentException("AI 응답에서 score를 파싱할 수 없습니다.")
        }

        val content = (json["content"] as? String)?.take(maxContentLength)
            ?: throw IllegalArgumentException("AI 응답에서 content를 파싱할 수 없습니다.")

        return ScoreAndContent(score = score, content = content)
    }
}
