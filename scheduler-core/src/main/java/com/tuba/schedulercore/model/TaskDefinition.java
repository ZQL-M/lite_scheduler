package com.tuba.schedulercore.model;

import com.tuba.schedulercore.plugin.TaskPlugin;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 任务定义，包含任务的元数据和执行信息
 */
public class TaskDefinition {
    private String id;
    private String name;
    private String group = "default";
    private String cron;
    private long fixedRate = -1; // 固定频率，单位毫秒
    private long fixedDelay = -1; // 固定延迟，单位毫秒
    private boolean async = true;
    private Object bean;
    private Method method;
    private String beanName; // Spring Bean名称（用于数据库恢复）
    private String methodName; // 方法名（用于数据库恢复）
    private String description;
    private boolean enabled = true; // 任务是否启用
    private boolean persistent = false; // 是否持久化
    private int repeatCount = -1; // 循环次数，-1表示无限循环
    private Instant lastFireTime; // 上次触发时间
    private Instant nextFireTime; // 下次触发时间
    private List<TaskPlugin> plugins;

    // constructors, getters, setters

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
    }

    // getters / setters

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

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
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

    public List<TaskPlugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<TaskPlugin> plugins) {
        this.plugins = plugins;
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

    public long getFixedRate() {
        return fixedRate;
    }

    public void setFixedRate(long fixedRate) {
        this.fixedRate = fixedRate;
    }

    public long getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(long fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public Instant getLastFireTime() {
        return lastFireTime;
    }

    public void setLastFireTime(Instant lastFireTime) {
        this.lastFireTime = lastFireTime;
    }

    public Instant getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(Instant nextFireTime) {
        this.nextFireTime = nextFireTime;
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