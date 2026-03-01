package com.tuba.schedulercore.persistence.impl;

import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.persistence.TaskPersistenceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存任务持久化服务实现
 */
@Service
public class InMemoryTaskPersistenceServiceImpl implements TaskPersistenceService {

    private final Map<String, TaskDefinition> taskMap = new ConcurrentHashMap<>();

    @Override
    public void save(TaskDefinition definition) {
        taskMap.put(definition.getId(), definition);
    }

    @Override
    public TaskDefinition findById(String id) {
        return taskMap.get(id);
    }

    @Override
    public List<TaskDefinition> findAll() {
        return new ArrayList<>(taskMap.values());
    }

    @Override
    public List<TaskDefinition> findEnabledTasks() {
        List<TaskDefinition> enabledTasks = new ArrayList<>();
        for (TaskDefinition task : taskMap.values()) {
            if (task.isEnabled()) {
                enabledTasks.add(task);
            }
        }
        return enabledTasks;
    }

    @Override
    public List<TaskDefinition> findTasksByNextFireTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<TaskDefinition> tasksInRange = new ArrayList<>();
        for (TaskDefinition task : taskMap.values()) {
            if (task.isEnabled()) {
                // 这里简化处理，实际应该从TaskStatus表中获取nextFireTime
                //todo  暂时返回所有启用的任务
                tasksInRange.add(task);
            }
        }
        return tasksInRange;
    }

    @Override
    public void update(TaskDefinition definition) {
        taskMap.put(definition.getId(), definition);
    }

    @Override
    public void delete(String id) {
        taskMap.remove(id);
    }

    @Override
    public boolean isAvailable() {
        // 内存持久化服务始终可用
        return true;
    }
}
