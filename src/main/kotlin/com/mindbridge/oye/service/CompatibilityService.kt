package com.mindbridge.oye.service

import com.mindbridge.oye.domain.Compatibility
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import com.mindbridge.oye.dto.CompatibilityResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.exception.CompatibilityGenerationException
import com.mindbridge.oye.exception.ConnectionNotFoundException
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.repository.CompatibilityRepository
import com.mindbridge.oye.repository.UserConnectionRepository
import com.mindbridge.oye.util.UserProfileBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class CompatibilityService(
    chatClientBuilder: ChatClient.Builder,
    private val compatibilityRepository: CompatibilityRepository,
    private val userConnectionRepository: UserConnectionRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val chatClient: ChatClient = chatClientBuilder.build()
    private val objectMapper = jacksonObjectMapper()

    companion object {
        private const val MAX_RETRY_COUNT = 2
        private const val CONTENT_MAX_LENGTH = 500

        private val SYSTEM_PROMPT = """
            당신은 두 사람의 궁합을 분석하는 전문가입니다.

            두 사람의 프로필(생년월일, 태어난 시각, 성별, 양력/음력, MBTI, 혈액형, 직업, 관심사)과 관계 유형을 기반으로 오늘의 궁합을 분석하세요.

            규칙:
            - 한국어로 작성
            - 해요체로 작성 (~돼요, ~있어요, ~이에요)
            - 두 사람의 프로필 특성을 자연스럽게 반영
            - 관계 유형(연인/친구/가족/동료)에 맞는 조언 포함
            - 오늘 날짜의 기운을 반영하여 매일 다른 내용 생성
            - 긍정적이고 건설적인 내용으로 작성
            - 이모지 없이 텍스트만
            - 500자 이내

            반드시 아래 JSON 형식으로만 응답하세요 (다른 텍스트 없이):
            {"score": 85, "content": "오늘 두 분의 궁합은..."}

            score: 0-100 사이의 정수
            content: 궁합 분석 내용 (500자 이내)
        """.trimIndent()
    }

    @Transactional(readOnly = true)
    fun getTodayCompatibility(connection: UserConnection): Compatibility? {
        return compatibilityRepository.findByConnectionAndDate(connection, LocalDate.now())
    }

    fun generateCompatibility(connection: UserConnection): Compatibility {
        val existing = getTodayCompatibility(connection)
        if (existing != null) return existing

        val result = callAiWithRetry(connection)
        return try {
            saveCompatibility(connection, result.score, result.content)
        } catch (e: DataIntegrityViolationException) {
            getTodayCompatibility(connection)
                ?: throw CompatibilityGenerationException("궁합 저장 중 오류가 발생했습니다.")
        }
    }

    fun getCompatibility(user: User, connectionId: Long): CompatibilityResponse {
        val connection = userConnectionRepository.findByIdWithUsers(connectionId)
            .orElseThrow { ConnectionNotFoundException() }

        if (connection.user.id != user.id && connection.partner.id != user.id) {
            throw ForbiddenException("해당 연결에 접근할 권한이 없습니다.")
        }

        // 1. 기존 결과 확인
        val existing = getTodayCompatibility(connection)
        if (existing != null) {
            return CompatibilityResponse.from(existing)
        }

        // 2. AI 호출 (트랜잭션 외부)
        val result = callAiWithRetry(connection)

        // 3. 저장 시도
        return try {
            val saved = saveCompatibility(connection, result.score, result.content)
            CompatibilityResponse.from(saved)
        } catch (e: DataIntegrityViolationException) {
            log.warn("중복 궁합 저장 시도 감지: connectionId={}", connectionId)
            val found = getTodayCompatibility(connection)
                ?: throw CompatibilityGenerationException("궁합 저장 중 오류가 발생했습니다.")
            CompatibilityResponse.from(found)
        }
    }

    @Transactional
    fun saveCompatibility(connection: UserConnection, score: Int, content: String): Compatibility {
        val existing = compatibilityRepository.findByConnectionAndDate(connection, LocalDate.now())
        if (existing != null) {
            return existing
        }

        val compatibility = Compatibility(
            connection = connection,
            score = score,
            content = content,
            date = LocalDate.now()
        )
        return compatibilityRepository.save(compatibility)
    }

    @Transactional(readOnly = true)
    fun getCompatibilityHistory(user: User, connectionId: Long, page: Int, size: Int): PageResponse<CompatibilityResponse> {
        val connection = userConnectionRepository.findByIdWithUsers(connectionId)
            .orElseThrow { ConnectionNotFoundException() }

        if (connection.user.id != user.id && connection.partner.id != user.id) {
            throw ForbiddenException("해당 연결에 접근할 권한이 없습니다.")
        }

        val pageable = PageRequest.of(page, size)
        val compatibilityPage = compatibilityRepository.findByConnectionOrderByDateDesc(connection, pageable)
        return PageResponse(
            content = compatibilityPage.content.map { CompatibilityResponse.from(it) },
            page = compatibilityPage.number,
            size = compatibilityPage.size,
            totalElements = compatibilityPage.totalElements,
            totalPages = compatibilityPage.totalPages
        )
    }

    private fun callAiWithRetry(connection: UserConnection): AiCompatibilityResult {
        val userPrompt = buildUserPrompt(connection)
        var lastException: Exception? = null

        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                if (attempt > 0) {
                    Thread.sleep(1000L * attempt)
                }

                val response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content()

                if (!response.isNullOrBlank()) {
                    return parseResponse(sanitizeResponse(response))
                }
                log.warn("AI 응답이 비어있습니다. 재시도 {}/{}", attempt + 1, MAX_RETRY_COUNT)
            } catch (e: CompatibilityGenerationException) {
                lastException = e
                log.warn("AI 응답 파싱 실패 (재시도 {}/{}): {}", attempt + 1, MAX_RETRY_COUNT, e.message)
            } catch (e: Exception) {
                lastException = e
                log.warn("AI 호출 실패 (재시도 {}/{}): {}", attempt + 1, MAX_RETRY_COUNT, e.message)
            }
        }

        throw CompatibilityGenerationException(
            "AI 궁합 생성에 실패했습니다: ${lastException?.message ?: "빈 응답"}"
        )
    }

    private fun buildUserPrompt(connection: UserConnection): String {
        val user1 = connection.user
        val user2 = connection.partner
        val relationText = when (connection.relationType) {
            RelationType.LOVER -> "연인"
            RelationType.FRIEND -> "친구"
            RelationType.FAMILY -> "가족"
            RelationType.COLLEAGUE -> "동료"
        }

        val parts = mutableListOf<String>()
        parts.add("=== 첫 번째 사람 ===")
        parts.addAll(buildUserProfile(user1))
        parts.add("")
        parts.add("=== 두 번째 사람 ===")
        parts.addAll(buildUserProfile(user2))
        parts.add("")
        parts.add("관계: $relationText")
        parts.add("오늘: ${LocalDate.now()}")

        return parts.joinToString("\n")
    }

    private fun buildUserProfile(user: User): List<String> {
        return UserProfileBuilder.buildProfileParts(user)
    }

    private fun sanitizeResponse(response: String): String {
        val trimmed = response.trim()
        val jsonStart = trimmed.indexOf('{')
        val jsonEnd = trimmed.lastIndexOf('}')
        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            return trimmed
        }
        return trimmed.substring(jsonStart, jsonEnd + 1)
    }

    private fun parseResponse(response: String): AiCompatibilityResult {
        return try {
            val json: Map<String, Any> = objectMapper.readValue(response)

            val score = when (val raw = json["score"]) {
                is Number -> raw.toInt().coerceIn(0, 100)
                else -> throw CompatibilityGenerationException("AI 응답에서 score를 파싱할 수 없습니다.")
            }

            val content = (json["content"] as? String)?.take(CONTENT_MAX_LENGTH)
                ?: throw CompatibilityGenerationException("AI 응답에서 content를 파싱할 수 없습니다.")

            AiCompatibilityResult(score = score, content = content)
        } catch (e: CompatibilityGenerationException) {
            throw e
        } catch (e: Exception) {
            log.error("AI 응답 파싱 실패. 원본 응답: {}", response, e)
            throw CompatibilityGenerationException("AI 응답 파싱에 실패했습니다: ${e.message}")
        }
    }

    private data class AiCompatibilityResult(
        val score: Int,
        val content: String
    )
}
