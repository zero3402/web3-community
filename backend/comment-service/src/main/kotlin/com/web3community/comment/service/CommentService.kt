package com.web3community.comment.service

import com.web3community.comment.dto.*
import com.web3community.comment.entity.*
import com.web3community.comment.repository.*
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class CommentService(
    private val commentRepository: CommentRepository,
    private val commentThreadRepository: CommentThreadRepository,
    private val commentReactionRepository: CommentReactionRepository,
    private val commentAttachmentRepository: CommentAttachmentRepository
) {

    @CacheEvict(value = ["comments", "comment-threads"], allEntries = true)
    fun createComment(request: CommentCreateRequest, authorId: Long): CommentResponse {
        val postId = request.postId.toLongOrNull() ?: throw IllegalArgumentException("Invalid post ID")
        
        // Validate parent comment if provided
        val parentComment = request.parentId?.let { parentId ->
            val parent = commentRepository.findById(parentId.toLongOrNull() ?: 0L)
                .orElseThrow { IllegalArgumentException("Parent comment not found") }
            
            if (parent.postId != postId) {
                throw IllegalArgumentException("Parent comment belongs to different post")
            }
            
            if (!parent.canReply()) {
                throw IllegalArgumentException("Maximum nesting level reached")
            }
            
            parent
        }
        
        // Create comment
        val level = (parentComment?.level ?: -1) + 1
        val threadId = parentComment?.threadId ?: generateThreadId()
        
        val comment = Comment(
            content = request.content.trim(),
            postId = postId,
            authorId = authorId,
            parentId = parentComment?.id,
            threadId = threadId,
            level = level,
            createdBy = authorId
        )
        
        val savedComment = commentRepository.save(comment)
        
        // Handle attachments
        if (request.attachments.isNotEmpty()) {
            handleCommentAttachments(savedComment, request.attachments)
        }
        
        // Update parent reply count
        parentComment?.let { parent ->
            commentRepository.incrementReplyCount(parent.id ?: 0)
        }
        
        // Create or update thread
        if (parentComment == null) {
            createCommentThread(savedComment)
        } else {
            updateCommentThread(savedComment.threadId)
        }
        
        return toCommentResponse(savedComment, authorId)
    }

    @CacheEvict(value = ["comments", "comment-threads"], allEntries = true)
    fun updateComment(commentId: Long, request: CommentUpdateRequest, userId: Long): CommentResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("Comment not found") }
            
        if (comment.authorId != userId) {
            throw IllegalArgumentException("Not authorized to edit this comment")
        }
        
        if (comment.isDeleted) {
            throw IllegalArgumentException("Cannot edit deleted comment")
        }
        
        val updatedComment = comment.copy(
            content = request.content.trim(),
            isEdited = true,
            editedContent = comment.content,
            editedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            updatedBy = userId
        )
        
        val savedComment = commentRepository.save(updatedComment)
        return toCommentResponse(savedComment, userId)
    }

    @CacheEvict(value = ["comments", "comment-threads"], allEntries = true)
    fun deleteComment(commentId: Long, userId: Long): String {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("Comment not found") }
            
        if (comment.authorId != userId) {
            throw IllegalArgumentException("Not authorized to delete this comment")
        }
        
        if (comment.isDeleted) {
            throw IllegalArgumentException("Comment already deleted")
        }
        
        val deletedComment = comment.copy(
            content = "[This comment has been deleted]",
            isDeleted = true,
            deletedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            updatedBy = userId
        )
        
        commentRepository.save(deletedComment)
        
        // Update parent reply count
        comment.parentId?.let { parentId ->
            commentRepository.decrementReplyCount(parentId)
        }
        
        // Update thread
        updateCommentThread(comment.threadId)
        
        return "Comment deleted successfully"
    }

    @Cacheable(value = ["comments"], key = "#commentId")
    fun getCommentById(commentId: Long, userId: Long? = null): CommentResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("Comment not found") }
            
        return toCommentResponse(comment, userId)
    }

    @Cacheable(value = ["comments"], key = "#request.toString() + #pageable.pageNumber + #pageable.pageSize")
    fun getComments(request: CommentSearchRequest, pageable: Pageable, userId: Long? = null): CommentListResponse {
        val page = when {
            request.postId?.isNotEmpty() == true && request.parentId != null -> {
                val postId = request.postId.toLongOrNull() ?: 0L
                val parentId = request.parentId?.toLongOrNull()
                commentRepository.findByPostIdAndParentIdOrderByCreatedAtAsc(postId, parentId, pageable)
            }
            request.postId?.isNotEmpty() == true -> {
                val postId = request.postId.toLongOrNull() ?: 0L
                commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable)
            }
            else -> {
                commentRepository.findByMultipleCriteria(
                    postId = request.postId?.toLongOrNull(),
                    authorId = request.authorId?.toLongOrNull(),
                    parentId = request.parentId?.toLongOrNull(),
                    threadId = request.threadId?.toLongOrNull(),
                    level = request.level,
                    isEdited = request.isEdited,
                    isDeleted = request.isDeleted,
                    isPinned = request.isPinned,
                    dateFrom = request.dateFrom?.let { LocalDateTime.parse(it) },
                    dateTo = request.dateTo?.let { LocalDateTime.parse(it) },
                    pageable = pageable
                )
            }
        }
        
        val comments = page.content.map { toCommentResponse(it, userId) }
        
        return CommentListResponse(
            comments = comments,
            pagination = PaginationResponse(
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious()
            )
        )
    }

    fun getPostComments(postId: Long, userId: Long? = null): List<CommentResponse> {
        val rootComments = commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId)
        return rootComments.map { buildCommentTree(it, userId) }
    }

    fun getPostCommentsFlat(postId: Long, page: Int = 0, size: Int = 20, userId: Long? = null): CommentListResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val pageResult = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable)
        
        val comments = pageResult.content.map { toCommentResponse(it, userId) }
        
        return CommentListResponse(
            comments = comments,
            pagination = PaginationResponse(
                page = pageResult.number,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages,
                hasNext = pageResult.hasNext(),
                hasPrevious = pageResult.hasPrevious()
            )
        )
    }

    fun getCommentReplies(commentId: Long, userId: Long? = null): List<CommentResponse> {
        val replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId)
        return replies.map { toCommentResponse(it, userId) }
    }

    fun getCommentThread(threadId: Long, userId: Long? = null): List<CommentResponse> {
        val comments = commentRepository.findByThreadIdOrderByCreatedAtAsc(threadId)
        return buildCommentTree(comments.filter { it.parentId == null }.first(), userId).let { listOf(it) }
    }

    fun addReaction(commentId: Long, request: CommentReactionRequest, userId: Long): CommentReactionResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("Comment not found") }
            
        // Check if user already reacted
        val existingReaction = commentReactionRepository.findByCommentIdAndUserId(commentId, userId)
        
        if (existingReaction.isPresent) {
            // Update existing reaction
            val reaction = existingReaction.get()
            val updatedReaction = reaction.copy(
                reactionType = request.reactionType,
                updatedAt = LocalDateTime.now()
            )
            val savedReaction = commentReactionRepository.save(updatedReaction)
            
            // Update comment reaction counts
            updateCommentReactionCounts(commentId)
            
            return toCommentReactionResponse(savedReaction)
        } else {
            // Create new reaction
            val reaction = CommentReaction(
                comment = comment,
                userId = userId,
                reactionType = request.reactionType
            )
            val savedReaction = commentReactionRepository.save(reaction)
            
            // Update comment reaction counts
            updateCommentReactionCounts(commentId)
            
            return toCommentReactionResponse(savedReaction)
        }
    }

    fun removeReaction(commentId: Long, userId: Long): String {
        val reaction = commentReactionRepository.findByCommentIdAndUserId(commentId, userId)
            .orElseThrow { IllegalArgumentException("Reaction not found") }
            
        commentReactionRepository.delete(reaction)
        
        // Update comment reaction counts
        updateCommentReactionCounts(commentId)
        
        return "Reaction removed successfully"
    }

    fun pinComment(commentId: Long): CommentResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("Comment not found") }
            
        val updatedComment = comment.copy(
            isPinned = true,
            updatedAt = LocalDateTime.now()
        )
        
        commentRepository.save(updatedComment)
        return toCommentResponse(updatedComment)
    }

    fun unpinComment(commentId: Long): CommentResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("Comment not found") }
            
        val updatedComment = comment.copy(
            isPinned = false,
            updatedAt = LocalDateTime.now()
        )
        
        commentRepository.save(updatedComment)
        return toCommentResponse(updatedComment)
    }

    fun reportComment(commentId: Long): String {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("Comment not found") }
            
        val updatedComment = comment.copy(
            isReported = true,
            updatedAt = LocalDateTime.now()
        )
        
        commentRepository.save(updatedComment)
        return "Comment reported successfully"
    }

    fun getCommentStats(postId: Long): CommentStatsResponse {
        val totalComments = commentRepository.countByPostIdAndIsDeletedFalse(postId)
        val activeThreads = commentThreadRepository.countActiveThreadsByPostId(postId)
        
        // Get user stats
        val topAuthors = commentRepository.findTopCommentAuthors(PageRequest.of(0, 10))
            .map { row ->
                UserCommentStats(
                    userId = (row[0] as Number).toLong(),
                    username = "", // Would need to fetch from user service
                    commentCount = (row[1] as Number).toLong(),
                    likeCount = 0, // Would need to calculate from reactions
                    replyCount = 0 // Would need to calculate from comments
                )
            }
        
        return CommentStatsResponse(
            totalComments = totalComments,
            activeThreads = activeThreads,
            totalParticipants = 0, // Would need to calculate from threads
            averageCommentsPerThread = if (activeThreads > 0) totalComments.toDouble() / activeThreads else 0.0,
            mostActiveUsers = topAuthors,
            mostCommentedPosts = emptyList() // This is for a specific post
        )
    }

    private fun generateThreadId(): Long {
        // Simple timestamp-based thread ID generation
        return System.currentTimeMillis()
    }

    private fun createCommentThread(comment: Comment) {
        val thread = CommentThread(
            postId = comment.postId,
            rootCommentId = comment.id ?: 0,
            commentCount = 1,
            participantCount = 1,
            lastCommentAt = comment.createdAt,
            lastCommentId = comment.id
        )
        commentThreadRepository.save(thread)
    }

    private fun updateCommentThread(threadId: Long) {
        val thread = commentThreadRepository.findById(threadId).orElse(null)
        thread?.let {
            val commentCount = commentRepository.countByThreadIdAndIsDeletedFalse(threadId)
            val lastComment = commentRepository.findByThreadIdOrderByCreatedAtAsc(threadId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
            
            val updatedThread = it.copy(
                commentCount = commentCount,
                lastCommentAt = lastComment.content.firstOrNull()?.createdAt,
                lastCommentId = lastComment.content.firstOrNull()?.id,
                updatedAt = LocalDateTime.now()
            )
            
            commentThreadRepository.save(updatedThread)
        }
    }

    private fun handleCommentAttachments(comment: Comment, attachments: List<CommentAttachmentRequest>) {
        attachments.forEach { attachment ->
            commentAttachmentRepository.save(CommentAttachment(
                comment = comment,
                filename = attachment.filename,
                originalFilename = attachment.originalFilename,
                mimeType = attachment.mimeType,
                fileSize = attachment.fileSize,
                filePath = attachment.filePath,
                thumbnailPath = attachment.thumbnailPath,
                type = attachment.type,
                displayOrder = attachment.displayOrder
            ))
        }
    }

    private fun updateCommentReactionCounts(commentId: Long) {
        val likeCount = commentReactionRepository.countByCommentIdAndReactionType(commentId, ReactionType.LIKE)
        val dislikeCount = commentReactionRepository.countByCommentIdAndReactionType(commentId, ReactionType.DISLIKE)
        
        commentRepository.updateLikeCount(commentId, likeCount)
        commentRepository.updateDislikeCount(commentId, dislikeCount)
    }

    private fun buildCommentTree(rootComment: Comment, userId: Long? = null): CommentResponse {
        val replies = commentRepository.findByParentIdOrderByCreatedAtAsc(rootComment.id ?: 0)
        val replyResponses = replies.map { buildCommentTree(it, userId) }
        
        return toCommentResponse(rootComment, userId, replyResponses)
    }

    private fun toCommentResponse(comment: Comment, userId: Long? = null, replies: List<CommentResponse> = emptyList()): CommentResponse {
        val attachments = commentAttachmentRepository.findByCommentIdOrderByDisplayOrder(comment.id ?: 0)
            .map { toAttachmentResponse(it) }
        
        val userReaction = userId?.let { uid ->
            commentReactionRepository.findByCommentIdAndUserId(comment.id ?: 0, uid)
                .map { it.reactionType.name }
                .orElse(null)
        }
        
        return CommentResponse(
            id = comment.id ?: 0,
            content = comment.content,
            postId = comment.postId,
            authorId = comment.authorId,
            authorName = "", // Would fetch from user service
            authorAvatar = "", // Would fetch from user service
            parentId = comment.parentId,
            threadId = comment.threadId,
            level = comment.level,
            likeCount = comment.likeCount,
            dislikeCount = comment.dislikeCount,
            replyCount = comment.replyCount,
            isEdited = comment.isEdited,
            isDeleted = comment.isDeleted,
            isPinned = comment.isPinned,
            isReported = comment.isReported,
            editedContent = comment.editedContent,
            editedAt = comment.editedAt?.toString(),
            createdAt = comment.createdAt.toString(),
            updatedAt = comment.updatedAt.toString(),
            attachments = attachments,
            replies = replies,
            userReaction = userReaction,
            canReply = comment.canReply(),
            canEdit = userId != null && comment.authorId == userId && !comment.isDeleted,
            canDelete = userId != null && comment.authorId == userId && !comment.isDeleted
        )
    }

    private fun toAttachmentResponse(attachment: CommentAttachment): AttachmentResponse {
        return AttachmentResponse(
            id = attachment.id ?: 0,
            filename = attachment.filename,
            originalFilename = attachment.originalFilename,
            mimeType = attachment.mimeType,
            fileSize = attachment.fileSize,
            filePath = attachment.filePath,
            thumbnailPath = attachment.thumbnailPath,
            type = attachment.type.name,
            displayOrder = attachment.displayOrder,
            createdAt = attachment.createdAt.toString()
        )
    }

    private fun toCommentReactionResponse(reaction: CommentReaction): CommentReactionResponse {
        return CommentReactionResponse(
            id = reaction.id ?: 0,
            commentId = reaction.comment.id ?: 0,
            userId = reaction.userId,
            reactionType = reaction.reactionType.name,
            createdAt = reaction.createdAt.toString()
        )
    }
}