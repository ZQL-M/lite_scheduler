-- Lite-Scheduler 数据库表结构

-- 任务定义表
CREATE TABLE IF NOT EXISTS lite_scheduler_task (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '任务ID',
    name VARCHAR(100) NOT NULL COMMENT '任务名称',
    group_name VARCHAR(50) DEFAULT 'default' COMMENT '任务分组',
    cron VARCHAR(50) COMMENT 'Cron表达式',
    fixed_rate BIGINT DEFAULT -1 COMMENT '固定频率（毫秒）',
    fixed_delay BIGINT DEFAULT -1 COMMENT '固定延迟（毫秒）',
    async BOOLEAN DEFAULT TRUE COMMENT '是否异步执行',
    bean_name VARCHAR(255) NOT NULL COMMENT 'Bean名称',
    method_name VARCHAR(100) NOT NULL COMMENT '方法名称',
    method_signature VARCHAR(500) NOT NULL COMMENT '方法签名',
    description VARCHAR(500) COMMENT '任务描述',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    persistent BOOLEAN DEFAULT FALSE COMMENT '是否持久化',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_bean_method (bean_name, method_signature)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务定义表';

-- 任务执行日志表
CREATE TABLE IF NOT EXISTS lite_scheduler_task_log (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '日志ID',
    task_id VARCHAR(36) NOT NULL COMMENT '任务ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    group_name VARCHAR(50) DEFAULT 'default' COMMENT '任务分组',
    start_time TIMESTAMP NOT NULL COMMENT '开始时间',
    end_time TIMESTAMP NOT NULL COMMENT '结束时间',
    duration_ms BIGINT NOT NULL COMMENT '执行时长（毫秒）',
    status VARCHAR(20) NOT NULL COMMENT '执行状态：SUCCESS/FAIL',
    message TEXT COMMENT '执行信息（错误信息等）',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (task_id) REFERENCES lite_scheduler_task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行日志表';

-- 索引优化
CREATE INDEX idx_task_group ON lite_scheduler_task(group_name);
CREATE INDEX idx_task_enabled ON lite_scheduler_task(enabled);
CREATE INDEX idx_task_log_task_id ON lite_scheduler_task_log(task_id);
CREATE INDEX idx_task_log_status ON lite_scheduler_task_log(status);
CREATE INDEX idx_task_log_start_time ON lite_scheduler_task_log(start_time);
