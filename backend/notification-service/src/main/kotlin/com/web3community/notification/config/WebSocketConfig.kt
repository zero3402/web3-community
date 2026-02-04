package com.web3community.notification.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        // Enable a simple broker for /topic and /queue destinations
        config.enableSimpleBroker("/topic", "/queue")
        
        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app")
        
        // Set user destination prefix
        config.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // Register STOMP endpoint with SockJS fallback
        registry.addEndpoint("/ws/notifications")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setHeartbeatTime(25000)
            .setDisconnectDelay(5000)
            
        // Also register raw WebSocket endpoint
        registry.addEndpoint("/ws/notifications-raw")
            .setAllowedOriginPatterns("*")
    }
}