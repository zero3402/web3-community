package com.web3community.post.application.service

import com.web3community.post.domain.service.PostDomainService
import com.web3community.post.application.dto.*
import com.web3community.post.domain.model.Post
import com.web3community.sharedkernel.domain.PostId
import com.web3community.sharedkernel.domain.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@Service
@Transactional
class PostApplicationService(
    private val postDomainService: PostDomainService
) {

    fun createPost(request: CreatePostRequest, authorId: Long): PostResponse {
        val post = postDomainService.createPost(
            title = request.title,
            content = request.content,
            authorId = UserId.of(authorId),
            categoryId = request.categoryId
        )
        
        // 태그 추가
        request.tags?.forEach { tagName ->
            post.addTag(tagName)
        }
        
        // 첨부파일 추가
        request.attachments?.forEach { attachment ->
            post.addAttachment(
                filename = attachment.filename,
                originalFilename = attachment.originalFilename,
                mimeType = attachment.mimeType,
                fileSize = attachment.fileSize,
                filePath = attachment.filePath,
                type = attachment.type,
                displayOrder = attachment.displayOrder
            )
        }
        
        return post.toResponse()
    }

    fun updatePost(postId: Long, request: UpdatePostRequest, userId: Long): PostResponse {
        val updatedPost = postDomainService.updatePost(
            postId = PostId.of(postId),
            title = request.title,
            content = request.content,
            categoryId = request.categoryId,
            userId = UserId.of(userId)
        )
        
        // 태그 업데이트
        updatedPost.tags.clear()
        request.tags?.forEach { tagName ->
            updatedPost.addTag(tagName)
        }
        
        // 첨부파일 업데이트
        request.attachments?.forEach { attachment ->
            updatedPost.addAttachment(
                filename = attachment.filename,
                originalFilename = attachment.originalFilename,
                mimeType = attachment.mimeType,
                fileSize = attachment.fileSize,
                filePath = attachment.filePath,
                type = attachment.type,
                displayOrder = attachment.displayOrder
            )
        }
        
        return updatedPost.toResponse()
    }

    fun getPostById(postId: Long): PostResponse {
        val post = postDomainService.getPostById(PostId.of(postId))
        return post.toResponse()
    }

    fun getPostsByAuthor(authorId: Long, page: Int = 0, size: Int = 20): PostListResponse {
        val posts = postDomainService.getPostsByAuthor(UserId.of(authorId), page, size)
        return PostListResponse(
            posts = posts.content.map { it.toResponse() },
            total = posts.totalElements,
            page = page,
            size = size
        )
    }

    fun getPostsByCategory(categoryId: Long, page: Int = 0, size: Int = 20): PostListResponse {
        val posts = postDomainService.getPostsByCategory(categoryId, page, size)
        return PostListResponse(
            posts = posts.content.map { it.toResponse() },
            total = posts.totalElements,
            page = page,
            size = size
        )
    }

    fun searchPosts(query: String, page: Int = 0, size: Int = 20): PostListResponse {
        val posts = postDomainService.searchPosts(query, page, size)
        return PostListResponse(
            posts = posts.content.map { it.toResponse() },
            total = posts.totalElements,
            page = page,
            size = size
        )
    }

    fun publishPost(postId: Long, userId: Long): PostResponse {
        val post = postDomainService.publishPost(PostId.of(postId), UserId.of(userId))
        return post.toResponse()
    }

    fun archivePost(postId: Long, userId: Long): PostResponse {
        val post = postDomainService.archivePost(PostId.of(postId), UserId.of(userId))
        return post.toResponse()
    }

    fun deletePost(postId: Long, userId: Long): String {
        return postDomainService.deletePost(PostId.of(postId), UserId.of(userId))
    }

    fun likePost(postId: Long, userId: Long): PostResponse {
        val post = postDomainService.likePost(PostId.of(postId), UserId.of(userId))
        return post.toResponse()
    }

    fun unlikePost(postId: Long, userId: Long): PostResponse {
        val post = postDomainService.unlikePost(PostId.of(postId), UserId.of(userId))
        return post.toResponse()
    }

    fun sharePost(postId: Long, userId: Long): PostResponse {
        val post = postDomainService.sharePost(PostId.of(postId), UserId.of(userId))
        return post.toResponse()
    }

    fun getFeaturedPosts(limit: Int = 5): List<PostResponse> {
        val posts = postDomainService.getFeaturedPosts(limit)
        return posts.map { it.toResponse() }
    }

    fun getPinnedPosts(limit: Int = 5): List<PostResponse> {
        val posts = postDomainService.getPinnedPosts(limit)
        return posts.map { it.toResponse() }
    }
}

// 확장 함수
fun Post.toResponse(): PostResponse {
    return PostResponse(
        id = this.id.value,
        title = this.title,
        content = this.content,
        authorId = this.authorId.value,
        categoryId = this.categoryId,
        featuredImageUrl = this.featuredImageUrl,
        excerpt = this.excerpt,
        status = this.status.name,
        isPinned = this.isPinned,
        isFeatured = this.isFeatured,
        viewCount = this.viewCount,
        likeCount = this.likeCount,
        commentCount = this.commentCount,
        shareCount = this.shareCount,
        tags = this.tags.map { 
            com.web3community.post.application.dto.TagResponse(
                id = it.tag.id.value,
                name = it.tag.name,
                slug = it.tag.slug,
                description = it.tag.description,
                postCount = it.tag.postCount
            )
        },
        attachments = this.attachments.map { 
            com.web3community.post.application.dto.AttachmentResponse(
                id = it.id.value,
                filename = it.filename,
                originalFilename = it.originalFilename,
                mimeType = it.mimeType,
                fileSize = it.fileSize,
                filePath = it.filePath,
                thumbnailPath = it.thumbnailPath,
                type = it.type.name,
                displayOrder = it.displayOrder,
                createdAt = it.createdAt.toString()
            )
        },
        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString()
    )
}