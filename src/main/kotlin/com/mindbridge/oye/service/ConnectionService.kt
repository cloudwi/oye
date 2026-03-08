package com.mindbridge.oye.service

import com.mindbridge.oye.domain.ConnectionStatus
import com.mindbridge.oye.domain.NotificationType
import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import com.mindbridge.oye.event.ConnectionCreatedEvent
import com.mindbridge.oye.dto.ConnectRequest
import com.mindbridge.oye.dto.ConnectionResponse
import com.mindbridge.oye.dto.MyCodeResponse
import com.mindbridge.oye.exception.CodeGenerationException
import com.mindbridge.oye.exception.ConnectionNotFoundException
import com.mindbridge.oye.exception.ConnectionNotPendingException
import com.mindbridge.oye.exception.DuplicateConnectionException
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.exception.SelfConnectionException
import com.mindbridge.oye.exception.UserNotFoundException
import com.mindbridge.oye.domain.RelationType
import com.mindbridge.oye.repository.CompatibilityRepository
import com.mindbridge.oye.repository.UserConnectionRepository
import com.mindbridge.oye.repository.UserRepository
import com.mindbridge.oye.util.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDate

@Service
class ConnectionService(
    private val userRepository: UserRepository,
    private val userConnectionRepository: UserConnectionRepository,
    private val compatibilityRepository: CompatibilityRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val userNotificationService: UserNotificationService,
    private val pushNotificationService: PushNotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CODE_LENGTH = 6
        private const val CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private val secureRandom = SecureRandom()
    }

    @Transactional
    fun getMyCode(user: User): MyCodeResponse {
        user.connectCode?.let { return MyCodeResponse(code = it, nickname = user.nickname) }

        val code = generateUniqueCode()
        user.connectCode = code
        userRepository.save(user)
        return MyCodeResponse(code = code, nickname = user.nickname)
    }

    @Transactional
    fun connect(user: User, request: ConnectRequest): ConnectionResponse {
        val partner = resolvePartner(request)

        if (partner.id == user.id) {
            throw SelfConnectionException()
        }

        val alreadyConnected = userConnectionRepository.existsByUserAndPartnerOrPartnerAndUser(
            user, partner, user, partner
        )
        if (alreadyConnected) {
            throw DuplicateConnectionException()
        }

        val connection = UserConnection(
            user = user,
            partner = partner,
            relationType = request.relationType,
            status = ConnectionStatus.PENDING
        )
        val saved = userConnectionRepository.save(connection)
        log.info("친구 요청 생성: userId={}, partnerId={}", user.id, partner.id)

        // 상대방에게 알림
        val requesterName = user.name ?: user.nickname
        userNotificationService.createNotification(
            user = partner,
            title = "친구 요청이 도착했어요!",
            body = "${requesterName}님이 친구 요청을 보냈습니다.",
            type = NotificationType.CONNECTION,
            metadata = """{"connectionId":${saved.id},"action":"CONNECTION_REQUEST","requesterId":${user.id}}"""
        )
        pushNotificationService.sendToUser(partner, "친구 요청이 도착했어요!", "${requesterName}님이 친구 요청을 보냈습니다.")

        return ConnectionResponse.from(saved, user)
    }

    @Transactional
    fun acceptConnection(user: User, connectionId: Long): ConnectionResponse {
        val connection = userConnectionRepository.findByIdWithUsers(connectionId)
            .orElseThrow { ConnectionNotFoundException() }

        if (connection.partner.id != user.id) {
            throw ForbiddenException("본인에게 온 요청만 수락할 수 있습니다.")
        }

        if (connection.status != ConnectionStatus.PENDING) {
            throw ConnectionNotPendingException()
        }

        connection.status = ConnectionStatus.ACCEPTED
        userConnectionRepository.save(connection)
        log.info("친구 요청 수락: connectionId={}, userId={}", connectionId, user.id)

        // 궁합 생성 이벤트 발행
        eventPublisher.publishEvent(ConnectionCreatedEvent(connection))

        // 요청자에게 수락 알림
        val accepterName = user.name ?: user.nickname
        userNotificationService.createNotification(
            user = connection.user,
            title = "친구 요청이 수락되었어요!",
            body = "${accepterName}님이 친구 요청을 수락했습니다.",
            type = NotificationType.CONNECTION,
            metadata = """{"connectionId":${connectionId},"action":"CONNECTION_ACCEPTED"}"""
        )
        pushNotificationService.sendToUser(connection.user, "친구 요청이 수락되었어요!", "${accepterName}님이 친구 요청을 수락했습니다.")

        return ConnectionResponse.from(connection, user)
    }

    @Transactional
    fun rejectConnection(user: User, connectionId: Long) {
        val connection = userConnectionRepository.findByIdWithUsers(connectionId)
            .orElseThrow { ConnectionNotFoundException() }

        if (connection.partner.id != user.id) {
            throw ForbiddenException("본인에게 온 요청만 거절할 수 있습니다.")
        }

        if (connection.status != ConnectionStatus.PENDING) {
            throw ConnectionNotPendingException()
        }

        userConnectionRepository.delete(connection)
        log.info("친구 요청 거절: connectionId={}, userId={}", connectionId, user.id)
    }

    @Transactional(readOnly = true)
    fun getPendingRequests(user: User): List<ConnectionResponse> {
        val pending = userConnectionRepository.findPendingRequestsForUser(user)
        return pending.map { ConnectionResponse.from(it, user) }
    }

    @Transactional(readOnly = true)
    fun getMyConnections(user: User): List<ConnectionResponse> {
        val connections = userConnectionRepository.findByUserOrPartnerWithUsers(user)
        if (connections.isEmpty()) return emptyList()

        val today = DateUtils.today()
        val compatibilities = compatibilityRepository.findByConnectionInAndDate(connections, today)
        val compatibilityByConnectionId = compatibilities.associateBy { it.connection.id }

        return connections.map { connection ->
            val compatibility = compatibilityByConnectionId[connection.id]
            ConnectionResponse.from(connection, user, compatibility?.score, compatibility?.content)
        }
    }

    @Transactional
    fun setLover(user: User, connectionId: Long): ConnectionResponse {
        val connection = userConnectionRepository.findById(connectionId)
            .orElseThrow { ConnectionNotFoundException() }

        if (connection.user.id != user.id && connection.partner.id != user.id) {
            throw ForbiddenException("해당 연결을 수정할 권한이 없습니다.")
        }

        // 기존 LOVER 연결을 FRIEND로 변경
        val existingConnections = userConnectionRepository.findByUserOrPartnerWithUsers(user)
        existingConnections
            .filter { it.relationType == RelationType.LOVER }
            .forEach {
                it.relationType = RelationType.FRIEND
                userConnectionRepository.save(it)
            }

        connection.relationType = RelationType.LOVER
        val saved = userConnectionRepository.save(connection)
        log.info("연인 설정: connectionId={}, userId={}", connectionId, user.id)
        return ConnectionResponse.from(saved, user)
    }

    @Transactional
    fun unsetLover(user: User, connectionId: Long): ConnectionResponse {
        val connection = userConnectionRepository.findById(connectionId)
            .orElseThrow { ConnectionNotFoundException() }

        if (connection.user.id != user.id && connection.partner.id != user.id) {
            throw ForbiddenException("해당 연결을 수정할 권한이 없습니다.")
        }

        connection.relationType = RelationType.FRIEND
        val saved = userConnectionRepository.save(connection)
        log.info("연인 해제: connectionId={}, userId={}", connectionId, user.id)
        return ConnectionResponse.from(saved, user)
    }

    @Transactional
    fun deleteConnection(user: User, connectionId: Long) {
        val connection = userConnectionRepository.findById(connectionId)
            .orElseThrow { ConnectionNotFoundException() }

        if (connection.user.id != user.id && connection.partner.id != user.id) {
            throw ForbiddenException("해당 연결을 삭제할 권한이 없습니다.")
        }

        compatibilityRepository.deleteAllByConnection(connection)
        userConnectionRepository.delete(connection)
        log.info("연결 삭제: connectionId={}, userId={}", connectionId, user.id)
    }

    private fun resolvePartner(request: ConnectRequest): User {
        if (request.nickname != null) {
            return userRepository.findByNickname(request.nickname)
                ?: throw UserNotFoundException("해당 닉네임의 사용자를 찾을 수 없습니다.")
        }
        if (request.code != null) {
            return userRepository.findByConnectCode(request.code)
                ?: throw UserNotFoundException("해당 초대 코드의 사용자를 찾을 수 없습니다.")
        }
        throw IllegalArgumentException("닉네임 또는 초대 코드를 입력해주세요.")
    }

    fun generateUniqueCode(): String {
        repeat(10) {
            val code = buildString {
                repeat(CODE_LENGTH) {
                    append(CODE_CHARS[secureRandom.nextInt(CODE_CHARS.length)])
                }
            }
            if (userRepository.findByConnectCode(code) == null) {
                return code
            }
        }
        throw CodeGenerationException("고유 초대 코드 생성에 실패했습니다.")
    }
}
