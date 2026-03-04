package com.tuba.schedulercore.core.task;

import com.tuba.schedulercore.model.TaskContext;

/**
 * TubaTask接口，定义统一的任务执行方法
 * 类似于Quartz的Job接口，用于标准化任务执行
 */
public interface TubaTask {
    /**
     * 执行任务
     * 
     * @param context 任务上下文，包含任务执行所需的信息
     */
    void execute(TaskContext context);
}
