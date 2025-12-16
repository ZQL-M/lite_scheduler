package com.tuba.schedulercore.mapper;

import com.tuba.schedulercore.model.TaskDefinition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务定义Mapper
 */
public interface TaskDefinitionMapper extends BaseMapper<TaskDefinition> {
    /**
     * 查询所有启用的持久化任务
     * 
     * @return 任务列表
     */
    List<TaskDefinition> selectEnabledTasks();

    /**
     * 根据分组查询任务
     * 
     * @param groupName 分组名称
     * @return 任务列表
     */
    List<TaskDefinition> selectByGroup(String groupName);

    /**
     * 根据下次触发时间范围查询临期任务
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 任务列表
     */
    List<TaskDefinition> selectByNextFireTimeRange(@Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
