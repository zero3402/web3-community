package com.web3community.board.config

import com.web3community.board.domain.document.Board
import com.web3community.board.domain.document.Reaction
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.ReactiveMongoTransactionManager
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener
import org.bson.Document
import jakarta.annotation.PostConstruct

/**
 * MongoDB Reactive 설정 클래스
 *
 * ## 역할
 * - ReactiveMongoTemplate 커스터마이징
 * - 컬렉션 인덱스 자동 생성 (애플리케이션 시작 시)
 * - MongoDB 트랜잭션 매니저 설정
 *
 * ## 인덱스 전략
 * ### boards 컬렉션
 * - `authorId`: 특정 사용자의 게시글 목록 조회 (마이페이지)
 * - `createdAt (DESC)`: 최신순 정렬 조회 (기본 정렬)
 * - `tags`: 태그 기반 게시글 필터링 (Multikey Index)
 * - `status + createdAt`: 활성 게시글 최신순 복합 조회 (가장 빈번한 쿼리)
 *
 * ### reactions 컬렉션
 * - `(boardId, userId)` UNIQUE: 동일 사용자의 동일 게시글 중복 반응 방지
 * - `boardId`: 특정 게시글의 모든 반응 조회
 *
 * @see ReactiveMongoTemplate
 */
@Configuration
class MongoConfig(
    // ReactiveMongoTemplate: 복잡한 쿼리, 집계, 인덱스 관리에 사용
    private val reactiveMongoTemplate: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(MongoConfig::class.java)

    /**
     * MongoDB 트랜잭션 매니저 등록
     *
     * MongoDB 4.0+ 레플리카셋 환경에서 멀티 도큐먼트 트랜잭션 지원.
     * 개발 환경(Standalone)에서는 트랜잭션 없이 동작하므로 주의.
     *
     * @param factory MongoDB 데이터베이스 팩토리 (자동 주입)
     * @return ReactiveMongoTransactionManager 트랜잭션 매니저 Bean
     */
    @Bean
    fun reactiveMongoTransactionManager(
        factory: ReactiveMongoDatabaseFactory
    ): ReactiveMongoTransactionManager {
        return ReactiveMongoTransactionManager(factory)
    }

    /**
     * 애플리케이션 시작 시 MongoDB 인덱스 자동 생성
     *
     * [PostConstruct]를 사용하여 Spring Context 초기화 완료 후 인덱스를 생성합니다.
     * 인덱스가 이미 존재하면 MongoDB가 자동으로 무시(idempotent)합니다.
     *
     * ## 인덱스 생성 순서
     * 1. boards 컬렉션 인덱스
     * 2. reactions 컬렉션 인덱스
     */
    @PostConstruct
    fun initializeIndexes() {
        logger.info("MongoDB 인덱스 초기화 시작...")
        createBoardIndexes()
        createReactionIndexes()
        logger.info("MongoDB 인덱스 초기화 완료")
    }

    /**
     * boards 컬렉션 인덱스 생성
     *
     * ### 생성되는 인덱스 목록
     * 1. `authorId_1`: 작성자별 게시글 목록 조회 (마이페이지)
     * 2. `createdAt_-1`: 최신순 정렬 (기본 목록 조회)
     * 3. `tags_1`: 태그 필터링 (Multikey Index - 배열 필드)
     * 4. `status_1_createdAt_-1`: 활성 게시글 최신순 복합 조회 (커버링 인덱스)
     */
    private fun createBoardIndexes() {
        // boards 컬렉션의 ReactiveIndexOperations 획득
        val indexOps: ReactiveIndexOperations = reactiveMongoTemplate.indexOps(Board::class.java)

        // 1. authorId 단일 인덱스: 작성자별 게시글 목록 조회
        // 사용 쿼리: db.boards.find({ authorId: "userId" })
        indexOps.ensureIndex(
            Index().on("authorId", Sort.Direction.ASC)
                .named("idx_boards_authorId")
        ).subscribe(
            { indexName -> logger.debug("인덱스 생성/확인: {}", indexName) },
            { ex -> logger.error("authorId 인덱스 생성 실패", ex) }
        )

        // 2. createdAt 내림차순 인덱스: 최신순 정렬
        // 사용 쿼리: db.boards.find({}).sort({ createdAt: -1 })
        indexOps.ensureIndex(
            Index().on("createdAt", Sort.Direction.DESC)
                .named("idx_boards_createdAt_desc")
        ).subscribe(
            { indexName -> logger.debug("인덱스 생성/확인: {}", indexName) },
            { ex -> logger.error("createdAt 인덱스 생성 실패", ex) }
        )

        // 3. tags Multikey 인덱스: 태그 기반 게시글 필터링
        // MongoDB는 배열 필드에 자동으로 Multikey Index 생성
        // 사용 쿼리: db.boards.find({ tags: "kotlin" })
        indexOps.ensureIndex(
            Index().on("tags", Sort.Direction.ASC)
                .named("idx_boards_tags")
        ).subscribe(
            { indexName -> logger.debug("인덱스 생성/확인: {}", indexName) },
            { ex -> logger.error("tags 인덱스 생성 실패", ex) }
        )

        // 4. (status, createdAt) 복합 인덱스: 활성 게시글 최신순 조회 (가장 빈번한 쿼리)
        // ESR Rule 적용: Equality(status) → Sort(createdAt)
        // 사용 쿼리: db.boards.find({ status: "ACTIVE" }).sort({ createdAt: -1 })
        indexOps.ensureIndex(
            CompoundIndexDefinition(
                Document()
                    .append("status", 1)       // status 오름차순 (Equality 조건)
                    .append("createdAt", -1)   // createdAt 내림차순 (Sort 조건)
            ).named("idx_boards_status_createdAt")
        ).subscribe(
            { indexName -> logger.debug("인덱스 생성/확인: {}", indexName) },
            { ex -> logger.error("status+createdAt 복합 인덱스 생성 실패", ex) }
        )

        // 5. (authorId, status, createdAt) 복합 인덱스: 마이페이지 활성 게시글 최신순
        // 사용 쿼리: db.boards.find({ authorId: "x", status: "ACTIVE" }).sort({ createdAt: -1 })
        indexOps.ensureIndex(
            CompoundIndexDefinition(
                Document()
                    .append("authorId", 1)
                    .append("status", 1)
                    .append("createdAt", -1)
            ).named("idx_boards_authorId_status_createdAt")
        ).subscribe(
            { indexName -> logger.debug("인덱스 생성/확인: {}", indexName) },
            { ex -> logger.error("authorId+status+createdAt 복합 인덱스 생성 실패", ex) }
        )
    }

    /**
     * reactions 컬렉션 인덱스 생성
     *
     * ### 생성되는 인덱스 목록
     * 1. `(boardId, userId)` UNIQUE: 중복 반응 방지 (동일 사용자/게시글 1회 제한)
     * 2. `boardId_1`: 게시글별 반응 목록 조회
     *
     * ### 복합 유니크 인덱스의 역할
     * - DB 레벨에서 중복을 방지하여 애플리케이션 코드의 중복 체크 로직을 보완
     * - 동시에 여러 요청이 들어와도 하나만 성공하도록 보장 (Race Condition 방지)
     */
    private fun createReactionIndexes() {
        val indexOps: ReactiveIndexOperations = reactiveMongoTemplate.indexOps(Reaction::class.java)

        // 1. (boardId, userId) 복합 유니크 인덱스: 중복 반응 방지
        // UNIQUE 제약으로 동일 (boardId, userId) 쌍의 두 번째 INSERT 시 DuplicateKeyException 발생
        // 사용 쿼리: db.reactions.findOne({ boardId: "b1", userId: "u1" })
        indexOps.ensureIndex(
            CompoundIndexDefinition(
                Document()
                    .append("boardId", 1)
                    .append("userId", 1)
            ).named("idx_reactions_boardId_userId_unique")
                .unique()   // UNIQUE 제약 추가
        ).subscribe(
            { indexName -> logger.debug("인덱스 생성/확인: {}", indexName) },
            { ex -> logger.error("reactions 복합 유니크 인덱스 생성 실패", ex) }
        )

        // 2. boardId 단일 인덱스: 게시글의 전체 반응 조회
        // 사용 쿼리: db.reactions.find({ boardId: "boardId" })
        indexOps.ensureIndex(
            Index().on("boardId", Sort.Direction.ASC)
                .named("idx_reactions_boardId")
        ).subscribe(
            { indexName -> logger.debug("인덱스 생성/확인: {}", indexName) },
            { ex -> logger.error("boardId 인덱스 생성 실패", ex) }
        )
    }
}
