package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 触发器基类，存储触发器的通用信息
 */
@Data
@EqualsAndHashCode(of = "id")
@TableName("task_trigger")
public class TaskTrigger {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("task_id")
    private String taskId; // 关联的任务ID

    @TableField("trigger_type")
    private String triggerType; // 触发器类型：CRON/FIXED_RATE/FIXED_DELAY

    @TableField("start_time")
    private LocalDateTime startTime; // 触发器开始时间

    @TableField("end_time")
    private LocalDateTime endTime; // 触发器结束时间

    private int priority = 5; // 优先级（1-10，5为默认）

    private boolean enabled = true; // 触发器是否启用

    @TableField("created_time")
    private LocalDateTime createdTime; // 创建时间

    @TableField("updated_time")
    private LocalDateTime updatedTime; // 更新时间

    // constructors

    public TaskTrigger() {
    }

    public TaskTrigger(String id, String taskId, String triggerType, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.taskId = taskId;
        this.triggerType = triggerType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.priority = 5;
        this.enabled = true;
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }
}