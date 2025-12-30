package com.tuba.schedulercore.service;

import com.tuba.schedulercore.model.TaskStatus;

/**
 * 任务状态服务接口
 * 负责管理任务的实时执行状态和统计信息
 */
public interface TaskStatusService {
    /**
     * 创建或更新任务状态
     *
     * @param taskStatus 任务状态对象
     */
    void saveOrUpdateTaskStatus(TaskStatus taskStatus);

    /**
     * 根据任务ID获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态对象
     */
    TaskStatus getTaskStatus(String taskId);

    /**
     * 更新任务的上次执行状态
     *
     * @param taskId       任务ID
     * @param status       执行状态（SUCCESS/FAILURE）
     * @param lastFireTime 上次触发时间
     * @param nextFireTime 下次触发时间
     */
    void updateExecutionStatus(String taskId, String status, java.time.LocalDateTime lastFireTime,
            java.time.LocalDateTime nextFireTime);

    /**
     * 增加任务执行次数
     *
     * @param taskId  任务ID
     * @param success 是否成功
     */
    void incrementExecutionCount(String taskId, boolean success);

    /**
     * 重置任务状态
     *
     * @param taskId 任务ID
     */
    void resetTaskStatus(String taskId);

    /**
     * 删除任务状态
     *
     * @param taskId 任务ID
     */
    void deleteTaskStatus(String taskId);
}