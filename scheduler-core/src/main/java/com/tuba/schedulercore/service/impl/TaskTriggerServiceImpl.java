package com.tuba.schedulercore.service.impl;

import com.tuba.schedulercore.mapper.CronTriggerMapper;
import com.tuba.schedulercore.mapper.SimpleTriggerMapper;
import com.tuba.schedulercore.mapper.TaskDefinitionMapper;
import com.tuba.schedulercore.mapper.TaskTriggerMapper;
import com.tuba.schedulercore.model.CronTrigger;
import com.tuba.schedulercore.model.SimpleTrigger;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.model.TaskTrigger;
import com.tuba.schedulercore.service.TaskTriggerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 任务触发器服务实现类
 * 负责管理任务触发器的创建、更新、删除和查询
 */
@Service
public class TaskTriggerServiceImpl implements TaskTriggerService {

    private static final Logger log = LoggerFactory.getLogger(TaskTriggerServiceImpl.class);

    @Resource
    private TaskTriggerMapper taskTriggerMapper;

    @Resource
    private CronTriggerMapper cronTriggerMapper;

    @Resource
    private SimpleTriggerMapper simpleTriggerMapper;

    @Resource
    private TaskDefinitionMapper taskDefinitionMapper;

    @Override
    @Transactional
    public String createCronTrigger(String taskId, String cronExpression, LocalDateTime startTime,
            LocalDateTime endTime, Integer priority, String timeZoneId,
            Integer misfireInstruction) {
        // 验证任务是否存在
        TaskDefinition task = taskDefinitionMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        // 创建触发器基本信息
        String triggerId = UUID.randomUUID().toString();
        TaskTrigger trigger = new TaskTrigger();
        trigger.setId(triggerId);
        trigger.setTaskId(taskId);
        trigger.setTriggerType("CRON");
        trigger.setStartTime(startTime);
        trigger.setEndTime(endTime);
        trigger.setPriority(priority != null ? priority : 5);
        trigger.setEnabled(true);
        trigger.setCreatedTime(LocalDateTime.now());
        trigger.setUpdatedTime(LocalDateTime.now());

        // 保存触发器
        taskTriggerMapper.insert(trigger);

        // 创建Cron触发器详细信息
        CronTrigger cronTrigger = new CronTrigger();
        cronTrigger.setTriggerId(triggerId);
        cronTrigger.setCronExpression(cronExpression);
        cronTrigger.setTimeZoneId(timeZoneId != null ? timeZoneId : "Asia/Shanghai");
        cronTrigger.setMisfireInstruction(misfireInstruction != null ? misfireInstruction : 0);

        // 保存Cron触发器
        cronTriggerMapper.insert(cronTrigger);

        log.info("Successfully created Cron trigger: {} for task: {} (ID: {})",
                cronExpression, task.getName(), taskId);

        return triggerId;
    }

    @Override
    @Transactional
    public String createFixedRateTrigger(String taskId, int repeatCount, long repeatInterval,
            LocalDateTime startTime, LocalDateTime endTime,
            Integer priority, Integer misfireInstruction) {
        return createSimpleTrigger(taskId, repeatCount, repeatInterval, "FIXED_RATE",
                startTime, endTime, priority, misfireInstruction);
    }

    @Override
    @Transactional
    public String createFixedDelayTrigger(String taskId, int repeatCount, long repeatInterval,
            LocalDateTime startTime, LocalDateTime endTime,
            Integer priority, Integer misfireInstruction) {
        return createSimpleTrigger(taskId, repeatCount, repeatInterval, "FIXED_DELAY",
                startTime, endTime, priority, misfireInstruction);
    }

    @Override
    @Transactional
    public void updateTrigger(TaskTrigger trigger) {
        if (trigger == null || trigger.getId() == null) {
            throw new IllegalArgumentException("Trigger or trigger ID cannot be null");
        }

        trigger.setUpdatedTime(LocalDateTime.now());
        taskTriggerMapper.updateById(trigger);
        log.info("Successfully updated trigger: {} (ID: {})", trigger.getTriggerType(), trigger.getId());
    }

