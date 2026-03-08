package com.tuba.schedulercore.enums;

/**
 * 任务执行状态枚举
 * <p>
 * 对应数据库表：task_execution_log.status
 * 用途：记录单次任务执行的瞬时状态
 * </p>
 * 
 */
public enum ExecutionStatus {
    // todo 数据库修改对应枚举

    /**
     * 执行中 - 任务正在执行
     */
    RUNNING("RUNNING", "执行中"),

    /**
     * 成功 - 任务执行完成
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 失败 - 任务执行过程中发生异常
     */
    FAILURE("FAILURE", "失败");

    private final String code;
    private final String description;

    ExecutionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取状态码
     * 
     * @return 状态码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取描述
     * 
     * @return 描述信息
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据状态码获取枚举值
     * 
     * @param code 状态码
     * @return ExecutionStatus 枚举值
     * @throws IllegalArgumentException 如果状态码无效
     */
    public static ExecutionStatus fromCode(String code) {
        for (ExecutionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown execution status: " + code);
    }

    /**
     * 根据状态码获取枚举值（安全版本）
     * 
     * @param code 状态码
     * @return ExecutionStatus 枚举值，如果无效返回 null
     */
    public static ExecutionStatus fromCodeSafe(String code) {
        if (code == null) {
            return null;
        }
        for (ExecutionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为执行中状态
     * 
     * @return true 如果是执行中
     */
    public boolean isRunning() {
        return this == RUNNING;
    }

    /**
     * 判断是否为完成状态（成功或失败）
     * 
     * @return true 如果是完成状态
     */
    public boolean isFinished() {
        return this == SUCCESS || this == FAILURE;
    }

    /**
     * 判断是否为成功状态
     * 
     * @return true 如果是成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 判断是否为失败状态
     * 
     * @return true 如果是失败状态
     */
    public boolean isFailure() {
        return this == FAILURE;
    }
}
