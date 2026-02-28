package com.tuba.schedulercore.task;

import com.tuba.schedulercore.model.TaskDefinition;

/**
 * TubaJobFactory接口，用于创建任务实例
 * 解耦任务创建与框架核心逻辑，支持无状态任务
 */
public interface TubaJobFactory {
    /**
     * 创建任务实例
     * @param definition 任务定义，包含任务的基本信息
     * @return 任务实例
     */
    Object newTask(TaskDefinition definition);
}
