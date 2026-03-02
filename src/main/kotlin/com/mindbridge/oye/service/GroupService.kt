package com.mindbridge.oye.service

import com.mindbridge.oye.domain.Group
import com.mindbridge.oye.domain.GroupMember
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.dto.CreateGroupRequest
import com.mindbridge.oye.dto.GroupCompatibilityResponse
import com.mindbridge.oye.dto.GroupDetailResponse
import com.mindbridge.oye.dto.GroupMemberResponse
import com.mindbridge.oye.dto.GroupSummaryResponse
import com.mindbridge.oye.dto.GroupTodayCompatibilityResponse
import com.mindbridge.oye.dto.JoinGroupRequest
import com.mindbridge.oye.dto.UpdateGroupRequest
import com.mindbridge.oye.event.GroupMemberJoinedEvent
import com.mindbridge.oye.exception.AlreadyGroupMemberException
import com.mindbridge.oye.exception.CodeGenerationException
import com.mindbridge.oye.exception.GroupFullException
import com.mindbridge.oye.exception.GroupNotFoundException
import com.mindbridge.oye.exception.InvalidRelationTypeException
import com.mindbridge.oye.exception.NotGroupMemberException
import com.mindbridge.oye.exception.NotGroupOwnerException
import com.mindbridge.oye.repository.GroupCompatibilityRepository
import com.mindbridge.oye.repository.GroupMemberRepository
import com.mindbridge.oye.repository.GroupRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDate

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupCompatibilityRepository: GroupCompatibilityRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_GROUP_MEMBERS = 10
        private const val CODE_LENGTH = 6
        private const val CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private val secureRandom = SecureRandom()
    }

    @Transactional
    fun createGroup(user: User, request: CreateGroupRequest): GroupSummaryResponse {
        if (request.relationType == RelationType.LOVER) {
            throw InvalidRelationTypeException("그룹에서는 연인 관계를 지원하지 않습니다.")
        }

        val inviteCode = generateUniqueCode()
        val group = Group(
            name = request.name,
            relationType = request.relationType,
            owner = user,
            inviteCode = inviteCode
        )
        val savedGroup = groupRepository.save(group)

        val member = GroupMember(group = savedGroup, user = user)
        groupMemberRepository.save(member)

        log.info("그룹 생성: groupId={}, userId={}, name={}", savedGroup.id, user.id, request.name)
        return GroupSummaryResponse.from(savedGroup, 1L, user)
    }

    @Transactional
    fun joinGroup(user: User, request: JoinGroupRequest): GroupSummaryResponse {
        val group = groupRepository.findByInviteCode(request.code)
            ?: throw GroupNotFoundException("해당 초대 코드의 그룹을 찾을 수 없습니다.")

        if (groupMemberRepository.existsByGroupAndUser(group, user)) {
            throw AlreadyGroupMemberException()
        }

        val memberCount = groupMemberRepository.countByGroup(group)
        if (memberCount >= MAX_GROUP_MEMBERS) {
            throw GroupFullException()
        }

        val member = GroupMember(group = group, user = user)
        groupMemberRepository.save(member)

        log.info("그룹 참여: groupId={}, userId={}", group.id, user.id)
        eventPublisher.publishEvent(GroupMemberJoinedEvent(group = group, newMember = user))

        return GroupSummaryResponse.from(group, memberCount + 1, user)
    }

    @Transactional(readOnly = true)
    fun getMyGroups(user: User): List<GroupSummaryResponse> {
        val memberships = groupMemberRepository.findByUserWithGroup(user)
        if (memberships.isEmpty()) return emptyList()

        return memberships.map { membership ->
            val group = membership.group
            val memberCount = groupMemberRepository.countByGroup(group)
            GroupSummaryResponse.from(group, memberCount, user)
        }
    }

    @Transactional(readOnly = true)
    fun getGroupDetail(user: User, groupId: Long): GroupDetailResponse {
        val group = groupRepository.findByIdWithOwner(groupId)
            .orElseThrow { GroupNotFoundException() }

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
            throw NotGroupMemberException()
        }

        val members = groupMemberRepository.findByGroupWithUsers(group)
        val ownerId = group.owner.id!!

        return GroupDetailResponse(
            id = group.id!!,
            name = group.name,
            relationType = group.relationType,
            inviteCode = group.inviteCode,
            ownerId = ownerId,
            ownerName = group.owner.name,
            members = members.map { GroupMemberResponse.from(it, ownerId) },
            createdAt = group.createdAt
        )
    }

    @Transactional
    fun updateGroup(user: User, groupId: Long, request: UpdateGroupRequest): GroupDetailResponse {
        val group = groupRepository.findByIdWithOwner(groupId)
            .orElseThrow { GroupNotFoundException() }

        if (group.owner.id != user.id) {
            throw NotGroupOwnerException()
        }

        group.name = request.name
        groupRepository.save(group)

        log.info("그룹 이름 변경: groupId={}, newName={}", groupId, request.name)
        return getGroupDetail(user, groupId)
    }

    @Transactional
    fun kickMember(user: User, groupId: Long, targetUserId: Long) {
        val group = groupRepository.findByIdWithOwner(groupId)
            .orElseThrow { GroupNotFoundException() }

        if (group.owner.id != user.id) {
            throw NotGroupOwnerException()
        }

        if (targetUserId == user.id) {
            throw IllegalArgumentException("자기 자신은 추방할 수 없습니다.")
        }

        val members = groupMemberRepository.findByGroupWithUsers(group)
        val targetMember = members.find { it.user.id == targetUserId }
            ?: throw NotGroupMemberException("대상 사용자가 그룹 멤버가 아닙니다.")

        groupMemberRepository.deleteByGroupAndUser(group, targetMember.user)
        log.info("그룹 멤버 추방: groupId={}, targetUserId={}", groupId, targetUserId)
    }

    @Transactional
    fun leaveGroup(user: User, groupId: Long) {
        val group = groupRepository.findByIdWithOwner(groupId)
            .orElseThrow { GroupNotFoundException() }

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
            throw NotGroupMemberException()
        }

        val members = groupMemberRepository.findByGroupWithUsers(group)

        if (members.size == 1) {
            groupCompatibilityRepository.deleteAllByGroup(group)
            groupMemberRepository.deleteByGroupAndUser(group, user)
            groupRepository.delete(group)
            log.info("그룹 삭제 (마지막 멤버 탈퇴): groupId={}, userId={}", groupId, user.id)
            return
        }

        groupMemberRepository.deleteByGroupAndUser(group, user)

        if (group.owner.id == user.id) {
            val nextOwner = members
                .filter { it.user.id != user.id }
                .minByOrNull { it.joinedAt }!!
                .user
            group.owner = nextOwner
            groupRepository.save(group)
            log.info("그룹 방장 위임 후 탈퇴: groupId={}, oldOwner={}, newOwner={}", groupId, user.id, nextOwner.id)
        } else {
            log.info("그룹 탈퇴: groupId={}, userId={}", groupId, user.id)
        }
    }

    @Transactional
    fun deleteGroup(user: User, groupId: Long) {
        val group = groupRepository.findByIdWithOwner(groupId)
            .orElseThrow { GroupNotFoundException() }

        if (group.owner.id != user.id) {
            throw NotGroupOwnerException()
        }

        groupCompatibilityRepository.deleteAllByGroup(group)
        val members = groupMemberRepository.findByGroupWithUsers(group)
        groupMemberRepository.deleteAll(members)
        groupRepository.delete(group)
        log.info("그룹 삭제: groupId={}, userId={}", groupId, user.id)
    }

    @Transactional(readOnly = true)
    fun getGroupTodayCompatibility(user: User, groupId: Long): GroupTodayCompatibilityResponse {
        val group = groupRepository.findById(groupId)
            .orElseThrow { GroupNotFoundException() }

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
            throw NotGroupMemberException()
        }

        val members = groupMemberRepository.findByGroupWithUsers(group)
        val today = LocalDate.now()
        val compatibilities = groupCompatibilityRepository.findByGroupAndDate(group, today)

        val memberMap = members.associate { it.user.id!! to it.user.name }

        return GroupTodayCompatibilityResponse(
            groupId = group.id!!,
            date = today,
            members = memberMap,
            compatibilities = compatibilities.map { GroupCompatibilityResponse.from(it) }
        )
    }

    @Transactional(readOnly = true)
    fun getGroupPairCompatibility(user: User, groupId: Long, targetUserId: Long): GroupCompatibilityResponse {
        val group = groupRepository.findById(groupId)
            .orElseThrow { GroupNotFoundException() }

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
            throw NotGroupMemberException()
        }

        val today = LocalDate.now()
        val (userAId, userBId) = if (user.id!! < targetUserId) Pair(user.id!!, targetUserId) else Pair(targetUserId, user.id!!)

        val members = groupMemberRepository.findByGroupWithUsers(group)
        val userA = members.find { it.user.id == userAId }?.user
            ?: throw NotGroupMemberException("대상 사용자가 그룹 멤버가 아닙니다.")
        val userB = members.find { it.user.id == userBId }?.user
            ?: throw NotGroupMemberException("대상 사용자가 그룹 멤버가 아닙니다.")

        val compatibility = groupCompatibilityRepository.findByGroupAndDateAndUsers(group, today, userA, userB)
            ?: throw GroupNotFoundException("해당 쌍의 오늘 궁합이 아직 생성되지 않았습니다.")

        return GroupCompatibilityResponse.from(compatibility)
    }

    private fun generateUniqueCode(): String {
        repeat(10) {
            val code = buildString {
                repeat(CODE_LENGTH) {
                    append(CODE_CHARS[secureRandom.nextInt(CODE_CHARS.length)])
                }
            }
            if (!groupRepository.existsByInviteCode(code)) {
                return code
            }
        }
        throw CodeGenerationException("고유 초대 코드 생성에 실패했습니다.")
    }
}
