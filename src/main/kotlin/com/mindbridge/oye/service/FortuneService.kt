package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Fortune
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.exception.FortuneGenerationException
import com.mindbridge.oye.dto.FortuneResponse
import com.mindbridge.oye.dto.PageResponse
import com.mindbridge.oye.repository.FortuneRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class FortuneService(
    chatClientBuilder: ChatClient.Builder,
    private val fortuneRepository: FortuneRepository
) {
    private val chatClient: ChatClient = chatClientBuilder.build()

    @Transactional(readOnly = true)
    fun getTodayFortune(user: User): Fortune? {
        return fortuneRepository.findByUserAndDate(user, LocalDate.now())
    }

    @Transactional
    fun generateFortune(user: User): Fortune {
        val existingFortune = getTodayFortune(user)
        if (existingFortune != null) {
            return existingFortune
        }

        val genderText = when (user.gender) {
            Gender.MALE -> "남성"
            Gender.FEMALE -> "여성"
            null -> "미지정"
        }
        val calendarText = when (user.calendarType) {
            CalendarType.SOLAR -> "양력"
            CalendarType.LUNAR -> "음력"
            null -> "양력"
        }

        val prompt = """
            당신은 하루의 분위기를 전해주는 예감 작가입니다.

            사용자: ${user.name} (${genderText}, ${user.birthDate}생, ${calendarText})
            오늘: ${LocalDate.now()}

            짧고 임팩트 있는 한 문장 예감을 작성하세요.

            규칙:
            - 반드시 한 문장, 40자 이내
            - 해요체로 작성 (~돼요, ~있어요, ~이에요)
            - 반말(~된다, ~있다) 금지
            - 권유/명령(~하세요, ~해보세요) 금지
            - 일상 속 상황을 언급하되, 결과는 단정짓지 않고 여운을 남기기
            - 검증 가능한 구체적 사건 금지 (물건 발견, 특정 시간, 특정 장소 등)
            - 감정만 나열하는 추상적 표현 금지 (설렘, 빛난다, 밝아진다 등)
            - 감각적 비유로 포장한 모호한 표현 금지 (다른 맛, 특별한 향, 새로운 색 등)
            - 이모지 없이 텍스트만
            - 따옴표나 부가 설명 없이 문장만 출력
            - 나쁜 습관을 조장하는 내용 금지

            좋은 예시 (일상 상황 + 열린 결말):
            "오늘 점심 메뉴 고르는 감이 유독 좋은 날이에요."
            "평소 안 보이던 것들이 눈에 들어오는 하루예요."
            "누군가와 나누는 짧은 대화가 오래 기억에 남아요."
            "익숙한 길에서 새로운 걸 발견할 수 있는 날이에요."
            "오늘 내린 작은 결정이 꽤 괜찮은 방향이에요."

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
        """.trimIndent()

        val response = try {
            chatClient.prompt(prompt).call().content()
        } catch (e: Exception) {
            throw FortuneGenerationException("AI 예감 생성 중 오류가 발생했습니다: ${e.message}")
        }

        if (response.isNullOrBlank()) {
            throw FortuneGenerationException("예감 내용이 비어있습니다.")
        }

        val fortune = Fortune(
            user = user,
            content = response,
            date = LocalDate.now()
        )
        return fortuneRepository.save(fortune)
    }

    @Transactional(readOnly = true)
    fun getFortuneHistory(user: User): List<Fortune> {
        return fortuneRepository.findByUserOrderByDateDesc(user)
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
}
