package com.tuba.schedulercore.core.store;

import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.model.TaskStatus;
import com.tuba.schedulercore.model.TaskTrigger;
import com.tuba.schedulercore.model.CronTrigger;
import com.tuba.schedulercore.model.SimpleTrigger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业存储（统一持久化）
 * 负责任务定义、任务状态、触发器的持久化
 */
public interface JobStore {
    
    // ========== TaskDefinition 相关 ==========
    
    /**
     * 存储任务定义
     * 
     * @param definition 任务定义
     * @param replace 如果已存在是否替换
     */
    void storeTaskDefinition(TaskDefinition definition, boolean replace);
    
    /**
     * 获取任务定义
     * 
     * @param taskId 任务 ID
     * @return 任务定义
     */
    TaskDefinition retrieveTaskDefinition(String taskId);
    
    /**
     * 删除任务定义
     * 
     * @param taskId 任务 ID
     * @return 是否删除成功
     */
    boolean removeTaskDefinition(String taskId);
    
    /**
     * 检查任务是否存在
     * 
     * @param taskId 任务 ID
     * @return true 如果任务存在
     */
    boolean isTaskExists(String taskId);
    
    // ========== TaskStatus 相关 ==========
    
    /**
     * 存储任务状态
     * 
     * @param status 任务状态
     * @param replace 如果已存在是否替换
     */
    void storeTaskStatus(TaskStatus status, boolean replace);
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务 ID
     * @return 任务状态
     */
    TaskStatus retrieveTaskStatus(String taskId);
    
    /**
     * 删除任务状态
     * 
     * @param taskId 任务 ID
     * @return 是否删除成功
     */
    boolean removeTaskStatus(String taskId);
    
    // ========== TaskTrigger 相关 ==========
    
    /**
     * 存储触发器
     * 
     * @param trigger 触发器
     * @param replace 如果已存在是否替换
     */
    void storeTrigger(TaskTrigger trigger, boolean replace);
    
    /**
     * 获取触发器
     * 
     * @param triggerId 触发器 ID
     * @return 触发器
     */
    TaskTrigger retrieveTrigger(String triggerId);
    
    /**
     * 获取任务的所有触发器
     * 
     * @param taskId 任务 ID
     * @return 触发器列表
     */
    List<TaskTrigger> retrieveTriggersByTaskId(String taskId);
    
    /**
     * 删除触发器
     * 
     * @param triggerId 触发器 ID
     * @return 是否删除成功
     */
    boolean removeTrigger(String triggerId);
    
    // ========== CronTrigger 相关 ==========
    
    /**
     * 存储 Cron 触发器
     * 
     * @param cronTrigger Cron 触发器
     */
    void storeCronTrigger(CronTrigger cronTrigger);
    
    /**
     * 获取 Cron 触发器
     * 
     * @param triggerId 触发器 ID
     * @return Cron 触发器
     */
    CronTrigger retrieveCronTrigger(String triggerId);
    
    // ========== SimpleTrigger 相关 ==========
    
    /**
     * 存储 Simple 触发器
     * 
     * @param simpleTrigger Simple 触发器
     */
    void storeSimpleTrigger(SimpleTrigger simpleTrigger);
    
    /**
     * 获取 Simple 触发器
     * 
     * @param triggerId 触发器 ID
     * @return Simple 触发器
     */
    SimpleTrigger retrieveSimpleTrigger(String triggerId);
    
    // ========== 查询方法 ==========
    
    /**
     * 获取所有任务 ID
     * 
     * @return 任务 ID 列表
     */
    List<String> retrieveTaskIds();
    
    /**
     * 获取所有启用的任务
     * 
     * @return 任务定义列表
     */
    List<TaskDefinition> retrieveEnabledTasks();
    
    /**
     * 获取临期任务（即将触发的任务）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 任务定义列表
     */
    List<TaskDefinition> retrieveDueTasks(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 检查 JobStore 是否可用
     * 
     * @return true 如果可用
     */
    boolean isAvailable();
}
