package com.tuba.schedulercore.core.persistence;

import com.tuba.schedulercore.model.TaskDefinition;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务持久化服务接口
 */
public interface TaskPersistenceService {
    /**
     * 保存任务定义
     * 
     * @param definition 任务定义
     */
    void save(TaskDefinition definition);

    /**
     * 根据ID查询任务定义
     * 
     * @param id 任务ID
     * @return 任务定义
     */
    TaskDefinition findById(String id);

    /**
     * 查询所有任务定义
     * 
     * @return 任务定义列表
     */
    List<TaskDefinition> findAll();

    /**
     * 查询所有启用的持久化任务
     * 
     * @return 任务定义列表
     */
    List<TaskDefinition> findEnabledTasks();

    /**
     * 根据下次触发时间范围查询临期任务
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 任务定义列表
     */
    List<TaskDefinition> findTasksByNextFireTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 更新任务定义
     * 
     * @param definition 任务定义
     */
    void update(TaskDefinition definition);

    /**
     * 删除任务定义
     * 
     * @param id 任务ID
     */
    void delete(String id);

    /**
     * 检查持久化服务是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
}
