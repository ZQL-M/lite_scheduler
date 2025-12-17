package com.tuba.schedulercore.enums;

/**
 * 任务状态枚举
 */
public enum  TaskStatus {
    /**
     * 未执行，任务已创建但从未被扫描器处理过
     */
    NOT_EXECUTED,
    
    /**
     * 待执行，任务被扫描器扫描到并已添加到执行线程中
     */
    PENDING,
    
    /**
     * 已完成，任务执行完成且无剩余执行次数
     */
    COMPLETED,
    
    /**
     * 已过期，任务超过预期执行时间且未执行
     */
    EXPIRED,
    
    /**
     * 执行失败，任务执行过程中发生异常
     */
    FAILED
}
