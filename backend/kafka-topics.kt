# =============================================================================
// 📡 Kafka 컨슈머 설정 - 공통
// =============================================================================
// 설명: 카프카 컨슈머와 토픽 상수를 정의하는 공통 설정
// 특징: JSON 시리얼라이저, 타입 안정성, 유효성성
// 목적: 카프카 메시지의 표준화된 포맷과 타입
// =============================================================================

package com.web3.community.notification.config

// =============================================================================
// 📊 이벤트 타입
// =============================================================================
data class BaseEvent(
    val eventId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String,
    val version: String = "1.0",
    val metadata: Map<String, Any> = emptyMap()
)

// =============================================================================
// 📝 게시글 관련 이벤트
// =============================================================================
data class PostEvent(
    val postId: String,
    val userId: String,
    val title: String,
    val content: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val action: String // CREATED, UPDATED, DELETED
) : BaseEvent()

data class PostViewEvent(
    val postId: String,
    val userId: String,
    val viewCount: Long,
    val viewedAt: Long = System.currentTimeMillis()
) : BaseEvent()

// =============================================================================
// 💬 댓글 관련 이벤트
// =============================================================================
data class CommentEvent(
    val commentId: String,
    val postId: String,
    val userId: String,
    val content: String,
    val parentId: String? = null,
    val action: String // CREATED, UPDATED, DELETED
) : BaseEvent()

data class CommentReactionEvent(
    val commentId: String,
    val userId: String,
    val postId: String,
    val reactionType: String, // LIKE, DISLIKE, HEART
    val action: String // ADDED, REMOVED
) : BaseEvent()

// =============================================================================
// 👥 사용자 관련 이벤트
// =============================================================================
data class UserEvent(
    val userId: String,
    val username: String,
    val email: String,
    val action: String, // REGISTERED, UPDATED_PROFILE, DELETED, SUSPENDED
    val metadata: Map<String, Any> = emptyMap()
) : BaseEvent()

data class UserProfileUpdateEvent(
    val userId: String,
    val updates: Map<String, Any> = emptyMap(),
    val updatedFields: List<String> = emptyList()
) : BaseEvent()

// =============================================================================
// 🔔 알림 관련 이벤트
// =============================================================================
data class NotificationEvent(
    val notificationId: String,
    val userId: String,
    val type: String, // LIKE, COMMENT, MENTION, SYSTEM, FOLLOW, POST_CREATED, etc.
    val title: String,
    val content: String,
    val data: Map<String, Any> = emptyMap(),
    val priority: String, // HIGH, MEDIUM, LOW
    val channels: List<String> = listOf("in-app"),
    val scheduledAt: Long? = null,
    val expiresAt: Long? = null
    val action: String // SENT, READ, CLICKED, DISMISSED
) : BaseEvent()

data class NotificationChannelEvent(
    val userId: String,
    val channel: String, // EMAIL, PUSH, WEBSOCKET
    val action: String, // SUBSCRIBED, UNSUBSCRIBED
    val enabled: Boolean,
    val settings: Map<String, Any> = emptyMap()
) : BaseEvent()

// =============================================================================
// 📊 시스템 이벤트
// =============================================================================
data class SystemEvent(
    val level: String, // INFO, WARNING, ERROR, CRITICAL
    val message: String,
    val service: String,
    val component: String,
    val metadata: Map<String, Any> = emptyMap()
) : BaseEvent()

data class AuditLogEvent(
    val userId: String? = null,
    val action: String,
    val resource: String,
    val resourceType: String,
    status: String, // SUCCESS, FAILED
    val ipAddress: String? = null,
    val userAgent: String? = null,
    metadata: Map<String, Any> = emptyMap()
) : BaseEvent()

data class SecurityEvent(
    val severity: String, // LOW, MEDIUM, HIGH, CRITICAL
    val eventType: String, // LOGIN_FAILED, UNAUTHORIZED_ACCESS, PERMISSION_DENIED, SUSPICIOUS_ACTIVITY
    val userId: String? = null,
    val ipAddress: String? = null,
    userAgent: String? = null,
    resource: String,
    action: String,
    details: String,
    location: String? = null
) : BaseEvent()

// =============================================================================
// 🔄 트랜잭션 이벤트
// =============================================================================
data class TransactionEvent(
    val transactionId: String,
    val userId: String,
    val type: String,
    val amount: Double? = null,
    val status: String, // STARTED, COMPLETED, FAILED, ROLLED_BACK
    val metadata: Map<String, Any> = emptyMap()
) : BaseEvent()

