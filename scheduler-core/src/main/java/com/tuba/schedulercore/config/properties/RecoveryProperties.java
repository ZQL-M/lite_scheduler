package com.tuba.schedulercore.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务恢复配置属性
 */
@ConfigurationProperties(prefix = "lite-scheduler.recovery")
public class RecoveryProperties {
    
    /**
     * 任务恢复模式：FAILURE 或 COMPENSATION
     */
    private RecoveryMode mode = RecoveryMode.FAILURE;
    
    public RecoveryMode getMode() {
        return mode;
    }
    
    public void setMode(RecoveryMode mode) {
        this.mode = mode;
    }
    
    /**
     * 任务恢复模式枚举
     */
    public enum RecoveryMode {
        /** 将过期任务状态改为失败 */
        FAILURE,
        /** 对过期任务进行补偿执行 */
        COMPENSATION
    }
}
