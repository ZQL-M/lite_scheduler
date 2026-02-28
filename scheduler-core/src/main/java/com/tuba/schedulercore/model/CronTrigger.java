package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Objects;

import lombok.Data;

/**
 * Cron触发器，存储Cron表达式类型的触发器配置
 */
@Data
@TableName("task_cron_trigger")
public class CronTrigger {
    @TableField("trigger_id")
    private String triggerId; // 关联的触发器ID

    @TableField("cron_expression")
    private String cronExpression; // Cron表达式

    @TableField("time_zone_id")
    private String timeZoneId = "Asia/Shanghai"; // 时区

    @TableField("misfire_instruction")
    private int misfireInstruction = 0; // 错过执行策略：0-忽略，1-立即执行，2-下次执行

    // constructors

    public CronTrigger() {
    }

    public CronTrigger(String triggerId, String cronExpression) {
        this.triggerId = triggerId;
        this.cronExpression = cronExpression;
        this.timeZoneId = "Asia/Shanghai";
        this.misfireInstruction = 0;
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
        CronTrigger that = (CronTrigger) o;
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