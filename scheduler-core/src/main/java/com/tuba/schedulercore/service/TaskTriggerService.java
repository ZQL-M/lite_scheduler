package com.tuba.schedulercore.service;

import com.tuba.schedulercore.model.CronTrigger;
import com.tuba.schedulercore.model.SimpleTrigger;
import com.tuba.schedulercore.model.TaskTrigger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务触发器服务接口
 * 负责管理任务触发器的创建、更新、删除和查询
 */
public interface TaskTriggerService {
    /**
     * 创建Cron触发器
     *
     * @param taskId             任务ID
     * @param cronExpression     Cron表达式
     * @param startTime          开始时间
     * @param endTime            结束时间
     * @param priority           优先级
     * @param timeZoneId         时区
     * @param misfireInstruction 错过执行策略
     * @return 创建的触发器ID
     */
    String createCronTrigger(String taskId, String cronExpression, LocalDateTime startTime, LocalDateTime endTime,
            Integer priority, String timeZoneId, Integer misfireInstruction);

    /**
     * 创建固定频率触发器
     *
     * @param taskId             任务ID
     * @param repeatCount        重复次数
     * @param repeatInterval     重复间隔（毫秒）
     * @param startTime          开始时间
     * @param endTime            结束时间
     * @param priority           优先级
     * @param misfireInstruction 错过执行策略
     * @return 创建的触发器ID
     */
    String createFixedRateTrigger(String taskId, int repeatCount, long repeatInterval,
            LocalDateTime startTime, LocalDateTime endTime, Integer priority,
            Integer misfireInstruction);

    /**
     * 创建固定延迟触发器
     *
     * @param taskId             任务ID
     * @param repeatCount        重复次数
     * @param repeatInterval     重复间隔（毫秒）
     * @param startTime          开始时间
     * @param endTime            结束时间
     * @param priority           优先级
     * @param misfireInstruction 错过执行策略
     * @return 创建的触发器ID
     */
    String createFixedDelayTrigger(String taskId, int repeatCount, long repeatInterval,
            LocalDateTime startTime, LocalDateTime endTime, Integer priority,
            Integer misfireInstruction);

    /**
     * 更新触发器
     *
     * @param trigger 触发器对象
     */
    void updateTrigger(TaskTrigger trigger);

    /**
     * 删除触发器
     *
     * @param triggerId 触发器ID
     */
    void deleteTrigger(String triggerId);

    /**
     * 根据触发器ID获取触发器
     *
     * @param triggerId 触发器ID
     * @return 触发器对象
     */
    TaskTrigger getTrigger(String triggerId);

    /**
     * 根据任务ID获取所有触发器
     *
     * @param taskId 任务ID
     * @return 触发器列表
     */
    List<TaskTrigger> getTriggersByTaskId(String taskId);

    /**
     * 获取所有启用的触发器
     *
     * @return 触发器列表
     */
    List<TaskTrigger> getAllEnabledTriggers();

    /**
     * 根据触发器ID获取Cron触发器
     *
     * @param triggerId 触发器ID
     * @return Cron触发器对象
     */
    CronTrigger getCronTrigger(String triggerId);

    /**
     * 根据触发器ID获取简单触发器
     *
     * @param triggerId 触发器ID
     * @return 简单触发器对象
     */
    SimpleTrigger getSimpleTrigger(String triggerId);

    /**
     * 启用触发器
     *
     * @param triggerId 触发器ID
     */
    void enableTrigger(String triggerId);

    /**
     * 禁用触发器
     *
     * @param triggerId 触发器ID
     */
    void disableTrigger(String triggerId);
}