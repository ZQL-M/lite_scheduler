package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tuba.schedulercore.plugin.TaskPlugin;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 任务定义，包含任务的基本信息和执行目标
 */
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

    @TableField("bean_name")
    private String beanName; // Spring Bean名称（用于数据库恢复）

    @TableField("method_name")
    private String methodName; // 方法名（用于数据库恢复）

    private String description;

    private boolean enabled = true; // 任务是否启用

    private boolean persistent = false; // 是否持久化

    @TableField("created_time")
    private LocalDateTime createdTime; // 创建时间

    @TableField("updated_time")
    private LocalDateTime updatedTime; // 更新时间

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

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public Object getBean() {
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public List<TaskPlugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<TaskPlugin> plugins) {
        this.plugins = plugins;
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