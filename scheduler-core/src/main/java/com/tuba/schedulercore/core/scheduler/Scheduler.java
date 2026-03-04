package com.tuba.schedulercore.core.scheduler;

import com.tuba.schedulercore.enums.SchedulerStatus;
import com.tuba.schedulercore.core.trigger.Trigger;
import com.tuba.schedulercore.model.TaskDefinition;

/**
 * 调度器接口，负责任务的调度管理
 */
public interface Scheduler {
    /**
     * 调度任务
     * 
     * @param definition 任务定义
     */
    void schedule(TaskDefinition definition);

    /**
     * 使用指定触发器调度任务
     * 
     * @param definition 任务定义
     * @param trigger 触发器
     */
    void schedule(TaskDefinition definition, Trigger trigger);

    /**
     * 取消调度任务
     * 
     * @param taskId 任务ID
     */
    void unschedule(String taskId);

    /**
     * 启动调度器
     */
    void start();

    /**
     * 停止调度器
     */
    void stop();

    /**
     * 暂停任务
     * 
     * @param taskId 任务ID
     */
    void pause(String taskId);

    /**
     * 恢复任务
     * 
     * @param taskId 任务ID
     */
    void resume(String taskId);

    /**
     * 立即触发任务执行（不加入调度计划）
     * 
     * @param definition 任务定义
     */
    void triggerNow(TaskDefinition definition);

    /**
     * 获取调度器状态
     */
    SchedulerStatus getStatus();
}