package com.tuba.schedulercore.service;

import com.tuba.schedulercore.model.TaskTrigger;
import com.tuba.schedulercore.model.CronTrigger;
import com.tuba.schedulercore.model.SimpleTrigger;

import java.util.List;

/**
 * 触发器服务接口
 */
public interface TaskTriggerService {
    /**
     * 根据任务ID查询触发器
     *
     * @param taskId 任务ID
     * @return 触发器列表
     */
    List<TaskTrigger> findByTaskId(String taskId);

    /**
     * 根据触发器ID查询Cron触发器
     *
     * @param triggerId 触发器ID
     * @return Cron触发器
     */
    CronTrigger findCronTriggerByTriggerId(String triggerId);

    /**
     * 根据触发器ID查询Simple触发器
     *
     * @param triggerId 触发器ID
     * @return Simple触发器
     */
    SimpleTrigger findSimpleTriggerByTriggerId(String triggerId);

    /**
     * 查询所有启用的触发器
     *
     * @return 触发器列表
     */
    List<TaskTrigger> findEnabledTriggers();
}
