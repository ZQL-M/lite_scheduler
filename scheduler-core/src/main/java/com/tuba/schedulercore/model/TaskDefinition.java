package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tuba.schedulercore.enums.TaskStatus;
import com.tuba.schedulercore.plugin.TaskPlugin;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务定义，包含任务的基本信息和执行目标
 */
@Data
@EqualsAndHashCode(of = "id")
@TableName("task_definition")
public class TaskDefinition {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    @TableField("group_name")
    private String groupName = "default";

    private boolean async = true;

    @TableField(exist = false)
    private Object bean;

    @TableField(exist = false)
    private Method method;

    @TableField("job_class")
    private String jobClass; // 任务类名（用于TubaJobFactory创建实例）

    private String description;

    private boolean enabled = true; // 任务是否启用

    private boolean persistent = false; // 是否持久化

    private boolean deleted = false; // 是否软删除

    @TableField("created_time")
    private LocalDateTime createdTime; // 创建时间

    @TableField("updated_time")
    private LocalDateTime updatedTime; // 更新时间

    @TableField("last_fire_time")
    private LocalDateTime lastFireTime; // 上次执行时间

    @TableField("next_fire_time")
    private LocalDateTime nextFireTime; // 下次执行时间

    @TableField("fixed_rate")
    private long fixedRate; // 固定速率执行间隔（毫秒）

    @TableField("fixed_delay")
    private long fixedDelay; // 固定延迟执行间隔（毫秒）

    private String cron; // CRON表达式

    @TableField("repeat_count")
    private int repeatCount; // 重复执行次数

    private String status; // 任务状态

    @TableField(exist = false)
    private List<TaskPlugin> plugins;

    // constructors

    public TaskDefinition() {
    }

    public TaskDefinition(String id, String name, String groupName, boolean async, Object bean,
            Method method,
            String description) {
        this.id = id;
        this.name = name;
        this.groupName = groupName;
        this.async = async;
        this.bean = bean;
        this.method = method;
        this.description = description;
        this.enabled = true;
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }

    // 自定义方法，处理TaskStatus类型
    public void setStatus(TaskStatus status) {
        this.status = status.name();
    }
}