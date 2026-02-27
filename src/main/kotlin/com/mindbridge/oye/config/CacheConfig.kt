package com.mindbridge.oye.config

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        const val FORTUNE_TODAY = "fortuneToday"
        const val COMPATIBILITY_TODAY = "compatibilityToday"
        private val ZONE = ZoneId.of("Asia/Seoul")
    }

    @Bean
    fun cacheManager(): CacheManager {
        val manager = CaffeineCacheManager(FORTUNE_TODAY, COMPATIBILITY_TODAY)
        manager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfter(MidnightExpiry())
        )
        return manager
    }

    private class MidnightExpiry : Expiry<Any, Any> {
        override fun expireAfterCreate(key: Any, value: Any, currentTime: Long): Long {
            return nanosUntilMidnight()
        }

        override fun expireAfterUpdate(key: Any, value: Any, currentTime: Long, currentDuration: Long): Long {
            return nanosUntilMidnight()
        }

        override fun expireAfterRead(key: Any, value: Any, currentTime: Long, currentDuration: Long): Long {
            return currentDuration
        }

        private fun nanosUntilMidnight(): Long {
            val now = ZonedDateTime.now(ZONE)
            val midnight = ZonedDateTime.of(LocalDate.now(ZONE).plusDays(1), LocalTime.MIDNIGHT, ZONE)
            return Duration.between(now, midnight).toNanos()
        }
    }
}
