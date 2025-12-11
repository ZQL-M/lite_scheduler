package com.tuba.schedulercore.service;

import com.tuba.schedulercore.enums.TimeUnit;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.task.Task;

/**
 * 任务调度服务接口，提供编程式API用于任务管理
 */
public interface TaskSchedulerService {
    /**
     * 注册任务
     * 
     * @param task       Task实例
     * @param definition 任务定义
     * @return 任务ID
     */
    String registerTask(Task task, TaskDefinition definition);

    /**
     * 使用cron表达式注册任务
     * 
     * @param task Task实例
     * @param cron cron表达式
     * @return 任务ID
     */
    String registerTask(Task task, String cron);

    /**
     * 使用固定频率注册任务
     * 
     * @param task     Task实例
     * @param interval 时间间隔
     * @param timeUnit 时间单位
     * @return 任务ID
     */
    String registerTask(Task task, long interval, TimeUnit timeUnit);

    /**
     * 触发任务立即执行
     * 
     * @param taskId 任务ID
     */
    void triggerTask(String taskId);

    /**
     * 暂停任务
     * 
     * @param taskId 任务ID
     */
    void pauseTask(String taskId);

    /**
     * 恢复任务
     * 
     * @param taskId 任务ID
     */
    void resumeTask(String taskId);

    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     */
    void cancelTask(String taskId);

    /**
     * 获取任务定义
     * 
     * @param taskId 任务ID
     * @return 任务定义
     */
    TaskDefinition getTaskDefinition(String taskId);
}
