package com.web3.community.post.service

import com.web3.community.common.dto.PageResponse
import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import com.web3.community.post.document.Post
import com.web3.community.post.dto.*
import com.web3.community.post.repository.PostRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PostService(
    private val postRepository: PostRepository,
    private val categoryService: CategoryService
) {

    fun createPost(authorId: Long, authorNickname: String, request: CreatePostRequest): PostResponse {
        val category = categoryService.getCategoryById(request.categoryId)

        val post = Post(
            title = request.title,
            content = request.content,
            authorId = authorId,
            authorNickname = authorNickname,
            categoryId = category.id!!,
            categoryName = category.name,
            tags = request.tags
        )
        return PostResponse.from(postRepository.save(post))
    }

    fun getPosts(categoryId: String?, tag: String?, keyword: String?, pageable: Pageable): PageResponse<PostSummaryResponse> {
        val page = when {
            !categoryId.isNullOrBlank() -> postRepository.findByCategoryIdAndPublishedTrue(categoryId, pageable)
            !tag.isNullOrBlank() -> postRepository.findByTagsContaining(tag, pageable)
            !keyword.isNullOrBlank() -> postRepository.findByTitleContainingIgnoreCaseAndPublishedTrue(keyword, pageable)
            else -> postRepository.findByPublishedTrue(pageable)
        }

        return PageResponse(
            content = page.content.map { PostSummaryResponse.from(it) },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            isFirst = page.isFirst,
            isLast = page.isLast
        )
    }

    fun getPostById(id: String): PostResponse {
        val post = findPostById(id)
        post.viewCount++
        postRepository.save(post)
        return PostResponse.from(post)
    }

    fun updatePost(id: String, userId: Long, request: UpdatePostRequest): PostResponse {
        val post = findPostById(id)

        if (post.authorId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        request.title?.let { post.title = it }
        request.content?.let { post.content = it }
        request.tags?.let { post.tags = it }
        request.categoryId?.let {
            val category = categoryService.getCategoryById(it)
            post.categoryId = category.id!!
            post.categoryName = category.name
        }
        post.updatedAt = LocalDateTime.now()

        return PostResponse.from(postRepository.save(post))
    }

    fun deletePost(id: String, userId: Long, userRole: String) {
        val post = findPostById(id)

        if (post.authorId != userId && userRole != "ADMIN") {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        postRepository.delete(post)
    }

    fun toggleLike(id: String, userId: Long): PostResponse {
        val post = findPostById(id)

        if (post.likedUserIds.contains(userId)) {
            post.likedUserIds.remove(userId)
            post.likeCount--
        } else {
            post.likedUserIds.add(userId)
            post.likeCount++
        }

        return PostResponse.from(postRepository.save(post))
    }

    private fun findPostById(id: String): Post {
        return postRepository.findById(id)
            .orElseThrow { BusinessException(ErrorCode.POST_NOT_FOUND) }
    }
}
