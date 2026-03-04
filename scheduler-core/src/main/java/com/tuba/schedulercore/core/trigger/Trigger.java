package com.tuba.schedulercore.core.trigger;

import java.time.LocalDateTime;

/**
 * 触发器接口，定义触发器的核心方法
 */
public interface Trigger {
    /**
     * 获取触发器ID
     */
    String getId();
    
    /**
     * 获取触发器类型
     */
    String getTriggerType();
    
    /**
     * 获取下次触发时间
     */
    LocalDateTime getNextFireTime();
    
    /**
     * 获取上次触发时间
     */
    LocalDateTime getPreviousFireTime();
    
    /**
     * 获取触发器优先级
     */
    int getPriority();
    
    /**
     * 检查是否还有更多触发时间
     */
    boolean hasMoreFires();
    
    /**
     * 触发后更新触发器状态
     */
    void updateAfterFire();
    
    /**
     * 检查触发器是否在有效期内
     */
    boolean isValid();
}
