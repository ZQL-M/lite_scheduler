package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

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

    @TableField("task_name")
    private String taskName;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("duration_ms")
    private Long durationMs;

    private String status;

    @TableField("error_message")
    private String errorMessage;

    private String exception;

    @TableField("created_time")
    private LocalDateTime createdTime;
}
