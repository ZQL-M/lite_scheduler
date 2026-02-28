package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务定义，包含任务的基本信息和执行目标
 */
@Data
@NoArgsConstructor
@TableName("task_definition")
public class TaskDefinition {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    @TableField("group_name")
    private String groupName = "default";

    @TableField("job_class")
    private String jobClass; // 任务类名（用于TubaJobFactory创建实例）

    private String description;

    private boolean async = true; // 是否异步执行

    private boolean enabled = true; // 任务是否启用

    private boolean persistent = false; // 是否持久化

    private boolean deleted = false; // 是否软删除

    @TableField("created_time")
    private LocalDateTime createdTime; // 创建时间

    @TableField("updated_time")
    private LocalDateTime updatedTime; // 更新时间

    public TaskDefinition(String id, String name, String groupName, String jobClass, String description) {
        this.id = id;
        this.name = name;
        this.groupName = groupName;
        this.jobClass = jobClass;
        this.description = description;
        this.enabled = true;
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }
}