package com.web3community.notification.service

import com.web3community.notification.entity.Notification
import com.web3community.notification.entity.NotificationDeliveryLog
import com.web3community.notification.entity.NotificationChannel
import com.web3community.notification.entity.DeliveryStatus
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import jakarta.mail.internet.MimeMessage
import java.time.LocalDateTime

@Service
class EmailNotificationService(
    private val mailSender: JavaMailSender,
    private val notificationDeliveryLogRepository: NotificationDeliveryLogRepository
) {

    fun sendEmail(notification: Notification) {
        try {
            val mimeMessage: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")
            
            // This would typically fetch user's email from user service
            val toEmail = "user${notification.recipientId}@web3community.com" // Placeholder
            
            helper.setTo(toEmail)
            helper.setSubject(generateEmailSubject(notification))
            helper.setText(generateEmailContent(notification), true)
            
            // Add sender info
            helper.setFrom("noreply@web3community.com", "Web3 Community")
            
            mailSender.send(mimeMessage)
            
            // Log successful delivery
            logDelivery(notification, NotificationChannel.EMAIL, DeliveryStatus.SENT, toEmail)
            
        } catch (e: Exception) {
            // Log failed delivery
            logDelivery(notification, NotificationChannel.EMAIL, DeliveryStatus.FAILED, null, e.message)
            throw e
        }
    }

    private fun generateEmailSubject(notification: Notification): String {
        return when (notification.type) {
            com.web3community.notification.entity.NotificationType.POST_LIKE -> "Your post received a new like"
            com.web3community.notification.entity.NotificationType.POST_COMMENT -> "Your post has a new comment"
            com.web3community.notification.entity.NotificationType.COMMENT_REPLY -> "Someone replied to your comment"
            com.web3community.notification.entity.NotificationType.COMMENT_LIKE -> "Your comment received a new like"
            com.web3community.notification.entity.NotificationType.FOLLOW -> "You have a new follower"
            com.web3community.notification.entity.NotificationType.MENTION -> "You were mentioned in a post"
            com.web3community.notification.entity.NotificationType.SYSTEM_ANNOUNCEMENT -> "Important System Announcement"
            com.web3community.notification.entity.NotificationType.ACCOUNT_VERIFICATION -> "Verify Your Account"
            com.web3community.notification.entity.NotificationType.PASSWORD_CHANGE -> "Password Changed"
            com.web3community.notification.entity.NotificationType.SECURITY_ALERT -> "Security Alert"
            com.web3community.notification.entity.NotificationType.ACHIEVEMENT_UNLOCKED -> "Achievement Unlocked!"
            else -> "New Notification"
        }
    }

    private fun generateEmailContent(notification: Notification): String {
        val actionUrl = notification.actionUrl ?: "#"
        val actionText = notification.actionText ?: "View Notification"
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>${notification.title}</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #28a745; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Web3 Community</h1>
                    </div>
                    <div class="content">
                        <h2>${notification.title}</h2>
                        <p>${notification.message}</p>
                        
                        ${notification.senderName?.let { "<p><strong>From:</strong> $it</p>" } ?: ""}
                        
                        ${notification.imageUrl?.let { "<img src='$it' alt='Notification image' style='max-width: 100%; height: auto;'>" } ?: ""}
                        
                        <div style="text-align: center;">
                            <a href="$actionUrl" class="button">$actionText</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from Web3 Community. Please do not reply to this email.</p>
                        <p>If you don't want to receive these emails, you can update your notification preferences in your account settings.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun logDelivery(
        notification: Notification,
        channel: NotificationChannel,
        status: DeliveryStatus,
        recipientAddress: String?,
        errorMessage: String? = null
    ) {
        val log = NotificationDeliveryLog(
            notificationId = notification.id ?: 0,
            channel = channel,
            recipientId = notification.recipientId,
            recipientAddress = recipientAddress,
            deliveryStatus = status,
            sentAt = if (status == DeliveryStatus.SENT) LocalDateTime.now() else null,
            failedAt = if (status == DeliveryStatus.FAILED) LocalDateTime.now() else null,
            errorMessage = errorMessage
        )
        
        notificationDeliveryLogRepository.save(log)
    }
}