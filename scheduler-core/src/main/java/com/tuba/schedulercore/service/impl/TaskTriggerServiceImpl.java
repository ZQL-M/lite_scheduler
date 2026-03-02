package com.tuba.schedulercore.service.impl;

import com.tuba.schedulercore.mapper.TaskTriggerMapper;
import com.tuba.schedulercore.mapper.CronTriggerMapper;
import com.tuba.schedulercore.mapper.SimpleTriggerMapper;
import com.tuba.schedulercore.model.TaskTrigger;
import com.tuba.schedulercore.model.CronTrigger;
import com.tuba.schedulercore.model.SimpleTrigger;
import com.tuba.schedulercore.service.TaskTriggerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 触发器服务实现
 */
@Service
public class TaskTriggerServiceImpl implements TaskTriggerService {

    @Resource
    private TaskTriggerMapper taskTriggerMapper;

    @Resource
    private CronTriggerMapper cronTriggerMapper;

    @Resource
    private SimpleTriggerMapper simpleTriggerMapper;

    @Override
    public List<TaskTrigger> findByTaskId(String taskId) {
        return taskTriggerMapper.selectByTaskId(taskId);
    }

    @Override
    public CronTrigger findCronTriggerByTriggerId(String triggerId) {
        return cronTriggerMapper.selectById(triggerId);
    }

    @Override
    public SimpleTrigger findSimpleTriggerByTriggerId(String triggerId) {
        return simpleTriggerMapper.selectById(triggerId);
    }

    @Override
    public List<TaskTrigger> findEnabledTriggers() {
        return taskTriggerMapper.selectEnabledTriggers();
    }
}
