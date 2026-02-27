package com.mindbridge.oye.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AiResponseParserTest {

    // === sanitizeJson ===

    @Test
    fun `sanitizeJson - 깨끗한 JSON은 그대로 반환한다`() {
        val input = """{"score": 85, "content": "좋은 하루"}"""
        val result = AiResponseParser.sanitizeJson(input)
        assertEquals("""{"score": 85, "content": "좋은 하루"}""", result)
    }

    @Test
    fun `sanitizeJson - 마크다운으로 감싼 JSON에서 JSON만 추출한다`() {
        val input = """
            ```json
            {"score": 75, "content": "오늘은 좋은 날"}
            ```
        """.trimIndent()
        val result = AiResponseParser.sanitizeJson(input)
        assertEquals("""{"score": 75, "content": "오늘은 좋은 날"}""", result)
    }

    @Test
    fun `sanitizeJson - JSON 앞뒤에 텍스트가 있어도 JSON만 추출한다`() {
        val input = """다음은 결과입니다: {"score": 60, "content": "보통의 하루"} 이상입니다."""
        val result = AiResponseParser.sanitizeJson(input)
        assertEquals("""{"score": 60, "content": "보통의 하루"}""", result)
    }

    @Test
    fun `sanitizeJson - 공백이 많아도 JSON을 추출한다`() {
        val input = """

            {"score": 90, "content": "최고의 하루"}

        """
        val result = AiResponseParser.sanitizeJson(input)
        assertEquals("""{"score": 90, "content": "최고의 하루"}""", result)
    }

    @Test
    fun `sanitizeJson - 중괄호가 없으면 원본 trimmed를 반환한다`() {
        val input = "  no json here  "
        val result = AiResponseParser.sanitizeJson(input)
        assertEquals("no json here", result)
    }

    @Test
    fun `sanitizeJson - 여는 중괄호만 있으면 원본 trimmed를 반환한다`() {
        val input = "{ only open"
        val result = AiResponseParser.sanitizeJson(input)
        assertEquals("{ only open", result)
    }

    @Test
    fun `sanitizeJson - 닫는 중괄호가 여는 중괄호보다 앞에 있으면 원본 반환`() {
        val input = "} before {"
        val result = AiResponseParser.sanitizeJson(input)
        // lastIndexOf('}') = 0, indexOf('{') = 9 -> jsonEnd(0) <= jsonStart(9), returns trimmed
        assertEquals("} before {", result)
    }

    // === parseScoreAndContent ===

    @Test
    fun `parseScoreAndContent - 유효한 JSON을 정상 파싱한다`() {
        val json = """{"score": 85, "content": "오늘 좋은 일이 있을 거예요"}"""
        val result = AiResponseParser.parseScoreAndContent(json)
        assertEquals(85, result.score)
        assertEquals("오늘 좋은 일이 있을 거예요", result.content)
    }

    @Test
    fun `parseScoreAndContent - score가 100 초과면 100으로 클램핑한다`() {
        val json = """{"score": 150, "content": "최고의 하루"}"""
        val result = AiResponseParser.parseScoreAndContent(json)
        assertEquals(100, result.score)
    }

    @Test
    fun `parseScoreAndContent - score가 0 미만이면 0으로 클램핑한다`() {
        val json = """{"score": -10, "content": "힘든 하루"}"""
        val result = AiResponseParser.parseScoreAndContent(json)
        assertEquals(0, result.score)
    }

    @Test
    fun `parseScoreAndContent - score가 경계값 0이면 그대로 반환한다`() {
        val json = """{"score": 0, "content": "시작"}"""
        val result = AiResponseParser.parseScoreAndContent(json)
        assertEquals(0, result.score)
    }

    @Test
    fun `parseScoreAndContent - score가 경계값 100이면 그대로 반환한다`() {
        val json = """{"score": 100, "content": "완벽"}"""
        val result = AiResponseParser.parseScoreAndContent(json)
        assertEquals(100, result.score)
    }

    @Test
    fun `parseScoreAndContent - content가 maxContentLength를 초과하면 잘린다`() {
        val longContent = "가".repeat(100)
        val json = """{"score": 50, "content": "$longContent"}"""
        val result = AiResponseParser.parseScoreAndContent(json, maxContentLength = 80)
        assertEquals(80, result.content.length)
        assertEquals("가".repeat(80), result.content)
    }

    @Test
    fun `parseScoreAndContent - content가 maxContentLength 이하면 그대로 반환한다`() {
        val json = """{"score": 50, "content": "짧은 내용"}"""
        val result = AiResponseParser.parseScoreAndContent(json, maxContentLength = 80)
        assertEquals("짧은 내용", result.content)
    }

    @Test
    fun `parseScoreAndContent - 유효하지 않은 JSON이면 예외 발생`() {
        assertThrows<Exception> {
            AiResponseParser.parseScoreAndContent("이것은 JSON이 아닙니다")
        }
    }

    @Test
    fun `parseScoreAndContent - score가 문자열이고 defaultScore가 없으면 예외 발생`() {
        val json = """{"score": "high", "content": "좋은 하루"}"""
        assertThrows<IllegalArgumentException> {
            AiResponseParser.parseScoreAndContent(json)
        }
    }

    @Test
    fun `parseScoreAndContent - score가 문자열이고 defaultScore가 있으면 기본값 사용`() {
        val json = """{"score": "high", "content": "좋은 하루"}"""
        val result = AiResponseParser.parseScoreAndContent(json, defaultScore = 50)
        assertEquals(50, result.score)
        assertEquals("좋은 하루", result.content)
    }

    @Test
    fun `parseScoreAndContent - content가 없으면 예외 발생`() {
        val json = """{"score": 85}"""
        assertThrows<IllegalArgumentException> {
            AiResponseParser.parseScoreAndContent(json)
        }
    }

    @Test
    fun `parseScoreAndContent - content가 숫자면 예외 발생`() {
        val json = """{"score": 85, "content": 123}"""
        assertThrows<IllegalArgumentException> {
            AiResponseParser.parseScoreAndContent(json)
        }
    }

    @Test
    fun `parseScoreAndContent - 커스텀 scoreRange를 사용할 수 있다`() {
        val json = """{"score": 150, "content": "테스트"}"""
        val result = AiResponseParser.parseScoreAndContent(json, scoreRange = 1..10)
        assertEquals(10, result.score)
    }

    @Test
    fun `parseScoreAndContent - score가 소수점이면 정수로 변환된다`() {
        val json = """{"score": 85.7, "content": "소수점 점수"}"""
        val result = AiResponseParser.parseScoreAndContent(json)
        assertEquals(85, result.score)
    }
}
