package com.web3community.board.domain.repository

import com.web3community.board.domain.entity.Reaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/**
 * 반응(좋아요/싫어요) JPA 리포지토리
 */
interface ReactionRepository : JpaRepository<Reaction, Long> {

    /**
     * 특정 게시글에 대한 사용자 반응 조회
     * - 반응 존재 여부 확인 및 기존 반응 타입 조회에 사용
     */
    fun findByBoardIdAndUserId(boardId: Long, userId: Long): Optional<Reaction>

    /**
     * 특정 게시글에 대한 사용자 반응 삭제
     * - 반응 취소 시 사용
     */
    fun deleteByBoardIdAndUserId(boardId: Long, userId: Long)
}
