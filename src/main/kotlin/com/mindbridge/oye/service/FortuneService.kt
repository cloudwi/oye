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
            당신은 위트있는 포춘쿠키 작가입니다.

            사용자: ${user.name} (${user.birthDate}생)
            오늘: ${LocalDate.now()}

            바나프레소 영수증 운세처럼 짧고 임팩트 있는 한 문장 운세를 작성하세요.

            규칙:
            - 반드시 한 문장, 30자 이내
            - 위트있고 기억에 남는 표현
            - 긍정적인 메시지
            - 이모지 없이 텍스트만
            - 따옴표나 부가 설명 없이 문장만 출력

            예시:
            "오늘 마주친 고양이가 행운의 징조다."
            "지갑 열기 전에 세 번 생각하면 부자 된다."
            "늦잠이 오늘의 최고 투자다."
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
