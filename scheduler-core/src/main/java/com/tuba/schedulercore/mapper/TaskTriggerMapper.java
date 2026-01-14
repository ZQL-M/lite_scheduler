package com.tuba.schedulercore.mapper;

import com.tuba.schedulercore.model.TaskTrigger;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 触发器Mapper
 */
public interface TaskTriggerMapper extends BaseMapper<TaskTrigger> {
    /**
     * 根据任务ID查询触发器列表
     * 
     * @param taskId 任务ID
     * @return 触发器列表
     */
    List<TaskTrigger> selectByTaskId(@Param("taskId") String taskId);
    
    /**
     * 查询所有启用的触发器
     * 
     * @return 触发器列表
     */
    List<TaskTrigger> selectEnabledTriggers();
    
    /**
     * 根据触发器类型查询触发器
     * 
     * @param triggerType 触发器类型
     * @return 触发器列表
     */
    List<TaskTrigger> selectByTriggerType(@Param("triggerType") String triggerType);
}