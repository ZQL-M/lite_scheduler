package com.tuba.schedulercore.core.store.impl;

import com.tuba.schedulercore.mapper.TaskDefinitionMapper;
import com.tuba.schedulercore.mapper.TaskStatusMapper;
import com.tuba.schedulercore.mapper.TaskTriggerMapper;
import com.tuba.schedulercore.mapper.CronTriggerMapper;
import com.tuba.schedulercore.mapper.SimpleTriggerMapper;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.model.TaskStatus;
import com.tuba.schedulercore.model.TaskTrigger;
import com.tuba.schedulercore.model.CronTrigger;
import com.tuba.schedulercore.model.SimpleTrigger;
import com.tuba.schedulercore.core.store.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库作业存储实现
 *
 */
@Component
public class DatabaseJobStore implements JobStore {

    private static final Logger log = LoggerFactory.getLogger(DatabaseJobStore.class);

    @Resource
    private TaskDefinitionMapper taskDefinitionMapper;

    @Resource
    private TaskStatusMapper taskStatusMapper;

    @Resource
    private TaskTriggerMapper taskTriggerMapper;

    @Resource
    private CronTriggerMapper cronTriggerMapper;

    @Resource
    private SimpleTriggerMapper simpleTriggerMapper;

    @Override
    public void storeTaskDefinition(TaskDefinition definition, boolean replace) {
        if (definition == null) {
            throw new IllegalArgumentException("TaskDefinition cannot be null");
        }

        if (replace && isTaskExists(definition.getId())) {
            taskDefinitionMapper.updateById(definition);
            log.debug("Updated TaskDefinition: {}", definition.getId());
        } else {
            taskDefinitionMapper.insert(definition);
            log.debug("Inserted TaskDefinition: {}", definition.getId());
        }
    }

    @Override
    public TaskDefinition retrieveTaskDefinition(String taskId) {
        if (taskId == null) {
            return null;
        }
        return taskDefinitionMapper.selectById(taskId);
    }

    @Override
    public boolean removeTaskDefinition(String taskId) {
        if (taskId == null) {
            return false;
        }
        int rows = taskDefinitionMapper.deleteById(taskId);
        log.debug("Removed TaskDefinition: {}, rows: {}", taskId, rows);
        return rows > 0;
    }

    @Override
    public boolean isTaskExists(String taskId) {
        if (taskId == null) {
            return false;
        }
        TaskDefinition definition = taskDefinitionMapper.selectById(taskId);
        return definition != null;
    }

    @Override
    public void storeTaskStatus(TaskStatus status, boolean replace) {
        if (status == null) {
            throw new IllegalArgumentException("TaskStatus cannot be null");
        }

        if (replace && taskStatusMapper.selectById(status.getTaskId()) != null) {
            taskStatusMapper.updateById(status);
            log.debug("Updated TaskStatus: {}", status.getTaskId());
        } else {
            taskStatusMapper.insert(status);
            log.debug("Inserted TaskStatus: {}", status.getTaskId());
        }
    }

    @Override
    public TaskStatus retrieveTaskStatus(String taskId) {
        if (taskId == null) {
            return null;
        }
        return taskStatusMapper.selectById(taskId);
    }

    @Override
    public boolean removeTaskStatus(String taskId) {
        if (taskId == null) {
            return false;
        }
        int rows = taskStatusMapper.deleteById(taskId);
        log.debug("Removed TaskStatus: {}, rows: {}", taskId, rows);
        return rows > 0;
    }

    @Override
    public void storeTrigger(TaskTrigger trigger, boolean replace) {
        if (trigger == null) {
            throw new IllegalArgumentException("TaskTrigger cannot be null");
        }

        if (replace && taskTriggerMapper.selectById(trigger.getId()) != null) {
            taskTriggerMapper.updateById(trigger);
            log.debug("Updated TaskTrigger: {}", trigger.getId());
        } else {
            taskTriggerMapper.insert(trigger);
            log.debug("Inserted TaskTrigger: {}", trigger.getId());
        }
    }

    @Override
    public TaskTrigger retrieveTrigger(String triggerId) {
        if (triggerId == null) {
            return null;
        }
        return taskTriggerMapper.selectById(triggerId);
    }

    @Override
    public List<TaskTrigger> retrieveTriggersByTaskId(String taskId) {
        if (taskId == null) {
            return List.of();
        }
        return taskTriggerMapper.selectByTaskId(taskId);
    }

    @Override
    public boolean removeTrigger(String triggerId) {
        if (triggerId == null) {
            return false;
        }
        int rows = taskTriggerMapper.deleteById(triggerId);
        log.debug("Removed TaskTrigger: {}, rows: {}", triggerId, rows);
        return rows > 0;
    }

    @Override
    public void storeCronTrigger(CronTrigger cronTrigger) {
        if (cronTrigger == null) {
            throw new IllegalArgumentException("CronTrigger cannot be null");
        }

        if (cronTriggerMapper.selectById(cronTrigger.getTriggerId()) != null) {
            cronTriggerMapper.updateById(cronTrigger);
            log.debug("Updated CronTrigger: {}", cronTrigger.getTriggerId());
        } else {
            cronTriggerMapper.insert(cronTrigger);
            log.debug("Inserted CronTrigger: {}", cronTrigger.getTriggerId());
        }
    }

    @Override
    public CronTrigger retrieveCronTrigger(String triggerId) {
        if (triggerId == null) {
            return null;
        }
        return cronTriggerMapper.selectById(triggerId);
    }

    @Override
    public void storeSimpleTrigger(SimpleTrigger simpleTrigger) {
        if (simpleTrigger == null) {
            throw new IllegalArgumentException("SimpleTrigger cannot be null");
        }

        if (simpleTriggerMapper.selectById(simpleTrigger.getTriggerId()) != null) {
            simpleTriggerMapper.updateById(simpleTrigger);
            log.debug("Updated SimpleTrigger: {}", simpleTrigger.getTriggerId());
        } else {
            simpleTriggerMapper.insert(simpleTrigger);
            log.debug("Inserted SimpleTrigger: {}", simpleTrigger.getTriggerId());
        }
    }

    @Override
    public SimpleTrigger retrieveSimpleTrigger(String triggerId) {
        if (triggerId == null) {
            return null;
        }
        return simpleTriggerMapper.selectById(triggerId);
    }

    @Override
    public List<String> retrieveTaskIds() {
        List<TaskDefinition> tasks = taskDefinitionMapper.selectList(null);
        List<String> taskIds = new ArrayList<>();
        for (TaskDefinition task : tasks) {
            taskIds.add(task.getId());
        }
        return taskIds;
    }

    @Override
    public List<TaskDefinition> retrieveEnabledTasks() {
        return taskDefinitionMapper.selectEnabledTasks();
    }

    @Override
    public List<TaskDefinition> retrieveDueTasks(LocalDateTime startTime, LocalDateTime endTime) {
        return taskDefinitionMapper.selectByNextFireTimeRange(startTime, endTime);
    }

    @Override
    public boolean isAvailable() {
        try {
            taskDefinitionMapper.selectCount(null);
            return true;
        } catch (Exception e) {
            log.warn("JobStore is not available: {}", e.getMessage());
            return false;
        }
    }
}
