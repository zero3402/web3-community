package com.web3community.board.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.web3community.board.domain.entity.Board
import com.web3community.board.domain.entity.BoardStatus
import com.web3community.board.domain.entity.Reaction
import com.web3community.board.domain.entity.ReactionType
import com.web3community.board.domain.repository.BoardRepository
import com.web3community.board.domain.repository.ReactionRepository
import com.web3community.board.dto.*
import com.web3community.common.exception.BusinessException
import com.web3community.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

/**
 * 게시판 비즈니스 로직 서비스
 *
 * ## 주요 기능
 * - 게시글 CRUD (Soft Delete)
 * - 좋아요/싫어요 반응 처리 (토글 방식)
 * - Redis 캐싱 (게시글 상세, TTL: 5분)
 * - Kafka 이벤트 발행 (board-events 토픽)
 */
@Service
@Transactional(readOnly = true)
class BoardService(
    private val boardRepository: BoardRepository,
    private val reactionRepository: ReactionRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(BoardService::class.java)

    companion object {
        private const val CACHE_KEY_PREFIX = "board:detail:"
        private const val CACHE_TTL_MINUTES = 5L
        private const val BOARD_TOPIC = "board-events"
    }

    /**
     * 게시글 목록 조회 (페이지네이션, 최신순)
     * - ACTIVE 상태 게시글만 반환
     */
    fun getBoards(page: Int, size: Int): BoardPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val boardPage = boardRepository.findByStatusOrderByCreatedAtDesc(BoardStatus.ACTIVE, pageable)

        return BoardPageResponse(
            content = boardPage.content.map { BoardResponse.from(it) },
            totalElements = boardPage.totalElements,
            totalPages = boardPage.totalPages,
            currentPage = page,
            pageSize = size,
            hasNext = boardPage.hasNext(),
            hasPrevious = boardPage.hasPrevious()
        )
    }

    /**
     * 게시글 상세 조회 (조회수 증가 + Redis 캐시)
     *
     * 처리 흐름:
     * 1. Redis 캐시 확인 → 있으면 반환 (캐시 히트)
     * 2. DB 조회 → 조회수 +1 → Redis 저장 → 반환 (캐시 미스)
     */
    @Transactional
    fun getBoard(boardId: Long): BoardResponse {
        val cacheKey = "$CACHE_KEY_PREFIX$boardId"

        // Redis 캐시 먼저 확인 (캐시 히트 시 DB 접근 없음)
        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) {
            return objectMapper.readValue(cached, BoardResponse::class.java)
        }

        val board = findActiveBoard(boardId)
        boardRepository.incrementViewCount(boardId)
        board.viewCount += 1  // 응답에 반영

        val response = BoardResponse.from(board)

        // Redis 캐싱 (5분 TTL)
        try {
            redisTemplate.opsForValue().set(
                cacheKey,
                objectMapper.writeValueAsString(response),
                Duration.ofMinutes(CACHE_TTL_MINUTES)
            )
        } catch (e: Exception) {
            logger.warn("Redis 캐시 저장 실패: boardId={}", boardId, e)
        }

        return response
    }

    /**
     * 게시글 작성
     * - Kafka로 CREATED 이벤트 발행
     */
    @Transactional
    fun createBoard(request: BoardCreateRequest, authorId: Long, authorNickname: String): BoardResponse {
        val board = Board(
            title = request.title,
            content = request.content,
            authorId = authorId,
            authorNickname = authorNickname,
            tags = request.tags
        )
        val saved = boardRepository.save(board)

        publishEvent("CREATED", saved.id, authorId)
        return BoardResponse.from(saved)
    }

    /**
     * 게시글 수정 (작성자 본인만 가능)
     * - Redis 캐시 무효화
     * - Kafka로 UPDATED 이벤트 발행
     */
    @Transactional
    fun updateBoard(boardId: Long, request: BoardUpdateRequest, userId: Long): BoardResponse {
        val board = findActiveBoard(boardId)
        checkAuthor(board, userId)

        board.title = request.title
        board.content = request.content
        board.tags = request.tags

        val saved = boardRepository.save(board)
        evictCache(boardId)
        publishEvent("UPDATED", saved.id, userId)

        return BoardResponse.from(saved)
    }

    /**
     * 게시글 삭제 (Soft Delete, 작성자 본인만 가능)
     * - status를 DELETED로 변경 (물리적 삭제 아님)
     * - Redis 캐시 무효화
     * - Kafka로 DELETED 이벤트 발행
     */
    @Transactional
    fun deleteBoard(boardId: Long, userId: Long) {
        val board = findActiveBoard(boardId)
        checkAuthor(board, userId)

        board.status = BoardStatus.DELETED
        boardRepository.save(board)
        evictCache(boardId)
        publishEvent("DELETED", boardId, userId)
    }

    /**
     * 반응(좋아요/싫어요) 처리 (토글 방식)
     *
     * 처리 흐름:
     * - 기존 반응 없음 → 새 반응 생성 (CREATED)
     * - 동일 반응 재요청 → 반응 취소 (DELETED)
     * - 다른 반응으로 변경 → 반응 타입 변경 (UPDATED)
     */
    @Transactional
    fun react(boardId: Long, request: ReactionRequest, userId: Long): ReactionResponse {
        val board = findActiveBoard(boardId)

        val existingReaction = reactionRepository.findByBoardIdAndUserId(boardId, userId)

        return if (existingReaction.isPresent) {
            val reaction = existingReaction.get()
            if (reaction.reactionType == request.reactionType) {
                // 동일 반응 → 취소
                reactionRepository.delete(reaction)
                adjustCount(board, reaction.reactionType, -1)
                boardRepository.save(board)
                evictCache(boardId)
                buildReactionResponse(boardId, userId, null, board, ReactionAction.DELETED)
            } else {
                // 반응 변경
                adjustCount(board, reaction.reactionType, -1)
                adjustCount(board, request.reactionType, 1)
                reaction.reactionType = request.reactionType
                reactionRepository.save(reaction)
                boardRepository.save(board)
                evictCache(boardId)
                buildReactionResponse(boardId, userId, request.reactionType, board, ReactionAction.UPDATED)
            }
        } else {
            // 새 반응 생성
            val reaction = Reaction(
                boardId = boardId,
                userId = userId,
                reactionType = request.reactionType
            )
            reactionRepository.save(reaction)
            adjustCount(board, request.reactionType, 1)
            boardRepository.save(board)
            evictCache(boardId)
            buildReactionResponse(boardId, userId, request.reactionType, board, ReactionAction.CREATED)
        }
    }

    /**
     * 내 게시글 목록 조회 (마이페이지)
     */
    fun getMyBoards(userId: Long, page: Int, size: Int): BoardPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val boardPage = boardRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(
            userId, BoardStatus.ACTIVE, pageable
        )

        return BoardPageResponse(
            content = boardPage.content.map { BoardResponse.from(it) },
            totalElements = boardPage.totalElements,
            totalPages = boardPage.totalPages,
            currentPage = page,
            pageSize = size,
            hasNext = boardPage.hasNext(),
            hasPrevious = boardPage.hasPrevious()
        )
    }

    // ─── 내부 헬퍼 메서드 ─────────────────────────────────────────────────────────

    /** ACTIVE 상태인 게시글 조회 (없거나 삭제된 경우 예외 발생) */
    private fun findActiveBoard(boardId: Long): Board {
        val board = boardRepository.findById(boardId)
            .orElseThrow { BusinessException(ErrorCode.BOARD_001) }

        if (board.status == BoardStatus.DELETED) {
            throw BusinessException(ErrorCode.BOARD_009)
        }
        return board
    }

    /** 게시글 작성자 본인 여부 확인 */
    private fun checkAuthor(board: Board, userId: Long) {
        if (board.authorId != userId) {
            throw BusinessException(ErrorCode.BOARD_002)
        }
    }

    /** 좋아요/싫어요 카운트 증감 */
    private fun adjustCount(board: Board, reactionType: ReactionType, delta: Long) {
        when (reactionType) {
            ReactionType.LIKE -> board.likeCount += delta
            ReactionType.DISLIKE -> board.dislikeCount += delta
        }
    }

    /** ReactionResponse 빌더 */
    private fun buildReactionResponse(
        boardId: Long,
        userId: Long,
        reactionType: ReactionType?,
        board: Board,
        action: ReactionAction
    ) = ReactionResponse(
        boardId = boardId.toString(),
        userId = userId.toString(),
        reactionType = reactionType,
        likeCount = board.likeCount,
        dislikeCount = board.dislikeCount,
        action = action
    )

    /** Redis 캐시 무효화 */
    private fun evictCache(boardId: Long) {
        try {
            redisTemplate.delete("$CACHE_KEY_PREFIX$boardId")
        } catch (e: Exception) {
            logger.warn("Redis 캐시 삭제 실패: boardId={}", boardId, e)
        }
    }

    /** Kafka 이벤트 발행 (실패해도 서비스 중단 없음) */
    private fun publishEvent(eventType: String, boardId: Long, userId: Long) {
        try {
            val event = mapOf(
                "eventType" to eventType,
                "boardId" to boardId,
                "userId" to userId
            )
            kafkaTemplate.send(BOARD_TOPIC, boardId.toString(), objectMapper.writeValueAsString(event))
        } catch (e: Exception) {
            logger.warn("Kafka 이벤트 발행 실패: eventType={}, boardId={}", eventType, boardId, e)
        }
    }
}
