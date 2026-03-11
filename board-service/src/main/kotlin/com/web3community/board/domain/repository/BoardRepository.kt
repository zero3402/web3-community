package com.web3community.board.domain.repository

import com.web3community.board.domain.entity.Board
import com.web3community.board.domain.entity.BoardStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 게시글 JPA 리포지토리
 *
 * - 기본 CRUD: JpaRepository에서 상속
 * - 커스텀 쿼리: @Query JPQL로 상태 필터링, 카운터 업데이트
 */
interface BoardRepository : JpaRepository<Board, Long> {

    /**
     * 상태별 게시글 목록 조회 (최신순 페이지네이션)
     * - 메인 피드: status=ACTIVE, 최신순 정렬
     */
    fun findByStatusOrderByCreatedAtDesc(status: BoardStatus, pageable: Pageable): Page<Board>

    /**
     * 특정 사용자의 게시글 목록 조회 (마이페이지)
     * - authorId + status 필터, 최신순 정렬
     */
    fun findByAuthorIdAndStatusOrderByCreatedAtDesc(
        authorId: Long,
        status: BoardStatus,
        pageable: Pageable
    ): Page<Board>

    /**
     * 조회수 1 증가 (DB 직접 업데이트 - Dirty Checking 불필요)
     */
    @Modifying
    @Query("UPDATE Board b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    fun incrementViewCount(@Param("id") id: Long)

    /**
     * 좋아요 수 증감
     * - delta: +1 (좋아요 추가), -1 (좋아요 취소)
     */
    @Modifying
    @Query("UPDATE Board b SET b.likeCount = b.likeCount + :delta WHERE b.id = :id")
    fun updateLikeCount(@Param("id") id: Long, @Param("delta") delta: Long)

    /**
     * 싫어요 수 증감
     * - delta: +1 (싫어요 추가), -1 (싫어요 취소)
     */
    @Modifying
    @Query("UPDATE Board b SET b.dislikeCount = b.dislikeCount + :delta WHERE b.id = :id")
    fun updateDislikeCount(@Param("id") id: Long, @Param("delta") delta: Long)
}
