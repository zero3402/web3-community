package com.web3community.notification.entity

import jakarta.persistence.*

@Entity
@Table(name = "notification_templates")
data class NotificationTemplate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    val type: NotificationType,
    
    @Column(name = "title_template", nullable = false)
    val titleTemplate: String,
    
    @Column(name = "message_template", nullable = false)
    val messageTemplate: String,
    
    @Column(name = "email_subject_template")
    val emailSubjectTemplate: String? = null,
    
    @Column(name = "email_body_template")
    val emailBodyTemplate: String? = null,
    
    @Column(name = "push_title_template")
    val pushTitleTemplate: String? = null,
    
    @Column(name = "push_body_template")
    val pushBodyTemplate: String? = null,
    
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    
    @Column(name = "supported_languages", columnDefinition = "JSON")
    val supportedLanguages: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)