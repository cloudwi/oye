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
import com.mindbridge.oye.util.AiResponseParser
import com.mindbridge.oye.util.UserProfileBuilder
import org.slf4j.LoggerFactory
import com.mindbridge.oye.config.CacheConfig
import org.springframework.ai.chat.client.ChatClient
import org.springframework.cache.annotation.Cacheable
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

    companion object {
        private const val MAX_RETRY_COUNT = 2
        private const val CONTENT_MAX_LENGTH = 80

        private val SYSTEM_PROMPT = """
            당신은 두 사람의 오늘 궁합을 한 문장으로 전하는 전문가입니다.

            두 사람의 프로필과 관계 유형을 기반으로 오늘의 궁합을 분석하세요.

            궁합 규칙:
            - 반드시 한 문장, 40자 이내
            - 해요체로 작성 (~돼요, ~있어요, ~이에요)
            - 반말(~된다, ~있다) 금지
            - 권유/명령(~하세요, ~해보세요) 금지
            - 두 사람 사이에서 오늘 일어날 수 있는 구체적인 상황 한 가지
            - 관계 유형(연인/친구/가족/동료)에 맞는 톤
            - 이모지 없이 텍스트만
            - 추상적 표현 금지 (빛난다, 설렘, 특별한 기운 등)
            - 검증 가능한 구체적 사건 금지 (선물 받음, 전화 옴 등)

            점수 규칙:
            - 0~100 사이 정수
            - 40~60: 평범, 60~80: 좋은 기운, 80~100: 아주 좋은 날, 20~40: 조심하면 좋은 날

            출력 형식 (반드시 JSON만 출력):
            {"score": 85, "content": "궁합 문장"}

            좋은 예시:
            {"score": 78, "content": "오늘은 같이 밥 먹으면 대화가 유독 잘 통하는 날이에요."}
            {"score": 65, "content": "사소한 취향 차이가 오히려 재미있게 느껴지는 하루예요."}
            {"score": 82, "content": "서로 눈치 안 보고 편하게 있을 수 있는 날이에요."}
            {"score": 55, "content": "오늘은 각자 시간을 보낸 뒤 만나면 더 반가워요."}

            나쁜 예시 - 너무 추상적:
            "두 분의 에너지가 조화롭게 어우러져요."
            "오늘은 서로에게 특별한 의미가 있는 날이에요."

            나쁜 예시 - 너무 구체적:
            "저녁에 깜짝 선물을 받게 돼요."
            "오후 3시에 연락이 와요."

            중요: 매일 다양한 결과를 만들어야 합니다.
            - 최근 결과가 제공되면, 같은 키워드나 비슷한 문장 구조를 피하세요.
            - 점수도 날마다 자연스럽게 달라져야 합니다.
        """.trimIndent()
    }

    @Cacheable(value = [CacheConfig.COMPATIBILITY_TODAY], key = "#connection.id")
    @Transactional(readOnly = true)
    fun getTodayCompatibility(connection: UserConnection): Compatibility? {
        return compatibilityRepository.findByConnectionAndDate(connection, LocalDate.now())
    }

    fun generateCompatibility(connection: UserConnection, date: LocalDate = LocalDate.now()): Compatibility {
        val existing = compatibilityRepository.findByConnectionAndDate(connection, date)
        if (existing != null) return existing

        val result = callAiWithRetry(connection, date)
        return try {
            saveCompatibility(connection, result.score, result.content, date)
        } catch (e: DataIntegrityViolationException) {
            compatibilityRepository.findByConnectionAndDate(connection, date)
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
    fun saveCompatibility(connection: UserConnection, score: Int, content: String, date: LocalDate = LocalDate.now()): Compatibility {
        val existing = compatibilityRepository.findByConnectionAndDate(connection, date)
        if (existing != null) {
            return existing
        }

        val compatibility = Compatibility(
            connection = connection,
            score = score,
            content = content,
            date = date
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

    private fun callAiWithRetry(connection: UserConnection, date: LocalDate = LocalDate.now()): AiCompatibilityResult {
        val userPrompt = buildUserPrompt(connection, date)
        var lastException: Exception? = null

        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                val response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content()

                if (!response.isNullOrBlank()) {
                    return parseResponse(response)
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

    private fun buildUserPrompt(connection: UserConnection, date: LocalDate = LocalDate.now()): String {
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
        parts.add("오늘: $date")

        val recentResults = getRecentCompatibilityContents(connection, 5)
        if (recentResults.isNotEmpty()) {
            parts.add("")
            parts.add("=== 최근 결과 (이와 다른 내용으로 작성) ===")
            recentResults.forEach { parts.add("- $it") }
        }

        return parts.joinToString("\n")
    }

    private fun getRecentCompatibilityContents(connection: UserConnection, count: Int): List<String> {
        return try {
            compatibilityRepository.findByConnectionOrderByDateDesc(connection, PageRequest.of(0, count))
                .content.map { it.content }
        } catch (e: Exception) {
            log.warn("최근 궁합 조회 실패: {}", e.message)
            emptyList()
        }
    }

    private fun buildUserProfile(user: User): List<String> {
        return UserProfileBuilder.buildProfileParts(user)
    }

    private fun parseResponse(response: String): AiCompatibilityResult {
        return try {
            val sanitized = AiResponseParser.sanitizeJson(response)
            val result = AiResponseParser.parseScoreAndContent(
                sanitized,
                scoreRange = 0..100,
                maxContentLength = CONTENT_MAX_LENGTH
            )
            AiCompatibilityResult(score = result.score, content = result.content)
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
