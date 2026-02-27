package com.mindbridge.oye.service

import com.mindbridge.oye.domain.User
import com.mindbridge.oye.domain.UserConnection
import com.mindbridge.oye.event.ConnectionCreatedEvent
import com.mindbridge.oye.dto.ConnectRequest
import com.mindbridge.oye.dto.ConnectionResponse
import com.mindbridge.oye.dto.MyCodeResponse
import com.mindbridge.oye.exception.CodeGenerationException
import com.mindbridge.oye.exception.ConnectionNotFoundException
import com.mindbridge.oye.exception.DuplicateConnectionException
import com.mindbridge.oye.exception.ForbiddenException
import com.mindbridge.oye.exception.SelfConnectionException
import com.mindbridge.oye.exception.UserNotFoundException
import com.mindbridge.oye.repository.CompatibilityRepository
import com.mindbridge.oye.repository.UserConnectionRepository
import com.mindbridge.oye.repository.UserRepository
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
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val CODE_LENGTH = 6
        private const val CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private val secureRandom = SecureRandom()
    }

    @Transactional
    fun getMyCode(user: User): MyCodeResponse {
        user.connectCode?.let { return MyCodeResponse(code = it) }

        val code = generateUniqueCode()
        user.connectCode = code
        userRepository.save(user)
        return MyCodeResponse(code = code)
    }

    @Transactional
    fun connect(user: User, request: ConnectRequest): ConnectionResponse {
        val partner = userRepository.findByConnectCode(request.code)
            ?: throw UserNotFoundException("해당 초대 코드의 사용자를 찾을 수 없습니다.")

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
            relationType = request.relationType
        )
        val saved = userConnectionRepository.save(connection)
        log.info("새 연결 생성: userId={}, partnerId={}, type={}", user.id, partner.id, request.relationType)
        eventPublisher.publishEvent(ConnectionCreatedEvent(saved))
        return ConnectionResponse.from(saved, user, null)
    }

    @Transactional(readOnly = true)
    fun getMyConnections(user: User): List<ConnectionResponse> {
        val connections = userConnectionRepository.findByUserOrPartnerWithUsers(user)
        if (connections.isEmpty()) return emptyList()

        val today = LocalDate.now()
        val compatibilities = compatibilityRepository.findByConnectionInAndDate(connections, today)
        val compatibilityByConnectionId = compatibilities.associateBy { it.connection.id }

        return connections.map { connection ->
            val compatibility = compatibilityByConnectionId[connection.id]
            ConnectionResponse.from(connection, user, compatibility?.score, compatibility?.content)
        }
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
