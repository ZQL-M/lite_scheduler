package com.tuba.schedulercore.mapper;

import com.tuba.schedulercore.model.TaskLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 任务执行日志Mapper
 */
public interface TaskLogMapper extends BaseMapper<TaskLog> {
    /**
     * 根据任务ID查询日志
     * 
     * @param taskId 任务ID
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 日志列表
     */
    List<TaskLog> selectByTaskId(String taskId, Integer offset, Integer limit);
}
