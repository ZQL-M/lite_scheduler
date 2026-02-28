package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务状态，存储任务的实时执行状态和统计信息
 */
@Data
@NoArgsConstructor
@TableName("task_status")
public class TaskStatus {
    @TableId("task_id")
    private String taskId; // 关联的任务ID

    private String state = "WAITING"; // 任务状态

    @TableField("last_fire_time")
    private LocalDateTime lastFireTime; // 上次触发时间

    @TableField("next_fire_time")
    private LocalDateTime nextFireTime; // 下次触发时间

    @TableField("last_execution_id")
    private String lastExecutionId; // 上次执行记录ID

    @TableField("last_execution_status")
    private String lastExecutionStatus = "NONE"; // 上次执行状态

    @TableField("consecutive_failures")
    private int consecutiveFailures = 0; // 连续失败次数

    @TableField("total_executions")
    private long totalExecutions = 0; // 总执行次数

    @TableField("total_successes")
    private long totalSuccesses = 0; // 总成功次数

    @TableField("total_failures")
    private long totalFailures = 0; // 总失败次数

    @TableField("updated_time")
    private LocalDateTime updatedTime; // 更新时间

    // 构造方法
    public TaskStatus(String taskId) {
        this.taskId = taskId;
        this.state = "WAITING";
        this.lastExecutionStatus = "NONE";
        this.consecutiveFailures = 0;
        this.totalExecutions = 0;
        this.totalSuccesses = 0;
        this.totalFailures = 0;
        this.updatedTime = LocalDateTime.now();
    }

    // 业务方法
    public void recordSuccess(String executionId) {
        this.lastExecutionId = executionId;
        this.lastExecutionStatus = "SUCCESS";
        this.lastFireTime = LocalDateTime.now();
        this.consecutiveFailures = 0;
        this.totalExecutions++;
        this.totalSuccesses++;
        this.updatedTime = LocalDateTime.now();
    }

    public void recordFailure(String executionId) {
        this.lastExecutionId = executionId;
        this.lastExecutionStatus = "FAILURE";
        this.lastFireTime = LocalDateTime.now();
        this.consecutiveFailures++;
        this.totalExecutions++;
        this.totalFailures++;
        this.updatedTime = LocalDateTime.now();
    }

    public void updateNextFireTime(LocalDateTime nextFireTime) {
        this.nextFireTime = nextFireTime;
        this.updatedTime = LocalDateTime.now();
    }
}