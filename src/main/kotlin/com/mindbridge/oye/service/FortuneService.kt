package com.mindbridge.oye.service

import com.mindbridge.oye.domain.CalendarType
import com.mindbridge.oye.domain.Fortune
import com.mindbridge.oye.domain.Gender
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.exception.FortuneGenerationException
import com.mindbridge.oye.repository.FortuneRepository
import org.springframework.ai.chat.client.ChatClient
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
            당신은 하루의 분위기를 전해주는 운세 작가입니다.

            사용자: ${user.name} (${genderText}, ${user.birthDate}생, ${calendarText})
            오늘: ${LocalDate.now()}

            짧고 임팩트 있는 한 문장 운세를 작성하세요.

            규칙:
            - 반드시 한 문장, 40자 이내
            - 해요체로 작성 (~돼요, ~있어요, ~이에요)
            - 반말(~된다, ~있다) 금지
            - 권유/명령(~하세요, ~해보세요) 금지
            - 하루의 흐름이나 분위기를 전달
            - 검증 가능한 구체적 사건 예언 금지 (물건 발견, 특정 시간 전화 등)
            - 추상적 표현 금지 (빛난다, 밝아진다, 색깔 비유 등)
            - 이모지 없이 텍스트만
            - 따옴표나 부가 설명 없이 문장만 출력
            - 나쁜 습관을 조장하는 내용 금지

            좋은 예시:
            "오늘은 뭘 해도 손이 잘 풀리는 날이에요."
            "사소한 선택이 의외로 좋은 결과로 이어져요."
            "오늘 하루는 생각보다 빠르게 지나가요."
            "평소와 같은 하루지만 기분이 좀 더 가벼워요."
            "작은 변화가 하루를 특별하게 만들어줘요."

            나쁜 예시 (이런 건 쓰지 마세요):
            "오후에 잊고 있던 물건을 찾게 돼요."
            "지갑 속 만원이 발견돼요."
            "오후 3시에 전화가 와요."
            "커피 주문 시, 점원에게 칭찬을 건네보세요."
            "당신의 미래는 초록색으로 빛나요."
        """.trimIndent()

        val response = try {
            chatClient.prompt(prompt).call().content()
        } catch (e: Exception) {
            throw FortuneGenerationException("AI 운세 생성 중 오류가 발생했습니다: ${e.message}")
        }

        if (response.isNullOrBlank()) {
            throw FortuneGenerationException("운세 내용이 비어있습니다.")
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
}
