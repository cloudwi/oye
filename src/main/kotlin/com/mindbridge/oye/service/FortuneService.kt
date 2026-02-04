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
            사용자 정보:
            - 이름: ${user.name}
            - 생년월일: ${user.birthDate}
            - 오늘 날짜: ${LocalDate.now()}

            위 정보를 바탕으로 오늘의 운세를 작성해주세요.
            긍정적이고 희망적인 메시지를 담아주세요.
            200자 내외로 작성해주세요.
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
