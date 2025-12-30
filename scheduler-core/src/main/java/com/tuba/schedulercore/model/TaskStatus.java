package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 任务状态，存储任务的实时执行状态
 */
@TableName("task_status")
public class TaskStatus {
    @TableId("task_id")
    private String taskId; // 关联的任务ID

    @TableField("last_fire_time")
    private LocalDateTime lastFireTime; // 上次触发时间

    @TableField("next_fire_time")
    private LocalDateTime nextFireTime; // 下次触发时间

    @TableField("last_execution_status")
    private String lastExecutionStatus; // 上次执行状态：SUCCESS/FAILURE/NONE

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

    // constructors

    public TaskStatus() {
    }

    public TaskStatus(String taskId) {
        this.taskId = taskId;
        this.lastExecutionStatus = "NONE";
        this.consecutiveFailures = 0;
        this.totalExecutions = 0;
        this.totalSuccesses = 0;
        this.totalFailures = 0;
        this.updatedTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public LocalDateTime getLastFireTime() {
        return lastFireTime;
    }

    public void setLastFireTime(LocalDateTime lastFireTime) {
        this.lastFireTime = lastFireTime;
    }

    public LocalDateTime getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(LocalDateTime nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    public String getLastExecutionStatus() {
        return lastExecutionStatus;
    }

    public void setLastExecutionStatus(String lastExecutionStatus) {
        this.lastExecutionStatus = lastExecutionStatus;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public void setConsecutiveFailures(int consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public long getTotalExecutions() {
        return totalExecutions;
    }

    public void setTotalExecutions(long totalExecutions) {
        this.totalExecutions = totalExecutions;
    }

    public long getTotalSuccesses() {
        return totalSuccesses;
    }

    public void setTotalSuccesses(long totalSuccesses) {
        this.totalSuccesses = totalSuccesses;
    }

    public long getTotalFailures() {
        return totalFailures;
    }

    public void setTotalFailures(long totalFailures) {
        this.totalFailures = totalFailures;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    /**
     * 基于 taskId 的 equals 方法
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TaskStatus that = (TaskStatus) o;
        return Objects.equals(taskId, that.taskId);
    }

    /**
     * 基于 taskId 的 hashCode 方法
     */
    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }
}