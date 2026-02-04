package com.web3community.post.service

import com.web3community.post.dto.*
import com.web3community.post.entity.*
import com.web3community.post.repository.*
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.stream.Collectors

@Service
@Transactional
class PostService(
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val postTagRepository: PostTagRepository,
    private val postAttachmentRepository: PostAttachmentRepository,
    private val postMetricRepository: PostMetricRepository
) {

    @CacheEvict(value = ["posts", "post-lists"], allEntries = true)
    fun createPost(request: PostCreateRequest, authorId: Long): PostResponse {
        val category = categoryRepository.findById(request.categoryId.toLongOrNull() ?: 0L)
            .orElseThrow { IllegalArgumentException("Category not found") }

        val post = Post(
            title = request.title.trim(),
            content = request.content.trim(),
            authorId = authorId,
            categoryId = request.categoryId.toLongOrNull() ?: 0L,
            featuredImageUrl = request.featuredImageUrl,
            excerpt = request.excerpt,
            status = request.status,
            isPinned = request.isPinned,
            isFeatured = request.isFeatured,
            publishedAt = if (request.status == PostStatus.PUBLISHED) LocalDateTime.now() else null,
            createdBy = authorId,
            updatedBy = authorId
        )

        val savedPost = postRepository.save(post)
        
        // Handle tags
        if (request.tags.isNotEmpty()) {
            handlePostTags(savedPost, request.tags)
        }
        
        // Handle attachments
        if (request.attachments.isNotEmpty()) {
            handlePostAttachments(savedPost, request.attachments)
        }
        
        // Initialize metrics
        initializePostMetrics(savedPost)
        
        return toPostResponse(savedPost)
    }

    @CacheEvict(value = ["posts", "post-lists"], allEntries = true)
    fun updatePost(postId: Long, request: PostUpdateRequest, userId: Long): PostResponse {
        val post = postRepository.findById(postId)
            .orElseThrow { IllegalArgumentException("Post not found") }
            
        if (post.authorId != userId) {
            throw IllegalArgumentException("Not authorized to update this post")
        }

        val category = categoryRepository.findById(request.categoryId.toLongOrNull() ?: 0L)
            .orElseThrow { IllegalArgumentException("Category not found") }

        val updatedPost = post.copy(
            title = request.title.trim(),
            content = request.content.trim(),
            categoryId = request.categoryId.toLongOrNull() ?: 0L,
            featuredImageUrl = request.featuredImageUrl,
            excerpt = request.excerpt,
            status = request.status,
            isPinned = request.isPinned,
            isFeatured = request.isFeatured,
            publishedAt = if (request.status == PostStatus.PUBLISHED && post.publishedAt == null) 
                LocalDateTime.now() else post.publishedAt,
            updatedAt = LocalDateTime.now(),
            updatedBy = userId
        )

        val savedPost = postRepository.save(updatedPost)
        
        // Handle tags update
        postTagRepository.deleteByPostId(postId)
        if (request.tags.isNotEmpty()) {
            handlePostTags(savedPost, request.tags)
        }
        
        return toPostResponse(savedPost)
    }

    @CacheEvict(value = ["posts", "post-lists"], allEntries = true)
    fun deletePost(postId: Long, userId: Long): String {
        val post = postRepository.findById(postId)
            .orElseThrow { IllegalArgumentException("Post not found") }
            
        if (post.authorId != userId) {
            throw IllegalArgumentException("Not authorized to delete this post")
        }

        val deletedPost = post.copy(
            status = PostStatus.DELETED,
            updatedAt = LocalDateTime.now(),
            updatedBy = userId
        )
        
        postRepository.save(deletedPost)
        return "Post deleted successfully"
    }

    @Cacheable(value = ["posts"], key = "#postId")
    fun getPostById(postId: Long): PostResponse {
        val post = postRepository.findById(postId)
            .orElseThrow { IllegalArgumentException("Post not found") }
            
        if (post.status == PostStatus.DELETED) {
            throw IllegalArgumentException("Post not found")
        }
        
        // Increment view count
        incrementPostMetrics(postId, MetricType.VIEWS)
        
        return toPostResponse(post)
    }

    @Cacheable(value = ["post-lists"], key = "#request.toString() + #pageable.pageNumber + #pageable.pageSize")
    fun getPosts(request: PostSearchRequest, pageable: Pageable): PostListResponse {
        val page = when {
            request.query?.isNotEmpty() == true && request.tags.isNotEmpty() -> {
                // Complex search with tags
                postRepository.searchByContent(request.query, pageable)
            }
            request.query?.isNotEmpty() == true -> {
                postRepository.searchByContent(request.query, pageable)
            }
            request.tags.isNotEmpty() -> {
                postRepository.findByTagNames(request.tags, pageable)
            }
            else -> {
                postRepository.findByMultipleCriteria(
                    status = request.status,
                    authorId = request.authorId?.toLongOrNull(),
                    categoryId = request.categoryId?.toLongOrNull(),
                    isPinned = request.isPinned,
                    isFeatured = request.isFeatured,
                    query = request.query,
                    dateFrom = request.dateFrom?.let { LocalDateTime.parse(it) },
                    dateTo = request.dateTo?.let { LocalDateTime.parse(it) },
                    pageable = pageable
                )
            }
        }
        
        return PostListResponse(
            posts = page.content.map { toPostResponse(it) },
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

    @Cacheable(value = ["post-lists"], key = "'author-' + #authorId + '-page-' + #page")
    fun getPostsByAuthor(authorId: Long, page: Int = 0, size: Int = 10): PostListResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val pageResult = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable)
        
        return PostListResponse(
            posts = pageResult.content.map { toPostResponse(it) },
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

    @Cacheable(value = ["post-lists"], key = "'category-' + #categoryId + '-page-' + #page")
    fun getPostsByCategory(categoryId: Long, page: Int = 0, size: Int = 10): PostListResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val pageResult = postRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId, pageable)
        
        return PostListResponse(
            posts = pageResult.content.map { toPostResponse(it) },
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

    fun getFeaturedPosts(limit: Int = 5): List<PostResponse> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        return postRepository.findByIsFeaturedTrueOrderByCreatedAtDesc(pageable)
            .content.map { toPostResponse(it) }
    }

    fun getPinnedPosts(limit: Int = 5): List<PostResponse> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        return postRepository.findByIsPinnedTrueOrderByCreatedAtDesc(pageable)
            .content.map { toPostResponse(it) }
    }

    private fun handlePostTags(post: Post, tagNames: List<String>) {
        tagNames.forEach { tagName ->
            val tag = tagRepository.findByName(tagName.trim())
                .orElseGet {
                    tagRepository.save(Tag(
                        name = tagName.trim(),
                        slug = tagName.trim().lowercase().replace(" ", "-")
                    ))
                }
            
            postTagRepository.save(PostTag(post = post, tag = tag))
        }
    }

    private fun handlePostAttachments(post: Post, attachments: List<PostAttachmentRequest>) {
        attachments.forEach { attachment ->
            postAttachmentRepository.save(PostAttachment(
                post = post,
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

    private fun initializePostMetrics(post: Post) {
        listOf(MetricType.VIEWS, MetricType.LIKES, MetricType.SHARES, MetricType.COMMENTS, MetricType.BOOKMARKS)
            .forEach { metricType ->
                postMetricRepository.save(PostMetric(
                    post = post,
                    metricType = metricType,
                    count = 0
                ))
            }
    }

    private fun incrementPostMetrics(postId: Long, metricType: MetricType) {
        postMetricRepository.findByPostIdAndMetricType(postId, metricType)
            .ifPresent { metric ->
                val updatedMetric = metric.copy(
                    count = metric.count + 1,
                    lastUpdatedAt = LocalDateTime.now()
                )
                postMetricRepository.save(updatedMetric)
            }
    }

    private fun toPostResponse(post: Post): PostResponse {
        val tags = postTagRepository.findByPostId(post.id ?: 0)
            .map { it.tag }
            .map { TagResponse(
                id = it.id ?: 0,
                name = it.name,
                slug = it.slug,
                description = it.description,
                postCount = it.postCount
            ) }

        val attachments = postAttachmentRepository.findByPostIdOrderByDisplayOrder(post.id ?: 0)
            .map { AttachmentResponse(
                id = it.id ?: 0,
                filename = it.filename,
                originalFilename = it.originalFilename,
                mimeType = it.mimeType,
                fileSize = it.fileSize,
                filePath = it.filePath,
                thumbnailPath = it.thumbnailPath,
                type = it.type.name,
                displayOrder = it.displayOrder,
                createdAt = it.createdAt.toString()
            ) }

        val metrics = postMetricRepository.findByPostId(post.id ?: 0)
            .associate { it.metricType.name.lowercase() to it.count }

        val category = categoryRepository.findById(post.categoryId).orElse(null)

        return PostResponse(
            id = post.id ?: 0,
            title = post.title,
            content = post.content,
            authorId = post.authorId,
            categoryId = post.categoryId,
            categoryName = category?.name,
            categorySlug = category?.slug,
            featuredImageUrl = post.featuredImageUrl,
            excerpt = post.excerpt,
            status = post.status.name,
            isPinned = post.isPinned,
            isFeatured = post.isFeatured,
            viewCount = post.viewCount,
            likeCount = post.likeCount,
            commentCount = post.commentCount,
            shareCount = post.shareCount,
            publishedAt = post.publishedAt?.toString(),
            createdAt = post.createdAt.toString(),
            updatedAt = post.updatedAt.toString(),
            tags = tags,
            attachments = attachments,
            metrics = metrics
        )
    }
}