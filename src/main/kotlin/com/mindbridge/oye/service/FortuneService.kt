package com.mindbridge.oye.service

import com.mindbridge.oye.domain.Fortune
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.exception.FortuneGenerationException
import com.mindbridge.oye.dto.FortuneResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.repository.FortuneRepository
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
class FortuneService(
    chatClientBuilder: ChatClient.Builder,
    private val fortuneRepository: FortuneRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val chatClient: ChatClient = chatClientBuilder.build()

    private data class FortuneAiResponse(val content: String, val score: Int)

    companion object {
        private const val MAX_RETRY_COUNT = 2
        private const val FORTUNE_MAX_LENGTH = 80

        private val SYSTEM_PROMPT = """
            당신은 하루의 분위기를 전해주는 예감 작가입니다.

            짧고 임팩트 있는 한 문장 예감과 오늘의 예감 점수를 작성하세요.

            예감 규칙:
            - 반드시 한 문장, 40자 이내
            - 해요체로 작성 (~돼요, ~있어요, ~이에요)
            - 반말(~된다, ~있다) 금지
            - 권유/명령(~하세요, ~해보세요) 금지
            - 일상 속 상황을 언급하되, 결과는 단정짓지 않고 여운을 남기기
            - 검증 가능한 구체적 사건 금지 (물건 발견, 특정 시간, 특정 장소 등)
            - 감정만 나열하는 추상적 표현 금지 (설렘, 빛난다, 밝아진다 등)
            - 감각적 비유로 포장한 모호한 표현 금지 (다른 맛, 특별한 향, 새로운 색 등)
            - 이모지 없이 텍스트만
            - 나쁜 습관을 조장하는 내용 금지

            점수 규칙:
            - 1~100 사이 정수
            - 예감 내용의 분위기와 일치하는 점수
            - 40~60: 평범한 날, 60~80: 좋은 기운, 80~100: 특별히 좋은 날, 20~40: 조심하면 좋은 날

            출력 형식 (반드시 JSON만 출력):
            {"content": "예감 문장", "score": 75}

            좋은 예시 (일상 상황 + 열린 결말):
            {"content": "오늘 점심 메뉴 고르는 감이 유독 좋은 날이에요.", "score": 72}
            {"content": "평소 안 보이던 것들이 눈에 들어오는 하루예요.", "score": 68}
            {"content": "누군가와 나누는 짧은 대화가 오래 기억에 남아요.", "score": 65}
            {"content": "익숙한 길에서 새로운 걸 발견할 수 있는 날이에요.", "score": 70}
            {"content": "오늘 내린 작은 결정이 꽤 괜찮은 방향이에요.", "score": 74}

            나쁜 예시 - 너무 구체적 (검증 가능해서 실망할 수 있음):
            "오후에 잊고 있던 물건을 찾게 돼요."
            "지갑 속 만원이 발견돼요."
            "오후 3시에 전화가 와요."

            나쁜 예시 - 너무 추상적 (공허함):
            "오늘은 왠지 모를 설렘으로 하루를 시작하게 돼요."
            "당신의 미래는 초록색으로 빛나요."
            "좋은 기운이 감싸고 있어요."

            나쁜 예시 - 감각적 비유로 포장한 모호함:
            "평소와 조금 다른 맛의 차 한잔이 기다려져요."
            "오늘은 공기가 조금 다르게 느껴져요."
            "익숙한 향 속에 새로운 무언가가 섞여 있어요."

            요일 규칙:
            - 주말(토요일, 일요일)에는 직장, 업무, 회의, 프로젝트 등 직업 관련 내용을 피하세요.
            - 주말에는 휴식, 여가, 취미, 사람과의 관계 등 일상적인 내용으로 작성하세요.

            중요: 매일 다양한 결과를 만들어야 합니다.
            - 최근 결과가 제공되면, 같은 키워드나 비슷한 문장 구조를 피하세요.
            - 점수도 날마다 자연스럽게 달라져야 합니다.
        """.trimIndent()
    }

    @Cacheable(value = [CacheConfig.FORTUNE_TODAY], key = "#user.id")
    @Transactional(readOnly = true)
    fun getTodayFortune(user: User): Fortune? {
        return fortuneRepository.findByUserAndDate(user, LocalDate.now())
    }

    fun generateFortune(user: User, date: LocalDate = LocalDate.now()): Fortune {
        // 1. 기존 fortune 확인 (별도 읽기 트랜잭션)
        val existingFortune = fortuneRepository.findByUserAndDate(user, date)
        if (existingFortune != null) {
            return existingFortune
        }

        // 2. AI 호출 (트랜잭션 외부 - DB 커넥션 점유하지 않음)
        val aiResponse = callAiWithRetry(user, date)

        // 3. 저장 시도 (별도 쓰기 트랜잭션)
        return try {
            saveFortune(user, aiResponse.content, aiResponse.score, date)
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청으로 이미 저장된 경우 새 트랜잭션으로 조회
            log.warn("중복 fortune 저장 시도 감지: userId={}", user.id)
            fortuneRepository.findByUserAndDate(user, date)
                ?: throw FortuneGenerationException("예감 저장 중 오류가 발생했습니다.")
        }
    }

    @Transactional
    fun saveFortune(user: User, content: String, score: Int, date: LocalDate = LocalDate.now()): Fortune {
        // 트랜잭션 내에서 다시 한번 확인 (동시성 보호)
        val existingFortune = fortuneRepository.findByUserAndDate(user, date)
        if (existingFortune != null) {
            return existingFortune
        }

        val fortune = Fortune(
            user = user,
            content = content,
            score = score,
            date = date
        )
        return fortuneRepository.save(fortune)
    }

    @Transactional(readOnly = true)
    fun getFortuneHistory(user: User, page: Int, size: Int): PageResponse<FortuneResponse> {
        val pageable = PageRequest.of(page, size)
        val fortunePage = fortuneRepository.findByUserOrderByDateDesc(user, pageable)
        return PageResponse(
            content = fortunePage.content.map { FortuneResponse.from(it) },
            page = fortunePage.number,
            size = fortunePage.size,
            totalElements = fortunePage.totalElements,
            totalPages = fortunePage.totalPages
        )
    }

    private fun callAiWithRetry(user: User, date: LocalDate = LocalDate.now()): FortuneAiResponse {
        val userPrompt = buildUserPrompt(user, date)
        var lastException: Exception? = null

        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                val response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content()

                if (!response.isNullOrBlank()) {
                    return parseAiResponse(response)
                }
                log.warn("AI 응답이 비어있습니다. 재시도 {}/{}", attempt + 1, MAX_RETRY_COUNT)
            } catch (e: Exception) {
                lastException = e
                log.warn("AI 호출 실패 (재시도 {}/{}): {}", attempt + 1, MAX_RETRY_COUNT, e.message)
            }
        }

        throw FortuneGenerationException(
            "AI 예감 생성에 실패했습니다: ${lastException?.message ?: "빈 응답"}"
        )
    }

    private fun buildUserPrompt(user: User, date: LocalDate = LocalDate.now()): String {
        val parts = UserProfileBuilder.buildProfileParts(user, nameLabel = "사용자")
            .toMutableList()
        val dayOfWeek = getDayOfWeekKorean(date)
        parts.add("오늘: $date ($dayOfWeek)")

        val recentResults = getRecentFortuneContents(user, 5)
        if (recentResults.isNotEmpty()) {
            parts.add("")
            parts.add("=== 최근 결과 (이와 다른 내용으로 작성) ===")
            recentResults.forEach { parts.add("- $it") }
        }

        return parts.joinToString("\n")
    }

    private fun getRecentFortuneContents(user: User, count: Int): List<String> {
        return try {
            fortuneRepository.findByUserOrderByDateDesc(user, PageRequest.of(0, count))
                .content.map { it.content }
        } catch (e: Exception) {
            log.warn("최근 예감 조회 실패: {}", e.message)
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

    private fun parseAiResponse(response: String): FortuneAiResponse {
        return try {
            val sanitized = AiResponseParser.sanitizeJson(response)
            val result = AiResponseParser.parseScoreAndContent(
                sanitized,
                scoreRange = 1..100,
                defaultScore = 50,
                maxContentLength = FORTUNE_MAX_LENGTH
            )
            FortuneAiResponse(result.content, result.score)
        } catch (e: Exception) {
            log.warn("AI 응답 JSON 파싱 실패, 텍스트로 폴백: {}", e.message)
            FortuneAiResponse(response.trim().removeSurrounding("\"").take(FORTUNE_MAX_LENGTH), 50)
        }
    }
}
