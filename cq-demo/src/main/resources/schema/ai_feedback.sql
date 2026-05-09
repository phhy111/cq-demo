CREATE TABLE IF NOT EXISTS ai_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    conversation_id VARCHAR(128) COMMENT '对话ID',
    ai_response TEXT COMMENT 'AI回答内容',
    rating INT COMMENT '评分(1-5)',
    correction_content TEXT COMMENT '纠错内容',
    correction_type VARCHAR(50) COMMENT '纠错类型: scenic(景点), food(美食), route(路线), other(其他)',
    feedback_note VARCHAR(500) COMMENT '反馈备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI反馈表';
