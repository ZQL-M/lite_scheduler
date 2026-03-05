package com.tuba.schedulercore.core.task;

import com.tuba.schedulercore.model.TaskContext;

/**
 * 任务接口，用户可实现此接口定义自定义任务逻辑
 */
public interface TubaTask {
    /**
     * 执行任务
     * 
     * @param context 任务执行上下文
     */
    void execute(TaskContext context);
}
