package com.mindbridge.oye.service

import com.mindbridge.oye.domain.Compatibility
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import com.mindbridge.oye.dto.CompatibilityResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.dto.RecordDatesResponse
import com.mindbridge.oye.dto.ScoreTrendPoint
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
import java.time.DayOfWeek
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
        private const val CONTENT_MAX_LENGTH = 200

        private val SYSTEM_PROMPT = """
            당신은 두 사람의 오늘 궁합을 분석하는 전문가입니다.

            두 사람의 프로필과 관계 유형을 기반으로 오늘의 궁합을 분석하세요.

            궁합 규칙:
            - 2~3문장, 80~150자 (최대 200자)
            - 첫 문장: 오늘 두 사람 사이에서 일어날 수 있는 구체적인 상황
            - 두 번째 문장: 그 상황이 두 사람에게 어떤 의미인지, 또는 어떻게 하면 더 좋은지
            - 해요체로 작성 (~돼요, ~있어요, ~이에요)
            - 반말(~된다, ~있다) 금지
            - 권유/명령(~하세요, ~해보세요) 금지
            - 이모지 없이 텍스트만
            - 추상적 표현 금지 (빛난다, 설렘, 특별한 기운 등)
            - 검증 가능한 구체적 사건 금지 (선물 받음, 전화 옴 등)
            - 두 사람의 프로필(MBTI, 혈액형, 관심사 등)을 자연스럽게 반영

            관계별 테마 (반드시 관계 유형에 맞는 테마로 작성):
            - 연인: 두 사람만의 감정 교류, 소소한 배려, 함께하는 일상 속 다정한 순간에 초점을 맞추세요.
            - 친구: 같이 놀거나 대화하며 느끼는 유쾌함, 서로에게 솔직할 수 있는 편안함에 초점을 맞추세요.
            - 가족: 밥상이나 거실 등 일상 공간에서의 대화, 서로 챙기는 작은 행동에 초점을 맞추세요.
            - 동료: 회의, 프로젝트, 점심 등 업무 환경에서의 호흡, 서로의 의견이 잘 맞는 순간에 초점을 맞추세요.

            점수 규칙:
            - 0~100 사이 정수
            - 40~60: 평범, 60~80: 좋은 기운, 80~100: 아주 좋은 날, 20~40: 조심하면 좋은 날

            출력 형식 (반드시 JSON만 출력):
            {"score": 85, "content": "궁합 문장"}

            좋은 예시 (연인):
            {"score": 78, "content": "오늘은 같이 밥 먹으면 대화가 유독 잘 통하는 날이에요. 평소 안 꺼내던 이야기도 자연스럽게 나올 수 있어서 서로를 더 알아가는 시간이 돼요."}
            {"score": 45, "content": "오늘은 서로 컨디션이 엇갈리기 쉬운 날이에요. 상대방의 반응이 평소와 다르더라도 너무 신경 쓰지 않는 게 서로에게 좋아요."}

            좋은 예시 (친구):
            {"score": 82, "content": "서로 눈치 안 보고 편하게 있을 수 있는 날이에요. 굳이 대화가 없어도 함께 있는 것만으로 충분히 편안한 시간이 돼요."}
            {"score": 65, "content": "사소한 취향 차이가 오히려 재미있게 느껴지는 하루예요. 각자 좋아하는 걸 공유하다 보면 의외의 공통점을 발견할 수 있어요."}

            좋은 예시 (가족):
            {"score": 75, "content": "오늘은 식사 자리에서 이런저런 이야기가 잘 오가는 날이에요. 사소한 안부도 서로에게 따뜻하게 닿아요."}
            {"score": 40, "content": "생활 습관 차이로 작은 불편함이 생기기 쉬운 날이에요. 각자 공간을 존중하면 하루가 훨씬 편안해져요."}

            좋은 예시 (동료):
            {"score": 80, "content": "오늘은 아이디어를 주고받으면 평소보다 속도가 빨라지는 날이에요. 서로의 관점이 딱 맞물려서 작업이 수월하게 진행돼요."}
            {"score": 50, "content": "업무 스타일이 살짝 엇갈릴 수 있는 하루예요. 진행 방향을 미리 맞춰두면 불필요한 수정을 줄일 수 있어요."}

            나쁜 예시 - 너무 추상적:
            "두 분의 에너지가 조화롭게 어우러져요."
            "오늘은 서로에게 특별한 의미가 있는 날이에요."

            나쁜 예시 - 너무 구체적:
            "저녁에 깜짝 선물을 받게 돼요."
            "오후 3시에 연락이 와요."

            요일 규칙:
            - 주말(토요일, 일요일)에는 직장, 업무, 회의, 프로젝트 등 직업 관련 내용을 피하세요.
            - 주말에는 휴식, 여가, 취미, 사람과의 관계 등 일상적인 내용으로 작성하세요.

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
    fun getScoreTrend(user: User, connectionId: Long, days: Int): List<ScoreTrendPoint> {
        val connection = userConnectionRepository.findByIdWithUsers(connectionId)
            .orElseThrow { ConnectionNotFoundException() }

        if (connection.user.id != user.id && connection.partner.id != user.id) {
            throw ForbiddenException("해당 연결에 접근할 권한이 없습니다.")
        }

        val end = LocalDate.now()
        val start = end.minusDays(days.toLong() - 1)
        return compatibilityRepository.findByConnectionAndDateBetweenOrderByDateAsc(connection, start, end)
            .map { ScoreTrendPoint(date = it.date, score = it.score) }
    }

    @Transactional(readOnly = true)
    fun getRecordDates(user: User, connectionId: Long, year: Int, month: Int): RecordDatesResponse {
        val connection = userConnectionRepository.findByIdWithUsers(connectionId)
            .orElseThrow { ConnectionNotFoundException() }

        if (connection.user.id != user.id && connection.partner.id != user.id) {
            throw ForbiddenException("해당 연결에 접근할 권한이 없습니다.")
        }

        val start = LocalDate.of(year, month, 1)
        val end = start.withDayOfMonth(start.lengthOfMonth())
        val dates = compatibilityRepository.findDatesByConnectionAndDateBetween(connection, start, end)
        return RecordDatesResponse(
            yearMonth = "%d-%02d".format(year, month),
            dates = dates
        )
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
        val dayOfWeek = getDayOfWeekKorean(date)
        parts.add("오늘: $date ($dayOfWeek)")

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

    private fun getDayOfWeekKorean(date: LocalDate): String {
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "월요일"
            DayOfWeek.TUESDAY -> "화요일"
            DayOfWeek.WEDNESDAY -> "수요일"
            DayOfWeek.THURSDAY -> "목요일"
            DayOfWeek.FRIDAY -> "금요일"
            DayOfWeek.SATURDAY -> "토요일"
            DayOfWeek.SUNDAY -> "일요일"
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
