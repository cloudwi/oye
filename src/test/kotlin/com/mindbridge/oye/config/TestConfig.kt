package com.mindbridge.oye.config

import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestConfig {

    @Bean
    fun chatClientBuilder(): ChatClient.Builder {
        val mockClient = mock(ChatClient::class.java)
        val mockBuilder = mock(ChatClient.Builder::class.java)
        `when`(mockBuilder.build()).thenReturn(mockClient)
        return mockBuilder
    }
}
