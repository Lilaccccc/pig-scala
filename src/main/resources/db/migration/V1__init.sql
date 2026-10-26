-- ==================== 用户表 ====================
CREATE TABLE IF NOT EXISTS `t_user`
(
    `id`          BIGINT       NOT NULL COMMENT '主键 ID（雪花算法生成）',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名（唯一）',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密存储）',
    `nickname`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户表';
