package com.tuba.schedulercore.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 执行器配置属性
 */
@ConfigurationProperties(prefix = "lite-scheduler.executor")
public class ExecutorProperties {

    /**
     * 执行器线程池大小，默认为 CPU 核心数
     */
    private int poolSize = Runtime.getRuntime().availableProcessors();

    /**
     * 调度器线程池大小，默认为 max(2, CPU核心数)
     */
    private int schedulerPoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());

    /**
     * 线程名称前缀
     */
    private String threadNamePrefix = "lite-scheduler-";

    /**
     * 任务执行超时时间（毫秒），默认60000ms
     */
    private long taskTimeout = 60000;

    /**
     * 执行器关闭超时时间（秒），默认30秒
     */
    private int shutdownTimeoutSeconds = 30;

    /**
     * 执行器强制关闭超时时间（秒），默认10秒
     */
    private int forceShutdownTimeoutSeconds = 10;

    /**
     * 任务默认重试次数，默认0次
     */
    private int defaultRetryCount = 0;

    /**
     * 任务默认重试间隔（毫秒），默认1000ms
     */
    private long defaultRetryInterval = 1000;

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize > 0 ? poolSize : Runtime.getRuntime().availableProcessors();
    }

    public int getSchedulerPoolSize() {
        return schedulerPoolSize;
    }

    public void setSchedulerPoolSize(int schedulerPoolSize) {
        this.schedulerPoolSize = schedulerPoolSize > 0 ? schedulerPoolSize
                : Math.max(2, Runtime.getRuntime().availableProcessors());
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix != null ? threadNamePrefix : "lite-scheduler-";
    }

    public long getTaskTimeout() {
        return taskTimeout;
    }

    public void setTaskTimeout(long taskTimeout) {
        this.taskTimeout = taskTimeout > 0 ? taskTimeout : 60000;
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    public void setShutdownTimeoutSeconds(int shutdownTimeoutSeconds) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds > 0 ? shutdownTimeoutSeconds : 30;
    }

    public int getForceShutdownTimeoutSeconds() {
        return forceShutdownTimeoutSeconds;
    }

    public void setForceShutdownTimeoutSeconds(int forceShutdownTimeoutSeconds) {
        this.forceShutdownTimeoutSeconds = forceShutdownTimeoutSeconds > 0 ? forceShutdownTimeoutSeconds : 10;
    }

    public int getDefaultRetryCount() {
        return defaultRetryCount;
    }

    public void setDefaultRetryCount(int defaultRetryCount) {
        this.defaultRetryCount = defaultRetryCount >= 0 ? defaultRetryCount : 0;
    }

    public long getDefaultRetryInterval() {
        return defaultRetryInterval;
    }

    public void setDefaultRetryInterval(long defaultRetryInterval) {
        this.defaultRetryInterval = defaultRetryInterval > 0 ? defaultRetryInterval : 1000;
    }
}
