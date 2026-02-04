package com.web3community.comment.repository

import com.web3community.comment.entity.CommentAttachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentAttachmentRepository : JpaRepository<CommentAttachment, Long> {
    
    fun findByCommentIdOrderByDisplayOrder(commentId: Long): List<CommentAttachment>
    
    fun findByCommentIdAndType(commentId: Long, type: com.web3community.comment.entity.AttachmentType): List<CommentAttachment>
    
    fun findByCommentIdAndTypeIn(commentId: Long, types: List<com.web3community.comment.entity.AttachmentType>): List<CommentAttachment>
    
    fun deleteByCommentId(commentId: Long): Void
}