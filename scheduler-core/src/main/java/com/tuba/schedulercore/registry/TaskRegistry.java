package com.tuba.schedulercore.registry;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.model.TaskStatus;
import org.springframework.stereotype.Component;

/**
 * 任务注册表，管理所有已注册的任务定义
 */
@Component
public class TaskRegistry {
    private final Map<String, TaskDefinition> taskMap = new ConcurrentHashMap<>();

    /**
     * 注册任务
     * @param def 任务定义
     */
    public void register(TaskDefinition def) {
        taskMap.put(def.getId(), def);
    }

    /**
     * 获取任务
     * @param id 任务ID
     * @return 任务定义，如果不存在返回 null
     */
    public TaskDefinition get(String id) {
        return taskMap.get(id);
    }

    /**
     * 获取所有任务
     * @return 所有任务定义的集合
     */
    public Collection<TaskDefinition> getAll() {
        return taskMap.values();
    }

    /**
     * 取消注册任务
     * @param id 任务ID
     */
    public void unregister(String id) {
        taskMap.remove(id);
    }

    /**
     * 检查任务是否存在
     * @param id 任务ID
     * @return 如果存在返回 true
     */
    public boolean contains(String id) { 
        return taskMap.containsKey(id); 
    }

    /**
     * 启用任务
     * @param id 任务ID
     */
    public void enable(String id) {
        TaskDefinition def = taskMap.get(id);
        if (def != null) {
            def.setEnabled(true);
        }
    }

    /**
     * 禁用任务
     * @param id 任务ID
     */
    public void disable(String id) {
        TaskDefinition def = taskMap.get(id);
        if (def != null) {
            def.setEnabled(false);
        }
    }
}