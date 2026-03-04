package com.tuba.schedulercore.core.trigger.impl;

import com.tuba.schedulercore.core.trigger.Trigger;
import org.springframework.scheduling.support.CronSequenceGenerator;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Cron 触发器实现
 */
public class CronTriggerImpl implements Trigger {
    private String id;
    private String cronExpression;
    private int priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private CronSequenceGenerator cronGenerator;
    private LocalDateTime nextFireTime;
    private LocalDateTime previousFireTime;

    /**
     * 构造方法
     * 
     * @param id             触发器ID
     * @param cronExpression Cron表达式
     */
    public CronTriggerImpl(String id, String cronExpression) {
        this.id = id;
        this.cronExpression = cronExpression;
        this.priority = 5; // 默认优先级
        this.cronGenerator = new CronSequenceGenerator(cronExpression);
    }

    /**
     * 构造方法
     * 
     * @param id             触发器ID
     * @param cronExpression Cron表达式
     * @param startTime      开始时间
     * @param endTime        结束时间
     */
    public CronTriggerImpl(String id, String cronExpression, LocalDateTime startTime, LocalDateTime endTime) {
        this(id, cronExpression);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTriggerType() {
        return "CRON";
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

        LocalDateTime start = startTime != null && now.isBefore(startTime) ? startTime : now;
        Date startDate = Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
        Date nextDate = cronGenerator.next(startDate);

        if (nextDate != null) {
            nextFireTime = LocalDateTime.ofInstant(nextDate.toInstant(), ZoneId.systemDefault());
            if (endTime != null && nextFireTime.isAfter(endTime)) {
                nextFireTime = null;
            }
        }
    }

    // getter和setter方法
    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
        this.cronGenerator = new CronSequenceGenerator(cronExpression);
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
