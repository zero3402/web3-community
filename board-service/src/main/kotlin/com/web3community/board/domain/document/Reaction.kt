package com.web3community.board.domain.document

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 반응(좋아요/싫어요) MongoDB Document
 *
 * ## 컬렉션 구조
 * MongoDB의 `reactions` 컬렉션에 저장되는 사용자 반응 문서입니다.
 *
 * ## 설계 결정
 * ### 별도 컬렉션으로 분리한 이유
 * - 게시글(Board) 문서에 reactions 배열로 내장(Embed)할 경우:
 *   - 인기 게시글의 반응이 수천 개가 되면 문서 크기가 16MB 제한에 근접
 *   - 특정 사용자의 반응 여부 조회 시 전체 배열 스캔 필요
 * - 별도 컬렉션 사용으로 확장성과 쿼리 효율성 확보
 *
 * ### 복합 유니크 인덱스의 역할
 * - `(boardId, userId)` UNIQUE 인덱스로 DB 레벨에서 중복 반응 방지
 * - 동시 요청(Race Condition) 상황에서도 단 하나만 성공 보장
 * - 애플리케이션 코드의 중복 체크(조회→삽입) 사이의 경합 조건 방지
 *
 * ### 반응 변경 처리 전략
 * - 동일 사용자가 좋아요 → 싫어요로 변경 시:
 *   1. 기존 Reaction 문서의 reactionType 필드 업데이트 (PATCH)
 *   2. Redis 카운터: 기존 타입 DECR + 새 타입 INCR (원자적 파이프라인)
 *   3. Board 문서의 likeCount/dislikeCount 업데이트
 *
 * ## 인덱스
 * - `(boardId, userId)` UNIQUE: 중복 반응 방지 + 빠른 존재 확인
 * - `boardId`: 게시글별 전체 반응 목록 조회
 *
 * @property id MongoDB ObjectId
 * @property boardId 반응 대상 게시글 ID (Board.id 참조)
 * @property userId 반응한 사용자 ID (user-service의 User.id 참조)
 * @property reactionType 반응 종류 (LIKE: 좋아요, DISLIKE: 싫어요)
 * @property createdAt 반응 최초 등록일시 (변경 시에도 갱신하지 않음)
 */
@Document(collection = "reactions")
// 복합 유니크 인덱스: 동일 사용자가 동일 게시글에 하나의 반응만 허용
@CompoundIndexes(
    CompoundIndex(
        name = "idx_reactions_boardId_userId_unique",
        def = "{'boardId': 1, 'userId': 1}",
        unique = true  // UNIQUE 제약: 중복 INSERT 시 DuplicateKeyException 발생
    )
)
data class Reaction(

    /** MongoDB 문서 고유 식별자 */
    @Id
    val id: String? = null,

    /**
     * 반응 대상 게시글 ID
     * - Board 컬렉션의 _id를 String으로 참조
     * - MongoDB는 레퍼런스를 DBRef 또는 Manual Reference로 관리
     * - 여기서는 단순 ID 참조(Manual Reference)를 사용하여 JOIN 없이 빠른 조회
     */
    val boardId: String,

    /**
     * 반응한 사용자 ID
     * - API Gateway의 JwtAuthFilter가 주입하는 X-User-Id 헤더 값
     * - user-service의 User.id와 일치
     */
    val userId: String,

    /**
     * 반응 종류
     * - LIKE: 좋아요 (Board.likeCount에 반영)
     * - DISLIKE: 싫어요 (Board.dislikeCount에 반영)
     *
     * 변경 시 upsert + $set 연산으로 기존 문서의 reactionType만 업데이트
     */
    val reactionType: ReactionType,

    /**
     * 반응 등록/변경 일시
     * - 최초 등록 시 @CreatedDate로 자동 설정
     * - 반응 타입 변경 시에도 createdAt은 유지 (변경 이력 불필요)
     */
    @CreatedDate
    val createdAt: LocalDateTime? = null
)

/**
 * 반응 타입 열거형
 *
 * @property LIKE 좋아요 반응 (Board.likeCount++)
 * @property DISLIKE 싫어요 반응 (Board.dislikeCount++)
 */
enum class ReactionType {
    /** 좋아요: 긍정적 반응 */
    LIKE,
    /** 싫어요: 부정적 반응 */
    DISLIKE
}
