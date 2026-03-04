package com.tuba.schedulercore.core.trigger.impl;

import com.tuba.schedulercore.core.trigger.Trigger;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 简单触发器实现
 */
public class SimpleTriggerImpl implements Trigger {
    private String id;
    private long interval;
    private int repeatCount;
    private int repeatCountDown;
    private String simpleType; // FIXED_RATE 或 FIXED_DELAY
    private int priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime nextFireTime;
    private LocalDateTime previousFireTime;
    
    /**
     * 构造方法
     * @param id 触发器ID
     * @param interval 间隔时间（毫秒）
     * @param simpleType 触发器类型：FIXED_RATE 或 FIXED_DELAY
     */
    public SimpleTriggerImpl(String id, long interval, String simpleType) {
        this.id = id;
        this.interval = interval;
        this.simpleType = simpleType;
        this.repeatCount = -1; // 默认无限循环
        this.repeatCountDown = repeatCount;
        this.priority = 5; // 默认优先级
    }
    
    /**
     * 构造方法
     * @param id 触发器ID
     * @param interval 间隔时间（毫秒）
     * @param repeatCount 重复次数
     * @param simpleType 触发器类型：FIXED_RATE 或 FIXED_DELAY
     */
    public SimpleTriggerImpl(String id, long interval, int repeatCount, String simpleType) {
        this(id, interval, simpleType);
        this.repeatCount = repeatCount;
        this.repeatCountDown = repeatCount;
    }
    
    /**
     * 构造方法
     * @param id 触发器ID
     * @param interval 间隔时间（毫秒）
     * @param repeatCount 重复次数
     * @param simpleType 触发器类型：FIXED_RATE 或 FIXED_DELAY
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    public SimpleTriggerImpl(String id, long interval, int repeatCount, String simpleType, 
                           LocalDateTime startTime, LocalDateTime endTime) {
        this(id, interval, repeatCount, simpleType);
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getTriggerType() {
        return simpleType;
    }
    
    @Override
    public LocalDateTime getNextFireTime() {
        if (nextFireTime == null) {
            calculateNextFireTime();
        }
        return nextFireTime;
    }
    
    @Override
    public LocalDateTime getPreviousFireTime() {
        return previousFireTime;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public boolean hasMoreFires() {
        return getNextFireTime() != null;
    }
    
    @Override
    public void updateAfterFire() {
        previousFireTime = nextFireTime;
        if (repeatCount != -1 && repeatCountDown > 0) {
            repeatCountDown--;
        }
        nextFireTime = null; // 重置下次触发时间，下次调用getNextFireTime时会重新计算
    }
    
    @Override
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && now.isBefore(startTime)) {
            return false;
        }
        if (endTime != null && now.isAfter(endTime)) {
            return false;
        }
        return true;
    }
    
    /**
     * 计算下次触发时间
     */
    private void calculateNextFireTime() {
        LocalDateTime now = LocalDateTime.now();
        if (endTime != null && now.isAfter(endTime)) {
            nextFireTime = null;
            return;
        }
        
        if (repeatCount != -1 && repeatCountDown <= 0) {
            nextFireTime = null;
            return;
        }
        
        LocalDateTime start = startTime != null && now.isBefore(startTime) ? startTime : now;
        nextFireTime = start.plus(interval, ChronoUnit.MILLIS);
        
        if (endTime != null && nextFireTime.isAfter(endTime)) {
            nextFireTime = null;
        }
    }
    
    // getter和setter方法
    public long getInterval() {
        return interval;
    }
    
    public void setInterval(long interval) {
        this.interval = interval;
    }
    
    public int getRepeatCount() {
        return repeatCount;
    }
    
    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
        this.repeatCountDown = repeatCount;
    }
    
    public String getSimpleType() {
        return simpleType;
    }
    
    public void setSimpleType(String simpleType) {
        this.simpleType = simpleType;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
