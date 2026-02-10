package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findFirstByKakaoId(kakaoId: String): User?
}
