package com.web3community.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 게시물 등록 요청 DTO
 * 게시물 작성 시 필요한 데이터를 담고 있음
 */
data class PostCreateRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(min = 1, max = 200, message = "제목은 1~200자 사이여야 합니다.")
    val title: String,
    
    @field:NotBlank(message = "내용은 필수입니다.")
    val content: String,
    
    @field:Size(max = 100, message = "카테고리는 100자 이하여야 합니다.")
    val category: String? = null,
    
    val tags: List<String> = emptyList(),
    
    @field:Size(max = 255, message = "썸네일 이미지 URL은 255자 이하여야 합니다.")
    val thumbnailImageUrl: String? = null,
    
    @field:Size(max = 50, message = "작성자 이름은 50자 이하여야 합니다.")
    val authorName: String? = null,
    
    @field:Size(max = 255, message = "작성자 프로필 이미지는 255자 이하여야 합니다.")
    val authorProfileImage: String? = null,
    
    val status: PostStatus = PostStatus.PUBLISHED,
    
    val featured: Boolean = false,
    
    val allowComments: Boolean = true,
    
    @field:Size(max = 100, message = "메타 설명은 100자 이하여야 합니다.")
    val metaDescription: String? = null
)

/**
 * 게시물 정보 수정 요청 DTO
 * 모든 필드를 수정할 수 있는 전체 수정용 DTO
 */
data class PostUpdateRequest(
    @field:Size(min = 1, max = 200, message = "제목은 1~200자 사이여야 합니다.")
    val title: String? = null,
    
    val content: String? = null,
    
    @field:Size(max = 100, message = "카테고리는 100자 이하여야 합니다.")
    val category: String? = null,
    
    val tags: List<String>? = null,
    
    @field:Size(max = 255, message = "썸네일 이미지 URL은 255자 이하여야 합니다.")
    val thumbnailImageUrl: String? = null,
    
    @field:Size(max = 50, message = "작성자 이름은 50자 이하여야 합니다.")
    val authorName: String? = null,
    
    @field:Size(max = 255, message = "작성자 프로필 이미지는 255자 이하여야 합니다.")
    val authorProfileImage: String? = null,
    
    val status: PostStatus? = null,
    
    val featured: Boolean? = null,
    
    val allowComments: Boolean? = null,
    
    @field:Size(max = 100, message = "메타 설명은 100자 이하여야 합니다.")
    val metaDescription: String? = null
)

/**
 * 게시물 부분 정보 수정 요청 DTO
 * 특정 필드만 수정할 수 있는 부분 수정용 DTO
 */
data class PostPatchRequest(
    @field:Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    val title: String? = null,
    
    @field:Size(max = 100, message = "카테고리는 100자 이하여야 합니다.")
    val category: String? = null,
    
    val tags: List<String>? = null,
    
    @field:Size(max = 255, message = "썸네일 이미지 URL은 255자 이하여야 합니다.")
    val thumbnailImageUrl: String? = null,
    
    val featured: Boolean? = null,
    
    val allowComments: Boolean? = null,
    
    @field:Size(max = 100, message = "메타 설명은 100자 이하여야 합니다.")
    val metaDescription: String? = null
)

/**
 * 게시물 상태 변경 요청 DTO
 */
data class PostStatusChangeRequest(
    @field:NotBlank(message = "상태는 필수입니다.")
    val status: PostStatus,
    
    @field:Size(max = 500, message = "상태 변경 사유는 500자 이하여야 합니다.")
    val reason: String? = null
)

/**
 * 게시물 응답 DTO
 * 클라이언트에게 전달되는 게시물 정보
 */
data class PostResponse(
    val id: String,
    val title: String,
    val content: String,
    val category: String?,
    val tags: List<String>,
    val thumbnailImageUrl: String?,
    val authorId: String,
    val authorName: String?,
    val authorProfileImage: String?,
    val status: PostStatus,
    val featured: Boolean,
    val allowComments: Boolean,
    val metaDescription: String?,
    val viewCount: Long,
    val likeCount: Long,
    val dislikeCount: Long,
    val commentCount: Long,
    val createdAt: String,
    val updatedAt: String,
    val publishedAt: String?
)

/**
 * 게시물 상세 응답 DTO
 * 추가 정보를 포함한 상세 게시물 정보
 */
data class PostDetailedResponse(
    val post: PostResponse,
    val comments: List<CommentSummary>,
    val relatedPosts: List<PostResponse>,
    val tagsInfo: Map<String, Long>,
    val categoryInfo: Map<String, Long>
)

/**
 * 댓글 요약 DTO
 */
data class CommentSummary(
    val id: String,
    val content: String,
    val authorId: String,
    val authorName: String?,
    val likeCount: Long,
    val createdAt: String
)

/**
 * 게시물 통계 응답 DTO
 */
