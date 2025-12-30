package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Objects;

/**
 * 简单触发器，存储固定频率和固定延迟类型的触发器配置
 */
@TableName("task_simple_trigger")
public class SimpleTrigger {
    @TableField("trigger_id")
    private String triggerId; // 关联的触发器ID

    @TableField("repeat_count")
    private int repeatCount = -1; // 重复次数（-1 表示无限循环）

    @TableField("repeat_interval")
    private long repeatInterval = 0; // 重复间隔（毫秒）

    @TableField("simple_type")
    private String simpleType; // 简单触发器类型：FIXED_RATE/FIXED_DELAY

    @TableField("misfire_instruction")
    private int misfireInstruction = 0; // 错过执行策略：0-忽略，1-立即执行，2-下次执行

    // constructors

    public SimpleTrigger() {
    }

    public SimpleTrigger(String triggerId, int repeatCount, long repeatInterval, String simpleType) {
        this.triggerId = triggerId;
        this.repeatCount = repeatCount;
        this.repeatInterval = repeatInterval;
        this.simpleType = simpleType;
        this.misfireInstruction = 0;
    }

    // Getters and Setters
    public String getTriggerId() {
        return triggerId;
    }

    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public long getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(long repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public String getSimpleType() {
        return simpleType;
    }

    public void setSimpleType(String simpleType) {
        this.simpleType = simpleType;
    }

    public int getMisfireInstruction() {
        return misfireInstruction;
    }

    public void setMisfireInstruction(int misfireInstruction) {
        this.misfireInstruction = misfireInstruction;
    }

    /**
     * 基于 triggerId 的 equals 方法
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SimpleTrigger that = (SimpleTrigger) o;
        return Objects.equals(triggerId, that.triggerId);
    }

    /**
     * 基于 triggerId 的 hashCode 方法
     */
    @Override
    public int hashCode() {
        return Objects.hash(triggerId);
    }
}