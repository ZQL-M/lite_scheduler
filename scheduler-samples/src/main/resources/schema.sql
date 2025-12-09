-- 创建任务定义表
CREATE TABLE IF NOT EXISTS lite_scheduler_task (
    id             varchar(36)                           NOT NULL COMMENT '任务ID' PRIMARY KEY,
    name           varchar(100)                          NOT NULL COMMENT '任务名称',
    group_name     varchar(50)  DEFAULT 'default'        NULL COMMENT '任务分组',
    cron           varchar(100)                          NULL COMMENT 'Cron表达式',
    fixed_rate     bigint        DEFAULT -1              NULL COMMENT '固定频率(ms)',
    fixed_delay    bigint        DEFAULT -1              NULL COMMENT '固定延迟(ms)',
    async          tinyint(1)    DEFAULT 1               NULL COMMENT '是否异步执行',
    bean_name      varchar(100)                          NOT NULL COMMENT 'Spring Bean名称',
    method_name    varchar(100)                          NOT NULL COMMENT '方法名',
    description    text                                  NULL COMMENT '任务描述',
    enabled        tinyint(1)    DEFAULT 1               NULL COMMENT '是否启用',
    persistent     tinyint(1)    DEFAULT 1               NULL COMMENT '是否持久化',
    repeat_count   int           DEFAULT -1              NULL COMMENT '循环次数(-1=无限)',
    last_fire_time timestamp                             NULL COMMENT '上次触发时间',
    next_fire_time timestamp                             NULL COMMENT '下次触发时间',
    create_time    timestamp     DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    update_time    timestamp     DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '任务定义表';

-- 创建任务执行日志表
CREATE TABLE IF NOT EXISTS lite_scheduler_task_log (
    id          varchar(36)  NOT NULL COMMENT '日志ID' PRIMARY KEY,
    task_id     varchar(36)  NOT NULL COMMENT '任务ID',
    task_name   varchar(100) NOT NULL COMMENT '任务名称',
    status      varchar(20)  NOT NULL COMMENT '执行状态(SUCCESS/FAIL)',
    start_time  timestamp    NOT NULL COMMENT '开始时间',
    end_time    timestamp    NULL COMMENT '结束时间',
    duration_ms bigint       NULL COMMENT '执行时长(ms)',
    message     text         NULL COMMENT '执行消息',
    error_stack text         NULL COMMENT '错误堆栈',
    CONSTRAINT lite_scheduler_task_log_ibfk_1
        FOREIGN KEY (task_id) REFERENCES lite_scheduler_task (id)
            ON DELETE CASCADE
) COMMENT '任务执行日志表';

-- 创建索引
CREATE INDEX IF NOT EXISTS task_id ON lite_scheduler_task_log (task_id);