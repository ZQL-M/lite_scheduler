-- Lite-Scheduler数据库表结构
-- 创建任务定义表
drop table if exists task_definition;
CREATE TABLE `task_definition` (
  `id` VARCHAR(64) NOT NULL COMMENT '任务ID',
  `name` VARCHAR(128) NOT NULL COMMENT '任务名称',
  `group_name` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '任务分组',
  `cron` VARCHAR(64) NULL COMMENT 'Cron表达式',
  `fixed_rate` BIGINT NULL DEFAULT -1 COMMENT '固定频率，单位毫秒',
  `fixed_delay` BIGINT NULL DEFAULT -1 COMMENT '固定延迟，单位毫秒',
  `async` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否异步执行',
  `bean_name` VARCHAR(128) NULL COMMENT 'Spring Bean名称',
  `method_name` VARCHAR(128) NULL COMMENT '方法名',
  `description` VARCHAR(256) NULL COMMENT '任务描述',
  `enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
  `persistent` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否持久化',
  `repeat_count` INT NOT NULL DEFAULT -1 COMMENT '剩余循环次数，-1表示无限循环',
  `last_fire_time` DATETIME NULL COMMENT '上次触发时间',
  `next_fire_time` DATETIME NULL COMMENT '下次触发时间',
  `status` VARCHAR(32) NOT NULL DEFAULT 'NOT_EXECUTED' COMMENT '任务状态：NOT_EXECUTED/PENDING/COMPLETED/EXPIRED/FAILED/CANCELLED',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_group_name` (`group_name`),
  INDEX `idx_next_fire_time` (`next_fire_time`),
  INDEX `idx_enabled` (`enabled`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务定义表';

-- 创建任务执行日志表
drop table if exists task_execution_log;
CREATE TABLE `task_execution_log` (
  `id` VARCHAR(64) NOT NULL COMMENT '日志ID',
  `task_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
  `task_name` VARCHAR(128) NOT NULL COMMENT '任务名称',
  `start_time` DATETIME NOT NULL COMMENT '执行开始时间',
  `end_time` DATETIME NULL COMMENT '执行结束时间',
  `duration_ms` BIGINT NULL COMMENT '执行耗时，单位毫秒',
  `status` VARCHAR(16) NOT NULL COMMENT '执行状态：SUCCESS/FAIL',
  `error_message` TEXT NULL COMMENT '错误信息',
  `exception` TEXT NULL COMMENT '异常堆栈',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_task_id` (`task_id`),
  INDEX `idx_start_time` (`start_time`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行日志表';
