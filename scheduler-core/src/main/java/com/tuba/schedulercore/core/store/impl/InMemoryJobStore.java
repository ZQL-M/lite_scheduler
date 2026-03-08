package com.tuba.schedulercore.core.store.impl;

import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.model.TaskStatus;
import com.tuba.schedulercore.model.TaskTrigger;
import com.tuba.schedulercore.model.CronTrigger;
import com.tuba.schedulercore.model.SimpleTrigger;
import com.tuba.schedulercore.core.store.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存作业存储实现
 * 适用于测试和不需持久化的场景
 */
@Component
@ConditionalOnProperty(name = "lite-scheduler.persistence", havingValue = "memory", matchIfMissing = false)
public class InMemoryJobStore implements JobStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryJobStore.class);

    private final Map<String, TaskDefinition> taskDefinitionMap = new ConcurrentHashMap<>();
    private final Map<String, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();
    private final Map<String, TaskTrigger> taskTriggerMap = new ConcurrentHashMap<>();
    private final Map<String, CronTrigger> cronTriggerMap = new ConcurrentHashMap<>();
    private final Map<String, SimpleTrigger> simpleTriggerMap = new ConcurrentHashMap<>();

    @Override
    public void storeTaskDefinition(TaskDefinition definition, boolean replace) {
        if (definition == null) {
            throw new IllegalArgumentException("TaskDefinition cannot be null");
        }

        if (replace) {
            taskDefinitionMap.put(definition.getId(), definition);
            log.debug("Updated TaskDefinition: {}", definition.getId());
        } else {
            taskDefinitionMap.putIfAbsent(definition.getId(), definition);
            log.debug("Inserted TaskDefinition: {}", definition.getId());
        }
    }

    @Override
    public TaskDefinition retrieveTaskDefinition(String taskId) {
        if (taskId == null) {
            return null;
        }
        return taskDefinitionMap.get(taskId);
    }

    @Override
    public boolean removeTaskDefinition(String taskId) {
        if (taskId == null) {
            return false;
        }
        TaskDefinition removed = taskDefinitionMap.remove(taskId);
        log.debug("Removed TaskDefinition: {}", taskId);
        return removed != null;
    }

    @Override
    public boolean isTaskExists(String taskId) {
        if (taskId == null) {
            return false;
        }
        return taskDefinitionMap.containsKey(taskId);
    }

    @Override
    public void storeTaskStatus(TaskStatus status, boolean replace) {
        if (status == null) {
            throw new IllegalArgumentException("TaskStatus cannot be null");
        }

        if (replace) {
            taskStatusMap.put(status.getTaskId(), status);
            log.debug("Updated TaskStatus: {}", status.getTaskId());
        } else {
            taskStatusMap.putIfAbsent(status.getTaskId(), status);
            log.debug("Inserted TaskStatus: {}", status.getTaskId());
        }
    }

    @Override
    public TaskStatus retrieveTaskStatus(String taskId) {
        if (taskId == null) {
            return null;
        }
        return taskStatusMap.get(taskId);
    }

    @Override
    public boolean removeTaskStatus(String taskId) {
        if (taskId == null) {
            return false;
        }
        TaskStatus removed = taskStatusMap.remove(taskId);
        log.debug("Removed TaskStatus: {}", taskId);
        return removed != null;
    }

    @Override
    public void storeTrigger(TaskTrigger trigger, boolean replace) {
        if (trigger == null) {
            throw new IllegalArgumentException("TaskTrigger cannot be null");
        }

        if (replace) {
            taskTriggerMap.put(trigger.getId(), trigger);
            log.debug("Updated TaskTrigger: {}", trigger.getId());
        } else {
            taskTriggerMap.putIfAbsent(trigger.getId(), trigger);
            log.debug("Inserted TaskTrigger: {}", trigger.getId());
        }
    }

    @Override
    public TaskTrigger retrieveTrigger(String triggerId) {
        if (triggerId == null) {
            return null;
        }
        return taskTriggerMap.get(triggerId);
    }

    @Override
    public List<TaskTrigger> retrieveTriggersByTaskId(String taskId) {
        if (taskId == null) {
            return List.of();
        }
        return taskTriggerMap.values().stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean removeTrigger(String triggerId) {
        if (triggerId == null) {
            return false;
        }
        TaskTrigger removed = taskTriggerMap.remove(triggerId);
        log.debug("Removed TaskTrigger: {}", triggerId);
        return removed != null;
    }

    @Override
    public void storeCronTrigger(CronTrigger cronTrigger) {
        if (cronTrigger == null) {
            throw new IllegalArgumentException("CronTrigger cannot be null");
        }

        cronTriggerMap.put(cronTrigger.getTriggerId(), cronTrigger);
        log.debug("Stored CronTrigger: {}", cronTrigger.getTriggerId());
    }

    @Override
    public CronTrigger retrieveCronTrigger(String triggerId) {
        if (triggerId == null) {
            return null;
        }
        return cronTriggerMap.get(triggerId);
    }

    @Override
    public void storeSimpleTrigger(SimpleTrigger simpleTrigger) {
        if (simpleTrigger == null) {
            throw new IllegalArgumentException("SimpleTrigger cannot be null");
        }

        simpleTriggerMap.put(simpleTrigger.getTriggerId(), simpleTrigger);
        log.debug("Stored SimpleTrigger: {}", simpleTrigger.getTriggerId());
    }

    @Override
    public SimpleTrigger retrieveSimpleTrigger(String triggerId) {
        if (triggerId == null) {
            return null;
        }
        return simpleTriggerMap.get(triggerId);
    }

    @Override
    public List<String> retrieveTaskIds() {
        return new ArrayList<>(taskDefinitionMap.keySet());
    }

    @Override
    public List<TaskDefinition> retrieveEnabledTasks() {
        return taskDefinitionMap.values().stream()
                .filter(TaskDefinition::isEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDefinition> retrieveDueTasks(LocalDateTime startTime, LocalDateTime endTime) {
        // 内存实现简化处理，返回所有启用的任务
        return taskDefinitionMap.values().stream()
                .filter(TaskDefinition::isEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAvailable() {
        // 内存存储始终可用
        return true;
    }
}
