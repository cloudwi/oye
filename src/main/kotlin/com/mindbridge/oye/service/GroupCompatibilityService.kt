package com.mindbridge.oye.service

import com.mindbridge.oye.domain.Group
import com.mindbridge.oye.domain.GroupCompatibility
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.exception.CompatibilityGenerationException
import com.mindbridge.oye.repository.GroupCompatibilityRepository
import com.mindbridge.oye.repository.GroupMemberRepository
import com.mindbridge.oye.util.AiResponseParser
import com.mindbridge.oye.util.DateUtils
import com.mindbridge.oye.util.UserProfileBuilder
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GroupCompatibilityService(
    private val aiChatService: AiChatService,
    private val groupCompatibilityRepository: GroupCompatibilityRepository,
    private val groupMemberRepository: GroupMemberRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
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
            - 친구: 같이 놀거나 대화하며 느끼는 유쾌함, 서로에게 솔직할 수 있는 편안함에 초점을 맞추세요.
            - 가족: 밥상이나 거실 등 일상 공간에서의 대화, 서로 챙기는 작은 행동에 초점을 맞추세요.
            - 동료: 회의, 프로젝트, 점심 등 업무 환경에서의 호흡, 서로의 의견이 잘 맞는 순간에 초점을 맞추세요.

            점수 규칙:
            - 0~100 사이 정수
            - 40~60: 평범, 60~80: 좋은 기운, 80~100: 아주 좋은 날, 20~40: 조심하면 좋은 날

            출력 형식 (반드시 JSON만 출력):
            {"score": 85, "content": "궁합 문장"}

            요일 규칙:
            - 주말(토요일, 일요일)에는 직장, 업무, 회의, 프로젝트 등 직업 관련 내용을 피하세요.
            - 주말에는 휴식, 여가, 취미, 사람과의 관계 등 일상적인 내용으로 작성하세요.

            중요: 매일 다양한 결과를 만들어야 합니다.
            - 최근 결과가 제공되면, 같은 키워드나 비슷한 문장 구조를 피하세요.
            - 점수도 날마다 자연스럽게 달라져야 합니다.
        """.trimIndent()
    }

    fun generateGroupCompatibilities(group: Group, date: LocalDate = LocalDate.now()) {
        val members = groupMemberRepository.findByGroupWithUsers(group).map { it.user }
        if (members.size < 2) return

        for (i in members.indices) {
            for (j in i + 1 until members.size) {
                val (userA, userB) = orderUsers(members[i], members[j])
                try {
                    generatePairCompatibility(group, userA, userB, date)
                } catch (e: Exception) {
                    log.warn("그룹 궁합 생성 실패: groupId={}, userA={}, userB={}, error={}",
                        group.id, userA.id, userB.id, e.message)
                }
            }
        }
    }

    fun generateNewMemberCompatibilities(group: Group, newMember: User, date: LocalDate = LocalDate.now()) {
        val members = groupMemberRepository.findByGroupWithUsers(group).map { it.user }

        for (existingMember in members) {
            if (existingMember.id == newMember.id) continue
            val (userA, userB) = orderUsers(existingMember, newMember)
            try {
                generatePairCompatibility(group, userA, userB, date)
            } catch (e: Exception) {
                log.warn("신규 멤버 궁합 생성 실패: groupId={}, userA={}, userB={}, error={}",
                    group.id, userA.id, userB.id, e.message)
            }
        }
    }

    @Transactional
    fun generatePairCompatibility(group: Group, userA: User, userB: User, date: LocalDate = LocalDate.now()): GroupCompatibility {
        val (orderedA, orderedB) = orderUsers(userA, userB)

        val existing = groupCompatibilityRepository.findByGroupAndDateAndUsers(group, date, orderedA, orderedB)
        if (existing != null) return existing

        val result = callAiWithRetry(orderedA, orderedB, group.relationType, date)

        return try {
            val compatibility = GroupCompatibility(
                group = group,
                userA = orderedA,
                userB = orderedB,
                score = result.score,
                content = result.content,
                date = date
            )
            groupCompatibilityRepository.save(compatibility)
        } catch (e: DataIntegrityViolationException) {
            groupCompatibilityRepository.findByGroupAndDateAndUsers(group, date, orderedA, orderedB)
                ?: throw CompatibilityGenerationException("그룹 궁합 저장 중 오류가 발생했습니다.")
        }
    }

    private fun callAiWithRetry(userA: User, userB: User, relationType: RelationType, date: LocalDate): AiResult {
        val userPrompt = buildUserPrompt(userA, userB, relationType, date)
        return try {
            aiChatService.callWithRetry(
                systemPrompt = SYSTEM_PROMPT,
                userPrompt = userPrompt,
                errorMessage = "AI 그룹 궁합 생성에 실패했습니다"
            ) { response -> parseResponse(response) }
        } catch (e: CompatibilityGenerationException) {
            throw e
        } catch (e: Exception) {
            throw CompatibilityGenerationException(e.message ?: "AI 그룹 궁합 생성에 실패했습니다")
        }
    }

    private fun buildUserPrompt(userA: User, userB: User, relationType: RelationType, date: LocalDate): String {
        val relationText = when (relationType) {
            RelationType.FRIEND -> "친구"
            RelationType.FAMILY -> "가족"
            RelationType.COLLEAGUE -> "동료"
            RelationType.LOVER -> "친구"
        }

        val parts = mutableListOf<String>()
        parts.add("=== 첫 번째 사람 ===")
        parts.addAll(UserProfileBuilder.buildProfileParts(userA))
        parts.add("")
        parts.add("=== 두 번째 사람 ===")
        parts.addAll(UserProfileBuilder.buildProfileParts(userB))
        parts.add("")
        parts.add("관계: $relationText")
        val dayOfWeek = DateUtils.getDayOfWeekKorean(date)
        parts.add("오늘: $date ($dayOfWeek)")

        return parts.joinToString("\n")
    }

    private fun parseResponse(response: String): AiResult {
        return try {
            val sanitized = AiResponseParser.sanitizeJson(response)
            val result = AiResponseParser.parseScoreAndContent(
                sanitized,
                scoreRange = 0..100,
                maxContentLength = CONTENT_MAX_LENGTH
            )
            AiResult(score = result.score, content = result.content)
        } catch (e: Exception) {
            log.error("AI 응답 파싱 실패. 원본 응답: {}", response, e)
            throw CompatibilityGenerationException("AI 응답 파싱에 실패했습니다: ${e.message}")
        }
    }

    private fun orderUsers(user1: User, user2: User): Pair<User, User> {
        return if (user1.id!! < user2.id!!) Pair(user1, user2) else Pair(user2, user1)
    }

    private data class AiResult(
        val score: Int,
        val content: String
    )
}
