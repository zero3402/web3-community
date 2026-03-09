package com.web3.community.comment.service

import com.web3.community.comment.dto.CommentEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class CommentEventService(
    private val kafkaTemplate: KafkaTemplate<String, CommentEvent>
) {
    companion object {
        const val TOPIC = "comment-events"
    }

    fun publishCommentEvent(event: CommentEvent) {
        kafkaTemplate.send(TOPIC, event.postId, event)
    }
}