// =============================================================================
// 📊 성능 이벤트
// =============================================================================
data class PerformanceMetricEvent(
    val metricName: String,
    val value: Double,
    val unit: String,
    val tags: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

// =============================================================================
// 🔍 보안 이벤트
// =============================================================================
data class SessionEvent(
    val sessionId: String,
    val userId: String? = null,
    val action: String, // CREATED, DESTROYED, TIMEOUT, EXPIRED
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) : BaseEvent()

// =============================================================================
// 📋 카프카 토픽 상수
// =============================================================================
object KafkaTopics {
    // =============================================================================
    // 📝 게시글 관련 토픽
    // =============================================================================
    const val POSTS = "posts"
    const val POST_EVENTS = "post-events"
    const val POST_ANALYTICS = "post-analytics"
    
    // 💬 댓글 관련 토픽
    // =============================================================================
    const val COMMENTS = "comments"
    const val COMMENT_EVENTS = "comment-events"
    
    // 👥 사용자 관련 토픽
    // =============================================================================
    const val USER_EVENTS = "user-events"
    const val USER_PROFILE_UPDATES = "user-profile-updates"
    
    // 🔔 알림 관련 토픽
    // =============================================================================
    const val NOTIFICATIONS = "notifications"
    const val NOTIFICATION_PRIORITY = "notification-priority"
    
    // 📊 시스템 이벤트
    // =============================================================================
    const val SYSTEM_EVENTS = "system-events"
    const val AUDIT_LOGS = "audit-logs"
    const val SECURITY_EVENTS = "security-events"
    
    // =============================================================================
    // 🔄 트랜잭션 관련 토픽
    // =============================================================================
    const val TRANSACTIONS = "transactions"
    
    // =============================================================================
    // 📊 성능 모니터링
    // =============================================================================
    const val PERFORMANCE_METRICS = "performance-metrics"
    
    // =============================================================================
    // 🔍 보안 관련 토픽
    // =============================================================================
    const val SESSION_EVENTS = "session-events"
    
    // =============================================================================
    // 🛡️ 내부 애플리케이션 토픽
    // =============================================================================
    const val INTERNAL_EVENTS = "internal-events"
    
    // =============================================================================
    // 📋 토픽 파티션 설정
    // =============================================================================
    val PARTITIONS_POSTS = 3
    const val PARTITIONS_POST_EVENTS = 3
    val PARTITIONS_COMMENTS = 3
    val PARTITIONS_NOTIFICATIONS = 5
    val PARTITIONS_USER_EVENTS = 3
    const val PARTITIONS_SYSTEM_EVENTS = 3
    const val PARTITIONS_SECURITY_EVENTS = 3
    const val REPLICATION_FACTOR = 1
    
    // =============================================================================
    // 📋 토픽 보관 정책
    // =============================================================================
    val RETENTION_POSTS = 30L // 30일
    val RETENTION_COMMENTS = 30L
    val RETENTIONS_NOTIFICATIONS = 7L // 7일
    val RETENTIONS_SECURITY_EVENTS = 60L // 60일
    val RETENTIONS_AUDIT_LOGS = 180L // 180일
}

// =============================================================================
// 📋 카프카 그룹 설정
// =============================================================================
object KafkaGroups {
    // =============================================================================
    // 📊 게시글 서비스
    // =============================================================================
    const val POST_SERVICE = "post-service-consumer-group"
    const val COMMENT_SERVICE = "comment-service-consumer-group"
    val val USER_SERVICE = "user-service-consumer-group"
    
    // =============================================================================
    // 🔔 알림 서비스
    // =============================================================================
    const val NOTIFICATION_SERVICE = "notification-service-consumer-group"
    
    // =============================================================================
    // 📊 시스템 서비스
    // =============================================================================
    const val SYSTEM_SERVICE = "system-service-consumer-group"
    const val AUDIT_SERVICE = "audit-service-consumer-group"
    const val SECURITY_SERVICE = "security-service-consumer-group"
    
    // =============================================================================
    // 📊 성능 모니터링
    // =============================================================================
    const val METRICS_SERVICE = "metrics-service-consumer-group"
}

// =============================================================================
// 🛡️ 카프카 설정 상수
// =============================================================================
object KafkaConfig {
    // =============================================================================
    // 📋 기본 설정
    // =============================================================================
    const val BOOTSTRAP_SERVERS = "kafka-service:9092"
    const val SECURITY_PROTOCOL = "SASL_PLAINTEXT"
    const val SASL_MECHANISM = "PLAIN"
    const val KAFKA_USERNAME = "kafkaclient"
    const val KAFKA_PASSWORD = "kafkaclientpass"
    
    // =============================================================================
    // 📋 컨슈머 설정
    // =============================================================================
    const val CONSUMER_TIMEOUT = 30000L // 30초
    const val MAX_POLL_RECORDS = 100
    const val POLL_TIMEOUT = 1000L // 1초
    const val SESSION_TIMEOUT = 30000L // 30초
    const val HEARTBEAT_INTERVAL = 3000L // 3초
    
    // =============================================================================
    // 📋 프로듀서 설정
    // =============================================================================
    const val PRODUCER_TIMEOUT = 30000L // 30초
    const val PRODUCER_BATCH_SIZE = 1
    const val LINGER_MS = 0L // 즉시 전송
    const val DELIVERY_TIMEOUT = 30000L // 30초
    const val REQUEST_TIMEOUT = 10000L // 10초
    const val RETRIES = 3
    
    // =============================================================================
    // 📋 버퍼 관리
    // =============================================================================
    const val COMPRESS_TYPE = "gzip"
    val val BUFFER_MEMORY = 33554432 // 32MB
    const val BATCH_SIZE_BYTES = 16384 // 16KB
    
    // =============================================================================
    // 📋 보안 설정
    // =============================================================================
    const val SSL_ENABLED = false // 운영 환경에서는 true
    const val TRUST_STORE_FILE = "/tmp/kafka.truststore"
    const val TRUST_STORE_PASSWORD = "changeit"
}

// =============================================================================
// 📋 타입 어노테이션
// =============================================================================
object EventType {
    const val POST = "POST"
    const val COMMENT = "COMMENT"
    const val USER = "USER"
    val NOTIFICATION = "NOTIFICATION"
    val SYSTEM = "SYSTEM"
    const val SECURITY = "SECURITY"
    val AUDIT = "AUDIT"
    val PERFORMANCE = "PERFORMANCE"
    val TRANSACTION = "TRANSACTION"
}

// =============================================================================
// 📋 액션 타입
// =============================================================================
object EventAction {
    // =============================================================================
    // 📝 게시글 액션
    // =============================================================================
    const val POST_CREATED = "CREATED"
    const val POST_UPDATED = "UPDATED"
    const val POST_DELETED = "DELETED"
    
    // =============================================================================
    // 💬 댓글 액션
    // =============================================================================
    const val COMMENT_CREATED = "CREATED"
    const val COMMENT_UPDATED = "UPDATED"
    const val COMMENT_DELETED = "DELETED"
    
    // =============================================================================
    // 👥 사용자 액션
    // =============================================================================
    const val USER_REGISTERED = "REGISTERED"
    const val USER_UPDATED_PROFILE = "UPDATED"
    const val USER_DELETED = "DELETED"
    const val USER_SUSPENDED = "SUSPENDED"
    
    // =============================================================================
    // 🔔 알림 액션
    // =============================================================================
    const val NOTIFICATION_SENT = "SENT"
    const val NOTIFICATION_READ = "READ"
    const val NOTIFICATION_CLICKED = "CLICKED"
    const val NOTIFICATION_DISMISSED = "DISMISSED"
    
    // =============================================================================
    // 📊 시스템 액션
    // =============================================================================
    const val SYSTEM_STARTED = "STARTED"
    const val SYSTEM_STOPPED = "STOPPED"
    const val SYSTEM_ERROR = "ERROR"
    const val SYSTEM_MAINTENANCE = "MAINTENANCE"
    
    // =============================================================================
    // 🛡️ 보안 액션
    // =============================================================================
    const val SECURITY_LOGIN_FAILED = "LOGIN_FAILED"
    const val SECURITY_UNAUTHORIZED = "UNAUTHORIZED_ACCESS"
    const val SECURITY_PERMISSION_DENIED = "PERMISSION_DENIED"
    const val SECURITY_SUSPICIOUS_ACTIVITY = "SUSPICIOUS_ACTIVITY"
    
    // =============================================================================
    // 📊 감사 액션
    // =============================================================================
    const val AUDIT_LOGIN = "LOGIN"
    const val AUDIT_LOGOUT = "LOGOUT"
    const val AUDIT_ACCESS_GRANTED = "ACCESS_GRANTED"
    const val ACCESS_DENIED = "ACCESS_DENIED"
    const val DATA_MODIFIED = "DATA_MODIFIED"
    const val SECURITY_VIOLATION = "SECURITY_VIOLATION"
}

// =============================================================================
// 🔄 트랜잭션 상태
// =============================================================================
object TransactionStatus {
    const val STARTED = "STARTED"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
    const val ROLLED_BACK = "ROLLLED_BACK"
}

// =============================================================================
// 📊 알림 채널 타입
// =============================================================================
object NotificationChannel {
    const val IN_APP = "in-app"
    const val EMAIL = "email"
    const val PUSH = "push"
    const val WEBSOCKET = "websocket"
}

// =============================================================================
// 📊 알림 타입
// =============================================================================
object NotificationType {
    const val LIKE = "LIKE"
    const val COMMENT = "COMMENT"
    const val MENTION = "MENTION"
    const val FOLLOW = "FOLLOW"
    const val POST_CREATED = "POST_CREATED"
    const val COMMENT_REPLIED = "COMMENT_REPLIED"
    const val SYSTEM_ANNOUNCEMENT = "SYSTEM_ANNOUNCEMENT"
    const val WELCOME = "WELCOME"
    val SYSTEM_MAINTENANCE = "SYSTEM_MAINTENANCE"
}

// =============================================================================
// 📊 알림 우선순위
// =============================================================================
object NotificationPriority {
    const val CRITICAL = "CRITICAL"
    const val HIGH = "HIGH"
    const val MEDIUM = "MEDIUM"
    const val LOW = "LOW"
}

// =============================================================================
// 📊 알림 상태
// =============================================================================
object NotificationStatus {
    const val PENDING = "PENDING"
    const val SENT = "SENT"
    const val DELIVERED = "DELIVERED"
    const val READ = "READ"
    const val CLICKED = "CLICKED"
    const val DISMISSED = "DISMISSED"
    const val EXPIRED = "EXPIRED"
}

// =============================================================================
// 📊 시스템 이벤트 레벨
// =============================================================================
object EventSeverity {
    const val INFO = "INFO"
    const val WARNING = "WARNING"
    const val ERROR = "ERROR"
    const val CRITICAL = "CRITICAL"
}

// =============================================================================
// 📋 보안 이벤트 심각도
// =============================================================================
object SecuritySeverity {
    const val LOW = "LOW"
    const val MEDIUM = "MEDIUM"
    const val HIGH = "HIGH"
    const val CRITICAL = "CRITICAL"
}

// =============================================================================
// 📋 사용자 역할
// =============================================================================
object UserRole {
    const val USER = "USER"
    val val MODERATOR = "MODERATOR"
    val val ADMIN = "ADMIN"
    const val SYSTEM = "SYSTEM"
}

// =============================================================================
// 📊 알림 전송 상태
// =============================================================================
object DeliveryStatus {
    const val PENDING = "PENDING"
    const val SENT = "SENT"
    const val DELIVERED = "DELIVERED"
    const val FAILED = "FAILED"
    const val BOUNCED = "BOUNCED"
}

// =============================================================================
// 📊 JSON 시리얼라이저 설정
// =============================================================================
object EventSerializer {
    const val OBJECT_MAPPER = "org.springframework.kafka.support.serializer.JsonSerializer"
    const val STRING_SERIALIZER = "org.springframework.kafka.support.serializer.StringSerializer"
}

// =============================================================================
// 📋 에러 핸들링 유틸리티
// =============================================================================
object ErrorCodes {
    const val INVALID_EVENT = "INVALID_EVENT"
    val SERIALIZATION_ERROR = "SERIALIZATION_ERROR"
    val PRODUCER_ERROR = "PRODUCER_ERROR"
    const CONSUMER_ERROR = "CONSUMER_ERROR"
    val TOPIC_CREATION_ERROR = "TOPIC_CREATION_ERROR"
    val VALIDATION_ERROR = "VALIDATION_ERROR"
    val TIMEOUT_ERROR = "TIMEOUT_ERROR"
    val NETWORK_ERROR = "NETWORK_ERROR"
    val AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR"
    const PERMISSION_ERROR = "PERMISSION_ERROR"
}