-- 旧任务保留为 NULL 归属，不自动分配给第一个注册者。
ALTER TABLE learning_task ADD COLUMN user_id BIGINT NULL;
ALTER TABLE learning_task ADD CONSTRAINT fk_task_user
    FOREIGN KEY (user_id) REFERENCES app_user(id);
CREATE INDEX idx_task_user_id ON learning_task(user_id, id);
CREATE TABLE knowledge_note (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT '未分类',
    content MEDIUMTEXT NOT NULL,
    revision BIGINT NOT NULL DEFAULT 1,
    index_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    index_error VARCHAR(500) NULL,
    index_attempts INT NOT NULL DEFAULT 0,
    next_index_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_note_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);
CREATE INDEX idx_note_user_updated ON knowledge_note(user_id, updated_at);
CREATE INDEX idx_note_pending ON knowledge_note(index_status, next_index_at);
