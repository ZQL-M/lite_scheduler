package com.tuba.schedulercore.config;

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
        this.schedulerPoolSize = schedulerPoolSize > 0 ? schedulerPoolSize : Math.max(2, Runtime.getRuntime().availableProcessors());
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix != null ? threadNamePrefix : "lite-scheduler-";
    }
}

