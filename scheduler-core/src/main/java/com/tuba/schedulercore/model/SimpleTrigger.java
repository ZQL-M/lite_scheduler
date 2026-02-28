package com.tuba.schedulercore.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简单触发器，存储固定频率和固定延迟类型的触发器配置
 */
@Data
@NoArgsConstructor
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

    private boolean deleted = false; // 是否软删除

    // constructors
    public SimpleTrigger(String triggerId, int repeatCount, long repeatInterval, String simpleType) {
        this.triggerId = triggerId;
        this.repeatCount = repeatCount;
        this.repeatInterval = repeatInterval;
        this.simpleType = simpleType;
        this.misfireInstruction = 0;
    }
}