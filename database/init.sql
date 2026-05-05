-- FaceSend 数据库初始化脚本
-- 创建数据库
DROP DATABASE IF EXISTS facesend;

CREATE DATABASE IF NOT EXISTS facesend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE facesend;

-- 创建房间表
CREATE TABLE IF NOT EXISTS room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '房间ID',
    code VARCHAR(6) NOT NULL UNIQUE COMMENT '6位数字码',
    creator_id VARCHAR(64) NOT NULL COMMENT '创建者设备ID',
    latitude DOUBLE COMMENT '创建者纬度',
    longitude DOUBLE COMMENT '创建者经度',
    is_closed BOOLEAN DEFAULT FALSE COMMENT '房间是否已关闭',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    allow_member_upload BOOLEAN DEFAULT FALSE COMMENT '是否允许成员上传',
    INDEX idx_code (code),
    INDEX idx_expire_at (expire_at),
    INDEX idx_is_closed (is_closed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间表';

-- 创建文件信息表
CREATE TABLE IF NOT EXISTS file_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文件ID',
    room_id BIGINT NOT NULL COMMENT '所属房间ID',
    uploader_id VARCHAR(64) NOT NULL COMMENT '上传者设备ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_path VARCHAR(512) NOT NULL COMMENT 'MinIO中的存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    mime_type VARCHAR(128) COMMENT 'MIME类型',
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE,
    INDEX idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- 创建房间成员表
CREATE TABLE IF NOT EXISTS room_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成员ID',
    room_id BIGINT NOT NULL COMMENT '所属房间ID',
    device_id VARCHAR(64) NOT NULL COMMENT '设备ID',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    last_active_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    UNIQUE KEY uk_room_device (room_id, device_id),
    FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE CASCADE,
    INDEX idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间成员表';