    @Override
    @Transactional
    public void deleteTrigger(String triggerId) {
        if (triggerId == null) {
            throw new IllegalArgumentException("Trigger ID cannot be null");
        }

        // 删除Cron触发器（如果存在）
        cronTriggerMapper.deleteById(triggerId);

        // 删除简单触发器（如果存在）
        simpleTriggerMapper.deleteById(triggerId);

        // 删除触发器基本信息
        taskTriggerMapper.deleteById(triggerId);

        log.info("Successfully deleted trigger: {}", triggerId);
    }

    @Override
    public TaskTrigger getTrigger(String triggerId) {
        if (triggerId == null) {
            throw new IllegalArgumentException("Trigger ID cannot be null");
        }
        return taskTriggerMapper.selectById(triggerId);
    }

    @Override
    public List<TaskTrigger> getTriggersByTaskId(String taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID cannot be null");
        }
        return taskTriggerMapper.selectByTaskId(taskId);
    }

    @Override
    public List<TaskTrigger> getAllEnabledTriggers() {
        return taskTriggerMapper.selectEnabledTriggers();
    }

    @Override
    public CronTrigger getCronTrigger(String triggerId) {
        if (triggerId == null) {
            throw new IllegalArgumentException("Trigger ID cannot be null");
        }
        return cronTriggerMapper.selectById(triggerId);
    }

    @Override
    public SimpleTrigger getSimpleTrigger(String triggerId) {
        if (triggerId == null) {
            throw new IllegalArgumentException("Trigger ID cannot be null");
        }
        return simpleTriggerMapper.selectById(triggerId);
    }

    @Override
    @Transactional
    public void enableTrigger(String triggerId) {
        TaskTrigger trigger = getTrigger(triggerId);
        if (trigger != null) {
            trigger.setEnabled(true);
            updateTrigger(trigger);
            log.info("Successfully enabled trigger: {}", triggerId);
        }
    }

    @Override
    @Transactional
    public void disableTrigger(String triggerId) {
        TaskTrigger trigger = getTrigger(triggerId);
        if (trigger != null) {
            trigger.setEnabled(false);
            updateTrigger(trigger);
            log.info("Successfully disabled trigger: {}", triggerId);
        }
    }

    /**
     * 创建简单触发器（固定频率或固定延迟）
     */
    private String createSimpleTrigger(String taskId, int repeatCount, long repeatInterval,
            String simpleType, LocalDateTime startTime,
            LocalDateTime endTime, Integer priority,
            Integer misfireInstruction) {
        // 验证任务是否存在
        TaskDefinition task = taskDefinitionMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found with ID: " + taskId);
        }

        // 创建触发器基本信息
        String triggerId = UUID.randomUUID().toString();
        TaskTrigger trigger = new TaskTrigger();
        trigger.setId(triggerId);
        trigger.setTaskId(taskId);
        trigger.setTriggerType(simpleType);
        trigger.setStartTime(startTime);
        trigger.setEndTime(endTime);
        trigger.setPriority(priority != null ? priority : 5);
        trigger.setEnabled(true);
        trigger.setCreatedTime(LocalDateTime.now());
        trigger.setUpdatedTime(LocalDateTime.now());

        // 保存触发器
        taskTriggerMapper.insert(trigger);

        // 创建简单触发器详细信息
        SimpleTrigger simpleTrigger = new SimpleTrigger();
        simpleTrigger.setTriggerId(triggerId);
        simpleTrigger.setRepeatCount(repeatCount);
        simpleTrigger.setRepeatInterval(repeatInterval);
        simpleTrigger.setSimpleType(simpleType);
        simpleTrigger.setMisfireInstruction(misfireInstruction != null ? misfireInstruction : 0);

        // 保存简单触发器
        simpleTriggerMapper.insert(simpleTrigger);

        log.info("Successfully created {} trigger: repeatCount={}, repeatInterval={}ms for task: {} (ID: {})",
                simpleType, repeatCount, repeatInterval, task.getName(), taskId);

        return triggerId;
    }
}