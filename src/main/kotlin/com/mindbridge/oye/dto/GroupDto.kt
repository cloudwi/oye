package com.mindbridge.oye.dto

import com.mindbridge.oye.domain.Group
import com.mindbridge.oye.domain.GroupCompatibility
import com.mindbridge.oye.domain.GroupMember
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "그룹 생성 요청")
data class CreateGroupRequest(
    @Schema(description = "그룹 이름", example = "우리 친구들")
    val name: String,

    @Schema(description = "관계 유형 (FRIEND, FAMILY, COLLEAGUE)")
    val relationType: RelationType
)

@Schema(description = "그룹 참여 요청")
data class JoinGroupRequest(
    @Schema(description = "6자리 초대 코드", example = "A1B2C3")
    val code: String
)

@Schema(description = "그룹 수정 요청")
data class UpdateGroupRequest(
    @Schema(description = "그룹 이름", example = "우리 친구들")
    val name: String
)

@Schema(description = "그룹 요약 응답")
data class GroupSummaryResponse(
    @Schema(description = "그룹 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "그룹 이름", example = "우리 친구들")
    val name: String,

    @Schema(description = "관계 유형")
    val relationType: RelationType,

    @Schema(description = "멤버 수", example = "4")
    val memberCount: Long,

    @Schema(description = "내가 방장인지 여부", example = "true")
    val isOwner: Boolean,

    @Schema(description = "그룹 생성일시", example = "2025-06-15T08:00:00")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(group: Group, memberCount: Long, currentUser: User): GroupSummaryResponse {
            return GroupSummaryResponse(
                id = group.id!!,
                name = group.name,
                relationType = group.relationType,
                memberCount = memberCount,
                isOwner = group.owner.id == currentUser.id,
                createdAt = group.createdAt
            )
        }
    }
}

@Schema(description = "그룹 상세 응답")
data class GroupDetailResponse(
    @Schema(description = "그룹 고유 ID", example = "1")
    val id: Long,

    @Schema(description = "그룹 이름", example = "우리 친구들")
    val name: String,

    @Schema(description = "관계 유형")
    val relationType: RelationType,

    @Schema(description = "초대 코드", example = "A1B2C3")
    val inviteCode: String,

    @Schema(description = "방장 사용자 ID", example = "1")
    val ownerId: Long,

    @Schema(description = "방장 이름", example = "홍길동", nullable = true)
    val ownerName: String?,

    @Schema(description = "멤버 목록")
    val members: List<GroupMemberResponse>,

    @Schema(description = "그룹 생성일시", example = "2025-06-15T08:00:00")
    val createdAt: LocalDateTime
)

@Schema(description = "그룹 멤버 응답")
data class GroupMemberResponse(
    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,

    @Schema(description = "사용자 이름", example = "홍길동", nullable = true)
    val name: String?,

    @Schema(description = "방장 여부", example = "true")
    val isOwner: Boolean,

    @Schema(description = "가입일시", example = "2025-06-15T08:00:00")
    val joinedAt: LocalDateTime
) {
    companion object {
        fun from(member: GroupMember, ownerId: Long): GroupMemberResponse {
            return GroupMemberResponse(
                userId = member.user.id!!,
                name = member.user.name,
                isOwner = member.user.id == ownerId,
                joinedAt = member.joinedAt
            )
        }
    }
}

@Schema(description = "그룹 궁합 응답")
data class GroupCompatibilityResponse(
    @Schema(description = "첫 번째 사용자 ID", example = "1")
    val userAId: Long,

    @Schema(description = "첫 번째 사용자 이름", example = "홍길동", nullable = true)
    val userAName: String?,

    @Schema(description = "두 번째 사용자 ID", example = "2")
    val userBId: Long,

    @Schema(description = "두 번째 사용자 이름", example = "김영희", nullable = true)
    val userBName: String?,

    @Schema(description = "궁합 점수 (0-100)", example = "85")
    val score: Int,

    @Schema(description = "AI가 생성한 궁합 본문", example = "오늘 두 분의 궁합은...")
    val content: String,

    @Schema(description = "궁합 대상 날짜", example = "2025-06-15")
    val date: LocalDate
) {
    companion object {
        fun from(gc: GroupCompatibility): GroupCompatibilityResponse {
            return GroupCompatibilityResponse(
                userAId = gc.userA.id!!,
                userAName = gc.userA.name,
                userBId = gc.userB.id!!,
                userBName = gc.userB.name,
                score = gc.score,
                content = gc.content,
                date = gc.date
            )
        }
    }
}

@Schema(description = "그룹 오늘의 궁합 응답")
data class GroupTodayCompatibilityResponse(
    @Schema(description = "그룹 ID", example = "1")
    val groupId: Long,

    @Schema(description = "날짜", example = "2025-06-15")
    val date: LocalDate,

    @Schema(description = "멤버 ID-이름 매핑")
    val members: Map<Long, String?>,

    @Schema(description = "멤버 간 궁합 목록")
    val compatibilities: List<GroupCompatibilityResponse>
)
