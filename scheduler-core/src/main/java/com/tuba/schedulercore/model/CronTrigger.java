package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Cron触发器，存储Cron表达式类型的触发器配置
 */
@Data
@EqualsAndHashCode(of = "triggerId")
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
}