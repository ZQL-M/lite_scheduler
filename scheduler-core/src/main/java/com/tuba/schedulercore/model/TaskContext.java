package com.tuba.schedulercore.model;

import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务执行上下文，包含任务执行过程中的所有信息
 */
@Data
public class TaskContext {
    private final TaskDefinition definition;
    private Instant startTime;
    private Instant endTime;
    private Map<String, Object> attributes = new HashMap<>();
    private Throwable error;

    public TaskContext(TaskDefinition definition) {
        this.definition = definition;
    }

    /**
     * 设置属性值
     * 
     * @param key   属性键
     * @param value 属性值
     */
    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取属性值
     * 
     * @param key 属性键
     * @return 属性值，如果不存在返回 null
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * 获取属性值（带类型转换）
     * 
     * @param key   属性键
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 属性值，如果不存在或类型不匹配返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> clazz) {
        Object value = attributes.get(key);
        if (value != null && clazz.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }

}