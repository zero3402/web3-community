package com.web3.community.comment.service

import com.web3.community.comment.document.Comment
import com.web3.community.comment.dto.CreateCommentRequest
import com.web3.community.comment.dto.UpdateCommentRequest
import com.web3.community.comment.repository.CommentRepository
import com.web3.community.common.exception.BusinessException
import com.web3.community.common.exception.ErrorCode
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class CommentServiceTest {

    @MockK
    private lateinit var commentRepository: CommentRepository

    @MockK
    private lateinit var commentEventService: CommentEventService

    private lateinit var commentService: CommentService

    private val testComment = Comment(
        id = "c1",
        postId = "post1",
        parentId = null,
        depth = 0,
        authorId = 1L,
        authorNickname = "tester",
        content = "Test comment"
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        commentService = CommentService(commentRepository, commentEventService)
    }

    @Test
    fun `createComment should save root comment`() {
        every { commentRepository.save(any()) } returns Mono.just(testComment)
        every { commentEventService.publishCommentEvent(any()) } just runs

        val request = CreateCommentRequest(postId = "post1", content = "Test comment")

        StepVerifier.create(commentService.createComment(1L, "tester", request))
            .expectNextMatches { it.content == "Test comment" && it.depth == 0 }
            .verifyComplete()
    }

    @Test
    fun `createComment should save nested comment with depth`() {
        val parentComment = testComment.copy(id = "parent1", depth = 0)
        val childComment = testComment.copy(id = "child1", parentId = "parent1", depth = 1)

        every { commentRepository.findById("parent1") } returns Mono.just(parentComment)
        every { commentRepository.save(any()) } returns Mono.just(childComment)
        every { commentEventService.publishCommentEvent(any()) } just runs

        val request = CreateCommentRequest(postId = "post1", parentId = "parent1", content = "Reply")

        StepVerifier.create(commentService.createComment(1L, "tester", request))
            .expectNextMatches { it.depth == 1 }
            .verifyComplete()
    }

    @Test
    fun `updateComment should update for owner`() {
        val updatedComment = testComment.copy(content = "Updated")
        every { commentRepository.findById("c1") } returns Mono.just(testComment)
        every { commentRepository.save(any()) } returns Mono.just(updatedComment)

        val request = UpdateCommentRequest(content = "Updated")

        StepVerifier.create(commentService.updateComment("c1", 1L, request))
            .expectNextMatches { it.content == "Updated" }
            .verifyComplete()
    }

    @Test
    fun `updateComment should throw FORBIDDEN for non-owner`() {
        every { commentRepository.findById("c1") } returns Mono.just(testComment)

        val request = UpdateCommentRequest(content = "Updated")

        StepVerifier.create(commentService.updateComment("c1", 999L, request))
            .expectErrorMatches { it is BusinessException && (it as BusinessException).errorCode == ErrorCode.FORBIDDEN }
            .verify()
    }

    @Test
    fun `updateComment should throw for non-existent comment`() {
        every { commentRepository.findById("none") } returns Mono.empty()

        val request = UpdateCommentRequest(content = "Updated")

        StepVerifier.create(commentService.updateComment("none", 1L, request))
            .expectErrorMatches { it is BusinessException && (it as BusinessException).errorCode == ErrorCode.COMMENT_NOT_FOUND }
            .verify()
    }

    @Test
    fun `deleteComment should soft-delete for owner`() {
        val deletedComment = testComment.copy(deleted = true, content = "")
        every { commentRepository.findById("c1") } returns Mono.just(testComment)
        every { commentRepository.save(any()) } returns Mono.just(deletedComment)

        StepVerifier.create(commentService.deleteComment("c1", 1L))
            .verifyComplete()

        verify { commentRepository.save(match { it.deleted && it.content == "" }) }
    }

    @Test
    fun `deleteComment should throw FORBIDDEN for non-owner`() {
        every { commentRepository.findById("c1") } returns Mono.just(testComment)

        StepVerifier.create(commentService.deleteComment("c1", 999L))
            .expectErrorMatches { it is BusinessException && (it as BusinessException).errorCode == ErrorCode.FORBIDDEN }
            .verify()
    }

    @Test
    fun `toggleLike should add like`() {
        val comment = testComment.copy(likeCount = 0, likedUserIds = mutableSetOf())
        val likedComment = comment.copy(likeCount = 1, likedUserIds = mutableSetOf(5L))
        every { commentRepository.findById("c1") } returns Mono.just(comment)
        every { commentRepository.save(any()) } returns Mono.just(likedComment)

        StepVerifier.create(commentService.toggleLike("c1", 5L))
            .expectNextMatches { it.likeCount == 1L }
            .verifyComplete()
    }

    @Test
    fun `toggleLike should remove like`() {
        val comment = testComment.copy(likeCount = 1, likedUserIds = mutableSetOf(5L))
        val unlikedComment = comment.copy(likeCount = 0, likedUserIds = mutableSetOf())
        every { commentRepository.findById("c1") } returns Mono.just(comment)
        every { commentRepository.save(any()) } returns Mono.just(unlikedComment)

        StepVerifier.create(commentService.toggleLike("c1", 5L))
            .expectNextMatches { it.likeCount == 0L }
            .verifyComplete()
    }

    @Test
    fun `getCommentCount should return count`() {
        every { commentRepository.countByPostIdAndDeletedFalse("post1") } returns Mono.just(5L)

        StepVerifier.create(commentService.getCommentCount("post1"))
            .expectNext(5L)
            .verifyComplete()
    }

    @Test
    fun `getCommentsByPostId should return hierarchical comments`() {
        val root = testComment.copy(id = "r1", parentId = null)
        val child = testComment.copy(id = "c1", parentId = "r1", depth = 1)

        every { commentRepository.findByPostIdOrderByCreatedAtAsc("post1") } returns Flux.just(root, child)

        StepVerifier.create(commentService.getCommentsByPostId("post1"))
            .expectNextMatches { it.id == "r1" && it.children.size == 1 }
            .verifyComplete()
    }
}
