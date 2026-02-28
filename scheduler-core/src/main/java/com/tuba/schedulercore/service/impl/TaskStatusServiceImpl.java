package com.tuba.schedulercore.service.impl;

import com.tuba.schedulercore.mapper.TaskStatusMapper;
import com.tuba.schedulercore.model.TaskStatus;
import com.tuba.schedulercore.service.TaskStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 任务状态服务实现类
 */
@Service
public class TaskStatusServiceImpl implements TaskStatusService {

    @Autowired
    private TaskStatusMapper statusMapper;

    @Override
    public TaskStatus getStatus(String taskId) {
        return statusMapper.selectById(taskId);
    }

    @Override
    public void updateStatus(TaskStatus status) {
        if (statusMapper.selectById(status.getTaskId()) == null) {
            statusMapper.insert(status);
        } else {
            statusMapper.updateById(status);
        }
    }

    @Override
    public void recordSuccess(String taskId, String executionId) {
        TaskStatus status = statusMapper.selectById(taskId);
        if (status == null) {
            status = new TaskStatus(taskId);
        }
        status.recordSuccess(executionId);
        updateStatus(status);
    }

    @Override
    public void recordFailure(String taskId, String executionId) {
        TaskStatus status = statusMapper.selectById(taskId);
        if (status == null) {
            status = new TaskStatus(taskId);
        }
        status.recordFailure(executionId);
        updateStatus(status);
    }

    @Override
    public void updateNextFireTime(String taskId, LocalDateTime nextFireTime) {
        TaskStatus status = statusMapper.selectById(taskId);
        if (status == null) {
            status = new TaskStatus(taskId);
        }
        status.updateNextFireTime(nextFireTime);
        updateStatus(status);
    }
}