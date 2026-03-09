CREATE TABLE IF NOT EXISTS comments (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    post_id     BIGINT NOT NULL,
    parent_id   BIGINT,
    depth       INT NOT NULL DEFAULT 0,
    author_id   BIGINT NOT NULL,
    author_nickname VARCHAR(100) NOT NULL,
    content     TEXT NOT NULL,
    like_count  BIGINT NOT NULL DEFAULT 0,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME NOT NULL,
    updated_at  DATETIME NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_comments_post_id (post_id),
    INDEX idx_comments_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS comment_likes (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    comment_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_likes (comment_id, user_id)
);
