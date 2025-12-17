package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tuba.schedulercore.plugin.TaskPlugin;
import com.tuba.schedulercore.model.TaskStatus;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import lombok.Data;

/**
 * 任务定义，包含任务的元数据和执行信息
 */
@Data
@TableName("task_definition")
public class TaskDefinition {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    @TableField("group_name")
    private String group = "default";

    private String cron;

    @TableField("fixed_rate")
    private long fixedRate = -1; // 固定频率，单位毫秒

    @TableField("fixed_delay")
    private long fixedDelay = -1; // 固定延迟，单位毫秒

    private boolean async = true;

    @TableField(exist = false)
    private Object bean;

    @TableField(exist = false)
    private Method method;

    @TableField("bean_name")
    private String beanName; // Spring Bean名称（用于数据库恢复）

    @TableField("method_name")
    private String methodName; // 方法名（用于数据库恢复）

    private String description;

    private boolean enabled = true; // 任务是否启用

    private boolean persistent = false; // 是否持久化

    @TableField("repeat_count")
    private int repeatCount = -1; // 循环次数，-1表示无限循环

    @TableField("last_fire_time")
    private LocalDateTime lastFireTime; // 上次触发时间

    @TableField("next_fire_time")
    private LocalDateTime nextFireTime; // 下次触发时间

    @TableField("status")
    private TaskStatus status = TaskStatus.NOT_EXECUTED; // 任务状态

    @TableField(exist = false)
    private List<TaskPlugin> plugins;

    // constructors

    public TaskDefinition() {
    }

    public TaskDefinition(String id, String name, String group, String cron, boolean async, Object bean, Method method,
            String description) {
        this.id = id;
        this.name = name;
        this.group = group;
        this.cron = cron;
        this.async = async;
        this.bean = bean;
        this.method = method;
        this.description = description;
        this.enabled = true;
        this.status = TaskStatus.NOT_EXECUTED;
    }

    /**
     * 基于 id 的 equals 方法
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TaskDefinition that = (TaskDefinition) o;
        return Objects.equals(id, that.id);
    }

    /**
     * 基于 id 的 hashCode 方法
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}