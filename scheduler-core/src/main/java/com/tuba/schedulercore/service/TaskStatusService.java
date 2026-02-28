package com.tuba.schedulercore.service;

import com.tuba.schedulercore.model.TaskStatus;

/**
 * 任务状态服务，用于管理任务的运行时状态
 */
public interface TaskStatusService {
    /**
     * 根据任务ID获取任务状态
     * @param taskId 任务ID
     * @return 任务状态
     */
    TaskStatus getStatus(String taskId);

    /**
     * 更新任务状态
     * @param status 任务状态
     */
    void updateStatus(TaskStatus status);

    /**
     * 记录任务执行成功
     * @param taskId 任务ID
     * @param executionId 执行记录ID
     */
    void recordSuccess(String taskId, String executionId);

    /**
     * 记录任务执行失败
     * @param taskId 任务ID
     * @param executionId 执行记录ID
     */
    void recordFailure(String taskId, String executionId);

    /**
     * 更新下次触发时间
     * @param taskId 任务ID
     * @param nextFireTime 下次触发时间
     */
    void updateNextFireTime(String taskId, java.time.LocalDateTime nextFireTime);
}