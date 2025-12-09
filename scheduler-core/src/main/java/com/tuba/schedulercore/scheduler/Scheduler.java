package com.tuba.schedulercore.scheduler;

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
     * 立即触发任务执行（不加入调度计划）
     * 
     * @param definition 任务定义
     */
    void triggerNow(TaskDefinition definition);
}