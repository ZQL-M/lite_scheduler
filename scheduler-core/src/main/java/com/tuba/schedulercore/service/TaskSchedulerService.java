package com.tuba.schedulercore.service;

import com.tuba.schedulercore.enums.TimeUnit;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.core.task.Task;

import java.util.concurrent.Callable;

/**
 * 任务调度服务接口，提供编程式API用于任务管理
 * todo 注册任务接口需要优化调整，task与tubaTask重复了
 */
public interface TaskSchedulerService {
    //todo 筛选对应的注册任务，有些参数应该是从对应model类获取
    /**
     * 注册任务
     * 
     * @param task       Task实例
     * @param definition 任务定义
     * @return 任务ID
     */
    String registerTask(Task task, TaskDefinition definition);
    
    /**
     * 注册任务（仅使用任务定义）
     * 
     * @param definition 任务定义
     * @return 任务ID
     */
    String registerTask(TaskDefinition definition);

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
     * 使用Runnable注册任务
     * 
     * @param runnable   Runnable实例
     * @param definition 任务定义
     * @return 任务ID
     */
    String registerTask(Runnable runnable, TaskDefinition definition);

    /**
     * 使用Runnable和cron表达式注册任务
     * 
     * @param runnable Runnable实例
     * @param cron     cron表达式
     * @return 任务ID
     */
    String registerTask(Runnable runnable, String cron);

    /**
     * 使用Runnable和固定频率注册任务
     * 
     * @param runnable Runnable实例
     * @param interval 时间间隔
     * @param timeUnit 时间单位
     * @return 任务ID
     */
    String registerTask(Runnable runnable, long interval, TimeUnit timeUnit);

    /**
     * 使用Callable注册任务
     * 
     * @param callable   Callable实例
     * @param definition 任务定义
     * @return 任务ID
     */
    <V> String registerTask(Callable<V> callable, TaskDefinition definition);

    /**
     * 使用Callable和cron表达式注册任务
     * 
     * @param callable Callable实例
     * @param cron     cron表达式
     * @return 任务ID
     */
    <V> String registerTask(Callable<V> callable, String cron);

    /**
     * 使用Callable和固定频率注册任务
     * 
     * @param callable Callable实例
     * @param interval 时间间隔
     * @param timeUnit 时间单位
     * @return 任务ID
     */
    <V> String registerTask(Callable<V> callable, long interval, TimeUnit timeUnit);

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
