package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

import com.tuba.schedulercore.enums.ExecutionStatus;
import lombok.Data;

/**
 * 任务执行日志实体
 */
@Data
@TableName("task_execution_log")
public class TaskLog {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("task_id")
    private String taskId;

    @TableField("trigger_id")
    private String triggerId; // 触发本次执行的触发器 ID

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("status")
    private ExecutionStatus status;

    @TableField("error_message")
    private String errorMessage;

    private boolean deleted = false; // 是否软删除

    @TableField("created_time")
    private LocalDateTime createdTime;
}
