package com.tuba.schedulercore.log;

import com.tuba.schedulercore.model.TaskContext;
import com.tuba.schedulercore.model.TaskLog;

import java.util.List;

/**
 * 任务执行日志服务接口
 */
public interface TaskLogService {
    /**
     * 记录任务开始执行
     * 
     * @param context 任务执行上下文
     * @return 任务日志
     */
    TaskLog recordTaskStart(TaskContext context);

    /**
     * 记录任务执行成功
     * 
     * @param context 任务执行上下文
     * @param log     任务日志
     */
    void recordTaskSuccess(TaskContext context, TaskLog log);

    /**
     * 记录任务执行失败
     * 
     * @param context 任务执行上下文
     * @param log     任务日志
     * @param error   异常信息
     */
    void recordTaskFailure(TaskContext context, TaskLog log, Throwable error);

    /**
     * 根据任务ID查询日志
     * 
     * @param taskId 任务ID
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 日志列表
     */
    List<TaskLog> queryByTaskId(String taskId, int offset, int limit);
}
