-- ============================================================
-- MySQL 建表脚本（Spring Boot 启动时自动执行）
-- 使用 IF NOT EXISTS，可重复执行；已有表结构不会被修改
-- ============================================================

CREATE TABLE IF NOT EXISTS users
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL,
    password     VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) DEFAULT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL,
    message_type    VARCHAR(16) NOT NULL,
    payload         LONGTEXT    NOT NULL,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_chat_message_conversation (conversation_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS user_memory
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                VARCHAR(64) NOT NULL,
    content                LONGTEXT    NOT NULL,
    category               VARCHAR(32)  DEFAULT NULL,
    importance             INT         NOT NULL DEFAULT 5,
    status                 VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    vector_id              VARCHAR(64)  DEFAULT NULL,
    source_conversation_id VARCHAR(64)  DEFAULT NULL,
    created_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_memory_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
