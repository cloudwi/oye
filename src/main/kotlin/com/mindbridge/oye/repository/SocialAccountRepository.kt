package com.mindbridge.oye.repository

import com.mindbridge.oye.domain.SocialAccount
import com.mindbridge.oye.domain.SocialProvider
import com.mindbridge.oye.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface SocialAccountRepository : JpaRepository<SocialAccount, Long> {
    fun findByProviderAndProviderId(provider: SocialProvider, providerId: String): SocialAccount?
    fun findFirstByUser(user: User): SocialAccount?
    fun deleteAllByUser(user: User)
}