data class PostStatsResponse(
    val totalPosts: Long,
    val publishedPosts: Long,
    val draftPosts: Long,
    val archivedPosts: Long,
    val featuredPosts: Long,
    val totalViews: Long,
    val totalLikes: Long,
    val totalComments: Long,
    val averageViewsPerPost: Double,
    val averageLikesPerPost: Double,
    val postsByCategory: Map<String, Long>,
    val postsByStatus: Map<PostStatus, Long>,
    val newPostsThisMonth: Long,
    val newPostsToday: Long
)

/**
 * 사용자 게시물 통계 응답 DTO
 */
data class UserPostStatsResponse(
    val userId: String,
    val totalPosts: Long,
    val publishedPosts: Long,
    val draftPosts: Long,
    val totalViews: Long,
    val totalLikes: Long,
    val totalDislikes: Long,
    val totalComments: Long,
    val averageViewsPerPost: Double,
    val averageLikesPerPost: Double,
    val mostPopularPost: PostResponse?,
    val recentPosts: List<PostResponse>
)

/**
 * 게시물 상태 enum
 */
enum class PostStatus {
    DRAFT,      // 임시 저장
    PUBLISHED,  // 발행됨
    ARCHIVED,   // 보관됨
    DELETED     // 삭제됨
}

/**
 * 게시물 정렬 기준 enum
 */
enum class PostSortBy {
    CREATED_AT,
    UPDATED_AT,
    PUBLISHED_AT,
    TITLE,
    VIEW_COUNT,
    LIKE_COUNT,
    COMMENT_COUNT,
    AUTHOR_NAME,
    CATEGORY
}

/**
 * 게시물 검색 타입 enum
 */
enum class PostSearchType {
    TITLE,      // 제목 검색
    CONTENT,    // 내용 검색
    TAGS,       // 태그 검색
    AUTHOR,     // 작성자 검색
    ALL         // 전체 검색
}

/**
 * 게시물 도큐먼트
 * MongoDB에 저장될 게시물 데이터 모델
 */
@Document(collection = "posts")
data class PostDocument(
    @Id
    val id: String? = null,
    
    val title: String,
    val content: String,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val thumbnailImageUrl: String? = null,
    
    val authorId: String,
    val authorName: String? = null,
    val authorProfileImage: String? = null,
    
    val status: PostStatus = PostStatus.DRAFT,
    val featured: Boolean = false,
    val allowComments: Boolean = true,
    val metaDescription: String? = null,
    
    val viewCount: Long = 0,
    val likeCount: Long = 0,
    val dislikeCount: Long = 0,
    val commentCount: Long = 0,
    
    val likedBy: List<String> = emptyList(),
    val dislikedBy: List<String> = emptyList(),
    
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val publishedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null
) {
    /**
     * 도큐먼트를 PostResponse DTO로 변환
     */
    fun toResponse(): PostResponse {
        return PostResponse(
            id = id ?: "",
            title = title,
            content = content,
            category = category,
            tags = tags,
            thumbnailImageUrl = thumbnailImageUrl,
            authorId = authorId,
            authorName = authorName,
            authorProfileImage = authorProfileImage,
            status = status,
            featured = featured,
            allowComments = allowComments,
            metaDescription = metaDescription,
            viewCount = viewCount,
            likeCount = likeCount,
            dislikeCount = dislikeCount,
            commentCount = commentCount,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString(),
            publishedAt = publishedAt?.toString()
        )
    }
    
    /**
     * copy 메소드 확장
     */
    fun copy(
        id: String? = this.id,
        title: String = this.title,
        content: String = this.content,
        category: String? = this.category,
        tags: List<String> = this.tags,
        thumbnailImageUrl: String? = this.thumbnailImageUrl,
        authorId: String = this.authorId,
        authorName: String? = this.authorName,
        authorProfileImage: String? = this.authorProfileImage,
        status: PostStatus = this.status,
        featured: Boolean = this.featured,
        allowComments: Boolean = this.allowComments,
        metaDescription: String? = this.metaDescription,
        viewCount: Long = this.viewCount,
        likeCount: Long = this.likeCount,
        dislikeCount: Long = this.dislikeCount,
        commentCount: Long = this.commentCount,
        likedBy: List<String> = this.likedBy,
        dislikedBy: List<String> = this.dislikedBy,
        createdAt: LocalDateTime = this.createdAt,
        updatedAt: LocalDateTime = LocalDateTime.now(),
        publishedAt: LocalDateTime? = this.publishedAt,
        deletedAt: LocalDateTime? = this.deletedAt
    ): PostDocument {
        return PostDocument(
            id = id,
            title = title,
            content = content,
            category = category,
            tags = tags,
            thumbnailImageUrl = thumbnailImageUrl,
            authorId = authorId,
            authorName = authorName,
            authorProfileImage = authorProfileImage,
            status = status,
            featured = featured,
            allowComments = allowComments,
            metaDescription = metaDescription,
            viewCount = viewCount,
            likeCount = likeCount,
            dislikeCount = dislikeCount,
            commentCount = commentCount,
            likedBy = likedBy,
            dislikedBy = dislikedBy,
            createdAt = createdAt,
            updatedAt = updatedAt,
            publishedAt = publishedAt,
            deletedAt = deletedAt
        )
    }
}