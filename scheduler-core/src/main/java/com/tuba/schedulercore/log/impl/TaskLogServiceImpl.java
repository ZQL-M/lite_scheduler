package com.tuba.schedulercore.log.impl;

import com.tuba.schedulercore.log.TaskLogService;
import com.tuba.schedulercore.mapper.TaskLogMapper;
import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskLog;
import com.tuba.schedulercore.enums.ExecutionStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 任务执行日志服务实现
 */
@Service
public class TaskLogServiceImpl implements TaskLogService {

    @Resource
    private TaskLogMapper taskLogMapper;

    @Override
    public TaskLog recordTaskStart(TaskContext context) {
        TaskLog log = new TaskLog();
        log.setTaskId(context.getDefinition().getId());
        log.setStartTime(java.time.LocalDateTime.now());
        log.setStatus(ExecutionStatus.RUNNING);
        taskLogMapper.insert(log);
        return log;
    }

    @Override
    public void recordTaskSuccess(TaskContext context, TaskLog log) {
        log.setEndTime(java.time.LocalDateTime.now());
        log.setDurationMs(calculateDuration(log.getStartTime(), log.getEndTime()));
        log.setStatus(ExecutionStatus.SUCCESS);
        taskLogMapper.updateById(log);
    }

    @Override
    public void recordTaskFailure(TaskContext context, TaskLog log, Throwable error) {
        log.setEndTime(java.time.LocalDateTime.now());
        log.setDurationMs(calculateDuration(log.getStartTime(), log.getEndTime()));
        log.setStatus(ExecutionStatus.FAILURE);
        log.setErrorMessage(error.getMessage());
        taskLogMapper.updateById(log);
    }

    @Override
    public List<TaskLog> queryByTaskId(String taskId, int offset, int limit) {
        return taskLogMapper.selectByTaskId(taskId, offset, limit);
    }

    private long calculateDuration(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }
}
