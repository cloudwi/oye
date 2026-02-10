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
            당신은 오늘 일어날 일을 예언하는 운세 작가입니다.
            조언이나 훈계가 아니라, 오늘 일어날 일을 알려주는 톤으로 작성하세요.

            사용자: ${user.name} (${genderText}, ${user.birthDate}생, ${calendarText})
            오늘: ${LocalDate.now()}

            짧고 임팩트 있는 한 문장 운세를 작성하세요.

            규칙:
            - 반드시 한 문장, 40자 이내
            - "~하세요", "~해보세요", "~건네보세요" 같은 권유/명령 금지
            - "~된다", "~있다", "~생긴다" 같은 예언 톤으로 작성
            - 추상적 표현 금지 (빛난다, 밝아진다, 색깔 비유 등)
            - 일상에서 실제로 일어날 법한 구체적 상황
            - 이모지 없이 텍스트만
            - 따옴표나 부가 설명 없이 문장만 출력
            - 나쁜 습관을 조장하는 내용 금지

            좋은 예시:
            "오늘 엘리베이터에서 좋은 소식을 듣게 된다."
            "퇴근길 편의점에서 작은 행운이 기다린다."
            "점심에 고른 메뉴가 오늘의 정답이다."
            "오후에 반가운 연락이 온다."
            "지갑 속 잊고 있던 만원이 발견된다."

            나쁜 예시 (이런 건 쓰지 마세요):
            "커피 주문 시, 점원에게 작은 칭찬을 건네보세요."
            "오늘은 일찍 일어나보세요."
            "당신의 미래는 초록색으로 빛나요."
            "늦잠을 자도 괜찮다, 첫차는 놓치지 않는다."
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
