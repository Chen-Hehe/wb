-- Weibo Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS weibo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE weibo_db;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    gender TINYINT DEFAULT 0 COMMENT '性别 0-未知 1-男 2-女',
    birthday DATE COMMENT '生日',
    introduction TEXT COMMENT '简介',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Weibos table
CREATE TABLE IF NOT EXISTS weibos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '微博ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT NOT NULL COMMENT '微博内容',
    images JSON COMMENT '图片URL数组',
    repost_count INT DEFAULT 0 COMMENT '转发数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    status TINYINT DEFAULT 1 COMMENT '状态 0-删除 1-正常',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_time (created_time),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微博表';

-- Comments table
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    weibo_id BIGINT NOT NULL COMMENT '微博ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父评论ID(0表示一级评论)',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    status TINYINT DEFAULT 1 COMMENT '状态 0-删除 1-正常',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_weibo_id (weibo_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_created_time (created_time),
    FOREIGN KEY (weibo_id) REFERENCES weibos(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- Attentions table (关注关系)
CREATE TABLE IF NOT EXISTS attentions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关注ID',
    follower_id BIGINT NOT NULL COMMENT '关注者ID',
    followee_id BIGINT NOT NULL COMMENT '被关注者ID',
    status TINYINT DEFAULT 1 COMMENT '状态 0-取消 1-关注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_follower_followee (follower_id, followee_id),
    INDEX idx_follower_id (follower_id),
    INDEX idx_followee_id (followee_id),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (followee_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系表';

-- Likes table (微博点赞)
CREATE TABLE IF NOT EXISTS weibo_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    weibo_id BIGINT NOT NULL COMMENT '微博ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_weibo_user (weibo_id, user_id),
    INDEX idx_weibo_id (weibo_id),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (weibo_id) REFERENCES weibos(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微博点赞表';

-- Comment Likes table (评论点赞)
CREATE TABLE IF NOT EXISTS comment_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论点赞ID',
    comment_id BIGINT NOT NULL COMMENT '评论ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    INDEX idx_comment_id (comment_id),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

-- Insert test data
-- Users (password is encrypted with BCrypt, default password: 123456)
INSERT INTO users (username, password, email, phone, nickname, avatar, gender, introduction, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS', 'admin@weibo.com', '13800138000', '管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', 1, '系统管理员', 1),
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS', 'user1@weibo.com', '13800138001', '小明', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user1', 1, '热爱生活，分享快乐', 1),
('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS', 'user2@weibo.com', '13800138002', '小红', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user2', 2, '爱美食，爱旅行', 1),
('user3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS', 'user3@weibo.com', '13800138003', '小刚', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user3', 1, '程序员一枚', 1),
('user4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lqkkO9QS3TzCjH3rS', 'user4@weibo.com', '13800138004', '小丽', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user4', 2, '摄影师', 1);

-- Weibos
INSERT INTO weibos (user_id, content, images, repost_count, comment_count, like_count, status) VALUES
(2, '今天天气真好，阳光明媚！适合出去走走～ #好心情#', '["https://picsum.photos/seed/weibo1/600/400"]', 5, 12, 88, 1),
(3, '新买的机械键盘到了，打字手感太棒了！#程序员日常#', '["https://picsum.photos/seed/weibo2/600/400", "https://picsum.photos/seed/weibo3/600/400"]', 3, 8, 56, 1),
(4, '分享一道今天做的菜，色香味俱全！#美食#', '["https://picsum.photos/seed/weibo4/600/400"]', 10, 25, 156, 1),
(2, '周末去爬山了，风景很美，就是有点累。#户外运动#', '["https://picsum.photos/seed/weibo5/600/400", "https://picsum.photos/seed/weibo6/600/400", "https://picsum.photos/seed/weibo7/600/400"]', 8, 15, 120, 1),
(5, '今天拍了一组人像照片，大家觉得怎么样？#摄影#', '["https://picsum.photos/seed/weibo8/600/400"]', 15, 30, 200, 1);

-- Comments
INSERT INTO comments (weibo_id, user_id, parent_id, content, like_count, status) VALUES
(1, 3, 0, '确实，今天阳光很好！', 5, 1),
(1, 4, 0, '准备去哪里玩呀？', 3, 1),
(2, 2, 0, '什么牌子的键盘？求推荐！', 8, 1),
(2, 4, 0, '程序员必备神器', 4, 1),
(3, 2, 0, '看起来好好吃！求做法～', 12, 1),
(3, 3, 0, '我也想吃！', 6, 1),
(4, 3, 0, '爬的哪座山呀？', 4, 1),
(5, 2, 0, '拍得真好！构图很棒！', 15, 1),
(5, 3, 0, '专业摄影师！', 10, 1);

-- Attentions (关注关系)
INSERT INTO attentions (follower_id, followee_id, status) VALUES
(2, 3, 1),
(2, 4, 1),
(2, 5, 1),
(3, 2, 1),
(3, 4, 1),
(4, 2, 1),
(4, 3, 1),
(5, 2, 1),
(5, 3, 1),
(5, 4, 1);

-- Weibo Likes
INSERT INTO weibo_likes (weibo_id, user_id) VALUES
(1, 2), (1, 3), (1, 4), (1, 5),
(2, 2), (2, 4), (2, 5),
(3, 2), (3, 3), (3, 4), (3, 5),
(4, 2), (4, 3), (4, 5),
(5, 2), (5, 3), (5, 4);

-- Comment Likes
INSERT INTO comment_likes (comment_id, user_id) VALUES
(1, 2), (1, 4),
(2, 3),
(3, 2), (3, 4),
(5, 2), (5, 3), (5, 4);
