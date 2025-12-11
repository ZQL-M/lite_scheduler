package com.tuba.schedulercore.persistence.impl;

import com.tuba.schedulercore.mapper.TaskDefinitionMapper;
import com.tuba.schedulercore.model.TaskDefinition;
import com.tuba.schedulercore.persistence.TaskPersistenceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 数据库任务持久化服务实现
 */
@Service
public class DatabaseTaskPersistenceServiceImpl implements TaskPersistenceService {

   @Resource
    private TaskDefinitionMapper taskDefinitionMapper;

    @Override
    public void save(TaskDefinition definition) {
        taskDefinitionMapper.insert(definition);
    }

    @Override
    public TaskDefinition findById(String id) {
        return taskDefinitionMapper.selectById(id);
    }

    @Override
    public List<TaskDefinition> findAll() {
        return taskDefinitionMapper.selectList(null);
    }

    @Override
    public List<TaskDefinition> findEnabledTasks() {
        return taskDefinitionMapper.selectEnabledTasks();
    }

    @Override
    public void update(TaskDefinition definition) {
        taskDefinitionMapper.updateById(definition);
    }

    @Override
    public void delete(String id) {
        taskDefinitionMapper.deleteById(id);
    }

    @Override
    public boolean isAvailable() {
        try {
            // 简单检查数据库连接是否可用
            taskDefinitionMapper.selectCount(null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
