package com.tuba.schedulercore.core.scheduler;

import com.tuba.schedulercore.core.executor.TaskExecutor;
import com.tuba.schedulercore.core.trigger.Trigger;
import com.tuba.schedulercore.enums.SchedulerStatus;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.model.TaskStatus;
import com.tuba.schedulercore.core.task.TubaTask;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 调度器（统一接口）
 * 负责任务注册、调度、管理和查询
 *
 */
public interface Scheduler {

    // ========== 任务注册（核心方法） ==========

    /**
     * 注册任务（支持 TubaTask）
     * @param task       任务实例（TubaTask）
     * @param definition 任务定义
     * @param trigger    触发器
     * @return 任务 ID
     */
    String scheduleTask(TubaTask task, TaskDefinition definition, Trigger trigger);

    /**
     * 注册任务（支持 Runnable）
     * 
     * @param task       任务实例（Runnable）
     * @param definition 任务定义
     * @param trigger    触发器
     * @return 任务 ID
     */
    String scheduleTask(Runnable task, TaskDefinition definition, Trigger trigger);

    /**
     * 注册任务（支持 Callable）
     * 
     * @param task       任务实例（Callable）
     * @param definition 任务定义
     * @param trigger    触发器
     * @return 任务 ID
     */
    <V> String scheduleTask(Callable<V> task, TaskDefinition definition, Trigger trigger);

    /**
     * 快速注册任务（Cron 表达式）
     * <p>
     * 使用示例：
     * 
     * <pre>{@code
     * String taskId = scheduler.scheduleTask(myTubaTask, "0/5 * * * * ?");
     * }</pre>
     * </p>
     * 
     * @param task           任务实例
     * @param cronExpression Cron 表达式
     * @return 任务 ID
     */
    String scheduleTask(TubaTask task, String cronExpression);

    /**
     * 快速注册任务（固定频率）
     * 
     * @param task     任务实例
     * @param interval 间隔时间
     * @param timeUnit 时间单位
     * @return 任务 ID
     */
    String scheduleTask(TubaTask task, long interval, java.util.concurrent.TimeUnit timeUnit);

    // ========== 任务管理 ==========

    /**
     * 暂停任务
     * 
     * @param taskId 任务 ID
     */
    void pauseTask(String taskId);

    /**
     * 恢复任务
     * 
     * @param taskId 任务 ID
     */
    void resumeTask(String taskId);

    /**
     * 取消任务（删除）
     * 
     * @param taskId 任务 ID
     */
    void cancelTask(String taskId);

    /**
     * 立即触发任务执行（不加入调度计划）
     * 
     * @param taskId 任务 ID
     */
    void triggerNow(String taskId);

    // ========== 任务查询 ==========

    /**
     * 获取任务定义
     * 
     * @param taskId 任务 ID
     * @return 任务定义
     */
    TaskDefinition getTaskDefinition(String taskId);

    /**
     * 获取任务状态
     * 
     * @param taskId 任务 ID
     * @return 任务状态
     */
    TaskStatus getTaskStatus(String taskId);

    /**
     * 获取任务的触发器
     * 
     * @param taskId 任务 ID
     * @return 触发器
     */
    Trigger getTrigger(String taskId);

    /**
     * 获取所有任务定义
     * 
     * @return 任务定义列表
     */
    List<TaskDefinition> getAllTasks();

    /**
     * 检查任务是否存在
     * 
     * @param taskId 任务 ID
     * @return true 如果任务存在
     */
    boolean isTaskExists(String taskId);

    // ========== 调度器生命周期 ==========

    /**
     * 启动调度器
     */
    void start();

    /**
     * 关闭调度器（优雅关闭）
     */
    void shutdown();

    /**
     * 获取调度器状态
     * 
     * @return 调度器状态
     */
    SchedulerStatus getStatus();

    /**
     * 检查调度器是否已关闭
     * 
     * @return true 如果已关闭
     */
    boolean isShutdown();
}