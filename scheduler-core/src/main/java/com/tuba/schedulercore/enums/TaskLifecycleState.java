package com.tuba.schedulercore.enums;

/**
 * 任务生命周期状态枚举
 * <p>
 * 对应数据库表：task_status.state
 * 用途：记录任务的实时生命周期状态
 * </p>
 *
 */
public enum TaskLifecycleState {
    
    /**
     * 等待中 - 任务已注册，等待执行
     */
    WAITING("WAITING", "等待中"),
    
    /**
     * 执行中 - 任务正在执行（调度器正在工作）
     */
    RUNNING("RUNNING", "执行中"),
    
    /**
     * 已暂停 - 任务被暂停，不会触发执行
     */
    PAUSED("PAUSED", "已暂停"),
    
    /**
     * 已完成 - 任务已执行完成，不会再执行
     */
    COMPLETED("COMPLETED", "已完成"),
    
    /**
     * 错误状态 - 任务配置错误或系统异常
     */
    ERROR("ERROR", "错误");
    
    private final String code;
    private final String description;
    
    TaskLifecycleState(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 获取状态码
     * @return 状态码
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 获取描述
     * @return 描述信息
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据状态码获取枚举值
     * @param code 状态码
     * @return TaskLifecycleState 枚举值
     * @throws IllegalArgumentException 如果状态码无效
     */
    public static TaskLifecycleState fromCode(String code) {
        for (TaskLifecycleState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown task lifecycle state: " + code);
    }
    
    /**
     * 根据状态码获取枚举值（安全版本）
     * @param code 状态码
     * @return TaskLifecycleState 枚举值，如果无效返回 null
     */
    public static TaskLifecycleState fromCodeSafe(String code) {
        if (code == null) {
            return null;
        }
        for (TaskLifecycleState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为活动状态（可以执行）
     * @return true 如果是活动状态
     */
    public boolean isActive() {
        return this == WAITING || this == RUNNING;
    }
    
    /**
     * 判断是否为执行中状态
     * @return true 如果是执行中
     */
    public boolean isRunning() {
        return this == RUNNING;
    }
    
    /**
     * 判断是否为暂停状态
     * @return true 如果是暂停状态
     */
    public boolean isPaused() {
        return this == PAUSED;
    }
    
    /**
     * 判断是否为完成状态
     * @return true 如果是完成状态
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }
    
    /**
     * 判断是否为错误状态
     * @return true 如果是错误状态
     */
    public boolean isError() {
        return this == ERROR;
    }
}
