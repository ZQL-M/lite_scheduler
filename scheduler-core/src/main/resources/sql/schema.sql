CREATE TABLE IF NOT EXISTS `task_definition`
(
    `id`           VARCHAR(64)  NOT NULL COMMENT '任务唯一标识（UUID）',
    `name`         VARCHAR(128) NOT NULL COMMENT '任务名称',
    `group_name`   VARCHAR(64)  NOT NULL DEFAULT 'default' COMMENT '任务分组',
    `job_class`    VARCHAR(255)          DEFAULT NULL COMMENT '任务类名（用于TubaJobFactory创建实例）',
    `description`  VARCHAR(512)          DEFAULT NULL COMMENT '任务描述',
    `async`        BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '是否异步执行',
    `enabled`      BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '任务是否启用',
    `persistent`   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否持久化',
    `deleted`      BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否软删除',
    `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_group` (`group_name`),
    INDEX `idx_task_enabled` (`enabled`),
    INDEX `idx_task_persistent` (`persistent`),
    INDEX `idx_task_deleted` (`deleted`),
    INDEX `idx_task_group_enabled` (`group_name`, `enabled`),
    INDEX `idx_task_job_class` (`job_class`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务定义表';

CREATE TABLE IF NOT EXISTS `task_trigger`
(
    `id`           VARCHAR(64) NOT NULL COMMENT '触发器唯一标识（UUID）',
    `task_id`      VARCHAR(64) NOT NULL COMMENT '关联的任务ID',
    `trigger_type` VARCHAR(32) NOT NULL COMMENT '触发器类型：CRON/FIXED_RATE/FIXED_DELAY',
    `start_time`   DATETIME             DEFAULT NULL COMMENT '触发器开始时间（NULL表示立即开始）',
    `end_time`     DATETIME             DEFAULT NULL COMMENT '触发器结束时间（NULL表示永久执行）',
    `priority`     INT         NOT NULL DEFAULT 5 COMMENT '优先级（1-10，5为默认）',
    `enabled`      BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '触发器是否启用',
    `deleted`      BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '是否软删除',
    `created_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_trigger_task` (`task_id`),
    INDEX `idx_trigger_type` (`trigger_type`),
    INDEX `idx_trigger_enabled` (`enabled`),
    INDEX `idx_trigger_deleted` (`deleted`),
    INDEX `idx_trigger_task_enabled` (`task_id`, `enabled`),
    INDEX `idx_trigger_start_time` (`start_time`),
    INDEX `idx_trigger_end_time` (`end_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='触发器表';

CREATE TABLE IF NOT EXISTS `task_execution_log`
(
    `id`              VARCHAR(64) NOT NULL COMMENT '日志唯一标识（UUID）',
    `task_id`         VARCHAR(64) NOT NULL COMMENT '关联的任务ID',
    `trigger_id`      VARCHAR(64)          DEFAULT NULL COMMENT '触发本次执行的触发器ID',
    `start_time`      DATETIME    NOT NULL COMMENT '执行开始时间',
    `end_time`        DATETIME             DEFAULT NULL COMMENT '执行结束时间（NULL表示执行中）',
    `duration_ms`     BIGINT               DEFAULT NULL COMMENT '执行时长（毫秒）',
    `status`          VARCHAR(32) NOT NULL COMMENT '执行状态：RUNNING/SUCCESS/FAILURE',
    `error_message`   TEXT                 DEFAULT NULL COMMENT '错误信息',
    `exception_stack` TEXT                 DEFAULT NULL COMMENT '异常堆栈信息',
    `deleted`         BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '是否软删除',
    `created_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_log_task` (`task_id`),
    INDEX `idx_log_status` (`status`),
    INDEX `idx_log_start_time` (`start_time`),
    INDEX `idx_log_deleted` (`deleted`),
    INDEX `idx_log_task_status` (`task_id`, `status`),
    INDEX `idx_log_task_time` (`task_id`, `start_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务执行日志表';

CREATE TABLE IF NOT EXISTS `task_simple_trigger`
(
    `trigger_id`          VARCHAR(64) NOT NULL COMMENT '关联的触发器ID',
    `repeat_count`        INT         NOT NULL DEFAULT -1 COMMENT '重复次数（-1表示无限循环）',
    `repeat_interval`     BIGINT      NOT NULL DEFAULT 0 COMMENT '重复间隔（毫秒）',
    `simple_type`         VARCHAR(32) NOT NULL COMMENT '简单触发器类型：FIXED_RATE/FIXED_DELAY',
    `misfire_instruction` INT         NOT NULL DEFAULT 0 COMMENT '错过执行策略：0-忽略，1-立即执行，2-下次执行',
    `deleted`             BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '是否软删除',
    PRIMARY KEY (`trigger_id`),
    INDEX `idx_simple_trigger_type` (`simple_type`),
    INDEX `idx_simple_trigger_deleted` (`deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='简单触发器表';

CREATE TABLE IF NOT EXISTS `task_cron_trigger`
(
    `trigger_id`          VARCHAR(64)  NOT NULL COMMENT '关联的触发器ID',
    `cron_expression`     VARCHAR(128) NOT NULL COMMENT 'Cron表达式',
    `time_zone_id`        VARCHAR(64)  NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '时区',
    `misfire_instruction` INT          NOT NULL DEFAULT 0 COMMENT '错过执行策略：0-忽略，1-立即执行，2-下次执行',
    `deleted`             BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否软删除',
    PRIMARY KEY (`trigger_id`),
    INDEX `idx_cron_expression` (`cron_expression`),
    INDEX `idx_cron_trigger_deleted` (`deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Cron触发器表';

CREATE TABLE IF NOT EXISTS `task_status`
(
    `task_id`               VARCHAR(64) NOT NULL COMMENT '关联的任务ID',
    `state`                 VARCHAR(32) NOT NULL DEFAULT 'WAITING' COMMENT '任务状态：WAITING/RUNNING/PAUSED/COMPLETED/ERROR',
    `last_fire_time`        DATETIME             DEFAULT NULL COMMENT '上次触发时间',
    `next_fire_time`        DATETIME             DEFAULT NULL COMMENT '下次触发时间',
    `last_execution_id`     VARCHAR(64)          DEFAULT NULL COMMENT '上次执行记录ID',
    `last_execution_status` VARCHAR(32)          DEFAULT 'NONE' COMMENT '上次执行状态：SUCCESS/FAILURE/NONE',
    `consecutive_failures`  INT         NOT NULL DEFAULT 0 COMMENT '连续失败次数',
    `total_executions`      BIGINT      NOT NULL DEFAULT 0 COMMENT '总执行次数',
    `total_successes`       BIGINT      NOT NULL DEFAULT 0 COMMENT '总成功次数',
    `total_failures`        BIGINT      NOT NULL DEFAULT 0 COMMENT '总失败次数',
    `updated_time`          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`task_id`),
    INDEX `idx_status_state` (`state`),
    INDEX `idx_status_next_fire` (`next_fire_time`),
    INDEX `idx_status_updated` (`updated_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务状态表';