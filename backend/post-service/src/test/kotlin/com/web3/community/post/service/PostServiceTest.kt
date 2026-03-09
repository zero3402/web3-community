package com.web3.community.post.service

import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import com.web3.community.post.document.Category
import com.web3.community.post.document.Post
import com.web3.community.post.dto.CreatePostRequest
import com.web3.community.post.dto.UpdatePostRequest
import com.web3.community.post.repository.PostRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.*

class PostServiceTest {

    @MockK
    private lateinit var postRepository: PostRepository

    @MockK
    private lateinit var categoryService: CategoryService

    private lateinit var postService: PostService

    private val testCategory = Category(id = "cat1", name = "General", displayOrder = 0)

    private val testPost = Post(
        id = "post1",
        title = "Test Post",
        content = "Test content",
        authorId = 1L,
        authorNickname = "tester",
        categoryId = "cat1",
        categoryName = "General",
        tags = listOf("kotlin", "spring")
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        postService = PostService(postRepository, categoryService)
    }

    @Test
    fun `createPost should save and return post`() {
        every { categoryService.getCategoryById("cat1") } returns testCategory
        every { postRepository.save(any()) } returns testPost

        val request = CreatePostRequest(title = "Test", content = "Content", categoryId = "cat1", tags = listOf("kotlin"))
        val result = postService.createPost(1L, "tester", request)

        assertEquals("Test Post", result.title)
        verify { postRepository.save(any()) }
    }

    @Test
    fun `getPostById should increment viewCount and return post`() {
        every { postRepository.findById("post1") } returns Optional.of(testPost)
        every { postRepository.save(any()) } returns testPost

        val result = postService.getPostById("post1")

        assertNotNull(result)
        verify { postRepository.save(any()) }
    }

    @Test
    fun `getPostById should throw for non-existent post`() {
        every { postRepository.findById("none") } returns Optional.empty()

        val exception = assertThrows<BusinessException> { postService.getPostById("none") }
        assertEquals(ErrorCode.POST_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `updatePost should update for owner`() {
        every { postRepository.findById("post1") } returns Optional.of(testPost)
        every { postRepository.save(any()) } returns testPost

        val request = UpdatePostRequest(title = "Updated")
        val result = postService.updatePost("post1", 1L, request)

        assertNotNull(result)
        verify { postRepository.save(any()) }
    }

    @Test
    fun `updatePost should throw FORBIDDEN for non-owner`() {
        every { postRepository.findById("post1") } returns Optional.of(testPost)

        val request = UpdatePostRequest(title = "Updated")

        val exception = assertThrows<BusinessException> { postService.updatePost("post1", 999L, request) }
        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    fun `deletePost should allow owner to delete`() {
        every { postRepository.findById("post1") } returns Optional.of(testPost)
        every { postRepository.delete(any()) } just runs

        postService.deletePost("post1", 1L, "USER")

        verify { postRepository.delete(testPost) }
    }

    @Test
    fun `deletePost should allow ADMIN to delete any post`() {
        every { postRepository.findById("post1") } returns Optional.of(testPost)
        every { postRepository.delete(any()) } just runs

        postService.deletePost("post1", 999L, "ADMIN")

        verify { postRepository.delete(testPost) }
    }

    @Test
    fun `deletePost should throw FORBIDDEN for non-owner non-admin`() {
        every { postRepository.findById("post1") } returns Optional.of(testPost)

        val exception = assertThrows<BusinessException> { postService.deletePost("post1", 999L, "USER") }
        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    fun `toggleLike should add like for new user`() {
        val post = testPost.copy(likeCount = 0, likedUserIds = mutableSetOf())
        every { postRepository.findById("post1") } returns Optional.of(post)
        every { postRepository.save(any()) } returns post

        postService.toggleLike("post1", 5L)

        verify { postRepository.save(match { it.likedUserIds.contains(5L) }) }
    }

    @Test
    fun `toggleLike should remove like for existing user`() {
        val post = testPost.copy(likeCount = 1, likedUserIds = mutableSetOf(5L))
        every { postRepository.findById("post1") } returns Optional.of(post)
        every { postRepository.save(any()) } returns post

        postService.toggleLike("post1", 5L)

        verify { postRepository.save(match { !it.likedUserIds.contains(5L) }) }
    }

    @Test
    fun `getPosts should return paginated results`() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(testPost), pageable, 1)
        every { postRepository.findByPublishedTrue(pageable) } returns page

        val result = postService.getPosts(null, null, null, pageable)

        assertEquals(1, result.content.size)
    }
}
