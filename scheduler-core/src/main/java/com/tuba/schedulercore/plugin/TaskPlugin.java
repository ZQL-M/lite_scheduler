package com.tuba.schedulercore.plugin;


import com.tuba.schedulercore.model.TaskContext;

/**
 * 任务插件接口，定义了任务执行过程中的生命周期回调方法
 * 该接口包含三个默认方法，分别用于任务执行前、执行后和发生错误时的处理
 */
public interface TaskPlugin {
    /**
     * 任务执行前的回调方法
     * @param context 任务上下文信息，包含任务执行相关的数据
     */
    default void before(TaskContext context) {}
    /**
     * 任务执行后的回调方法
     * @param context 任务上下文信息，包含任务执行相关的数据
     */
    default void after(TaskContext context) {}
    /**
     * 任务执行过程中发生错误时的回调方法
     * @param context 任务上下文信息，包含任务执行相关的数据
     * @param e 任务执行过程中发生的异常对象
     */
    default void onError(TaskContext context, Throwable e) {}
}