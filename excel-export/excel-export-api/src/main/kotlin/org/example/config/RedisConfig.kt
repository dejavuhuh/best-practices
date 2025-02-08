package org.example.config

import org.example.ExportEventChannel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter


@Configuration
class RedisConfig {

    @Bean
    fun container(
        connectionFactory: RedisConnectionFactory,
        exportEventChannel: ExportEventChannel
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(MessageListenerAdapter(exportEventChannel), PatternTopic("export:*"))
        return container
    }
}
