package com.mindbridge.oye.service

import com.mindbridge.oye.domain.Fortune
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

        val prompt = """
            당신은 현실적인 조언을 주는 운세 작가입니다.

            사용자: ${user.name} (${user.birthDate}생)
            오늘: ${LocalDate.now()}

            짧고 임팩트 있는 한 문장 운세를 작성하세요.

            규칙:
            - 반드시 한 문장, 40자 이내
            - 추상적 표현 금지 (빛난다, 밝아진다, 색깔 비유 등)
            - 구체적인 상황이나 행동 제시
            - 일상에서 실제로 일어날 법한 내용
            - 이모지 없이 텍스트만
            - 따옴표나 부가 설명 없이 문장만 출력
            - 나쁜 습관을 조장하는 내용 금지 (늦잠, 게으름, 무단결석 등)

            좋은 예시:
            "점심 메뉴 고민하지 마라, 첫 번째가 정답이다."
            "오늘 엘리베이터에서 좋은 소식을 듣게 된다."
            "퇴근길 편의점에서 작은 행운이 기다린다."
            "미뤄둔 연락, 오늘 하면 좋은 일이 생긴다."
            "오후 3시에 들어오는 전화는 꼭 받아라."

            나쁜 예시 (이런 건 쓰지 마세요):
            "당신의 미래는 초록색으로 빛나요."
            "행운의 기운이 당신을 감싸고 있어요."
            "오늘 하루가 반짝반짝 빛날 거예요."
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
