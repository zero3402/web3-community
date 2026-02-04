package com.web3community.comment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CommentCreateRequest(
    @field:NotBlank(message = "Content is required")
    @field:Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters")
    val content: String,
    
    @field:NotBlank(message = "Post ID is required")
    val postId: String,
    
    val parentId: String? = null,
    val attachments: List<CommentAttachmentRequest> = emptyList()
)

data class CommentUpdateRequest(
    @field:NotBlank(message = "Content is required")
    @field:Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters")
    val content: String
)

data class CommentAttachmentRequest(
    val filename: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val filePath: String,
    val thumbnailPath: String? = null,
    val type: AttachmentType = AttachmentType.FILE,
    val displayOrder: Int = 0
)

data class CommentResponse(
    val id: Long,
    val content: String,
    val postId: Long,
    val authorId: Long,
    val authorName: String? = null,
    val authorAvatar: String? = null,
    val parentId: Long? = null,
    val threadId: Long,
    val level: Int,
    val likeCount: Long,
    val dislikeCount: Long,
    val replyCount: Long,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val isPinned: Boolean,
    val isReported: Boolean,
    val editedContent: String? = null,
    val editedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val attachments: List<AttachmentResponse> = emptyList(),
    val replies: List<CommentResponse> = emptyList(),
    val userReaction: String? = null,
    val canReply: Boolean = true,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false
)

data class AttachmentResponse(
    val id: Long,
    val filename: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val filePath: String,
    val thumbnailPath: String? = null,
    val type: String,
    val displayOrder: Int,
    val createdAt: String
)

data class CommentSearchRequest(
    val postId: String? = null,
    val authorId: String? = null,
    val parentId: String? = null,
    val threadId: String? = null,
    val level: Int? = null,
    val isEdited: Boolean? = null,
    val isDeleted: Boolean? = null,
    val isPinned: Boolean? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val sortBy: CommentSortBy = CommentSortBy.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val includeReplies: Boolean = true
)

data class CommentListResponse(
    val comments: List<CommentResponse>,
    val pagination: PaginationResponse
)

data class CommentThreadResponse(
    val id: Long,
    val postId: Long,
    val rootCommentId: Long,
    val commentCount: Long,
    val participantCount: Long,
    val lastCommentAt: String? = null,
    val lastCommentId: Long? = null,
    val isActive: Boolean,
    val isLocked: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val rootComment: CommentResponse? = null
)

data class CommentStatsResponse(
    val totalComments: Long,
    val activeThreads: Long,
    val totalParticipants: Long,
    val averageCommentsPerThread: Double,
    val mostActiveUsers: List<UserCommentStats>,
    val mostCommentedPosts: List<PostCommentStats>
)

data class UserCommentStats(
    val userId: Long,
    val username: String,
    val commentCount: Long,
    val likeCount: Long,
    val replyCount: Long
)

data class PostCommentStats(
    val postId: Long,
    val postTitle: String,
    val commentCount: Long,
    val participantCount: Long,
    val lastCommentAt: String
)

data class CommentReactionRequest(
    val reactionType: ReactionType
)

data class CommentReactionResponse(
    val id: Long,
    val commentId: Long,
    val userId: Long,
    val reactionType: String,
    val createdAt: String
)

data class PaginationResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

enum class CommentSortBy {
    CREATED_AT, UPDATED_AT, LIKE_COUNT, DISLIKE_COUNT, REPLY_COUNT
}

enum class SortDirection {
    ASC, DESC
}

enum class AttachmentType {
    IMAGE, VIDEO, DOCUMENT, FILE, THUMBNAIL
}

enum class ReactionType {
    LIKE, DISLIKE, LAUGH, LOVE, ANGRY, SAD
}