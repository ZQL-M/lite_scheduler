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
        // 在新的设计中，任务的调度信息已经从TaskDefinition中分离出来
        // 简单返回所有已启用的任务，由调度器自行处理触发逻辑
        return findEnabledTasks();
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
