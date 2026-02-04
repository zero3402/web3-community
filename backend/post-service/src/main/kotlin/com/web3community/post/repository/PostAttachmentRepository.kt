package com.web3community.post.repository

import com.web3community.post.entity.PostAttachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostAttachmentRepository : JpaRepository<PostAttachment, Long> {
    
    fun findByPostIdOrderByDisplayOrder(postId: Long): List<PostAttachment>
    
    fun findByPostIdAndType(postId: Long, type: com.web3community.post.entity.AttachmentType): List<PostAttachment>
    
    fun findByPostIdAndTypeIn(postId: Long, types: List<com.web3community.post.entity.AttachmentType>): List<PostAttachment>
    
    fun deleteByPostId(postId: Long): Void
}