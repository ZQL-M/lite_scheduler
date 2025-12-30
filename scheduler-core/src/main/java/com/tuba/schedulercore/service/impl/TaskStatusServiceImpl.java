package com.tuba.schedulercore.service.impl;

import com.tuba.schedulercore.mapper.TaskStatusMapper;
import com.tuba.schedulercore.model.TaskStatus;
import com.tuba.schedulercore.service.TaskStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 任务状态服务实现类
 * 负责管理任务的实时执行状态和统计信息
 */
@Service
public class TaskStatusServiceImpl implements TaskStatusService {

    private static final Logger log = LoggerFactory.getLogger(TaskStatusServiceImpl.class);

    @Resource
    private TaskStatusMapper taskStatusMapper;

    @Override
    @Transactional
    public void saveOrUpdateTaskStatus(TaskStatus taskStatus) {
        if (taskStatus == null || taskStatus.getTaskId() == null) {
            throw new IllegalArgumentException("TaskStatus or taskId cannot be null");
        }

        // 设置更新时间
        taskStatus.setUpdatedTime(LocalDateTime.now());

        // 检查是否已存在
        TaskStatus existingStatus = taskStatusMapper.selectById(taskStatus.getTaskId());
        if (existingStatus == null) {
            // 新增
            taskStatusMapper.insert(taskStatus);
            log.info("Successfully created task status for task: {}", taskStatus.getTaskId());
        } else {
            // 更新
            taskStatusMapper.updateById(taskStatus);
            log.debug("Successfully updated task status for task: {}", taskStatus.getTaskId());
        }
    }

    @Override
    public TaskStatus getTaskStatus(String taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID cannot be null");
        }
        return taskStatusMapper.selectById(taskId);
    }

    @Override
    @Transactional
    public void updateExecutionStatus(String taskId, String status, LocalDateTime lastFireTime,
            LocalDateTime nextFireTime) {
        if (taskId == null || status == null) {
            throw new IllegalArgumentException("Task ID or status cannot be null");
        }

        // 获取现有状态或创建新状态
        TaskStatus taskStatus = getTaskStatus(taskId);
        if (taskStatus == null) {
            taskStatus = new TaskStatus(taskId);
        }

        // 更新状态
        taskStatus.setLastFireTime(lastFireTime);
        taskStatus.setNextFireTime(nextFireTime);
        taskStatus.setLastExecutionStatus(status);

        // 更新连续失败次数
        if ("FAILURE".equals(status)) {
            taskStatus.setConsecutiveFailures(taskStatus.getConsecutiveFailures() + 1);
        } else if ("SUCCESS".equals(status)) {
            taskStatus.setConsecutiveFailures(0);
        }

        // 更新总执行次数和成功/失败次数
        taskStatus.setTotalExecutions(taskStatus.getTotalExecutions() + 1);
        if ("SUCCESS".equals(status)) {
            taskStatus.setTotalSuccesses(taskStatus.getTotalSuccesses() + 1);
        } else if ("FAILURE".equals(status)) {
            taskStatus.setTotalFailures(taskStatus.getTotalFailures() + 1);
        }

        // 保存更新
        saveOrUpdateTaskStatus(taskStatus);

        log.debug("Updated execution status for task: {} - Status: {}, Last Fire Time: {}, Next Fire Time: {}",
                taskId, status, lastFireTime, nextFireTime);
    }

    @Override
    @Transactional
    public void incrementExecutionCount(String taskId, boolean success) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID cannot be null");
        }

        // 获取现有状态或创建新状态
        TaskStatus taskStatus = getTaskStatus(taskId);
        if (taskStatus == null) {
            taskStatus = new TaskStatus(taskId);
        }

        // 更新执行次数
        taskStatus.setTotalExecutions(taskStatus.getTotalExecutions() + 1);
        if (success) {
            taskStatus.setTotalSuccesses(taskStatus.getTotalSuccesses() + 1);
            taskStatus.setConsecutiveFailures(0);
        } else {
            taskStatus.setTotalFailures(taskStatus.getTotalFailures() + 1);
            taskStatus.setConsecutiveFailures(taskStatus.getConsecutiveFailures() + 1);
        }

        // 保存更新
        saveOrUpdateTaskStatus(taskStatus);

        log.debug("Incremented execution count for task: {} - Success: {}", taskId, success);
    }

    @Override
    @Transactional
    public void resetTaskStatus(String taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID cannot be null");
        }

        // 创建新的状态对象，重置所有统计信息
        TaskStatus taskStatus = new TaskStatus(taskId);
        taskStatus.setLastFireTime(null);
        taskStatus.setNextFireTime(null);
        taskStatus.setLastExecutionStatus("NONE");
        taskStatus.setConsecutiveFailures(0);
        taskStatus.setTotalExecutions(0);
        taskStatus.setTotalSuccesses(0);
        taskStatus.setTotalFailures(0);

        // 保存更新
        saveOrUpdateTaskStatus(taskStatus);

        log.info("Reset task status for task: {}", taskId);
    }

    @Override
    @Transactional
    public void deleteTaskStatus(String taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID cannot be null");
        }

        // 删除任务状态
        int result = taskStatusMapper.deleteById(taskId);
        if (result > 0) {
            log.info("Successfully deleted task status for task: {}", taskId);
        } else {
            log.debug("No task status found for task: {}", taskId);
        }
    }
}